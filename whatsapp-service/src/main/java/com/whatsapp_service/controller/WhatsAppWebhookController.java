package com.whatsapp_service.controller;

import com.whatsapp_service.client.WuzApiClient;
import com.whatsapp_service.dto.MensagemFilaDTO;
import com.whatsapp_service.dto.WuzapiWebhookPayload;
import com.whatsapp_service.producer.WhatsAppQueueProducer;
import com.whatsapp_service.service.ConversationStateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.ObjectMapper;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/v1/webhook")
@RequiredArgsConstructor
public class WhatsAppWebhookController {

    private static final Set<String> MENU_COMMANDS = Set.of(
            "oi", "ola", "menu", "inicio", "comecar"
    );

    private final WhatsAppQueueProducer producer;
    private final ObjectMapper objectMapper;
    private final WuzApiClient wuzApiClient;
    private final ConversationStateService conversationStateService;

    @PostMapping("/wuzapi")
    public ResponseEntity<Void> receberMensagem(@RequestBody WuzapiWebhookPayload payload) {
        try {
            if (payload.type() == null || !payload.type().equalsIgnoreCase("Message")) {
                return ResponseEntity.ok().build();
            }

            if (payload.event() == null || !payload.event().isObject()) {
                log.debug("Evento do WuzAPI ignorado por não possuir payload de mensagem");
                return ResponseEntity.ok().build();
            }

            WuzapiWebhookPayload.Event event = objectMapper.treeToValue(
                    payload.event(),
                    WuzapiWebhookPayload.Event.class
            );

            if (event.info() == null) {
                log.warn("Webhook recebido sem informações do remetente");
                return ResponseEntity.ok().build();
            }

            String telefone = event.info().getTelefone();
            String messageId = event.info().id();

            if (messageId == null || messageId.isBlank()) {
                log.warn("Mensagem ignorada sem identificador do WuzAPI");
                return ResponseEntity.ok().build();
            }

            if (telefone == null) {
                log.debug("Mensagem ignorada sem telefone válido");
                return ResponseEntity.ok().build();
            }

            if (event.message() == null) {
                return ResponseEntity.ok().build();
            }

            String texto = event.message().getTexto();

            if (texto == null || texto.isBlank()) {
                return ResponseEntity.ok().build();
            }

            String comando = normalizar(texto);

            if (MENU_COMMANDS.contains(comando)) {
                wuzApiClient.enviarMenuPrincipal(telefone);
                return ResponseEntity.ok().build();
            }

            if ("registrar gasto".equals(comando) || "registrar_gasto".equals(comando)) {
                conversationStateService.aguardarRegistroDeGasto(telefone);
                wuzApiClient.enviarMensagem(
                        telefone,
                        "Envie agora a descrição do gasto. Exemplo: Gastei 50 reais no futebol."
                );
                return ResponseEntity.ok().build();
            }

            if ("ver relatorio".equals(comando)
                    || "ver_relatorio".equals(comando)
                    || "mais opcoes".equals(comando)
                    || "mais_opcoes".equals(comando)) {
                wuzApiClient.enviarMensagem(
                        telefone,
                        "Essa opção estará disponível em breve. Digite menu para voltar."
                );
                return ResponseEntity.ok().build();
            }

            conversationStateService.consumirSeAguardandoGasto(telefone);

            producer.enviarParaProcessamento(new MensagemFilaDTO(
                    messageId,
                    telefone,
                    "TEXTO",
                    texto
            ));

            log.info("Mensagem enviada para fila. Telefone: {}", telefone);
        } catch (Exception e) {
            log.error("Erro ao processar webhook WuzAPI", e);
        }

        return ResponseEntity.ok().build();
    }

    private String normalizar(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .trim()
                .toLowerCase(Locale.ROOT);
    }
}

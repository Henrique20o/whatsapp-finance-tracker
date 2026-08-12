package com.whatsapp_service.controller;

import com.whatsapp_service.dto.WuzapiWebhookPayload;
import com.whatsapp_service.flow.WhatsAppFlowRouter;
import com.whatsapp_service.security.WebhookSignatureVerifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@RestController
@RequestMapping("/v1/webhook")
@RequiredArgsConstructor
public class WhatsAppWebhookController {

    private final ObjectMapper objectMapper;
    private final WhatsAppFlowRouter flowRouter;
    private final WebhookSignatureVerifier signatureVerifier;

    @PostMapping("/wuzapi")
    public ResponseEntity<Void> receberMensagem(
            @RequestHeader(value = "x-hmac-signature", required = false) String signature,
            @RequestBody byte[] rawBody
    ) {
        if (!signatureVerifier.isValid(rawBody, signature)) {
            log.warn("Webhook WuzAPI rejeitado por assinatura ausente ou inválida");
            return ResponseEntity.status(401).build();
        }

        try {
            WuzapiWebhookPayload payload = objectMapper.readValue(rawBody, WuzapiWebhookPayload.class);
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

            if (telefone == null || event.message() == null) {
                return ResponseEntity.ok().build();
            }

            String texto = event.message().getTexto();

            if (texto == null || texto.isBlank()) {
                return ResponseEntity.ok().build();
            }

            flowRouter.processar(messageId, telefone, texto);
        } catch (Exception e) {
            log.error("Erro ao processar webhook WuzAPI", e);
        }

        return ResponseEntity.ok().build();
    }
}

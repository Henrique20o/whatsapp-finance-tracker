package com.whatsapp_service.controller;

import com.whatsapp_service.client.WuzApiClient;
import com.whatsapp_service.client.FinancialReportClient;
import com.whatsapp_service.dto.MensagemFilaDTO;
import com.whatsapp_service.dto.WuzapiWebhookPayload;
import com.whatsapp_service.producer.WhatsAppQueueProducer;
import com.whatsapp_service.service.ConversationStateService;
import com.whatsapp_service.service.PieChartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.ObjectMapper;

import java.text.Normalizer;
import java.text.NumberFormat;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.List;
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
    private final FinancialReportClient financialReportClient;
    private final PieChartService pieChartService;

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

            if ("ver relatorio".equals(comando) || "ver_relatorio".equals(comando)) {
                var resumo = financialReportClient.buscarResumo(telefone);
                NumberFormat moeda = NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));
                List<com.whatsapp_service.dto.GastoPorCategoriaDTO> gastos =
                        resumo.gastosPorCategoria() == null ? List.of() : resumo.gastosPorCategoria();

                wuzApiClient.enviarMensagem(
                        telefone,
                        "📊 *Resumo financeiro*\n\n"
                                + "Últimos 7 dias: " + moeda.format(resumo.totalSeteDias()) + "\n"
                                + "Últimos 30 dias: " + moeda.format(resumo.totalTrintaDias())
                                + formatarCategorias(gastos, resumo.totalTrintaDias(), moeda)
                );

                if (!gastos.isEmpty() && resumo.totalTrintaDias().signum() > 0) {
                    wuzApiClient.enviarImagem(
                            telefone,
                            "Gastos por categoria nos últimos 30 dias",
                            pieChartService.gerarGraficoBase64(gastos)
                    );
                }
                return ResponseEntity.ok().build();
            }

            if ("mais opcoes".equals(comando) || "mais_opcoes".equals(comando)) {
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

    private String formatarCategorias(
            List<com.whatsapp_service.dto.GastoPorCategoriaDTO> gastos,
            BigDecimal total,
            NumberFormat moeda
    ) {
        if (gastos.isEmpty() || total == null || total.signum() <= 0) {
            return "\n\nNenhum gasto registrado nos últimos 30 dias.";
        }

        StringBuilder texto = new StringBuilder("\n\n*Por categoria:*\n");

        for (var gasto : gastos) {
            BigDecimal percentual = gasto.total()
                    .multiply(BigDecimal.valueOf(100))
                    .divide(total, 1, RoundingMode.HALF_UP);

            texto.append("• ")
                    .append(gasto.categoria())
                    .append(": ")
                    .append(moeda.format(gasto.total()))
                    .append(" — ")
                    .append(percentual.toPlainString())
                    .append("%\n");
        }

        return texto.toString().stripTrailing();
    }
}

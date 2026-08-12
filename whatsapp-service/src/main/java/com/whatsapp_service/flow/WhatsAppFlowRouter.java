package com.whatsapp_service.flow;

import com.whatsapp_service.client.FinancialReportClient;
import com.whatsapp_service.client.WuzApiClient;
import com.whatsapp_service.dto.GastoPorCategoriaDTO;
import com.whatsapp_service.dto.MensagemFilaDTO;
import com.whatsapp_service.producer.WhatsAppQueueProducer;
import com.whatsapp_service.service.ConversationStateService;
import com.whatsapp_service.service.PieChartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class WhatsAppFlowRouter {

    private final WhatsAppActionResolver actionResolver;
    private final WhatsAppQueueProducer producer;
    private final WuzApiClient wuzApiClient;
    private final ConversationStateService conversationStateService;
    private final FinancialReportClient financialReportClient;
    private final PieChartService pieChartService;

    public void processar(String messageId, String telefone, String texto) {
        ResolvedWhatsAppAction resolved = actionResolver.resolver(texto);

        if (resolved.action() == WhatsAppAction.TEXTO_LIVRE
                && conversationStateService.consumirSeAguardandoNomeDaCategoria(telefone)) {
            criarCategoria(telefone, texto);
            return;
        }

        switch (resolved.action()) {
            case ABRIR_MENU -> wuzApiClient.enviarMenuPrincipal(telefone);
            case REGISTRAR_GASTO -> iniciarRegistroDeGasto(telefone);
            case VER_RELATORIO -> enviarRelatorio(telefone);
            case MAIS_OPCOES -> wuzApiClient.enviarMenuMaisOpcoes(telefone);
            case GERENCIAR_CATEGORIAS -> wuzApiClient.enviarMenuCategorias(telefone);
            case LISTAR_CATEGORIAS -> enviarCategorias(telefone);
            case CRIAR_CATEGORIA -> solicitarNomeDaCategoria(telefone);
            case AJUDA -> enviarAjuda(telefone);
            case VOLTAR_MENU -> wuzApiClient.enviarMenuPrincipal(telefone);
            case CANCELAR_TRANSACAO -> cancelarTransacao(telefone, resolved.transacaoId());
            case TEXTO_LIVRE -> encaminharTextoParaIa(messageId, telefone, texto);
        }
    }

    private void solicitarNomeDaCategoria(String telefone) {
        conversationStateService.aguardarNomeDaCategoria(telefone);
        wuzApiClient.enviarMensagem(
                telefone,
                "Digite o nome da nova categoria. Exemplo: *Viagens*."
        );
    }

    private void criarCategoria(String telefone, String nome) {
        try {
            String categoria = financialReportClient.criarCategoria(telefone, nome);
            wuzApiClient.enviarMensagem(
                    telefone,
                    "✅ Categoria *" + categoria + "* criada com sucesso. Digite *menu* para voltar."
            );
        } catch (org.springframework.web.client.RestClientResponseException exception) {
            log.warn("Categoria inválida para {}: status={}", telefone, exception.getStatusCode());
            wuzApiClient.enviarMensagem(
                    telefone,
                    "Não foi possível criar a categoria. Use um nome entre 2 e 50 caracteres."
            );
        }
    }

    private void iniciarRegistroDeGasto(String telefone) {
        conversationStateService.aguardarRegistroDeGasto(telefone);
        wuzApiClient.enviarMensagem(
                telefone,
                "Envie agora a descrição do gasto. Exemplo: Gastei 50 reais no futebol."
        );
    }

    private void cancelarTransacao(String telefone, Long transacaoId) {
        var cancelamento = financialReportClient.cancelarTransacao(transacaoId, telefone);
        NumberFormat moeda = criarFormatadorDeMoeda();

        wuzApiClient.enviarMensagem(
                telefone,
                cancelamento.canceladaAgora()
                        ? "↩️ Gasto de " + moeda.format(cancelamento.valor()) + " cancelado."
                        : "ℹ️ Esse gasto já estava cancelado."
        );
    }

    private void enviarRelatorio(String telefone) {
        var resumo = financialReportClient.buscarResumo(telefone);
        NumberFormat moeda = criarFormatadorDeMoeda();
        List<GastoPorCategoriaDTO> gastos = resumo.gastosPorCategoria() == null
                ? List.of()
                : resumo.gastosPorCategoria();

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
    }

    private void enviarCategorias(String telefone) {
        List<String> categorias = financialReportClient.buscarCategorias(telefone);
        String lista = categorias == null || categorias.isEmpty()
                ? "Nenhuma categoria ativa foi encontrada."
                : categorias.stream()
                        .map(categoria -> "• " + categoria)
                        .collect(java.util.stream.Collectors.joining("\n"));

        wuzApiClient.enviarMensagem(
                telefone,
                "📂 *Suas categorias ativas*\n\n" + lista
                        + "\n\nDigite *menu* para voltar."
        );
    }

    private void enviarAjuda(String telefone) {
        wuzApiClient.enviarMensagem(
                telefone,
                "❓ *Como usar*\n\n"
                        + "• Use *Registrar gasto* e descreva o que comprou e o valor.\n"
                        + "• Use *Ver relatório* para consultar totais e gastos por categoria.\n"
                        + "• Após registrar um gasto, use o botão *Cancelar* para desfazê-lo.\n"
                        + "• Digite *menu* a qualquer momento para abrir o menu principal."
        );
    }

    private void encaminharTextoParaIa(String messageId, String telefone, String texto) {
        conversationStateService.consumirSeAguardandoGasto(telefone);
        producer.enviarParaProcessamento(new MensagemFilaDTO(
                messageId,
                telefone,
                "TEXTO",
                texto
        ));
        log.info("Mensagem enviada para fila. Telefone: {}", telefone);
    }

    private NumberFormat criarFormatadorDeMoeda() {
        return NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));
    }

    private String formatarCategorias(
            List<GastoPorCategoriaDTO> gastos,
            BigDecimal total,
            NumberFormat moeda
    ) {
        if (gastos.isEmpty() || total == null || total.signum() <= 0) {
            return "\n\nNenhum gasto registrado nos últimos 30 dias.";
        }

        StringBuilder texto = new StringBuilder("\n\n*Por categoria:*\n");

        for (GastoPorCategoriaDTO gasto : gastos) {
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

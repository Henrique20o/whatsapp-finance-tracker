package com.whatsapp_service.flow;

import com.whatsapp_service.client.FinancialReportClient;
import com.whatsapp_service.client.WuzApiClient;
import com.whatsapp_service.dto.GastoPorCategoriaDTO;
import com.whatsapp_service.dto.MensagemFilaDTO;
import com.whatsapp_service.dto.TransacaoRelatorioDTO;
import com.whatsapp_service.service.ConversationStateService.ReportCategoryOption;
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
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class WhatsAppFlowRouter {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Sao_Paulo");

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

        if (resolved.action() == WhatsAppAction.TEXTO_LIVRE
                && conversationStateService.estaAguardandoCategoriaParaDesativar(telefone)) {
            prepararDesativacaoDeCategoria(telefone, texto);
            return;
        }

        switch (resolved.action()) {
            case ABRIR_MENU -> abrirMenuPrincipal(telefone);
            case REGISTRAR_GASTO -> iniciarRegistroDeGasto(telefone);
            case VER_RELATORIO -> wuzApiClient.enviarMenuPeriodoRelatorio(
                    telefone, YearMonth.now(BUSINESS_ZONE));
            case RELATORIO_SETE_DIAS -> enviarRelatorioDosUltimosDias(telefone, 7);
            case RELATORIO_TRINTA_DIAS -> enviarRelatorioDosUltimosDias(telefone, 30);
            case RELATORIO_MENSAL -> enviarRelatorioMensal(telefone, resolved.parametro());
            case EXPLORAR_CATEGORIAS_RELATORIO -> explorarCategoriasDoRelatorio(telefone);
            case DETALHAR_CATEGORIA_RELATORIO -> detalharCategoriaDoRelatorio(
                    telefone, resolved.parametro());
            case VOLTAR_RESUMO_RELATORIO -> voltarAoResumoDoRelatorio(telefone);
            case MAIS_OPCOES -> wuzApiClient.enviarMenuMaisOpcoes(telefone);
            case GERENCIAR_CATEGORIAS -> wuzApiClient.enviarMenuCategorias(telefone);
            case LISTAR_CATEGORIAS -> enviarCategorias(telefone);
            case CRIAR_CATEGORIA -> solicitarNomeDaCategoria(telefone);
            case DESATIVAR_CATEGORIA -> solicitarCategoriaParaDesativar(telefone);
            case CONFIRMAR_DESATIVACAO_CATEGORIA -> confirmarDesativacaoDeCategoria(telefone);
            case CANCELAR_FLUXO -> cancelarFluxo(telefone);
            case AJUDA -> enviarAjuda(telefone);
            case VOLTAR_MENU -> abrirMenuPrincipal(telefone);
            case CANCELAR_TRANSACAO -> cancelarTransacao(telefone, resolved.transacaoId());
            case TEXTO_LIVRE -> encaminharTextoParaIa(messageId, telefone, texto);
        }
    }

    private void abrirMenuPrincipal(String telefone) {
        conversationStateService.cancelarFluxo(telefone);
        wuzApiClient.enviarMenuPrincipal(telefone);
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
            log.warn("Categoria inválida: status={}", exception.getStatusCode());
            wuzApiClient.enviarMensagem(
                    telefone,
                    "Não foi possível criar a categoria. Use um nome entre 2 e 50 caracteres."
            );
        }
    }

    private void solicitarCategoriaParaDesativar(String telefone) {
        List<String> categorias = financialReportClient.buscarCategorias(telefone).stream()
                .filter(categoria -> !"Outros".equalsIgnoreCase(categoria))
                .toList();

        if (categorias.isEmpty()) {
            wuzApiClient.enviarMensagem(telefone, "Não há categorias disponíveis para desativar.");
            return;
        }

        conversationStateService.aguardarCategoriaParaDesativar(telefone);
        String lista = categorias.stream()
                .map(categoria -> "• " + categoria)
                .collect(java.util.stream.Collectors.joining("\n"));
        wuzApiClient.enviarMensagem(
                telefone,
                "Digite exatamente o nome da categoria que deseja desativar:\n\n" + lista
        );
    }

    private void prepararDesativacaoDeCategoria(String telefone, String nomeInformado) {
        String categoria = financialReportClient.buscarCategorias(telefone).stream()
                .filter(nome -> nome.equalsIgnoreCase(nomeInformado.trim()))
                .findFirst()
                .orElse(null);

        if (categoria == null || "Outros".equalsIgnoreCase(categoria)) {
            wuzApiClient.enviarMensagem(
                    telefone,
                    "Categoria inválida ou protegida. Digite outro nome da lista ou *menu* para sair."
            );
            return;
        }

        conversationStateService.aguardarConfirmacaoDeDesativacao(telefone, categoria);
        wuzApiClient.enviarConfirmacaoDesativacaoCategoria(telefone, categoria);
    }

    private void confirmarDesativacaoDeCategoria(String telefone) {
        String categoria = conversationStateService.consumirCategoriaParaConfirmarDesativacao(telefone);
        if (categoria == null) {
            wuzApiClient.enviarMensagem(telefone, "Essa confirmação expirou. Inicie novamente pelo menu.");
            return;
        }

        try {
            String desativada = financialReportClient.desativarCategoria(telefone, categoria);
            wuzApiClient.enviarMensagem(
                    telefone,
                    "✅ Categoria *" + desativada + "* desativada. As transações antigas foram preservadas."
            );
        } catch (org.springframework.web.client.RestClientResponseException exception) {
            log.warn("Falha ao desativar categoria: status={}", exception.getStatusCode());
            wuzApiClient.enviarMensagem(telefone, "Não foi possível desativar essa categoria.");
        }
    }

    private void cancelarFluxo(String telefone) {
        conversationStateService.cancelarFluxo(telefone);
        wuzApiClient.enviarMensagem(telefone, "Operação cancelada. Digite *menu* para voltar.");
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

    private void enviarRelatorioDosUltimosDias(String telefone, int dias) {
        LocalDate fim = LocalDate.now(BUSINESS_ZONE);
        LocalDate inicio = fim.minusDays(dias - 1L);
        enviarRelatorioDoPeriodo(telefone, inicio, fim, "Últimos " + dias + " dias");
        wuzApiClient.enviarAcoesRelatorio(telefone);
    }

    private void enviarRelatorioMensal(String telefone, String referencia) {
        YearMonth mes;
        try {
            mes = YearMonth.parse(referencia);
        } catch (RuntimeException exception) {
            wuzApiClient.enviarMensagem(telefone, "Não foi possível identificar o mês do relatório.");
            return;
        }

        YearMonth mesAtual = YearMonth.now(BUSINESS_ZONE);
        if (mes.isAfter(mesAtual)) {
            wuzApiClient.enviarMensagem(telefone, "Não é possível consultar um mês futuro.");
            return;
        }

        String titulo = mes.format(DateTimeFormatter.ofPattern("MMMM 'de' yyyy", Locale.of("pt", "BR")));
        enviarRelatorioDoPeriodo(telefone, mes.atDay(1), mes.atEndOfMonth(), capitalizar(titulo));
        wuzApiClient.enviarNavegacaoMensal(telefone, mes);
    }

    private void enviarRelatorioDoPeriodo(
            String telefone,
            LocalDate inicio,
            LocalDate fim,
            String tituloPeriodo
    ) {
        var resumo = financialReportClient.buscarRelatorioPorPeriodo(telefone, inicio, fim);
        NumberFormat moeda = criarFormatadorDeMoeda();
        List<GastoPorCategoriaDTO> gastos = resumo.gastosPorCategoria() == null
                ? List.of()
                : resumo.gastosPorCategoria();

        wuzApiClient.enviarMensagem(
                telefone,
                "📊 *Resumo financeiro — " + tituloPeriodo + "*\n\n"
                        + "Total: " + moeda.format(resumo.total())
                        + formatarCategorias(gastos, resumo.total(), moeda)
        );

        if (!gastos.isEmpty() && resumo.total() != null && resumo.total().signum() > 0) {
            wuzApiClient.enviarImagem(
                    telefone,
                    "Gastos por categoria — " + tituloPeriodo,
                    pieChartService.gerarGraficoBase64(gastos)
            );
        }

        conversationStateService.armazenarContextoRelatorio(
                telefone,
                inicio,
                fim,
                tituloPeriodo,
                gastos.stream()
                        .map(gasto -> new ReportCategoryOption(
                                gasto.categoriaId(), gasto.categoria()))
                        .toList()
        );
    }

    private void explorarCategoriasDoRelatorio(String telefone) {
        List<ReportCategoryOption> categorias =
                conversationStateService.buscarCategoriasDoRelatorio(telefone);
        if (categorias.isEmpty()) {
            wuzApiClient.enviarMensagem(
                    telefone,
                    "O relatório expirou ou não possui categorias com gastos. Gere um novo relatório."
            );
            return;
        }
        wuzApiClient.enviarSelecaoCategoriasRelatorio(telefone, categorias);
    }

    private void detalharCategoriaDoRelatorio(String telefone, String parametro) {
        long[] navegacao = extrairNavegacaoDaCategoria(parametro);
        if (navegacao == null) {
            wuzApiClient.enviarMensagem(telefone, "Não foi possível identificar a categoria selecionada.");
            return;
        }

        var selecao = conversationStateService.selecionarCategoriaDoRelatorio(
                telefone, navegacao[0]);
        if (selecao == null) {
            wuzApiClient.enviarMensagem(
                    telefone,
                    "Essa seleção expirou. Gere o relatório novamente para explorar as categorias."
            );
            return;
        }

        var detalhe = financialReportClient.buscarDetalheDaCategoria(
                telefone,
                selecao.categoriaId(),
                selecao.dataInicio(),
                selecao.dataFim(),
                Math.toIntExact(navegacao[1])
        );
        NumberFormat moeda = criarFormatadorDeMoeda();
        DateTimeFormatter dataHora = DateTimeFormatter.ofPattern("dd/MM 'às' HH:mm");
        String transacoes = detalhe.transacoes() == null || detalhe.transacoes().isEmpty()
                ? "Nenhuma transação encontrada."
                : detalhe.transacoes().stream()
                        .map(item -> formatarTransacao(item, moeda, dataHora))
                        .collect(java.util.stream.Collectors.joining("\n\n"));

        wuzApiClient.enviarMensagem(
                telefone,
                "🔎 *" + detalhe.categoria() + "*\n"
                        + detalhe.dataInicio().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                        + " a "
                        + detalhe.dataFim().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                        + "\n\nTotal: " + moeda.format(detalhe.total())
                        + "\nTransações: " + detalhe.quantidade()
                        + "\nPágina " + (detalhe.pagina() + 1)
                        + " de " + Math.max(detalhe.totalPaginas(), 1)
                        + "\n\n" + transacoes
        );
        wuzApiClient.enviarNavegacaoDetalheCategoria(
                telefone,
                selecao.categoriaId(),
                detalhe.pagina(),
                detalhe.totalPaginas()
        );
    }

    private void voltarAoResumoDoRelatorio(String telefone) {
        var periodo = conversationStateService.buscarPeriodoDoRelatorio(telefone);
        if (periodo == null) {
            wuzApiClient.enviarMensagem(
                    telefone,
                    "Esse relatório expirou. Gere um novo relatório para continuar."
            );
            return;
        }
        enviarRelatorioDoPeriodo(
                telefone,
                periodo.dataInicio(),
                periodo.dataFim(),
                periodo.tituloPeriodo()
        );
        wuzApiClient.enviarAcoesRelatorio(telefone);
    }

    private long[] extrairNavegacaoDaCategoria(String parametro) {
        try {
            String[] partes = parametro.split("_");
            if (partes.length != 2) {
                return null;
            }
            long categoriaId = Long.parseLong(partes[0]);
            long pagina = Long.parseLong(partes[1]);
            return categoriaId <= 0 || pagina < 0 || pagina > Integer.MAX_VALUE
                    ? null
                    : new long[]{categoriaId, pagina};
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private String formatarTransacao(
            TransacaoRelatorioDTO transacao,
            NumberFormat moeda,
            DateTimeFormatter dataHora
    ) {
        String descricao = transacao.descricao() == null ? "Sem descrição" : transacao.descricao();
        if (descricao.length() > 80) {
            descricao = descricao.substring(0, 79) + "…";
        }
        String data = transacao.dataHora() == null
                ? "Data não informada"
                : transacao.dataHora().format(dataHora);
        return "• " + data + " — " + moeda.format(transacao.valor()) + "\n  " + descricao;
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
        log.info("Mensagem enviada para fila: messageId={}", messageId);
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
            return "\n\nNenhum gasto registrado nesse período.";
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

    private String capitalizar(String texto) {
        return texto.isEmpty() ? texto : Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }
}

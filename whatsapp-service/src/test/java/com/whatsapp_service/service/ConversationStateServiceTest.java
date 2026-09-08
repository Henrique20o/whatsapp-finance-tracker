package com.whatsapp_service.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import java.time.LocalDate;
import java.util.List;

class ConversationStateServiceTest {

    private final ConversationStateService service = new ConversationStateService();

    @Test
    void deveConsumirEstadoDeRegistroUmaUnicaVez() {
        service.aguardarRegistroDeGasto("5531999998888");

        assertThat(service.consumirSeAguardandoGasto("5531999998888")).isTrue();
        assertThat(service.consumirSeAguardandoGasto("5531999998888")).isFalse();
    }

    @Test
    void deveManterEstadosSeparadosPorUsuario() {
        service.aguardarRegistroDeGasto("5531999998888");

        assertThat(service.consumirSeAguardandoGasto("5531888887777")).isFalse();
        assertThat(service.consumirSeAguardandoGasto("5531999998888")).isTrue();
    }

    @Test
    void deveConsumirEstadoDeCriacaoDeCategoriaUmaUnicaVez() {
        service.aguardarNomeDaCategoria("5531999998888");

        assertThat(service.consumirSeAguardandoNomeDaCategoria("5531999998888")).isTrue();
        assertThat(service.consumirSeAguardandoNomeDaCategoria("5531999998888")).isFalse();
    }

    @Test
    void deveManterCategoriaAteConfirmacaoDeDesativacao() {
        service.aguardarConfirmacaoDeDesativacao("5531999998888", "Viagens");

        assertThat(service.consumirCategoriaParaConfirmarDesativacao("5531999998888"))
                .isEqualTo("Viagens");
        assertThat(service.consumirCategoriaParaConfirmarDesativacao("5531999998888"))
                .isNull();
    }

    @Test
    void verificarOutroPassoNaoDeveApagarEstadoDeDesativacao() {
        service.aguardarCategoriaParaDesativar("5531999998888");

        assertThat(service.consumirSeAguardandoNomeDaCategoria("5531999998888")).isFalse();
        assertThat(service.estaAguardandoCategoriaParaDesativar("5531999998888")).isTrue();
        assertThat(service.estaAguardandoCategoriaParaDesativar("5531999998888")).isTrue();
    }

    @Test
    void abrirMenuDeveCancelarEstadoPendente() {
        service.aguardarCategoriaParaDesativar("5531999998888");

        service.cancelarFluxo("5531999998888");

        assertThat(service.estaAguardandoCategoriaParaDesativar("5531999998888")).isFalse();
    }

    @Test
    void deveGuardarPeriodoECategoriaDoRelatorio() {
        String telefone = "5531999998888";
        LocalDate inicio = LocalDate.of(2026, 7, 1);
        LocalDate fim = LocalDate.of(2026, 7, 31);
        service.armazenarContextoRelatorio(
                telefone,
                inicio,
                fim,
                "Julho de 2026",
                List.of(
                        new ConversationStateService.ReportCategoryOption(1L, "Alimentação"),
                        new ConversationStateService.ReportCategoryOption(2L, "Lazer")
                )
        );

        var selecao = service.selecionarCategoriaDoRelatorio(telefone, 2L);

        assertThat(selecao).isNotNull();
        assertThat(selecao.categoria()).isEqualTo("Lazer");
        assertThat(selecao.dataInicio()).isEqualTo(inicio);
        assertThat(selecao.dataFim()).isEqualTo(fim);
        assertThat(service.selecionarCategoriaDoRelatorio(telefone, 3L)).isNull();
        assertThat(service.buscarPeriodoDoRelatorio(telefone).tituloPeriodo())
                .isEqualTo("Julho de 2026");
    }

    @Test
    void abrirMenuDeveRemoverContextoDoRelatorio() {
        String telefone = "5531999998888";
        service.armazenarContextoRelatorio(
                telefone,
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31),
                "Julho de 2026",
                List.of(new ConversationStateService.ReportCategoryOption(2L, "Lazer"))
        );

        service.cancelarFluxo(telefone);

        assertThat(service.buscarCategoriasDoRelatorio(telefone)).isEmpty();
    }
}

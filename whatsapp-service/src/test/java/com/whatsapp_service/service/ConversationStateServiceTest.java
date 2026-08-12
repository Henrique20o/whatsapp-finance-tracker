package com.whatsapp_service.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
}

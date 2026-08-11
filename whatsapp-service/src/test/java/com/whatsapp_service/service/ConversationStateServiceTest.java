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
}

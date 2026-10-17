package com.wa.finance.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DesafioAutenticacaoTest {

    private static final Instant CRIADO_EM = Instant.parse("2026-10-14T17:00:00Z");

    @Test
    void deveControlarDisponibilidadeEExpiracaoNoLimiteExato() {
        DesafioAutenticacao desafio = desafio();

        assertThat(desafio.estaDisponivel(CRIADO_EM.plus(299, ChronoUnit.SECONDS))).isTrue();
        assertThat(desafio.estaExpirado(CRIADO_EM.plus(5, ChronoUnit.MINUTES))).isTrue();
        assertThat(desafio.estaDisponivel(CRIADO_EM.plus(5, ChronoUnit.MINUTES))).isFalse();
    }

    @Test
    void deveInvalidarUmaUnicaVezSemExporSegredosNoToString() {
        DesafioAutenticacao desafio = desafio();
        Instant invalidadoEm = CRIADO_EM.plusSeconds(30);

        desafio.invalidar(invalidadoEm);
        desafio.invalidar(CRIADO_EM.plusSeconds(40));

        assertThat(desafio.foiFinalizado()).isTrue();
        assertThat(desafio.getInvalidadoEm()).isEqualTo(invalidadoEm);
        assertThat(desafio.toString())
                .doesNotContain("telefone-protegido")
                .doesNotContain("aaaaaaaa")
                .doesNotContain("bbbbbbbb")
                .doesNotContain("cccccccc");
    }

    @Test
    void deveRejeitarPeriodosInvalidos() {
        assertThatThrownBy(() -> DesafioAutenticacao.criar(
                UUID.randomUUID(),
                "telefone-protegido",
                "a".repeat(64),
                "b".repeat(64),
                "c".repeat(64),
                CRIADO_EM,
                CRIADO_EM,
                CRIADO_EM.plusSeconds(60)
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deveContabilizarTentativasEInvalidarNaQuintaFalha() {
        DesafioAutenticacao desafio = desafio();

        for (int tentativa = 1; tentativa <= DesafioAutenticacao.MAX_TENTATIVAS; tentativa++) {
            desafio.registrarTentativaIncorreta(CRIADO_EM.plusSeconds(tentativa));
            assertThat(desafio.getTentativas()).isEqualTo((short) tentativa);
        }

        assertThat(desafio.getInvalidadoEm()).isEqualTo(CRIADO_EM.plusSeconds(5));
        assertThat(desafio.estaDisponivel(CRIADO_EM.plusSeconds(6))).isFalse();
        assertThatThrownBy(() -> desafio.registrarTentativaIncorreta(CRIADO_EM.plusSeconds(7)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void deveConsumirUmaUnicaVez() {
        DesafioAutenticacao desafio = desafio();
        Instant consumidoEm = CRIADO_EM.plusSeconds(30);

        desafio.consumir(consumidoEm);

        assertThat(desafio.getConsumidoEm()).isEqualTo(consumidoEm);
        assertThat(desafio.estaDisponivel(consumidoEm.plusSeconds(1))).isFalse();
        assertThatThrownBy(() -> desafio.consumir(consumidoEm.plusSeconds(2)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void naoDeveConsumirDesafioExpiradoOuInvalidado() {
        DesafioAutenticacao expirado = desafio();
        DesafioAutenticacao invalidado = desafio();
        invalidado.invalidar(CRIADO_EM.plusSeconds(10));

        assertThatThrownBy(() -> expirado.consumir(CRIADO_EM.plus(5, ChronoUnit.MINUTES)))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> invalidado.consumir(CRIADO_EM.plusSeconds(20)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void deveRejeitarTentativaOuConsumoAnterioresACriacao() {
        DesafioAutenticacao desafio = desafio();
        Instant instanteInvalido = CRIADO_EM.minusSeconds(1);

        assertThatThrownBy(() -> desafio.registrarTentativaIncorreta(instanteInvalido))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Tentativa anterior à criação");
        assertThatThrownBy(() -> desafio.consumir(instanteInvalido))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Consumo anterior à criação");
        assertThat(desafio.getTentativas()).isZero();
        assertThat(desafio.getConsumidoEm()).isNull();
    }

    private DesafioAutenticacao desafio() {
        return DesafioAutenticacao.criar(
                UUID.randomUUID(),
                "telefone-protegido",
                "a".repeat(64),
                "b".repeat(64),
                "c".repeat(64),
                CRIADO_EM,
                CRIADO_EM.plus(5, ChronoUnit.MINUTES),
                CRIADO_EM.plus(1, ChronoUnit.MINUTES)
        );
    }
}

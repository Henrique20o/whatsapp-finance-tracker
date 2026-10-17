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

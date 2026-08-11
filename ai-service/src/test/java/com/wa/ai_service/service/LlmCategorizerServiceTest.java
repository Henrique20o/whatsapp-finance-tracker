package com.wa.ai_service.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LlmCategorizerServiceTest {

    private static final List<String> CATEGORIAS = List.of(
            "Alimenta\u00e7\u00e3o",
            "Transporte",
            "Lazer",
            "Outros"
    );

    @Test
    void devePreservarGrafiaOficialDaCategoria() {
        String categoria = LlmCategorizerService.validarCategoria("  alimenta\u00e7\u00e3o ", CATEGORIAS);

        assertThat(categoria).isEqualTo("Alimenta\u00e7\u00e3o");
    }

    @Test
    void deveUsarOutrosQuandoIaInventarCategoria() {
        String categoria = LlmCategorizerService.validarCategoria("Restaurante", CATEGORIAS);

        assertThat(categoria).isEqualTo("Outros");
    }

    @Test
    void deveUsarOutrosQuandoIaNaoRetornarCategoria() {
        String categoria = LlmCategorizerService.validarCategoria(null, CATEGORIAS);

        assertThat(categoria).isEqualTo("Outros");
    }

    @Test
    void deveFalharQuandoCategoriaForInvalidaESemFallback() {
        assertThatThrownBy(() -> LlmCategorizerService.validarCategoria(
                "Categoria inventada",
                List.of("Lazer", "Transporte")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("não pertence ao catálogo");
    }

    @Test
    void deveFalharQuandoCatalogoEstiverVazio() {
        assertThatThrownBy(() -> LlmCategorizerService.validarCategoria("Lazer", List.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Nenhuma categoria disponível");
    }
}

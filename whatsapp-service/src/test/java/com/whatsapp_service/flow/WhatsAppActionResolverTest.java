package com.whatsapp_service.flow;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WhatsAppActionResolverTest {

    private final WhatsAppActionResolver resolver = new WhatsAppActionResolver();

    @ParameterizedTest
    @CsvSource({
            "Olá, ABRIR_MENU",
            "menu, ABRIR_MENU",
            "registrar_gasto, REGISTRAR_GASTO",
            "Registrar gasto, REGISTRAR_GASTO",
            "ver_relatorio, VER_RELATORIO",
            "relatorio_7_dias, RELATORIO_SETE_DIAS",
            "relatorio_30_dias, RELATORIO_TRINTA_DIAS",
            "Mais opções, MAIS_OPCOES",
            "gerenciar_categorias, GERENCIAR_CATEGORIAS",
            "listar_categorias, LISTAR_CATEGORIAS",
            "criar_categoria, CRIAR_CATEGORIA",
            "desativar_categoria, DESATIVAR_CATEGORIA",
            "confirmar_desativacao_categoria, CONFIRMAR_DESATIVACAO_CATEGORIA",
            "cancelar_fluxo, CANCELAR_FLUXO",
            "Ajuda, AJUDA",
            "voltar_menu, VOLTAR_MENU",
            "Gastei 50 reais, TEXTO_LIVRE"
    })
    void deveResolverAcoesConhecidas(String texto, WhatsAppAction esperada) {
        assertThat(resolver.resolver(texto).action()).isEqualTo(esperada);
    }

    @Test
    void deveExtrairIdDaAcaoDeCancelamento() {
        ResolvedWhatsAppAction resultado = resolver.resolver("cancelar_transacao_99");

        assertThat(resultado.action()).isEqualTo(WhatsAppAction.CANCELAR_TRANSACAO);
        assertThat(resultado.transacaoId()).isEqualTo(99L);
    }

    @Test
    void deveExtrairMesDoRelatorio() {
        ResolvedWhatsAppAction resultado = resolver.resolver("relatorio_mes_2026-07");

        assertThat(resultado.action()).isEqualTo(WhatsAppAction.RELATORIO_MENSAL);
        assertThat(resultado.parametro()).isEqualTo("2026-07");
    }

    @Test
    void deveRejeitarIdDeCancelamentoInvalido() {
        assertThatThrownBy(() -> resolver.resolver("cancelar_transacao_invalida"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Identificador de cancelamento inválido");
    }
}

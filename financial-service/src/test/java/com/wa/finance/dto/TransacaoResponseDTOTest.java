package com.wa.finance.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wa.finance.domain.Categoria;
import com.wa.finance.domain.Transacao;
import com.wa.finance.domain.Usuario;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class TransacaoResponseDTOTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void deveExporSomenteCamposPublicosDaTransacao() throws Exception {
        Usuario usuario = Usuario.builder()
                .id(1L)
                .telefone("5531999998888")
                .telefoneCriptografado("ciphertext-secreto")
                .telefoneHash("hash-secreto")
                .build();
        Categoria categoria = Categoria.builder()
                .id(2L)
                .nome("Lazer")
                .usuario(usuario)
                .ativa(true)
                .build();
        Transacao transacao = Transacao.builder()
                .id(3L)
                .externalMessageId("message-id-interno")
                .valor(new BigDecimal("50.00"))
                .descricao("Futebol")
                .dataHora(LocalDateTime.of(2026, 8, 12, 10, 30))
                .deletado(false)
                .usuario(usuario)
                .categoria(categoria)
                .build();

        JsonNode json = objectMapper.readTree(
                objectMapper.writeValueAsString(TransacaoResponseDTO.from(transacao))
        );

        assertThat(json.path("id").asLong()).isEqualTo(3L);
        assertThat(json.path("categoria").asText()).isEqualTo("Lazer");
        assertThat(json.path("cancelada").asBoolean()).isFalse();
        assertThat(json.has("usuario")).isFalse();
        assertThat(json.has("telefone")).isFalse();
        assertThat(json.has("telefoneCriptografado")).isFalse();
        assertThat(json.has("telefoneHash")).isFalse();
        assertThat(json.has("externalMessageId")).isFalse();
    }
}

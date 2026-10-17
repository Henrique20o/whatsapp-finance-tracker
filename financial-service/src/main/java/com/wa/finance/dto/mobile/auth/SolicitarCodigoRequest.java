package com.wa.finance.dto.mobile.auth;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SolicitarCodigoRequest(
        @NotBlank
        @Pattern(regexp = "^[1-9][0-9]{9,14}$")
        String telefone
) {

    @JsonAnySetter
    public void rejeitarCampoDesconhecido(String nome, Object valor) {
        throw new IllegalArgumentException("Campo não previsto no contrato");
    }
}

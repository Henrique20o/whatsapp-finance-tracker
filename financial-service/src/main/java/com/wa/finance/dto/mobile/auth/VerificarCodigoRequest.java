package com.wa.finance.dto.mobile.auth;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record VerificarCodigoRequest(
        @NotNull UUID challengeId,
        @NotNull
        @Pattern(regexp = "^[0-9]{6}$")
        String codigo,
        @Size(min = 1, max = 100)
        @Pattern(regexp = "^[^\\p{Cntrl}]*\\S[^\\p{Cntrl}]*$")
        String nomeDispositivo
) {

    @JsonAnySetter
    public void rejeitarCampoDesconhecido(String nome, Object valor) {
        throw new IllegalArgumentException("Campo não previsto no contrato");
    }
}

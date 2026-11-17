package com.wa.finance.service;

import com.wa.finance.dto.mobile.auth.TokenResponse;
import java.util.Objects;

public record CodigoConfirmado(Long usuarioId, String nomeDispositivo, TokenResponse tokenResponse) {

    public CodigoConfirmado(Long usuarioId, String nomeDispositivo) {
        this(usuarioId, nomeDispositivo, null);
    }

    public CodigoConfirmado {
        Objects.requireNonNull(usuarioId);
    }
}

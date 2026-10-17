package com.wa.finance.service;

import java.util.Objects;

public record CodigoConfirmado(Long usuarioId, String nomeDispositivo) {

    public CodigoConfirmado {
        Objects.requireNonNull(usuarioId);
    }
}

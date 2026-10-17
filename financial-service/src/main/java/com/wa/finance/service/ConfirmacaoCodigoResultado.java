package com.wa.finance.service;

import java.util.Objects;
import java.util.Optional;

public final class ConfirmacaoCodigoResultado {

    private static final ConfirmacaoCodigoResultado REJEITADO =
            new ConfirmacaoCodigoResultado(null);

    private final CodigoConfirmado codigoConfirmado;

    private ConfirmacaoCodigoResultado(CodigoConfirmado codigoConfirmado) {
        this.codigoConfirmado = codigoConfirmado;
    }

    public static ConfirmacaoCodigoResultado rejeitado() {
        return REJEITADO;
    }

    public static ConfirmacaoCodigoResultado confirmado(CodigoConfirmado codigoConfirmado) {
        return new ConfirmacaoCodigoResultado(Objects.requireNonNull(codigoConfirmado));
    }

    public boolean foiConfirmado() {
        return codigoConfirmado != null;
    }

    public Optional<CodigoConfirmado> codigoConfirmado() {
        return Optional.ofNullable(codigoConfirmado);
    }
}

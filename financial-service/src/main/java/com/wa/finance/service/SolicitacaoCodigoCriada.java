package com.wa.finance.service;

import java.util.Objects;
import java.util.UUID;

public record SolicitacaoCodigoCriada(
        UUID challengeId,
        long expiraEmSegundos,
        long reenviarEmSegundos
) {

    public SolicitacaoCodigoCriada {
        Objects.requireNonNull(challengeId, "challengeId obrigatório");
        if (expiraEmSegundos < 1 || reenviarEmSegundos < 1) {
            throw new IllegalArgumentException("Durações da solicitação são inválidas");
        }
    }
}

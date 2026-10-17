package com.wa.finance.dto.mobile.auth;

import java.util.UUID;

public record SolicitarCodigoResponse(
        UUID challengeId,
        long expiraEmSegundos,
        long reenviarEmSegundos
) {
}

package com.wa.finance.dto.mobile;

import java.time.Instant;
import java.util.UUID;

public record SessaoMobileResponse(
        UUID id,
        String nomeDispositivo,
        Instant criadaEm,
        Instant ultimoUsoEm,
        Instant expiraEm,
        boolean ativa,
        boolean atual
) { }

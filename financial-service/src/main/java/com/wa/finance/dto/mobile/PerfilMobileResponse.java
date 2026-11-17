package com.wa.finance.dto.mobile;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PerfilMobileResponse(
        UUID usuarioId, String nome, String email, String telefoneMascarado, OffsetDateTime cadastradoEm
) { }

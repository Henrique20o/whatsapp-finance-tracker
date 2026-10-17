package com.wa.finance.dto.mobile.auth;

import java.time.OffsetDateTime;

public record MobileErrorResponse(
        String code,
        String message,
        OffsetDateTime timestamp,
        String path,
        String correlationId
) {
}

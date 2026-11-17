package com.wa.finance.dto.mobile.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RefreshTokenRequest(
        @NotBlank
        @Pattern(regexp = "^[A-Za-z0-9_-]{43}$")
        String refreshToken
) {
}

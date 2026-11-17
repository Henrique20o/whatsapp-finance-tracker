package com.wa.finance.dto.mobile.auth;

public record TokenResponse(
        String accessToken,
        String tokenType,
        long accessTokenExpiraEmSegundos,
        String refreshToken,
        long refreshTokenExpiraEmSegundos
) {
    public TokenResponse {
        tokenType = "Bearer";
    }
}

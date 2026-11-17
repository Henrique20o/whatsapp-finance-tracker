package com.wa.finance.web.mobile;

public final class InvalidRefreshTokenException extends RuntimeException {
    public InvalidRefreshTokenException() {
        super("Refresh token inválido");
    }
}

package com.wa.finance.security;

import org.springframework.security.oauth2.jwt.Jwt;

public final class MobilePrincipal {
    private MobilePrincipal() { }

    public static Long usuarioId(Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null) {
            throw new IllegalArgumentException("Principal mobile inválido");
        }
        try {
            long userId = Long.parseLong(jwt.getSubject());
            if (userId <= 0) throw new NumberFormatException();
            return userId;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Principal mobile inválido", exception);
        }
    }
}

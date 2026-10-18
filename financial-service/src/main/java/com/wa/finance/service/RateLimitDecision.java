package com.wa.finance.service;

import java.time.Instant;

public record RateLimitDecision(
        boolean allowed,
        Instant evaluatedAt,
        Instant blockedUntil,
        long retryAfterSeconds,
        RateLimitScope scope,
        RateLimitRule rule
) {

    public static RateLimitDecision permit(Instant evaluatedAt) {
        if (evaluatedAt == null) {
            throw new IllegalArgumentException("Instante da decisão é obrigatório");
        }
        return new RateLimitDecision(true, evaluatedAt, null, 0, null, null);
    }

    public static RateLimitDecision blocked(
            Instant evaluatedAt,
            Instant blockedUntil,
            long retryAfterSeconds,
            RateLimitScope scope,
            RateLimitRule rule
    ) {
        if (evaluatedAt == null || blockedUntil == null
                || retryAfterSeconds < 1 || scope == null || rule == null) {
            throw new IllegalArgumentException("Decisão de limite inválida");
        }
        return new RateLimitDecision(
                false, evaluatedAt, blockedUntil, retryAfterSeconds, scope, rule);
    }
}

package com.wa.finance.service;

import java.time.Instant;

public record RateLimitDecision(
        boolean allowed,
        Instant blockedUntil,
        long retryAfterSeconds,
        RateLimitScope scope,
        RateLimitRule rule
) {

    public static RateLimitDecision permit() {
        return new RateLimitDecision(true, null, 0, null, null);
    }

    public static RateLimitDecision blocked(
            Instant blockedUntil,
            long retryAfterSeconds,
            RateLimitScope scope,
            RateLimitRule rule
    ) {
        if (blockedUntil == null || retryAfterSeconds < 1 || scope == null || rule == null) {
            throw new IllegalArgumentException("Decisão de limite inválida");
        }
        return new RateLimitDecision(false, blockedUntil, retryAfterSeconds, scope, rule);
    }
}

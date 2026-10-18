package com.wa.finance.service;

public class RateLimitExceededException extends RuntimeException {

    private final long retryAfterSeconds;
    private final RateLimitScope scope;
    private final RateLimitRule rule;

    public RateLimitExceededException(
            long retryAfterSeconds,
            RateLimitScope scope,
            RateLimitRule rule
    ) {
        super("Limite de autenticação excedido");
        if (retryAfterSeconds < 1 || scope == null || rule == null) {
            throw new IllegalArgumentException("Dados do limite são inválidos");
        }
        this.retryAfterSeconds = retryAfterSeconds;
        this.scope = scope;
        this.rule = rule;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    public RateLimitScope getScope() {
        return scope;
    }

    public RateLimitRule getRule() {
        return rule;
    }
}

package com.wa.finance.web.mobile;

public final class IdempotencyConflictException extends RuntimeException {
    public IdempotencyConflictException() { super("Chave de idempotência reutilizada com dados diferentes"); }
}

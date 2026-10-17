package com.wa.finance.web.mobile;

public final class InvalidVerificationCodeException extends RuntimeException {

    public InvalidVerificationCodeException() {
        super("Código ou desafio rejeitado");
    }
}

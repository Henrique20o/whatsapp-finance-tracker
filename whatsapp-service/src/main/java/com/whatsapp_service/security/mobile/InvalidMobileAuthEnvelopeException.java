package com.whatsapp_service.security.mobile;

public class InvalidMobileAuthEnvelopeException extends RuntimeException {
    public InvalidMobileAuthEnvelopeException() {
        super("INVALID_ENVELOPE");
    }

    public InvalidMobileAuthEnvelopeException(Throwable cause) {
        super("INVALID_ENVELOPE", cause);
    }
}

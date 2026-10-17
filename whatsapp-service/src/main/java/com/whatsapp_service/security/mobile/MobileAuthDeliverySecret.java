package com.whatsapp_service.security.mobile;

public record MobileAuthDeliverySecret(String telefone, String codigo) {
    @Override
    public String toString() {
        return "MobileAuthDeliverySecret{redacted}";
    }
}

package com.wa.finance.security.mobile.delivery;

public record MobileAuthDeliverySecret(String telefone, String codigo) {
    @Override
    public String toString() {
        return "MobileAuthDeliverySecret{redacted}";
    }
}

package com.whatsapp_service.service;

public class TransientMobileAuthDeliveryException extends RuntimeException {
    public TransientMobileAuthDeliveryException(String category) {
        super(category);
    }
}

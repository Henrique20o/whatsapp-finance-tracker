package com.whatsapp_service.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Component
public class WebhookSignatureVerifier {

    private static final String ALGORITHM = "HmacSHA256";
    private final byte[] secret;

    public WebhookSignatureVerifier(@Value("${app.security.wuzapi-hmac-key}") String secret) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("WUZAPI_HMAC_KEY deve possuir pelo menos 32 caracteres");
        }
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    public boolean isValid(byte[] rawBody, String receivedSignature) {
        if (rawBody == null || receivedSignature == null || receivedSignature.isBlank()) {
            return false;
        }

        try {
            byte[] received = HexFormat.of().parseHex(receivedSignature.trim());
            return MessageDigest.isEqual(sign(rawBody), received);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    byte[] sign(byte[] rawBody) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret, ALGORITHM));
            return mac.doFinal(rawBody);
        } catch (Exception exception) {
            throw new IllegalStateException("Não foi possível validar a assinatura do webhook", exception);
        }
    }
}

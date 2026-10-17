package com.wa.finance.security.mobile.delivery;

import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Component
public class MobileAuthDeliveryCipher {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final ObjectMapper objectMapper;
    private final MobileAuthDeliveryProperties properties;
    private final SecureRandom secureRandom;

    public MobileAuthDeliveryCipher(
            ObjectMapper objectMapper,
            MobileAuthDeliveryProperties properties,
            SecureRandom secureRandom
    ) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.secureRandom = secureRandom;
    }

    public MobileAuthDeliveryEnvelope encrypt(
            UUID messageId,
            UUID challengeId,
            UUID correlationId,
            Instant expiresAt,
            String telefone,
            String codigo
    ) {
        byte[] iv = new byte[IV_BYTES];
        secureRandom.nextBytes(iv);

        MobileAuthDeliveryEnvelope unsigned = new MobileAuthDeliveryEnvelope(
                MobileAuthDeliveryEnvelope.CURRENT_SCHEMA_VERSION,
                messageId,
                challengeId,
                correlationId,
                expiresAt,
                properties.getKeyId(),
                Base64.getEncoder().encodeToString(iv),
                "pending"
        );

        try {
            byte[] plaintext = objectMapper.writeValueAsBytes(
                    new MobileAuthDeliverySecret(telefone, codigo));
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    new SecretKeySpec(properties.decodedKey(), "AES"),
                    new GCMParameterSpec(TAG_BITS, iv)
            );
            cipher.updateAAD(unsigned.additionalAuthenticatedData());
            byte[] ciphertext = cipher.doFinal(plaintext);
            return new MobileAuthDeliveryEnvelope(
                    unsigned.schemaVersion(),
                    unsigned.messageId(),
                    unsigned.challengeId(),
                    unsigned.correlationId(),
                    unsigned.expiresAt(),
                    unsigned.keyId(),
                    unsigned.iv(),
                    Base64.getEncoder().encodeToString(ciphertext)
            );
        } catch (GeneralSecurityException | RuntimeException exception) {
            throw new IllegalStateException("Não foi possível proteger o comando de entrega", exception);
        }
    }
}

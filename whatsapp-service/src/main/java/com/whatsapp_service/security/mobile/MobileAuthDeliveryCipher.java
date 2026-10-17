package com.whatsapp_service.security.mobile;

import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.util.Base64;

@Component
public class MobileAuthDeliveryCipher {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final ObjectMapper objectMapper;
    private final MobileAuthDeliveryProperties properties;

    public MobileAuthDeliveryCipher(ObjectMapper objectMapper, MobileAuthDeliveryProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public MobileAuthDeliverySecret decrypt(MobileAuthDeliveryEnvelope envelope) {
        validateEnvelope(envelope);
        try {
            byte[] iv = Base64.getDecoder().decode(envelope.iv());
            if (iv.length != IV_BYTES) {
                throw new InvalidMobileAuthEnvelopeException();
            }
            byte[] ciphertext = Base64.getDecoder().decode(envelope.ciphertext());
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(
                    Cipher.DECRYPT_MODE,
                    new SecretKeySpec(properties.decodedKey(), "AES"),
                    new GCMParameterSpec(TAG_BITS, iv)
            );
            cipher.updateAAD(envelope.additionalAuthenticatedData());
            byte[] plaintext = cipher.doFinal(ciphertext);
            return objectMapper.readValue(plaintext, MobileAuthDeliverySecret.class);
        } catch (InvalidMobileAuthEnvelopeException exception) {
            throw exception;
        } catch (GeneralSecurityException | RuntimeException exception) {
            throw new InvalidMobileAuthEnvelopeException(exception);
        }
    }

    private void validateEnvelope(MobileAuthDeliveryEnvelope envelope) {
        if (envelope == null
                || envelope.schemaVersion() != MobileAuthDeliveryEnvelope.CURRENT_SCHEMA_VERSION
                || !properties.getKeyId().equals(envelope.keyId())) {
            throw new InvalidMobileAuthEnvelopeException();
        }
    }
}

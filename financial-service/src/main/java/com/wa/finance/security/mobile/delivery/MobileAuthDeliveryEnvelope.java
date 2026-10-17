package com.wa.finance.security.mobile.delivery;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record MobileAuthDeliveryEnvelope(
        int schemaVersion,
        UUID messageId,
        UUID challengeId,
        UUID correlationId,
        Instant expiresAt,
        String keyId,
        String iv,
        String ciphertext
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public MobileAuthDeliveryEnvelope {
        Objects.requireNonNull(messageId);
        Objects.requireNonNull(challengeId);
        Objects.requireNonNull(correlationId);
        Objects.requireNonNull(expiresAt);
        Objects.requireNonNull(keyId);
        Objects.requireNonNull(iv);
        Objects.requireNonNull(ciphertext);
    }

    public byte[] additionalAuthenticatedData() {
        return ("mobile-auth-code|" + schemaVersion
                + "|" + messageId
                + "|" + challengeId
                + "|" + correlationId
                + "|" + expiresAt
                + "|" + keyId).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String toString() {
        return "MobileAuthDeliveryEnvelope{" +
                "schemaVersion=" + schemaVersion +
                ", messageId=" + messageId +
                ", challengeId=" + challengeId +
                ", correlationId=" + correlationId +
                ", expiresAt=" + expiresAt +
                ", keyId='" + keyId + '\'' +
                '}';
    }
}

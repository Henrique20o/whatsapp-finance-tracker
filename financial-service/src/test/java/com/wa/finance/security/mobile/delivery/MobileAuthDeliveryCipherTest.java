package com.wa.finance.security.mobile.delivery;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MobileAuthDeliveryCipherTest {

    @Test
    void deveCifrarTelefoneECodigoSemExporSegredosNoEnvelope() throws Exception {
        MobileAuthDeliveryProperties properties = properties();
        MobileAuthDeliveryCipher cipher = new MobileAuthDeliveryCipher(
                new ObjectMapper(), properties, new SecureRandom());

        MobileAuthDeliveryEnvelope envelope = cipher.encrypt(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.parse("2026-10-17T20:05:00Z"),
                "5531999998888",
                "004291"
        );

        String serialized = new ObjectMapper().writeValueAsString(envelope);
        assertThat(serialized)
                .doesNotContain("5531999998888")
                .doesNotContain("004291");
        assertThat(envelope.toString())
                .doesNotContain(envelope.iv())
                .doesNotContain(envelope.ciphertext());

        Cipher decryptor = Cipher.getInstance("AES/GCM/NoPadding");
        decryptor.init(
                Cipher.DECRYPT_MODE,
                new SecretKeySpec(properties.decodedKey(), "AES"),
                new GCMParameterSpec(128, Base64.getDecoder().decode(envelope.iv()))
        );
        decryptor.updateAAD(envelope.additionalAuthenticatedData());
        String plaintext = new String(decryptor.doFinal(
                Base64.getDecoder().decode(envelope.ciphertext())));
        assertThat(plaintext)
                .contains("5531999998888")
                .contains("004291");
    }

    @Test
    void deveUsarIvNovoEmCadaCifragem() {
        MobileAuthDeliveryCipher cipher = new MobileAuthDeliveryCipher(
                new ObjectMapper(), properties(), new SecureRandom());
        UUID challengeId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();
        Instant expiresAt = Instant.parse("2026-10-17T20:05:00Z");

        MobileAuthDeliveryEnvelope first = cipher.encrypt(
                UUID.randomUUID(), challengeId, correlationId, expiresAt,
                "5531999998888", "004291");
        MobileAuthDeliveryEnvelope second = cipher.encrypt(
                UUID.randomUUID(), challengeId, correlationId, expiresAt,
                "5531999998888", "004291");

        assertThat(first.iv()).isNotEqualTo(second.iv());
        assertThat(first.ciphertext()).isNotEqualTo(second.ciphertext());
    }

    private MobileAuthDeliveryProperties properties() {
        MobileAuthDeliveryProperties properties = new MobileAuthDeliveryProperties();
        properties.setKey("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=");
        properties.setKeyId("v1");
        return properties;
    }
}

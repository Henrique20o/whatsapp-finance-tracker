package com.whatsapp_service.security.mobile;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MobileAuthDeliveryCipherTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MobileAuthDeliveryProperties properties = properties();
    private final MobileAuthDeliveryCipher cipher = new MobileAuthDeliveryCipher(objectMapper, properties);

    @Test
    void deveDecifrarEnvelopeCompativelComContrato() throws Exception {
        MobileAuthDeliveryEnvelope envelope = encrypt(
                "5531999998888", "004291", Instant.parse("2026-10-17T20:05:00Z"));

        MobileAuthDeliverySecret secret = cipher.decrypt(envelope);

        assertThat(secret.telefone()).isEqualTo("5531999998888");
        assertThat(secret.codigo()).isEqualTo("004291");
        assertThat(secret.toString()).doesNotContain("5531999998888", "004291");
    }

    @Test
    void deveRejeitarAlteracaoDosMetadadosAutenticados() throws Exception {
        MobileAuthDeliveryEnvelope original = encrypt(
                "5531999998888", "004291", Instant.parse("2026-10-17T20:05:00Z"));
        MobileAuthDeliveryEnvelope tampered = new MobileAuthDeliveryEnvelope(
                original.schemaVersion(), original.messageId(), original.challengeId(),
                original.correlationId(), original.expiresAt().plusSeconds(60),
                original.keyId(), original.iv(), original.ciphertext());

        assertThatThrownBy(() -> cipher.decrypt(tampered))
                .isInstanceOf(InvalidMobileAuthEnvelopeException.class)
                .hasMessage("INVALID_ENVELOPE");
    }

    @Test
    void deveRejeitarVersaoOuChaveDesconhecida() throws Exception {
        MobileAuthDeliveryEnvelope original = encrypt(
                "5531999998888", "004291", Instant.parse("2026-10-17T20:05:00Z"));
        MobileAuthDeliveryEnvelope unknownKey = new MobileAuthDeliveryEnvelope(
                original.schemaVersion(), original.messageId(), original.challengeId(),
                original.correlationId(), original.expiresAt(), "v2",
                original.iv(), original.ciphertext());

        assertThatThrownBy(() -> cipher.decrypt(unknownKey))
                .isInstanceOf(InvalidMobileAuthEnvelopeException.class);
    }

    private MobileAuthDeliveryEnvelope encrypt(
            String phone,
            String code,
            Instant expiresAt
    ) throws Exception {
        byte[] iv = Base64.getDecoder().decode("AAECAwQFBgcICQoL");
        MobileAuthDeliveryEnvelope unsigned = new MobileAuthDeliveryEnvelope(
                1,
                UUID.fromString("ff468b6d-6d8c-4f15-9677-6d08beb8e221"),
                UUID.fromString("538f1567-a3eb-4e6d-b06d-e5987a340672"),
                UUID.fromString("b5f670f5-a9b8-4597-ad03-2af7b35fb391"),
                expiresAt,
                "v1",
                Base64.getEncoder().encodeToString(iv),
                "pending"
        );
        Cipher encryptor = Cipher.getInstance("AES/GCM/NoPadding");
        encryptor.init(
                Cipher.ENCRYPT_MODE,
                new SecretKeySpec(properties.decodedKey(), "AES"),
                new GCMParameterSpec(128, iv)
        );
        encryptor.updateAAD(unsigned.additionalAuthenticatedData());
        byte[] ciphertext = encryptor.doFinal(objectMapper.writeValueAsBytes(
                new MobileAuthDeliverySecret(phone, code)));
        return new MobileAuthDeliveryEnvelope(
                unsigned.schemaVersion(), unsigned.messageId(), unsigned.challengeId(),
                unsigned.correlationId(), unsigned.expiresAt(), unsigned.keyId(),
                unsigned.iv(), Base64.getEncoder().encodeToString(ciphertext));
    }

    private MobileAuthDeliveryProperties properties() {
        MobileAuthDeliveryProperties result = new MobileAuthDeliveryProperties();
        result.setKey("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=");
        result.setKeyId("v1");
        return result;
    }
}

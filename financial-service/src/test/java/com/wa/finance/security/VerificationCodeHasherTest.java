package com.wa.finance.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VerificationCodeHasherTest {

    private VerificationCodeHasher hasher;

    @BeforeEach
    void configurar() {
        VerificationCodeProperties properties = validProperties();
        hasher = new VerificationCodeHasher(new MobileAuthenticationHmacKeys(properties));
    }

    @Test
    void deveProduzirHmacDeterministicoSemRevelarCodigo() {
        UUID challengeId = UUID.fromString("538f1567-a3eb-4e6d-b06d-e5987a340672");

        String first = hasher.hash(challengeId, "5531999998888", "482913");
        String second = hasher.hash(challengeId, "5531999998888", "482913");

        assertThat(first)
                .isEqualTo(second)
                .matches("[0-9a-f]{64}")
                .doesNotContain("482913")
                .doesNotContain("5531999998888");
        assertThat(hasher.matches(first, challengeId, "5531999998888", "482913")).isTrue();
    }

    @Test
    void deveSepararCodigoTelefoneEDesafio() {
        UUID challengeId = UUID.randomUUID();
        String original = hasher.hash(challengeId, "5531999998888", "482913");

        assertThat(hasher.hash(challengeId, "5531999998888", "482914")).isNotEqualTo(original);
        assertThat(hasher.hash(challengeId, "5531988887777", "482913")).isNotEqualTo(original);
        assertThat(hasher.hash(UUID.randomUUID(), "5531999998888", "482913")).isNotEqualTo(original);
        assertThat(hasher.matches(original, challengeId, "5531999998888", "482914")).isFalse();
    }

    @Test
    void deveRejeitarFormatoInvalidoSemEcoarValores() {
        assertThat(hasher.matches("valor-malformado", UUID.randomUUID(), "5531999998888", "482913"))
                .isFalse();

        assertThatThrownBy(() -> hasher.hash(UUID.randomUUID(), "5531999998888", "codigo-secreto"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Dados de verificação inválidos")
                .hasMessageNotContaining("codigo-secreto");
    }

    static VerificationCodeProperties validProperties() {
        VerificationCodeProperties properties = new VerificationCodeProperties();
        properties.setHmacKey(Base64.getEncoder().encodeToString(new byte[32]));
        properties.setTtl(Duration.ofMinutes(5));
        properties.setResendInterval(Duration.ofMinutes(1));
        return properties;
    }
}

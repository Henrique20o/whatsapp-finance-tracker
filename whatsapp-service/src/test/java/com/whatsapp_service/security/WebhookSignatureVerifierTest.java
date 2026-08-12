package com.whatsapp_service.security;

import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WebhookSignatureVerifierTest {

    private static final String SECRET = "chave-hmac-de-teste-com-32-caracteres";
    private final WebhookSignatureVerifier verifier = new WebhookSignatureVerifier(SECRET);

    @Test
    void deveAceitarAssinaturaHexadecimalValida() throws Exception {
        byte[] body = "{\"type\":\"Message\"}".getBytes(StandardCharsets.UTF_8);

        assertThat(verifier.isValid(body, signature(body))).isTrue();
    }

    @Test
    void deveRejeitarCorpoAlteradoAssinaturaAusenteOuMalformada() throws Exception {
        byte[] original = "{\"type\":\"Message\"}".getBytes(StandardCharsets.UTF_8);

        assertThat(verifier.isValid("{}".getBytes(StandardCharsets.UTF_8), signature(original))).isFalse();
        assertThat(verifier.isValid(original, null)).isFalse();
        assertThat(verifier.isValid(original, "não-é-hexadecimal")).isFalse();
    }

    @Test
    void deveExigirChaveComNoMinimo32Caracteres() {
        assertThatThrownBy(() -> new WebhookSignatureVerifier("curta"))
                .isInstanceOf(IllegalStateException.class);
    }

    private String signature(byte[] body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(body));
    }
}

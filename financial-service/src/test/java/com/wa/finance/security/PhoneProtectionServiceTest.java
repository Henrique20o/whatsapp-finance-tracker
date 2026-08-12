package com.wa.finance.security;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PhoneProtectionServiceTest {

    private final PhoneProtectionService service = new PhoneProtectionService(
            Base64.getEncoder().encodeToString(new byte[32])
    );

    @Test
    void deveCriptografarEDescriptografarTelefone() {
        String encrypted = service.encrypt("+55 (31) 99999-8888");

        assertThat(encrypted).startsWith("v1:").doesNotContain("5531999998888");
        assertThat(service.decrypt(encrypted)).isEqualTo("5531999998888");
    }

    @Test
    void deveUsarIvAleatorioSemAlterarHashDeBusca() {
        String first = service.encrypt("5531999998888");
        String second = service.encrypt("5531999998888");

        assertThat(first).isNotEqualTo(second);
        assertThat(service.lookupHash("5531999998888"))
                .isEqualTo(service.lookupHash("+55 (31) 99999-8888"));
    }

    @Test
    void deveRejeitarChaveComTamanhoIncorreto() {
        assertThatThrownBy(() -> new PhoneProtectionService(Base64.getEncoder().encodeToString(new byte[16])))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }
}

package com.wa.finance.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClientIpProtectionServiceTest {

    private final ClientIpProtectionService service = new ClientIpProtectionService(
            new MobileAuthenticationHmacKeys(VerificationCodeHasherTest.validProperties())
    );

    @Test
    void deveGerarHashDeterministicoSemConterIpOriginal() {
        String first = service.hash("2001:db8::1");

        assertThat(first)
                .isEqualTo(service.hash("2001:DB8::1"))
                .matches("[0-9a-f]{64}")
                .doesNotContain("2001:db8::1");
        assertThat(service.hash("192.0.2.10")).isNotEqualTo(first);
    }

    @Test
    void deveRejeitarValorQueNaoSejaIpCanonico() {
        assertThatThrownBy(() -> service.hash("ip-do-cliente"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Endereço IP canônico inválido");
    }
}

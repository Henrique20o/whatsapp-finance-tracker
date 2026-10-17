package com.wa.finance.security;

import org.junit.jupiter.api.Test;

import java.security.SecureRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VerificationCodeGeneratorTest {

    @Test
    void deveGerarSeisDigitosPreservandoZerosAEsquerda() {
        SecureRandom secureRandom = mock(SecureRandom.class);
        when(secureRandom.nextInt(VerificationCodeGenerator.UPPER_BOUND)).thenReturn(42);

        String codigo = new VerificationCodeGenerator(secureRandom).generate();

        assertThat(codigo).isEqualTo("000042").matches("[0-9]{6}");
        verify(secureRandom).nextInt(1_000_000);
    }

    @Test
    void deveAceitarOsLimitesDoIntervalo() {
        SecureRandom secureRandom = mock(SecureRandom.class);
        when(secureRandom.nextInt(VerificationCodeGenerator.UPPER_BOUND)).thenReturn(0, 999_999);
        var generator = new VerificationCodeGenerator(secureRandom);

        assertThat(generator.generate()).isEqualTo("000000");
        assertThat(generator.generate()).isEqualTo("999999");
    }
}

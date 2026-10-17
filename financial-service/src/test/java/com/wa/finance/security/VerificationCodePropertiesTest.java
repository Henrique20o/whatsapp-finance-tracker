package com.wa.finance.security;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class VerificationCodePropertiesTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void deveAceitarConfiguracaoValida() {
        assertThat(validator.validate(validProperties())).isEmpty();
    }

    @Test
    void deveRejeitarChaveInvalidaOuCurta() {
        VerificationCodeProperties invalidBase64 = validProperties();
        invalidBase64.setHmacKey("não-é-base64");
        assertThat(validator.validate(invalidBase64)).anyMatch(
                violation -> violation.getPropertyPath().toString().equals("hmacKeyValid")
        );

        VerificationCodeProperties shortKey = validProperties();
        shortKey.setHmacKey(Base64.getEncoder().encodeToString(new byte[16]));
        assertThat(validator.validate(shortKey)).anyMatch(
                violation -> violation.getPropertyPath().toString().equals("hmacKeyValid")
        );
    }

    @Test
    void deveRejeitarPeriodosInvalidos() {
        VerificationCodeProperties zeroTtl = validProperties();
        zeroTtl.setTtl(Duration.ZERO);
        assertThat(validator.validate(zeroTtl)).anyMatch(
                violation -> violation.getPropertyPath().toString().equals("ttlValid")
        );

        VerificationCodeProperties resendMaior = validProperties();
        resendMaior.setResendInterval(Duration.ofMinutes(6));
        assertThat(validator.validate(resendMaior)).anyMatch(
                violation -> violation.getPropertyPath().toString().equals("resendIntervalValid")
        );
    }

    private VerificationCodeProperties validProperties() {
        return VerificationCodeHasherTest.validProperties();
    }
}

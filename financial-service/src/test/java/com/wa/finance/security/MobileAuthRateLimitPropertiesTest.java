package com.wa.finance.security;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MobileAuthRateLimitPropertiesTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void deveAceitarLimitesPadrao() {
        MobileAuthRateLimitProperties properties = new MobileAuthRateLimitProperties();

        assertThat(properties.getPhonePerHour()).isEqualTo(5);
        assertThat(properties.getPhonePerDay()).isEqualTo(10);
        assertThat(properties.getIpPerHour()).isEqualTo(30);
        assertThat(validator.validate(properties)).isEmpty();
    }

    @Test
    void deveRejeitarLimitesNaoPositivos() {
        MobileAuthRateLimitProperties properties = new MobileAuthRateLimitProperties();
        properties.setPhonePerHour(0);
        properties.setPhonePerDay(-1);
        properties.setIpPerHour(0);

        assertThat(validator.validate(properties)).hasSize(3);
    }

    @Test
    void deveRejeitarLimiteDiarioMenorQueHorario() {
        MobileAuthRateLimitProperties properties = new MobileAuthRateLimitProperties();
        properties.setPhonePerHour(6);
        properties.setPhonePerDay(5);

        assertThat(validator.validate(properties)).anyMatch(
                violation -> violation.getPropertyPath().toString().equals("phoneDailyLimitValid")
        );
    }
}

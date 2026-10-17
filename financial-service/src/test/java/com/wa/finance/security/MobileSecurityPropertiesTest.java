package com.wa.finance.security;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class MobileSecurityPropertiesTest {

    @Test
    void deveRejeitarConfiguracaoInseguraOuIncoerente() {
        var properties = new MobileSecurityProperties();
        properties.setIssuer(" ");
        properties.setAudience("finance-tracker-mobile");
        properties.setAccessTokenTtl(Duration.ZERO);
        properties.setRefreshTokenInactivity(Duration.ofDays(30));
        properties.setSessionAbsoluteTtl(Duration.ofDays(10));

        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var violations = factory.getValidator().validate(properties);

            assertThat(violations)
                    .extracting(violation -> violation.getPropertyPath().toString())
                    .contains(
                            "issuer",
                            "accessTokenTtlValid",
                            "sessionAbsoluteTtlValid",
                            "privateKey",
                            "publicKey"
                    );
        }
    }

    @Test
    void deveRejeitarIssuerQueNaoSejaHttps() {
        var properties = new MobileSecurityProperties();
        properties.setIssuer("finance-tracker");

        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertThat(factory.getValidator().validate(properties))
                    .extracting(violation -> violation.getPropertyPath().toString())
                    .contains("issuerValid");
        }
    }
}

package com.wa.finance;

import com.wa.finance.security.MobileJwtTestKeySupport;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

public abstract class MobileSecurityIntegrationTest {

    @DynamicPropertySource
    static void mobileJwtProperties(DynamicPropertyRegistry registry) {
        MobileJwtTestKeySupport.registerProperties(registry);
        registry.add(
                "app.security.mobile.verification-code.hmac-key",
                () -> "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
        );
        registry.add(
                "app.security.mobile.auth-delivery.key",
                () -> "BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB="
        );
    }
}

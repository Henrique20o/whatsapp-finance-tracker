package com.wa.finance.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.SecureRandom;
import java.time.Clock;

@Configuration
@EnableConfigurationProperties({
        VerificationCodeProperties.class,
        MobileAuthRateLimitProperties.class
})
public class VerificationCodeConfiguration {

    @Bean
    SecureRandom verificationCodeSecureRandom() {
        return new SecureRandom();
    }

    @Bean
    Clock applicationClock() {
        return Clock.systemUTC();
    }
}

package com.whatsapp_service.security.mobile;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.SecureRandom;
import java.time.Clock;

@Configuration
@EnableConfigurationProperties(MobileAuthDeliveryProperties.class)
public class MobileAuthDeliveryConfiguration {

    @Bean
    SecureRandom mobileAuthSecureRandom() {
        return new SecureRandom();
    }

    @Bean
    Clock mobileAuthClock() {
        return Clock.systemUTC();
    }
}

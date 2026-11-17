package com.wa.finance.service;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class MobileSessionMetrics {
    private final MeterRegistry registry;

    public MobileSessionMetrics(MeterRegistry registry) { this.registry = registry; }

    public void refresh(String outcome) {
        registry.counter("mobile.auth.refresh", "outcome", outcome).increment();
    }

    public void logout(String outcome) {
        registry.counter("mobile.auth.logout", "outcome", outcome).increment();
    }
}

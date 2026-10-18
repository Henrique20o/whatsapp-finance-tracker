package com.wa.finance.service;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class MobileAuthRateLimitMetrics {

    private static final String METRIC_NAME = "mobile.auth.rate.limit.rejections";

    private final MeterRegistry meterRegistry;

    public MobileAuthRateLimitMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordRejection(RateLimitDecision decision) {
        if (decision.allowed()) {
            return;
        }
        meterRegistry.counter(
                METRIC_NAME,
                "endpoint", "codes_request",
                "scope", decision.scope().name().toLowerCase(),
                "rule", decision.rule().name().toLowerCase()
        ).increment();
    }
}

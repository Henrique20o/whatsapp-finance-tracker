package com.wa.finance.service;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class MobileAuthRateLimitMetricsTest {

    @Test
    void deveRegistrarSomenteTagsDeBaixaCardinalidade() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        MobileAuthRateLimitMetrics metrics = new MobileAuthRateLimitMetrics(registry);
        RateLimitDecision decision = RateLimitDecision.blocked(
                Instant.parse("2026-10-18T15:00:00Z"),
                Instant.parse("2026-10-18T15:01:00Z"),
                60,
                RateLimitScope.PHONE,
                RateLimitRule.RESEND
        );

        metrics.recordRejection(decision);

        var counter = registry.get("mobile.auth.rate.limit.rejections")
                .tags(
                        "endpoint", "codes_request",
                        "scope", "phone",
                        "rule", "resend"
                )
                .counter();
        assertThat(counter.count()).isEqualTo(1.0);
        assertThat(counter.getId().getTags())
                .extracting(tag -> tag.getKey())
                .containsExactlyInAnyOrder("endpoint", "scope", "rule");
    }
}

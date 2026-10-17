package com.wa.finance.security;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.Base64;

@Validated
@ConfigurationProperties(prefix = "app.security.mobile.verification-code")
public class VerificationCodeProperties {

    private static final int MINIMUM_KEY_BYTES = 32;

    @NotBlank
    private String hmacKey;

    @NotNull
    private Duration ttl;

    @NotNull
    private Duration resendInterval;

    @AssertTrue(message = "hmac-key deve ser Base64 e representar pelo menos 32 bytes")
    public boolean isHmacKeyValid() {
        if (hmacKey == null || hmacKey.isBlank()) {
            return true;
        }
        try {
            return Base64.getDecoder().decode(hmacKey).length >= MINIMUM_KEY_BYTES;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    @AssertTrue(message = "ttl deve ser maior que zero")
    public boolean isTtlValid() {
        return ttl == null || ttl.isPositive();
    }

    @AssertTrue(message = "resend-interval deve ser maior que zero e não superar ttl")
    public boolean isResendIntervalValid() {
        return resendInterval == null
                || ttl == null
                || (resendInterval.isPositive() && resendInterval.compareTo(ttl) <= 0);
    }

    byte[] decodedHmacKey() {
        return Base64.getDecoder().decode(hmacKey);
    }

    public String getHmacKey() {
        return hmacKey;
    }

    public void setHmacKey(String hmacKey) {
        this.hmacKey = hmacKey;
    }

    public Duration getTtl() {
        return ttl;
    }

    public void setTtl(Duration ttl) {
        this.ttl = ttl;
    }

    public Duration getResendInterval() {
        return resendInterval;
    }

    public void setResendInterval(Duration resendInterval) {
        this.resendInterval = resendInterval;
    }
}

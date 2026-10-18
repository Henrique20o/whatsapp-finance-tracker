package com.wa.finance.security;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.security.mobile.verification-code.rate-limit")
public class MobileAuthRateLimitProperties {

    @Min(1)
    private int phonePerHour = 5;

    @Min(1)
    private int phonePerDay = 10;

    @Min(1)
    private int ipPerHour = 30;

    @AssertTrue(message = "phone-per-day deve ser maior ou igual a phone-per-hour")
    public boolean isPhoneDailyLimitValid() {
        if (phonePerHour < 1 || phonePerDay < 1) {
            return true;
        }
        return phonePerDay >= phonePerHour;
    }

    public int getPhonePerHour() {
        return phonePerHour;
    }

    public void setPhonePerHour(int phonePerHour) {
        this.phonePerHour = phonePerHour;
    }

    public int getPhonePerDay() {
        return phonePerDay;
    }

    public void setPhonePerDay(int phonePerDay) {
        this.phonePerDay = phonePerDay;
    }

    public int getIpPerHour() {
        return ipPerHour;
    }

    public void setIpPerHour(int ipPerHour) {
        this.ipPerHour = ipPerHour;
    }
}

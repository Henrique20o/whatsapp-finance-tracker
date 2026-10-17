package com.wa.finance.security;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Locale;

@Component
public class VerificationCodeGenerator {

    static final int UPPER_BOUND = 1_000_000;

    private final SecureRandom secureRandom;

    public VerificationCodeGenerator(SecureRandom secureRandom) {
        this.secureRandom = secureRandom;
    }

    public String generate() {
        return String.format(Locale.ROOT, "%06d", secureRandom.nextInt(UPPER_BOUND));
    }
}

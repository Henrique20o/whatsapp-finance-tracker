package com.wa.finance.security;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
public class VerificationCodeHasher {

    private static final Pattern PHONE_PATTERN = Pattern.compile("[1-9][0-9]{9,14}");
    private static final Pattern CODE_PATTERN = Pattern.compile("[0-9]{6}");
    private static final Pattern HASH_PATTERN = Pattern.compile("[0-9a-fA-F]{64}");
    private static final String ALGORITHM = "HmacSHA256";

    private final MobileAuthenticationHmacKeys keys;

    public VerificationCodeHasher(MobileAuthenticationHmacKeys keys) {
        this.keys = keys;
    }

    public String hash(UUID challengeId, String normalizedPhone, String code) {
        validate(challengeId, normalizedPhone, code);
        String canonicalValue = "v1:" + challengeId + ":" + normalizedPhone + ":" + code;
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(keys.verificationCodeKey());
            return HexFormat.of().formatHex(mac.doFinal(canonicalValue.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Não foi possível proteger o código de verificação", exception);
        }
    }

    public boolean matches(String expectedHmac, UUID challengeId, String normalizedPhone, String receivedCode) {
        if (expectedHmac == null || !HASH_PATTERN.matcher(expectedHmac).matches()) {
            return false;
        }
        try {
            byte[] expected = HexFormat.of().parseHex(expectedHmac);
            byte[] actual = HexFormat.of().parseHex(hash(challengeId, normalizedPhone, receivedCode));
            return MessageDigest.isEqual(expected, actual);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private void validate(UUID challengeId, String normalizedPhone, String code) {
        if (challengeId == null
                || normalizedPhone == null
                || !PHONE_PATTERN.matcher(normalizedPhone).matches()
                || code == null
                || !CODE_PATTERN.matcher(code).matches()) {
            throw new IllegalArgumentException("Dados de verificação inválidos");
        }
    }
}

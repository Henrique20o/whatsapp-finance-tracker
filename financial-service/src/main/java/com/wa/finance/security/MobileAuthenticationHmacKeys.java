package com.wa.finance.security;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;

@Component
public class MobileAuthenticationHmacKeys {

    private static final String ALGORITHM = "HmacSHA256";

    private final SecretKeySpec verificationCodeKey;
    private final SecretKeySpec clientIpKey;

    public MobileAuthenticationHmacKeys(VerificationCodeProperties properties) {
        byte[] masterKey = properties.decodedHmacKey();
        this.verificationCodeKey = derive(masterKey, "mobile-verification-code-v1");
        this.clientIpKey = derive(masterKey, "mobile-client-ip-v1");
    }

    SecretKeySpec verificationCodeKey() {
        return verificationCodeKey;
    }

    SecretKeySpec clientIpKey() {
        return clientIpKey;
    }

    private SecretKeySpec derive(byte[] masterKey, String purpose) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(masterKey, ALGORITHM));
            return new SecretKeySpec(mac.doFinal(purpose.getBytes(StandardCharsets.UTF_8)), ALGORITHM);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Não foi possível derivar as chaves de autenticação mobile", exception);
        }
    }
}

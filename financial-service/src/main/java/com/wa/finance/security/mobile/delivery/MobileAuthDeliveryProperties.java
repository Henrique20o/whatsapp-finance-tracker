package com.wa.finance.security.mobile.delivery;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.Base64;

@Validated
@ConfigurationProperties(prefix = "app.security.mobile.auth-delivery")
public class MobileAuthDeliveryProperties {

    private static final int KEY_BYTES = 32;

    @NotBlank
    private String key;

    @NotBlank
    @Pattern(regexp = "^[A-Za-z0-9._-]{1,32}$")
    private String keyId = "v1";

    @AssertTrue(message = "key deve ser Base64 e representar exatamente 32 bytes")
    public boolean isKeyValid() {
        if (key == null || key.isBlank()) {
            return true;
        }
        try {
            return Base64.getDecoder().decode(key).length == KEY_BYTES;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    public byte[] decodedKey() {
        return Base64.getDecoder().decode(key);
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }
}

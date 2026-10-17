package com.wa.finance.security;

import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class ClientIpProtectionService {

    private static final Pattern CANONICAL_IP_PATTERN = Pattern.compile("[0-9a-fA-F:.]{2,45}");
    private static final String ALGORITHM = "HmacSHA256";

    private final MobileAuthenticationHmacKeys keys;

    public ClientIpProtectionService(MobileAuthenticationHmacKeys keys) {
        this.keys = keys;
    }

    public String hash(String canonicalIpAddress) {
        if (canonicalIpAddress == null || !CANONICAL_IP_PATTERN.matcher(canonicalIpAddress).matches()) {
            throw new IllegalArgumentException("Endereço IP canônico inválido");
        }
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(keys.clientIpKey());
            byte[] value = canonicalIpAddress.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8);
            return HexFormat.of().formatHex(mac.doFinal(value));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Não foi possível proteger o endereço IP", exception);
        }
    }
}

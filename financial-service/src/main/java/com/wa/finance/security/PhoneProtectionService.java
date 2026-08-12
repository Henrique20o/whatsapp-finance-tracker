package com.wa.finance.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class PhoneProtectionService {

    private static final String VERSION_PREFIX = "v1:";
    private static final int IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;

    private final SecretKeySpec encryptionKey;
    private final SecretKeySpec hashKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public PhoneProtectionService(@Value("${app.security.phone-key}") String encodedKey) {
        byte[] masterKey;
        try {
            masterKey = Base64.getDecoder().decode(encodedKey);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("PHONE_ENCRYPTION_KEY deve estar em Base64", exception);
        }
        if (masterKey.length != 32) {
            throw new IllegalStateException("PHONE_ENCRYPTION_KEY deve representar exatamente 32 bytes");
        }

        this.encryptionKey = new SecretKeySpec(derive(masterKey, "phone-encryption"), "AES");
        this.hashKey = new SecretKeySpec(derive(masterKey, "phone-lookup"), "HmacSHA256");
    }

    public String normalize(String phone) {
        if (phone == null) {
            throw new IllegalArgumentException("Telefone obrigatório");
        }
        String normalized = phone.replaceAll("\\D", "");
        if (normalized.length() < 8 || normalized.length() > 15) {
            throw new IllegalArgumentException("Telefone inválido");
        }
        return normalized;
    }

    public String encrypt(String phone) {
        String normalized = normalize(phone);
        byte[] iv = new byte[IV_LENGTH];
        secureRandom.nextBytes(iv);

        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] encrypted = cipher.doFinal(normalized.getBytes(StandardCharsets.UTF_8));
            byte[] payload = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);
            return VERSION_PREFIX + Base64.getEncoder().encodeToString(payload);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Não foi possível criptografar o telefone", exception);
        }
    }

    public String decrypt(String encryptedPhone) {
        if (encryptedPhone == null || !encryptedPhone.startsWith(VERSION_PREFIX)) {
            throw new IllegalArgumentException("Telefone não está no formato criptografado esperado");
        }
        byte[] payload = Base64.getDecoder().decode(encryptedPhone.substring(VERSION_PREFIX.length()));
        if (payload.length <= IV_LENGTH) {
            throw new IllegalArgumentException("Telefone criptografado inválido");
        }

        byte[] iv = java.util.Arrays.copyOfRange(payload, 0, IV_LENGTH);
        byte[] encrypted = java.util.Arrays.copyOfRange(payload, IV_LENGTH, payload.length);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Não foi possível descriptografar o telefone; confira a chave", exception);
        }
    }

    public String lookupHash(String phone) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(hashKey);
            return HexFormat.of().formatHex(mac.doFinal(normalize(phone).getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Não foi possível gerar o identificador do telefone", exception);
        }
    }

    public boolean isEncrypted(String value) {
        return value != null && value.startsWith(VERSION_PREFIX);
    }

    public String mask(String phone) {
        String normalized = normalize(phone);
        return "****" + normalized.substring(normalized.length() - 4);
    }

    private byte[] derive(byte[] masterKey, String purpose) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(masterKey, "HmacSHA256"));
            return mac.doFinal(purpose.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Não foi possível derivar a chave", exception);
        }
    }
}

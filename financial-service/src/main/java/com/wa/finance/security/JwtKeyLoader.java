package com.wa.finance.security;

import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.Signature;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

final class JwtKeyLoader {

    private static final int MINIMUM_RSA_BITS = 2048;

    private JwtKeyLoader() {
    }

    static KeyPair load(Resource privateKeyResource, Resource publicKeyResource) {
        try {
            var keyFactory = KeyFactory.getInstance("RSA");
            var privateKey = (RSAPrivateKey) keyFactory.generatePrivate(
                    new PKCS8EncodedKeySpec(readPem(privateKeyResource, "PRIVATE KEY"))
            );
            var publicKey = (RSAPublicKey) keyFactory.generatePublic(
                    new X509EncodedKeySpec(readPem(publicKeyResource, "PUBLIC KEY"))
            );

            validateStrength(privateKey, publicKey);
            validatePair(privateKey, publicKey);
            return new KeyPair(publicKey, privateKey);
        } catch (IOException | GeneralSecurityException | ClassCastException exception) {
            throw new IllegalStateException("Não foi possível carregar o par de chaves RSA do JWT mobile", exception);
        }
    }

    private static byte[] readPem(Resource resource, String type) throws IOException {
        if (resource == null || !resource.exists() || !resource.isReadable()) {
            throw new IllegalStateException("Chave JWT mobile ausente ou não legível: " + resource);
        }

        String pem = resource.getContentAsString(StandardCharsets.US_ASCII);
        String begin = "-----BEGIN " + type + "-----";
        String end = "-----END " + type + "-----";
        if (!pem.contains(begin) || !pem.contains(end)) {
            throw new IllegalStateException("Formato PEM inválido para " + type);
        }

        String encoded = pem
                .replace(begin, "")
                .replace(end, "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(encoded);
    }

    private static void validateStrength(RSAPrivateKey privateKey, RSAPublicKey publicKey) {
        if (privateKey.getModulus().bitLength() < MINIMUM_RSA_BITS
                || publicKey.getModulus().bitLength() < MINIMUM_RSA_BITS) {
            throw new IllegalStateException("As chaves RSA do JWT mobile devem possuir ao menos 2048 bits");
        }
    }

    private static void validatePair(RSAPrivateKey privateKey, RSAPublicKey publicKey)
            throws GeneralSecurityException {
        byte[] message = "mobile-jwt-key-validation".getBytes(StandardCharsets.UTF_8);
        var signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(message);
        byte[] signedMessage = signature.sign();

        signature.initVerify(publicKey);
        signature.update(message);
        if (!signature.verify(signedMessage)) {
            throw new IllegalStateException("As chaves pública e privada do JWT mobile não formam um par");
        }
    }
}

package com.wa.finance.security;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.test.context.DynamicPropertyRegistry;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPairGenerator;
import java.util.Base64;

public final class MobileJwtTestKeySupport {

    private static final TestKeyResources KEYS = generateKeys(2048);

    private MobileJwtTestKeySupport() {
    }

    public static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("app.security.mobile.private-key", () -> resourceLocation(KEYS.privateKey()));
        registry.add("app.security.mobile.public-key", () -> resourceLocation(KEYS.publicKey()));
    }

    public static TestKeyResources keys() {
        return KEYS;
    }

    public static TestKeyResources generateKeys(int bits) {
        try {
            var generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(bits);
            var keyPair = generator.generateKeyPair();
            Path directory = Files.createTempDirectory("finance-mobile-jwt-test-");
            Path privateKey = directory.resolve("private.pem");
            Path publicKey = directory.resolve("public.pem");
            Files.writeString(privateKey, pem("PRIVATE KEY", keyPair.getPrivate().getEncoded()), StandardCharsets.US_ASCII);
            Files.writeString(publicKey, pem("PUBLIC KEY", keyPair.getPublic().getEncoded()), StandardCharsets.US_ASCII);
            directory.toFile().deleteOnExit();
            privateKey.toFile().deleteOnExit();
            publicKey.toFile().deleteOnExit();
            return new TestKeyResources(new FileSystemResource(privateKey), new FileSystemResource(publicKey));
        } catch (Exception exception) {
            throw new IllegalStateException("Falha ao gerar chaves RSA para os testes", exception);
        }
    }

    private static String pem(String type, byte[] encoded) {
        return "-----BEGIN " + type + "-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII)).encodeToString(encoded)
                + "\n-----END " + type + "-----\n";
    }

    private static String resourceLocation(Resource resource) {
        try {
            return resource.getURI().toString();
        } catch (IOException exception) {
            throw new IllegalStateException("Não foi possível obter a localização da chave de teste", exception);
        }
    }

    public record TestKeyResources(Resource privateKey, Resource publicKey) {

        public TestKeyResources {
            try {
                privateKey.getURI();
                publicKey.getURI();
            } catch (IOException exception) {
                throw new IllegalArgumentException("Recursos de chave inválidos", exception);
            }
        }
    }
}

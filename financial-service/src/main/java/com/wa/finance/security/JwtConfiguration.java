package com.wa.finance.security;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
@EnableConfigurationProperties(MobileSecurityProperties.class)
public class JwtConfiguration {

    @Bean
    KeyPair mobileJwtKeyPair(MobileSecurityProperties properties) {
        return JwtKeyLoader.load(properties.getPrivateKey(), properties.getPublicKey());
    }

    @Bean
    JwtEncoder mobileJwtEncoder(KeyPair mobileJwtKeyPair, MobileSecurityProperties properties) {
        var rsaKey = new RSAKey.Builder((RSAPublicKey) mobileJwtKeyPair.getPublic())
                .privateKey((RSAPrivateKey) mobileJwtKeyPair.getPrivate())
                .keyID(properties.getActiveKeyId())
                .build();
        ImmutableJWKSet<SecurityContext> jwkSource = new ImmutableJWKSet<>(new JWKSet(rsaKey));
        return new NimbusJwtEncoder(jwkSource);
    }

    JwtEncoder mobileJwtEncoder(KeyPair keyPair) {
        var rsaKey = new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                .privateKey((RSAPrivateKey) keyPair.getPrivate())
                .keyID("v1")
                .build();
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(rsaKey)));
    }

    @Bean
    JwtDecoder mobileJwtDecoder(
            KeyPair mobileJwtKeyPair,
            MobileSecurityProperties properties,
            Clock clock
    ) {
        Map<String, JwtDecoder> decoders = new LinkedHashMap<>();
        validateKeyId(properties.getActiveKeyId());
        properties.getPublicKeys().forEach((kid, resource) -> {
            validateKeyId(kid);
            decoders.put(kid, decoderFor(JwtKeyLoader.loadPublic(resource), properties, clock));
        });
        decoders.put(properties.getActiveKeyId(), decoderFor(
                (RSAPublicKey) mobileJwtKeyPair.getPublic(), properties, clock));
        return token -> {
            try {
                String kid = SignedJWT.parse(token).getHeader().getKeyID();
                JwtDecoder selected = decoders.get(kid);
                if (kid == null || selected == null) {
                    throw new BadJwtException("Chave de assinatura JWT desconhecida");
                }
                return selected.decode(token);
            } catch (java.text.ParseException exception) {
                throw new BadJwtException("JWT inválido", exception);
            }
        };
    }

    // Kept as a convenience for focused configuration callers.
    JwtDecoder mobileJwtDecoder(KeyPair keyPair, MobileSecurityProperties properties) {
        return mobileJwtDecoder(keyPair, properties, Clock.systemUTC());
    }

    private JwtDecoder decoderFor(RSAPublicKey publicKey, MobileSecurityProperties properties, Clock clock) {
        var decoder = NimbusJwtDecoder.withPublicKey(publicKey)
                .signatureAlgorithm(SignatureAlgorithm.RS256)
                .build();
        JwtTimestampValidator timestampValidator = new JwtTimestampValidator(properties.getJwtClockSkew());
        timestampValidator.setClock(clock);
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                timestampValidator,
                new JwtIssuerValidator(properties.getIssuer()),
                audienceValidator(properties.getAudience()),
                requiredClaimsValidator()
        ));
        return decoder;
    }

    private OAuth2TokenValidator<Jwt> audienceValidator(String audience) {
        return jwt -> jwt.getAudience() != null && jwt.getAudience().contains(audience)
                ? OAuth2TokenValidatorResult.success()
                : failure("Audiência JWT inválida");
    }

    private OAuth2TokenValidator<Jwt> requiredClaimsValidator() {
        return jwt -> {
            Object subject = jwt.getClaims().get("sub");
            Object sessionId = jwt.getClaims().get("sid");
            Object tokenId = jwt.getClaims().get("jti");
            Object issuedAt = jwt.getClaims().get("iat");
            Object expiresAt = jwt.getClaims().get("exp");
            if (!(subject instanceof String userId) || !userId.matches("[1-9][0-9]*")
                    || !(sessionId instanceof String sid) || !isUuid(sid)
                    || !(tokenId instanceof String jti) || jti.isBlank()
                    || !(issuedAt instanceof java.time.Instant)
                    || !(expiresAt instanceof java.time.Instant)) {
                return failure("Claims JWT obrigatórias ausentes ou inválidas");
            }
            return OAuth2TokenValidatorResult.success();
        };
    }

    private static OAuth2TokenValidatorResult failure(String description) {
        return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", description, null));
    }

    private static boolean isUuid(String value) {
        try {
            java.util.UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static void validateKeyId(String kid) {
        if (kid == null || !kid.matches("[A-Za-z0-9._-]{1,64}")) {
            throw new IllegalStateException("Cada kid de chave JWT mobile deve ter entre 1 e 64 caracteres seguros");
        }
    }
}

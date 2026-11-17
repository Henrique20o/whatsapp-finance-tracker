package com.wa.finance.service;

import com.wa.finance.security.MobileSecurityProperties;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MobileAccessTokenServiceTest {
    @Test
    void emiteClaimsObrigatoriasEAssinaturaComKidAtivo() throws Exception {
        Instant now = Instant.parse("2026-11-17T12:00:00Z");
        var properties = new MobileSecurityProperties();
        properties.setIssuer("https://api.example.test");
        properties.setAudience("finance-tracker-mobile");
        properties.setActiveKeyId("v1");
        properties.setAccessTokenTtl(Duration.ofMinutes(15));
        var generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        var keyPair = generator.generateKeyPair();
        var signingKey = new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                .privateKey((RSAPrivateKey) keyPair.getPrivate())
                .keyID(properties.getActiveKeyId())
                .build();
        ImmutableJWKSet<SecurityContext> jwkSource = new ImmutableJWKSet<>(new JWKSet(signingKey));
        JwtEncoder encoder = new NimbusJwtEncoder(jwkSource);
        var service = new MobileAccessTokenService(encoder, properties,
                Clock.fixed(now, ZoneOffset.UTC));
        UUID familyId = UUID.randomUUID();

        var result = service.emit(987L, familyId);
        var token = SignedJWT.parse(result.value());

        assertThat(token.getHeader().getAlgorithm().getName()).isEqualTo("RS256");
        assertThat(token.getHeader().getKeyID()).isEqualTo("v1");
        assertThat(token.getJWTClaimsSet().getSubject()).isEqualTo("987");
        assertThat(token.getJWTClaimsSet().getIssuer()).isEqualTo("https://api.example.test");
        assertThat(token.getJWTClaimsSet().getAudience()).containsExactly("finance-tracker-mobile");
        assertThat(token.getJWTClaimsSet().getClaim("sid")).isEqualTo(familyId.toString());
        assertThat(token.getJWTClaimsSet().getJWTID()).isNotBlank();
        assertThat(token.getJWTClaimsSet().getIssueTime().toInstant()).isEqualTo(now);
        assertThat(token.getJWTClaimsSet().getExpirationTime().toInstant()).isEqualTo(now.plusSeconds(900));
        assertThat(result.expiresInSeconds()).isEqualTo(900);
        assertThat(token.getJWTClaimsSet().getClaims()).doesNotContainKeys("telefone", "nome", "email");
    }

}

package com.wa.finance.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.core.io.FileSystemResource;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtConfigurationTest {

    private final JwtConfiguration configuration = new JwtConfiguration();

    @Test
    void deveAssinarEValidarTokenComIssuerEAudienceEsperados() {
        var properties = properties(MobileJwtTestKeySupport.keys());
        var keyPair = configuration.mobileJwtKeyPair(properties);
        var encoder = configuration.mobileJwtEncoder(keyPair);
        var decoder = configuration.mobileJwtDecoder(keyPair, properties);

        Instant now = Instant.now();
        var claims = JwtClaimsSet.builder()
                .issuer(properties.getIssuer())
                .audience(List.of(properties.getAudience()))
                .subject("123")
                .issuedAt(now)
                .expiresAt(now.plus(properties.getAccessTokenTtl()))
                .id(java.util.UUID.randomUUID().toString())
                .claim("sid", java.util.UUID.randomUUID().toString())
                .build();

        String token = encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
        var decoded = decoder.decode(token);

        assertThat(decoded.getSubject()).isEqualTo("123");
        assertThat(decoded.getIssuer().toString()).isEqualTo(properties.getIssuer());
        assertThat(decoded.getAudience()).containsExactly(properties.getAudience());
    }

    @Test
    void deveRejeitarAudienceIncorreta() {
        var properties = properties(MobileJwtTestKeySupport.keys());
        var keyPair = configuration.mobileJwtKeyPair(properties);
        var encoder = configuration.mobileJwtEncoder(keyPair);
        var decoder = configuration.mobileJwtDecoder(keyPair, properties);
        Instant now = Instant.now();
        var claims = JwtClaimsSet.builder()
                .issuer(properties.getIssuer())
                .audience(List.of("outro-aplicativo"))
                .subject("123")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(60))
                .id(java.util.UUID.randomUUID().toString())
                .claim("sid", java.util.UUID.randomUUID().toString())
                .build();

        String token = encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtValidationException.class);
    }

    @Test
    void deveRejeitarIssuerIncorreto() {
        var properties = properties(MobileJwtTestKeySupport.keys());
        var keyPair = configuration.mobileJwtKeyPair(properties);
        var encoder = configuration.mobileJwtEncoder(keyPair);
        var decoder = configuration.mobileJwtDecoder(keyPair, properties);
        Instant now = Instant.now();
        var claims = JwtClaimsSet.builder()
                .issuer("outro-emissor")
                .audience(List.of(properties.getAudience()))
                .subject("123")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(60))
                .id(java.util.UUID.randomUUID().toString())
                .claim("sid", java.util.UUID.randomUUID().toString())
                .build();

        String token = encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtValidationException.class);
    }

    @Test
    void deveRejeitarTokenAssinadoPorOutraChave() {
        var properties = properties(MobileJwtTestKeySupport.keys());
        var trustedPair = configuration.mobileJwtKeyPair(properties);
        var decoder = configuration.mobileJwtDecoder(trustedPair, properties);
        var untrustedProperties = properties(MobileJwtTestKeySupport.generateKeys(2048));
        var untrustedEncoder = configuration.mobileJwtEncoder(configuration.mobileJwtKeyPair(untrustedProperties));
        Instant now = Instant.now();
        var claims = JwtClaimsSet.builder()
                .issuer(properties.getIssuer())
                .audience(List.of(properties.getAudience()))
                .subject("123")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(60))
                .id(java.util.UUID.randomUUID().toString())
                .claim("sid", java.util.UUID.randomUUID().toString())
                .build();

        String token = untrustedEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void deveRejeitarTokenExpirado() {
        var properties = properties(MobileJwtTestKeySupport.keys());
        var keyPair = configuration.mobileJwtKeyPair(properties);
        var encoder = configuration.mobileJwtEncoder(keyPair);
        var decoder = configuration.mobileJwtDecoder(keyPair, properties);
        Instant past = Instant.now().minusSeconds(120);
        var claims = JwtClaimsSet.builder()
                .issuer(properties.getIssuer())
                .audience(List.of(properties.getAudience()))
                .subject("123")
                .issuedAt(past)
                .expiresAt(past.plusSeconds(30))
                .id(java.util.UUID.randomUUID().toString())
                .claim("sid", java.util.UUID.randomUUID().toString())
                .build();

        String token = encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtValidationException.class);
    }

    @Test
    void deveRejeitarClaimsDeIdentidadeAusentesOuMalformed() {
        var properties = properties(MobileJwtTestKeySupport.keys());
        var keyPair = configuration.mobileJwtKeyPair(properties);
        var encoder = configuration.mobileJwtEncoder(keyPair);
        var decoder = configuration.mobileJwtDecoder(keyPair, properties);
        Instant now = Instant.now();
        var missingSession = JwtClaimsSet.builder()
                .issuer(properties.getIssuer()).audience(List.of(properties.getAudience()))
                .subject("123").issuedAt(now).expiresAt(now.plusSeconds(60))
                .id(java.util.UUID.randomUUID().toString()).build();
        var malformedSubject = JwtClaimsSet.builder()
                .issuer(properties.getIssuer()).audience(List.of(properties.getAudience()))
                .subject("user-123").issuedAt(now).expiresAt(now.plusSeconds(60))
                .id(java.util.UUID.randomUUID().toString())
                .claim("sid", java.util.UUID.randomUUID().toString()).build();

        assertThatThrownBy(() -> decoder.decode(encoder.encode(JwtEncoderParameters.from(missingSession))
                .getTokenValue())).isInstanceOf(JwtValidationException.class);
        assertThatThrownBy(() -> decoder.decode(encoder.encode(JwtEncoderParameters.from(malformedSubject))
                .getTokenValue())).isInstanceOf(JwtValidationException.class);
    }

    @Test
    void aceitaChavePublicaAnteriorAteExpirarOsTokensAssinadosPorEla() {
        var oldResources = MobileJwtTestKeySupport.generateKeys(2048);
        var oldProperties = properties(oldResources);
        var oldPair = configuration.mobileJwtKeyPair(oldProperties);
        var currentProperties = properties(MobileJwtTestKeySupport.generateKeys(2048));
        currentProperties.setActiveKeyId("v2");
        currentProperties.setPublicKeys(Map.of("v1", oldResources.publicKey()));
        var decoder = configuration.mobileJwtDecoder(
                configuration.mobileJwtKeyPair(currentProperties), currentProperties);
        var oldEncoder = configuration.mobileJwtEncoder(oldPair);
        Instant now = Instant.now();
        var claims = JwtClaimsSet.builder()
                .issuer(currentProperties.getIssuer())
                .audience(List.of(currentProperties.getAudience()))
                .subject("123")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(60))
                .id(java.util.UUID.randomUUID().toString())
                .claim("sid", java.util.UUID.randomUUID().toString())
                .build();
        String oldToken = oldEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(SignatureAlgorithm.RS256).keyId("v1").build(), claims)).getTokenValue();

        assertThat(decoder.decode(oldToken).getSubject()).isEqualTo("123");
    }

    @Test
    void deveRejeitarChaveRsaFraca() {
        var weakKeys = MobileJwtTestKeySupport.generateKeys(1024);

        assertThatThrownBy(() -> JwtKeyLoader.load(weakKeys.privateKey(), weakKeys.publicKey()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("2048 bits");
    }

    @Test
    void deveRejeitarChavesQueNaoFormamUmPar() {
        var first = MobileJwtTestKeySupport.generateKeys(2048);
        var second = MobileJwtTestKeySupport.generateKeys(2048);

        assertThatThrownBy(() -> JwtKeyLoader.load(first.privateKey(), second.publicKey()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("não formam um par");
    }

    @Test
    void deveRejeitarArquivoDeChaveAusente() {
        var missing = new FileSystemResource("arquivo-que-nao-existe.pem");

        assertThatThrownBy(() -> JwtKeyLoader.load(missing, missing))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ausente ou não legível");
    }

    private MobileSecurityProperties properties(MobileJwtTestKeySupport.TestKeyResources keys) {
        var properties = new MobileSecurityProperties();
        properties.setIssuer("https://api.whatsapp-finance-tracker.local");
        properties.setAudience("finance-tracker-mobile");
        properties.setAccessTokenTtl(Duration.ofMinutes(15));
        properties.setRefreshTokenInactivity(Duration.ofDays(30));
        properties.setSessionAbsoluteTtl(Duration.ofDays(90));
        properties.setPrivateKey(keys.privateKey());
        properties.setPublicKey(keys.publicKey());
        return properties;
    }
}

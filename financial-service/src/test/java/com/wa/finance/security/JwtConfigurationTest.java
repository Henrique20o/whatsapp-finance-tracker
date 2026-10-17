package com.wa.finance.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.core.io.FileSystemResource;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

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
                .subject("usuario-123")
                .issuedAt(now)
                .expiresAt(now.plus(properties.getAccessTokenTtl()))
                .build();

        String token = encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
        var decoded = decoder.decode(token);

        assertThat(decoded.getSubject()).isEqualTo("usuario-123");
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
                .subject("usuario-123")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(60))
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
                .subject("usuario-123")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(60))
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
                .subject("usuario-123")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(60))
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
                .subject("usuario-123")
                .issuedAt(past)
                .expiresAt(past.plusSeconds(30))
                .build();

        String token = encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtValidationException.class);
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

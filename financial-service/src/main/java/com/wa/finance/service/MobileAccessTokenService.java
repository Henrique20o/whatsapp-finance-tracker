package com.wa.finance.service;

import com.wa.finance.security.MobileSecurityProperties;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class MobileAccessTokenService {

    private final JwtEncoder jwtEncoder;
    private final MobileSecurityProperties properties;
    private final Clock clock;

    public MobileAccessTokenService(
            JwtEncoder jwtEncoder,
            MobileSecurityProperties properties,
            Clock clock
    ) {
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
        this.clock = clock;
    }

    public AccessToken emit(Long usuarioId, UUID sessaoId) {
        if (usuarioId == null || usuarioId <= 0 || sessaoId == null) {
            throw new IllegalArgumentException("Identidade da sessão mobile inválida");
        }
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(properties.getAccessTokenTtl());
        var claims = JwtClaimsSet.builder()
                .issuer(properties.getIssuer())
                .audience(List.of(properties.getAudience()))
                .subject(usuarioId.toString())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .id(UUID.randomUUID().toString())
                .claim("sid", sessaoId.toString())
                .build();
        var headers = JwsHeader.with(SignatureAlgorithm.RS256)
                .type("JWT")
                .keyId(properties.getActiveKeyId())
                .build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(headers, claims)).getTokenValue();
        return new AccessToken(token, properties.getAccessTokenTtl().toSeconds(), expiresAt);
    }

    public record AccessToken(String value, long expiresInSeconds, Instant expiresAt) {
    }
}

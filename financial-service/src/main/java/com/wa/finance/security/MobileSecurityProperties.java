package com.wa.finance.security;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.net.URI;

@Validated
@ConfigurationProperties(prefix = "app.security.mobile")
public class MobileSecurityProperties {

    @NotBlank
    private String issuer;

    @NotBlank
    private String audience;

    @NotNull
    private Duration accessTokenTtl;

    @NotNull
    private Duration refreshTokenInactivity;

    @NotNull
    private Duration sessionAbsoluteTtl;

    @NotNull
    private Resource privateKey;

    @NotNull
    private Resource publicKey;

    @AssertTrue(message = "access-token-ttl deve ser maior que zero")
    public boolean isAccessTokenTtlValid() {
        return accessTokenTtl == null || accessTokenTtl.isPositive();
    }

    @AssertTrue(message = "issuer deve ser uma URI HTTPS absoluta")
    public boolean isIssuerValid() {
        if (issuer == null || issuer.isBlank()) {
            return true;
        }
        try {
            URI uri = URI.create(issuer);
            return uri.isAbsolute() && "https".equalsIgnoreCase(uri.getScheme());
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    @AssertTrue(message = "refresh-token-inactivity deve ser maior que zero")
    public boolean isRefreshTokenInactivityValid() {
        return refreshTokenInactivity == null || refreshTokenInactivity.isPositive();
    }

    @AssertTrue(message = "session-absolute-ttl deve ser maior ou igual à inatividade do refresh token")
    public boolean isSessionAbsoluteTtlValid() {
        return sessionAbsoluteTtl == null
                || refreshTokenInactivity == null
                || sessionAbsoluteTtl.compareTo(refreshTokenInactivity) >= 0;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    public Duration getAccessTokenTtl() {
        return accessTokenTtl;
    }

    public void setAccessTokenTtl(Duration accessTokenTtl) {
        this.accessTokenTtl = accessTokenTtl;
    }

    public Duration getRefreshTokenInactivity() {
        return refreshTokenInactivity;
    }

    public void setRefreshTokenInactivity(Duration refreshTokenInactivity) {
        this.refreshTokenInactivity = refreshTokenInactivity;
    }

    public Duration getSessionAbsoluteTtl() {
        return sessionAbsoluteTtl;
    }

    public void setSessionAbsoluteTtl(Duration sessionAbsoluteTtl) {
        this.sessionAbsoluteTtl = sessionAbsoluteTtl;
    }

    public Resource getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(Resource privateKey) {
        this.privateKey = privateKey;
    }

    public Resource getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(Resource publicKey) {
        this.publicKey = publicKey;
    }
}

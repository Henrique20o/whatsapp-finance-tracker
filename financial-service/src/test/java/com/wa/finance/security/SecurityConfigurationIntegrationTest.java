package com.wa.finance.security;

import com.wa.finance.MobileSecurityIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.security.phone-key=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        "app.security.internal-api-key=chave-interna-financeiro-de-teste-123456",
        "spring.flyway.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:security-context;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "management.health.rabbit.enabled=false",
        "app.outbox.enabled=false"
})
@AutoConfigureMockMvc
@Import(SecurityConfigurationIntegrationTest.SecurityProbeConfiguration.class)
class SecurityConfigurationIntegrationTest extends MobileSecurityIntegrationTest {

    private static final String API_KEY = "chave-interna-financeiro-de-teste-123456";
    private static final String MOBILE_PROBE = "/mobile/v1/security-probe";
    private static final String INTERNAL_PROBE = "/v1/security-probe";
    private static final String UNKNOWN_PROBE = "/outside-security-probe";
    private static final String UUID_PATTERN =
            "[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Test
    void deveManterHealthcheckPublicoESemSessaoHttp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(cookie().doesNotExist("JSESSIONID"));

        mockMvc.perform(get("/actuator"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void deveLiberarSomentePostNasRotasPublicasDeAutenticacao() throws Exception {
        List<String> publicPaths = List.of(
                "/mobile/v1/auth/codes/verify",
                "/mobile/v1/auth/refresh",
                "/mobile/v1/auth/logout"
        );

        for (String path : publicPaths) {
            mockMvc.perform(post(path))
                    .andExpect(status().isNoContent())
                    .andExpect(cookie().doesNotExist("JSESSIONID"));
        }

        mockMvc.perform(post("/mobile/v1/auth/codes")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        mockMvc.perform(get("/mobile/v1/auth/codes"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deveExigirJwtNaRotaMobileProtegida() throws Exception {
        mockMvc.perform(get(MOBILE_PROBE))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.path").value(MOBILE_PROBE))
                .andExpect(jsonPath("$.correlationId", matchesPattern(UUID_PATTERN)))
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(header().string(
                        SecurityErrorResponseWriter.CORRELATION_ID_HEADER,
                        matchesPattern(UUID_PATTERN)
                ))
                .andExpect(cookie().doesNotExist("JSESSIONID"));
    }

    @Test
    void deveAceitarJwtValidoNaRotaMobileProtegida() throws Exception {
        mockMvc.perform(get(MOBILE_PROBE)
                        .header(HttpHeaders.AUTHORIZATION, bearer(validAccessToken())))
                .andExpect(status().isNoContent())
                .andExpect(cookie().doesNotExist("JSESSIONID"));
    }

    @Test
    void deveRejeitarApiKeyComoCredencialMobile() throws Exception {
        mockMvc.perform(get(MOBILE_PROBE)
                        .header(InternalApiKeyFilter.HEADER_NAME, API_KEY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void deveRejeitarJwtInvalidoOuExpiradoNaRotaMobile() throws Exception {
        mockMvc.perform(get(MOBILE_PROBE)
                        .header(HttpHeaders.AUTHORIZATION, bearer("token-invalido")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_ACCESS_TOKEN"))
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer error=\"invalid_token\""));

        mockMvc.perform(get(MOBILE_PROBE)
                        .header(HttpHeaders.AUTHORIZATION, bearer(expiredAccessToken())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_ACCESS_TOKEN"));
    }

    @Test
    void deveExigirApiKeyNaRotaInterna() throws Exception {
        mockMvc.perform(get(INTERNAL_PROBE))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("unauthorized"))
                .andExpect(cookie().doesNotExist("JSESSIONID"));

        mockMvc.perform(get(INTERNAL_PROBE)
                        .header(InternalApiKeyFilter.HEADER_NAME, "chave-incorreta-com-mais-de-32-caracteres"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("unauthorized"));
    }

    @Test
    void deveAceitarApiKeyNaRotaInterna() throws Exception {
        mockMvc.perform(get(INTERNAL_PROBE)
                        .header(InternalApiKeyFilter.HEADER_NAME, API_KEY))
                .andExpect(status().isNoContent())
                .andExpect(cookie().doesNotExist("JSESSIONID"));
    }

    @Test
    void deveRejeitarJwtComoCredencialInterna() throws Exception {
        mockMvc.perform(get(INTERNAL_PROBE)
                        .header(HttpHeaders.AUTHORIZATION, bearer(validAccessToken())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("unauthorized"));
    }

    @Test
    void deveNegarRotaDesconhecidaComOuSemCredenciais() throws Exception {
        mockMvc.perform(get(UNKNOWN_PROBE))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        mockMvc.perform(get(UNKNOWN_PROBE)
                        .header(InternalApiKeyFilter.HEADER_NAME, API_KEY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        mockMvc.perform(get(UNKNOWN_PROBE)
                        .header(HttpHeaders.AUTHORIZATION, bearer(validAccessToken())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    private String validAccessToken() {
        Instant now = Instant.now();
        return encodeToken(now, now.plusSeconds(900));
    }

    private String expiredAccessToken() {
        Instant issuedAt = Instant.now().minusSeconds(3600);
        return encodeToken(issuedAt, issuedAt.plusSeconds(60));
    }

    private String encodeToken(Instant issuedAt, Instant expiresAt) {
        var claims = JwtClaimsSet.builder()
                .issuer("https://api.whatsapp-finance-tracker.local")
                .audience(List.of("finance-tracker-mobile"))
                .subject("usuario-123")
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class SecurityProbeConfiguration {

        @Bean
        SecurityProbeController securityProbeController() {
            return new SecurityProbeController();
        }
    }

    @RestController
    static class SecurityProbeController {

        @GetMapping(MOBILE_PROBE)
        ResponseEntity<Void> mobileProtected() {
            return ResponseEntity.noContent().build();
        }

        @GetMapping(INTERNAL_PROBE)
        ResponseEntity<Void> internalProtected() {
            return ResponseEntity.noContent().build();
        }

        @PostMapping({
                "/mobile/v1/auth/codes/verify",
                "/mobile/v1/auth/refresh",
                "/mobile/v1/auth/logout"
        })
        ResponseEntity<Void> publicAuthenticationRoute() {
            return ResponseEntity.noContent().build();
        }

        @GetMapping(UNKNOWN_PROBE)
        ResponseEntity<Void> unknown() {
            return ResponseEntity.noContent().build();
        }
    }
}

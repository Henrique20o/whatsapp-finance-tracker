package com.wa.finance.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class InternalApiKeyFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Internal-Api-Key";

    private final byte[] expectedApiKey;

    public InternalApiKeyFilter(@Value("${app.security.internal-api-key}") String apiKey) {
        if (apiKey == null || apiKey.length() < 32) {
            throw new IllegalStateException("FINANCIAL_SERVICE_API_KEY deve possuir pelo menos 32 caracteres");
        }
        this.expectedApiKey = apiKey.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/actuator/health");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String receivedApiKey = request.getHeader(HEADER_NAME);
        boolean authenticated = receivedApiKey != null && MessageDigest.isEqual(
                expectedApiKey,
                receivedApiKey.getBytes(StandardCharsets.UTF_8)
        );

        if (!authenticated) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\":\"unauthorized\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}

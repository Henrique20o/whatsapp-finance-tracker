package com.wa.finance.web.mobile;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MobileCorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Correlation-Id";
    public static final String REQUEST_ATTRIBUTE = MobileCorrelationIdFilter.class.getName() + ".correlationId";
    public static final String MDC_KEY = "correlationId";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.equals("/mobile/v1") && !path.startsWith("/mobile/v1/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String correlationId = UUID.randomUUID().toString();
        request.setAttribute(REQUEST_ATTRIBUTE, correlationId);
        response.setHeader(HEADER_NAME, correlationId);
        response.setHeader("Cache-Control", "no-store");
        MDC.put(MDC_KEY, correlationId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    public static String getOrCreate(HttpServletRequest request) {
        Object current = request.getAttribute(REQUEST_ATTRIBUTE);
        if (current instanceof String correlationId && !correlationId.isBlank()) {
            return correlationId;
        }
        String correlationId = UUID.randomUUID().toString();
        request.setAttribute(REQUEST_ATTRIBUTE, correlationId);
        return correlationId;
    }
}

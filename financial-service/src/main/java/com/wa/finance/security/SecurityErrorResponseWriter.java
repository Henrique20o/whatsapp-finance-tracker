package com.wa.finance.security;

import com.wa.finance.dto.mobile.auth.MobileErrorResponse;
import com.wa.finance.web.mobile.MobileCorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Clock;
import java.time.OffsetDateTime;

@Component
public class SecurityErrorResponseWriter {

    static final String CORRELATION_ID_HEADER = MobileCorrelationIdFilter.HEADER_NAME;

    private final ObjectMapper objectMapper;
    private final Clock clock;

    public SecurityErrorResponseWriter(ObjectMapper objectMapper, Clock clock) {
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public void write(
            HttpServletRequest request,
            HttpServletResponse response,
            int status,
            String code,
            String message
    ) throws IOException {
        String correlationId = MobileCorrelationIdFilter.getOrCreate(request);
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader(CORRELATION_ID_HEADER, correlationId);
        objectMapper.writeValue(response.getOutputStream(), new MobileErrorResponse(
                code,
                message,
                OffsetDateTime.now(clock),
                request.getRequestURI(),
                correlationId
        ));
    }
}

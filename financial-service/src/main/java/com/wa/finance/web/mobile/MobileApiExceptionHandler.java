package com.wa.finance.web.mobile;

import com.wa.finance.controller.mobile.MobileAuthController;
import com.wa.finance.dto.mobile.auth.MobileErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.TransactionException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.time.Clock;
import java.time.OffsetDateTime;

@RestControllerAdvice(assignableTypes = MobileAuthController.class)
public class MobileApiExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(MobileApiExceptionHandler.class);

    private final Clock clock;

    public MobileApiExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<MobileErrorResponse> invalidRequest(
            Exception exception,
            HttpServletRequest request
    ) {
        return error(
                request,
                HttpStatus.BAD_REQUEST,
                "INVALID_REQUEST",
                "Os dados enviados são inválidos"
        );
    }

    @ExceptionHandler({InvalidClientAddressException.class, DataAccessException.class, TransactionException.class})
    public ResponseEntity<MobileErrorResponse> unavailable(
            Exception exception,
            HttpServletRequest request
    ) {
        logFailure(request, exception);
        return serviceUnavailable(request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<MobileErrorResponse> unexpected(
            Exception exception,
            HttpServletRequest request
    ) {
        logFailure(request, exception);
        return serviceUnavailable(request);
    }

    private ResponseEntity<MobileErrorResponse> serviceUnavailable(HttpServletRequest request) {
        return error(
                request,
                HttpStatus.SERVICE_UNAVAILABLE,
                "SERVICE_UNAVAILABLE",
                "Não foi possível processar a solicitação agora"
        );
    }

    private ResponseEntity<MobileErrorResponse> error(
            HttpServletRequest request,
            HttpStatus status,
            String code,
            String message
    ) {
        String correlationId = MobileCorrelationIdFilter.getOrCreate(request);
        return ResponseEntity.status(status)
                .cacheControl(CacheControl.noStore())
                .header(MobileCorrelationIdFilter.HEADER_NAME, correlationId)
                .body(new MobileErrorResponse(
                        code,
                        message,
                        OffsetDateTime.now(clock),
                        request.getRequestURI(),
                        correlationId
                ));
    }

    private void logFailure(HttpServletRequest request, Exception exception) {
        LOGGER.error(
                "Falha ao solicitar código mobile correlationId={} type={}",
                MobileCorrelationIdFilter.getOrCreate(request),
                exception.getClass().getSimpleName()
        );
    }
}

package com.wa.finance.web.mobile;

import com.wa.finance.controller.mobile.MobileAuthController;
import com.wa.finance.dto.mobile.auth.MobileErrorResponse;
import com.wa.finance.service.RateLimitExceededException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.TransactionException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.time.Clock;
import java.time.OffsetDateTime;

@RestControllerAdvice(basePackages = "com.wa.finance.controller.mobile")
public class MobileApiExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(MobileApiExceptionHandler.class);

    private final Clock clock;

    public MobileApiExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class,
            MissingRequestHeaderException.class, MethodArgumentTypeMismatchException.class})
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

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<MobileErrorResponse> responseStatus(
            ResponseStatusException exception, HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.resolve(exception.getStatusCode().value());
        if (status == null || status.is5xxServerError()) status = HttpStatus.SERVICE_UNAVAILABLE;
        String code = switch (status) {
            case BAD_REQUEST -> "INVALID_REQUEST";
            case NOT_FOUND -> "RESOURCE_NOT_FOUND";
            case UNPROCESSABLE_ENTITY -> "BUSINESS_RULE_VIOLATION";
            case CONFLICT -> "CONFLICT";
            default -> "REQUEST_REJECTED";
        };
        String message = switch (status) {
            case BAD_REQUEST -> "Os dados enviados são inválidos";
            case NOT_FOUND -> "O recurso solicitado não foi encontrado";
            case UNPROCESSABLE_ENTITY -> "A solicitação não atende às regras de negócio";
            case CONFLICT -> "A solicitação conflita com um registro existente";
            default -> "Não foi possível processar a solicitação";
        };
        return error(request, status, code, message);
    }

    @ExceptionHandler(InvalidVerificationCodeException.class)
    public ResponseEntity<MobileErrorResponse> invalidOrExpiredCode(
            InvalidVerificationCodeException exception,
            HttpServletRequest request
    ) {
        return error(
                request,
                HttpStatus.UNAUTHORIZED,
                "INVALID_OR_EXPIRED_CODE",
                "O código é inválido ou expirou"
        );
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<MobileErrorResponse> invalidRefreshToken(
            InvalidRefreshTokenException exception,
            HttpServletRequest request
    ) {
        return error(request, HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN",
                "A sessão expirou ou não pode ser renovada");
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<MobileErrorResponse> idempotencyConflict(
            IdempotencyConflictException exception, HttpServletRequest request
    ) {
        return error(request, HttpStatus.CONFLICT, "IDEMPOTENCY_KEY_REUSED",
                "A chave de idempotência já foi usada com outros dados");
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<MobileErrorResponse> rateLimitExceeded(
            RateLimitExceededException exception,
            HttpServletRequest request
    ) {
        String correlationId = MobileCorrelationIdFilter.getOrCreate(request);
        LOGGER.info(
                "event=MOBILE_AUTH_RATE_LIMITED endpoint=CODES_REQUEST scope={} rule={} "
                        + "retryAfterSeconds={} correlationId={}",
                exception.getScope(),
                exception.getRule(),
                exception.getRetryAfterSeconds(),
                correlationId
        );
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.RETRY_AFTER, Long.toString(exception.getRetryAfterSeconds()))
                .header(MobileCorrelationIdFilter.HEADER_NAME, correlationId)
                .body(new MobileErrorResponse(
                        "RATE_LIMIT_EXCEEDED",
                        "Aguarde antes de tentar novamente",
                        OffsetDateTime.now(clock),
                        request.getRequestURI(),
                        correlationId
                ));
    }

    @ExceptionHandler({InvalidClientAddressException.class, DataAccessException.class, TransactionException.class})
    public ResponseEntity<MobileErrorResponse> unavailable(
            Exception exception,
            HttpServletRequest request
    ) {
        logFailure(request, exception);
        return serviceUnavailable(request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<MobileErrorResponse> integrityViolation(
            DataIntegrityViolationException exception, HttpServletRequest request
    ) {
        if (hasUniqueViolation(exception)) {
            return error(request, HttpStatus.CONFLICT, "RESOURCE_CONFLICT",
                    "A solicitação conflita com um registro existente");
        }
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

    private boolean hasUniqueViolation(Throwable throwable) {
        for (Throwable cause = throwable; cause != null; cause = cause.getCause()) {
            if (cause instanceof java.sql.SQLException sql && "23505".equals(sql.getSQLState())) return true;
        }
        return false;
    }
}

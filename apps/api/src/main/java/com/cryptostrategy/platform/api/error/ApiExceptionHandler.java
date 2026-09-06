package com.cryptostrategy.platform.api.error;

import com.cryptostrategy.platform.api.observability.CorrelationId;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);
    private final PublicErrorMapper errorMapper;
    private final int retryAfterSeconds;

    public ApiExceptionHandler(
            PublicErrorMapper errorMapper,
            @Value("${platform.error.retry-after-seconds:5}") int retryAfterSeconds) {
        this.errorMapper = errorMapper;
        if (retryAfterSeconds < 1) {
            throw new IllegalArgumentException("Retry-After seconds must be positive");
        }
        this.retryAfterSeconds = retryAfterSeconds;
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorEnvelope> handle(Exception exception, HttpServletRequest request) {
        PublicErrorMapper.MappedError error = errorMapper.map(exception);
        if (error.status().is5xxServerError()) {
            LOGGER.error("request_failed code={} status={} exceptionType={}",
                    error.code(), error.status().value(), exception.getClass().getSimpleName());
        }
        return response(error, request);
    }

    private ResponseEntity<ErrorEnvelope> response(
            PublicErrorMapper.MappedError error,
            HttpServletRequest request) {
        String correlationId = correlationId(request);
        ErrorEnvelope envelope = new ErrorEnvelope(
                error.code(), error.message(), error.details(), correlationId, Instant.now());
        ResponseEntity.BodyBuilder response = ResponseEntity.status(error.status())
                .header(CorrelationId.HEADER, correlationId);
        if (requiresRetryAfter(error)) {
            response.header(HttpHeaders.RETRY_AFTER, Integer.toString(retryAfterSeconds));
        }
        return response.body(envelope);
    }

    private static boolean requiresRetryAfter(PublicErrorMapper.MappedError error) {
        boolean retryable = Boolean.TRUE.equals(error.details().get("retryable"));
        return retryable && (error.status() == HttpStatus.TOO_MANY_REQUESTS
                || error.status() == HttpStatus.SERVICE_UNAVAILABLE
                || error.status() == HttpStatus.GATEWAY_TIMEOUT);
    }

    private static String correlationId(HttpServletRequest request) {
        Object requestValue = request.getAttribute(CorrelationId.MDC_KEY);
        if (requestValue instanceof String value && !value.isBlank()) {
            return value;
        }
        String mdcValue = MDC.get(CorrelationId.MDC_KEY);
        return mdcValue == null ? CorrelationId.resolve(null) : mdcValue;
    }
}

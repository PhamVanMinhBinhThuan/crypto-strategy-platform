package com.cryptostrategy.platform.api.error;

import com.cryptostrategy.platform.api.observability.CorrelationId;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ErrorEnvelope> validationFailure(IllegalArgumentException exception, HttpServletRequest request) {
        return response(
                HttpStatus.BAD_REQUEST,
                "REQUEST_VALIDATION_FAILED",
                "Request validation failed.",
                request);
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ErrorEnvelope> authenticationFailure(AuthenticationException exception, HttpServletRequest request) {
        return response(
                HttpStatus.UNAUTHORIZED,
                "AUTHENTICATION_REQUIRED",
                "Authentication is required.",
                request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ErrorEnvelope> resourceNotFound(NoResourceFoundException exception, HttpServletRequest request) {
        return response(
                HttpStatus.NOT_FOUND,
                "RESOURCE_NOT_FOUND",
                "The requested resource was not found.",
                request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorEnvelope> unexpectedFailure(Exception exception, HttpServletRequest request) {
        LOGGER.error("request_failed exceptionType={}", exception.getClass().getSimpleName());
        return response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR",
                "An unexpected error occurred.",
                request);
    }

    private static ResponseEntity<ErrorEnvelope> response(
            HttpStatus status,
            String code,
            String message,
            HttpServletRequest request) {
        String correlationId = correlationId(request);
        ErrorEnvelope envelope = new ErrorEnvelope(code, message, Map.of(), correlationId, Instant.now());
        return ResponseEntity.status(status)
                .header(CorrelationId.HEADER, correlationId)
                .body(envelope);
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

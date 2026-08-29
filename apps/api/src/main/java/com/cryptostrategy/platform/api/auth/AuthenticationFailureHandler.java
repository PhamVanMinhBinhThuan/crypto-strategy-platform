package com.cryptostrategy.platform.api.auth;

import com.cryptostrategy.platform.api.error.ErrorEnvelope;
import com.cryptostrategy.platform.api.observability.CorrelationId;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationFailureHandler implements AuthenticationEntryPoint {
    private final ObjectMapper objectMapper;

    public AuthenticationFailureHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authenticationException) throws IOException, ServletException {
        String correlationId = correlationId(request);
        ErrorEnvelope envelope = new ErrorEnvelope(
                "AUTHENTICATION_REQUIRED",
                "Authentication is required.",
                Map.of(),
                correlationId,
                Instant.now());
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(CorrelationId.HEADER, correlationId);
        objectMapper.writeValue(response.getOutputStream(), envelope);
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

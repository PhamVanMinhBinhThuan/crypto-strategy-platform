package com.cryptostrategy.platform.api.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(CorrelationIdFilter.class);

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String correlationId = CorrelationId.resolve(request.getHeader(CorrelationId.HEADER));
        request.setAttribute(CorrelationId.MDC_KEY, correlationId);
        response.setHeader(CorrelationId.HEADER, correlationId);
        MDC.put(CorrelationId.MDC_KEY, correlationId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            LOGGER.info("request_completed method={} path={} status={}",
                    request.getMethod(), request.getRequestURI(), response.getStatus());
            MDC.remove(CorrelationId.MDC_KEY);
        }
    }
}

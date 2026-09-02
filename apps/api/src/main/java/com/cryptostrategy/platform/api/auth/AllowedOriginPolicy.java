package com.cryptostrategy.platform.api.auth;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/** Enforces an exact, environment-configured browser Origin allowlist. */
@Component
public final class AllowedOriginPolicy {
    private final Set<String> allowedOrigins;

    public AllowedOriginPolicy(
            @Value("${platform.security.allowed-origins:}") String configuredOrigins) {
        try {
            allowedOrigins = Arrays.stream(configuredOrigins.split(","))
                    .map(String::trim)
                    .filter(origin -> !origin.isEmpty())
                    .map(AllowedOriginPolicy::normalize)
                    .collect(Collectors.toUnmodifiableSet());
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "Configured WebSocket Origin allowlist is invalid", exception);
        }
    }

    public String requireAllowed(String origin) {
        String normalized;
        try {
            normalized = normalize(origin);
        } catch (IllegalArgumentException exception) {
            throw denied();
        }
        if (!allowedOrigins.contains(normalized)) {
            throw denied();
        }
        return normalized;
    }

    public Set<String> allowedOrigins() {
        return allowedOrigins;
    }

    private static String normalize(String origin) {
        if (origin == null || origin.isBlank()) {
            throw new IllegalArgumentException("Origin is required");
        }
        URI parsed;
        try {
            parsed = new URI(origin.trim());
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException("Origin is malformed", exception);
        }
        String scheme = parsed.getScheme() == null
                ? ""
                : parsed.getScheme().toLowerCase(Locale.ROOT);
        if (!(scheme.equals("http") || scheme.equals("https"))
                || parsed.getHost() == null
                || parsed.getRawUserInfo() != null
                || parsed.getRawQuery() != null
                || parsed.getRawFragment() != null
                || (parsed.getRawPath() != null && !parsed.getRawPath().isEmpty())) {
            throw new IllegalArgumentException("Origin is malformed");
        }
        int port = parsed.getPort();
        if ((scheme.equals("https") && port == 443)
                || (scheme.equals("http") && port == 80)) {
            port = -1;
        }
        try {
            return new URI(
                            scheme,
                            null,
                            parsed.getHost().toLowerCase(Locale.ROOT),
                            port,
                            null,
                            null,
                            null)
                    .toASCIIString();
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException("Origin is malformed", exception);
        }
    }

    private static AccessDeniedException denied() {
        return new AccessDeniedException("The request origin is not permitted.");
    }
}

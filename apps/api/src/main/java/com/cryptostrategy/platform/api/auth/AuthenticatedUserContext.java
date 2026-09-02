package com.cryptostrategy.platform.api.auth;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record AuthenticatedUserContext(UUID userId, Instant authenticationExpiresAt) {
    public AuthenticatedUserContext {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(authenticationExpiresAt, "authenticationExpiresAt");
    }

    public static AuthenticatedUserContext fromSubject(
            String subject, Instant authenticationExpiresAt) {
        return new AuthenticatedUserContext(
                userIdFromSubject(subject), authenticationExpiresAt);
    }

    public static UUID userIdFromSubject(String subject) {
        Objects.requireNonNull(subject, "subject");
        UUID userId = UUID.fromString(subject);
        if (!userId.toString().equalsIgnoreCase(subject)) {
            throw new IllegalArgumentException("JWT subject must be a canonical UUID");
        }
        return userId;
    }
}

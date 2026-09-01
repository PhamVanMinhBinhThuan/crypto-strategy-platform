package com.cryptostrategy.platform.api.auth;

import java.util.Objects;
import java.util.UUID;

public record AuthenticatedUserContext(UUID userId) {
    public AuthenticatedUserContext {
        Objects.requireNonNull(userId, "userId");
    }

    public static AuthenticatedUserContext fromSubject(String subject) {
        Objects.requireNonNull(subject, "subject");
        UUID userId = UUID.fromString(subject);
        if (!userId.toString().equalsIgnoreCase(subject)) {
            throw new IllegalArgumentException("JWT subject must be a canonical UUID");
        }
        return new AuthenticatedUserContext(userId);
    }
}

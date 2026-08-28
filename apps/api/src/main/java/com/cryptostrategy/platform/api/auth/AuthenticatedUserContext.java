package com.cryptostrategy.platform.api.auth;

import java.util.Objects;
import java.util.UUID;

public record AuthenticatedUserContext(UUID userId) {
    public AuthenticatedUserContext {
        Objects.requireNonNull(userId, "userId");
    }
}

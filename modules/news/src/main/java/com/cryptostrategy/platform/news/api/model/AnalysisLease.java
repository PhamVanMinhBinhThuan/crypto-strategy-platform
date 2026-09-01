package com.cryptostrategy.platform.news.api.model;

import com.cryptostrategy.platform.domain.api.identity.Ulids;
import java.time.Instant;
import java.util.Objects;

public record AnalysisLease(String owner, String token, Instant expiresAt, int attemptCount, String targetModelVersion) {
    public AnalysisLease {
        owner = Objects.requireNonNull(owner, "owner").trim();
        if (owner.isEmpty()) throw new IllegalArgumentException("Lease owner is required");
        token = Ulids.requireValid(token);
        Objects.requireNonNull(expiresAt, "expiresAt");
        if (attemptCount < 0) throw new IllegalArgumentException("Attempt count cannot be negative");
        targetModelVersion = requireText(targetModelVersion, "targetModelVersion");
    }
    private static String requireText(String value, String name) {
        value = Objects.requireNonNull(value, name).trim();
        if (value.isEmpty()) throw new IllegalArgumentException(name + " is required");
        return value;
    }
}

package com.cryptostrategy.platform.strategy.api.model.user;

import java.util.Objects;

/** Owner-scoped root and its authoritative latest immutable version. */
public record UserStrategyDetails(
        UserStrategy strategy, UserStrategyVersion latestVersion) {
    public UserStrategyDetails {
        Objects.requireNonNull(strategy, "strategy");
        Objects.requireNonNull(latestVersion, "latestVersion");
        if (!strategy.id().equals(latestVersion.userStrategyId())) {
            throw new IllegalArgumentException("Latest version belongs to another Strategy");
        }
        if (strategy.kind() != latestVersion.kind()) {
            throw new IllegalArgumentException("Strategy kind does not match latest version");
        }
    }
}

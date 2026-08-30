package com.cryptostrategy.platform.strategy.api.model.user;
import com.cryptostrategy.platform.strategy.api.model.StrategyKind;
import com.cryptostrategy.platform.strategy.api.model.UserStrategyId;
import com.cryptostrategy.platform.strategy.api.model.UserStrategyStatus;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
public record UserStrategy(UserStrategyId id, UUID ownerUserId, StrategyKind kind, String name,
        String description, UserStrategyStatus status, Optional<Instant> archivedAt, Instant createdAt, Instant updatedAt) {
    public UserStrategy {
        Objects.requireNonNull(id); Objects.requireNonNull(ownerUserId); Objects.requireNonNull(kind); Objects.requireNonNull(name);
        Objects.requireNonNull(description); Objects.requireNonNull(status); Objects.requireNonNull(archivedAt); Objects.requireNonNull(createdAt); Objects.requireNonNull(updatedAt);
        name = name.trim(); if (name.isBlank()) throw new IllegalArgumentException("Strategy name is blank");
        if ((status == UserStrategyStatus.ACTIVE) != archivedAt.isEmpty()) throw new IllegalArgumentException("Invalid archive state");
    }
    public UserStrategy archive(Instant at) { if (status == UserStrategyStatus.ARCHIVED) return this; return new UserStrategy(id, ownerUserId, kind, name, description, UserStrategyStatus.ARCHIVED, Optional.of(at), createdAt, at); }
}

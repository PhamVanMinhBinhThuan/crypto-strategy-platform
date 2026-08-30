package com.cryptostrategy.platform.strategy.api.model.user;
import com.cryptostrategy.platform.strategy.api.model.StrategyKind;
import com.cryptostrategy.platform.strategy.api.model.UserStrategyId;
import com.cryptostrategy.platform.strategy.api.model.UserStrategyVersionId;
import com.cryptostrategy.platform.strategy.api.model.UserStrategyVersionStatus;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
public record UserStrategyVersion(UserStrategyVersionId id, UserStrategyId userStrategyId, int versionNo,
        StrategyKind kind, StrategyDraftSource source, UserStrategyVersionStatus status, String fingerprint,
        Optional<Instant> publishedAt, Instant createdAt) {
    public UserStrategyVersion {
        Objects.requireNonNull(id); Objects.requireNonNull(userStrategyId); Objects.requireNonNull(kind); Objects.requireNonNull(source);
        Objects.requireNonNull(status); Objects.requireNonNull(fingerprint); Objects.requireNonNull(publishedAt); Objects.requireNonNull(createdAt);
        if (versionNo < 1 || fingerprint.isBlank()) throw new IllegalArgumentException("Invalid Strategy version");
        if ((status == UserStrategyVersionStatus.PUBLISHED) != publishedAt.isPresent()) throw new IllegalArgumentException("Invalid publication state");
    }
    public UserStrategyVersion publish(Instant at) {
        if (status != UserStrategyVersionStatus.DRAFT) throw new IllegalStateException("Published version is immutable");
        return new UserStrategyVersion(id, userStrategyId, versionNo, kind, source, UserStrategyVersionStatus.PUBLISHED, fingerprint, Optional.of(at), createdAt);
    }
}

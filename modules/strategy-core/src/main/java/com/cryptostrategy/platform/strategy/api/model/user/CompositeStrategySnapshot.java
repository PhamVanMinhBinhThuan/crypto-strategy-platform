package com.cryptostrategy.platform.strategy.api.model.user;
import com.cryptostrategy.platform.strategy.api.model.UserStrategyId;
import com.cryptostrategy.platform.strategy.api.model.UserStrategyVersionId;
import java.util.Objects;
import java.util.UUID;
public record CompositeStrategySnapshot(UserStrategyId userStrategyId, UserStrategyVersionId userStrategyVersionId,
        int versionNo, UUID ownerUserId, CompositeStrategyDraftSource source, String fingerprint) implements StrategySnapshot {
    public CompositeStrategySnapshot { Objects.requireNonNull(userStrategyId); Objects.requireNonNull(userStrategyVersionId); Objects.requireNonNull(ownerUserId); Objects.requireNonNull(source); Objects.requireNonNull(fingerprint); }
}

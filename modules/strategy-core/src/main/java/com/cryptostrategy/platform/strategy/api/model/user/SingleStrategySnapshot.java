package com.cryptostrategy.platform.strategy.api.model.user;
import com.cryptostrategy.platform.strategy.api.model.UserStrategyId;
import com.cryptostrategy.platform.strategy.api.model.UserStrategyVersionId;
import java.util.Objects;
import java.util.UUID;
public record SingleStrategySnapshot(UserStrategyId userStrategyId, UserStrategyVersionId userStrategyVersionId,
        int versionNo, UUID ownerUserId, SingleStrategyDraftSource source, String fingerprint) implements StrategySnapshot {
    public SingleStrategySnapshot { Objects.requireNonNull(userStrategyId); Objects.requireNonNull(userStrategyVersionId); Objects.requireNonNull(ownerUserId); Objects.requireNonNull(source); Objects.requireNonNull(fingerprint); }
}

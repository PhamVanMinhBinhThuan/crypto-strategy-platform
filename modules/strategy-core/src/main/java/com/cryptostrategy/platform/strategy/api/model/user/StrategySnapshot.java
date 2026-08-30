package com.cryptostrategy.platform.strategy.api.model.user;
import com.cryptostrategy.platform.strategy.api.model.UserStrategyId;
import com.cryptostrategy.platform.strategy.api.model.UserStrategyVersionId;
import java.util.UUID;
public sealed interface StrategySnapshot permits SingleStrategySnapshot, CompositeStrategySnapshot {
    UserStrategyId userStrategyId(); UserStrategyVersionId userStrategyVersionId(); int versionNo(); UUID ownerUserId(); String fingerprint();
}

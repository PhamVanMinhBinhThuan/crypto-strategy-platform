package com.cryptostrategy.platform.strategy.api.model.user.command;
import com.cryptostrategy.platform.strategy.api.model.UserStrategyId;
import com.cryptostrategy.platform.strategy.api.model.UserStrategyVersionId;
import java.util.Objects;
public record PublishStrategyVersionCommand(UserStrategyId userStrategyId, UserStrategyVersionId versionId, int expectedVersionNo) {
    public PublishStrategyVersionCommand { Objects.requireNonNull(userStrategyId); Objects.requireNonNull(versionId); if(expectedVersionNo<1) throw new IllegalArgumentException("Invalid expected version"); }
}

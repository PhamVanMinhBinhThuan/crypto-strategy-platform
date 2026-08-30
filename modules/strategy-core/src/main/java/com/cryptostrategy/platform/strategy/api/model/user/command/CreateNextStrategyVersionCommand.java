package com.cryptostrategy.platform.strategy.api.model.user.command;
import com.cryptostrategy.platform.strategy.api.model.UserStrategyId;
import com.cryptostrategy.platform.strategy.api.model.user.StrategyDraftSource;
import java.util.Objects;
public record CreateNextStrategyVersionCommand(UserStrategyId userStrategyId, int expectedLatestVersionNo, StrategyDraftSource source) {
    public CreateNextStrategyVersionCommand { Objects.requireNonNull(userStrategyId); Objects.requireNonNull(source); if(expectedLatestVersionNo<1) throw new IllegalArgumentException("Invalid expected version"); }
}

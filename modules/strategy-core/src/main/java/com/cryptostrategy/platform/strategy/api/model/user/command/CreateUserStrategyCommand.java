package com.cryptostrategy.platform.strategy.api.model.user.command;
import com.cryptostrategy.platform.strategy.api.model.StrategyKind;
import com.cryptostrategy.platform.strategy.api.model.user.StrategyDraftSource;
import java.util.Objects;
public record CreateUserStrategyCommand(String name, String description, StrategyKind kind, StrategyDraftSource source) {
    public CreateUserStrategyCommand { Objects.requireNonNull(name); Objects.requireNonNull(description); Objects.requireNonNull(kind); Objects.requireNonNull(source); }
}

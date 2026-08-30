package com.cryptostrategy.platform.strategy.api.model.user.command;
import com.cryptostrategy.platform.strategy.api.model.UserStrategyId;
import java.util.Objects;
public record ArchiveUserStrategyCommand(UserStrategyId userStrategyId) { public ArchiveUserStrategyCommand { Objects.requireNonNull(userStrategyId); } }

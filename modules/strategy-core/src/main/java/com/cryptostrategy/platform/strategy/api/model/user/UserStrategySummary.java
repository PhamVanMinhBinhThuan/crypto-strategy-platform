package com.cryptostrategy.platform.strategy.api.model.user;
import com.cryptostrategy.platform.strategy.api.model.StrategyKind;
import com.cryptostrategy.platform.strategy.api.model.UserStrategyId;
import java.time.Instant;
public record UserStrategySummary(UserStrategyId id, StrategyKind kind, String name, String description, Instant createdAt) { }

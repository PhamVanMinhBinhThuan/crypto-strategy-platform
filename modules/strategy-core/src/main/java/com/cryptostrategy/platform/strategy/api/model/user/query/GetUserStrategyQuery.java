package com.cryptostrategy.platform.strategy.api.model.user.query;
import com.cryptostrategy.platform.strategy.api.model.UserStrategyId;
import java.util.Objects;
public record GetUserStrategyQuery(UserStrategyId userStrategyId) { public GetUserStrategyQuery { Objects.requireNonNull(userStrategyId); } }

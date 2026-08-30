package com.cryptostrategy.platform.strategy.api.port.in;
import com.cryptostrategy.platform.strategy.api.model.user.UserStrategy;
import com.cryptostrategy.platform.strategy.api.model.user.query.GetUserStrategyQuery;
import java.util.UUID;
public interface GetUserStrategyUseCase { UserStrategy getUserStrategy(UUID authenticatedUserId, GetUserStrategyQuery query); }

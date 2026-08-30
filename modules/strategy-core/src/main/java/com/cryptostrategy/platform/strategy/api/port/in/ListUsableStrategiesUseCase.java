package com.cryptostrategy.platform.strategy.api.port.in;
import com.cryptostrategy.platform.strategy.api.model.user.query.UsableStrategyCatalog;
import com.cryptostrategy.platform.strategy.api.model.user.query.UsableStrategyPageRequest;
import java.util.UUID;
public interface ListUsableStrategiesUseCase { UsableStrategyCatalog listUsableStrategies(UUID authenticatedUserId, UsableStrategyPageRequest request); }

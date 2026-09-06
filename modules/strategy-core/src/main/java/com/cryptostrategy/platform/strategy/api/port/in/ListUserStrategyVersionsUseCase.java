package com.cryptostrategy.platform.strategy.api.port.in;

import com.cryptostrategy.platform.strategy.api.model.UserStrategyId;
import com.cryptostrategy.platform.strategy.api.model.user.UserStrategyVersion;
import java.util.List;
import java.util.UUID;

public interface ListUserStrategyVersionsUseCase {
    List<UserStrategyVersion> listVersions(UUID authenticatedUserId, UserStrategyId userStrategyId);
}

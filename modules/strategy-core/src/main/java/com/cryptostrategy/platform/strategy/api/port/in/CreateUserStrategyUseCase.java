package com.cryptostrategy.platform.strategy.api.port.in;
import com.cryptostrategy.platform.strategy.api.model.user.UserStrategyVersion;
import com.cryptostrategy.platform.strategy.api.model.user.command.CreateUserStrategyCommand;
import java.util.UUID;
public interface CreateUserStrategyUseCase { UserStrategyVersion createUserStrategy(UUID authenticatedUserId, CreateUserStrategyCommand command); }

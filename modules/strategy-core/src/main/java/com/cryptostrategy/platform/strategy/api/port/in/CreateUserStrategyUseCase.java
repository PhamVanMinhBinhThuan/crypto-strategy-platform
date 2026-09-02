package com.cryptostrategy.platform.strategy.api.port.in;
import com.cryptostrategy.platform.strategy.api.model.user.UserStrategyDetails;
import com.cryptostrategy.platform.strategy.api.model.user.command.CreateUserStrategyCommand;
import java.util.UUID;
public interface CreateUserStrategyUseCase { UserStrategyDetails createUserStrategy(UUID authenticatedUserId, CreateUserStrategyCommand command); }

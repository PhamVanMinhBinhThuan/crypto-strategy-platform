package com.cryptostrategy.platform.strategy.api.port.in;
import com.cryptostrategy.platform.strategy.api.model.user.UserStrategyVersion;
import com.cryptostrategy.platform.strategy.api.model.user.command.CreateNextStrategyVersionCommand;
import java.util.UUID;
public interface CreateUserStrategyVersionUseCase { UserStrategyVersion createNextVersion(UUID authenticatedUserId, CreateNextStrategyVersionCommand command); }

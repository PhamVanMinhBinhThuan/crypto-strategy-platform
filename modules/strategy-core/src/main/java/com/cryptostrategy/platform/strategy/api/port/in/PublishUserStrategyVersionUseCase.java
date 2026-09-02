package com.cryptostrategy.platform.strategy.api.port.in;
import com.cryptostrategy.platform.strategy.api.model.user.UserStrategyVersion;
import com.cryptostrategy.platform.strategy.api.model.user.command.PublishStrategyVersionCommand;
import java.util.UUID;
public interface PublishUserStrategyVersionUseCase { UserStrategyVersion publish(UUID authenticatedUserId, PublishStrategyVersionCommand command); }

package com.cryptostrategy.platform.strategy.api.port.in;
import com.cryptostrategy.platform.strategy.api.model.user.StrategySnapshot;
import com.cryptostrategy.platform.strategy.api.model.user.command.PublishStrategyVersionCommand;
import java.util.UUID;
public interface PublishUserStrategyVersionUseCase { StrategySnapshot publish(UUID authenticatedUserId, PublishStrategyVersionCommand command); }

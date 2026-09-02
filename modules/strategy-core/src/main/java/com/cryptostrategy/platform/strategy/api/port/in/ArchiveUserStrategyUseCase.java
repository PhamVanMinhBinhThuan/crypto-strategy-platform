package com.cryptostrategy.platform.strategy.api.port.in;
import com.cryptostrategy.platform.strategy.api.model.user.UserStrategyDetails;
import com.cryptostrategy.platform.strategy.api.model.user.command.ArchiveUserStrategyCommand;
import java.util.UUID;
public interface ArchiveUserStrategyUseCase { UserStrategyDetails archive(UUID authenticatedUserId, ArchiveUserStrategyCommand command); }

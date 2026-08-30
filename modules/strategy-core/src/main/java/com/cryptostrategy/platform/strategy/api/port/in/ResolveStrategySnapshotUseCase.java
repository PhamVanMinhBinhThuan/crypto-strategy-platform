package com.cryptostrategy.platform.strategy.api.port.in;
import com.cryptostrategy.platform.strategy.api.model.user.StrategySnapshot;
import com.cryptostrategy.platform.strategy.api.model.user.query.ResolveStrategySnapshotQuery;
import java.util.UUID;
public interface ResolveStrategySnapshotUseCase { StrategySnapshot resolveSnapshot(UUID authenticatedUserId, ResolveStrategySnapshotQuery query); }

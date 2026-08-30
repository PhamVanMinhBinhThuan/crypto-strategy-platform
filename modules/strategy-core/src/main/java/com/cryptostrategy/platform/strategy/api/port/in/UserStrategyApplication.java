package com.cryptostrategy.platform.strategy.api.port.in;
public interface UserStrategyApplication extends ListUsableStrategiesUseCase, CreateUserStrategyUseCase,
        CreateUserStrategyVersionUseCase, PublishUserStrategyVersionUseCase, GetUserStrategyUseCase,
        ResolveStrategySnapshotUseCase, ArchiveUserStrategyUseCase { }

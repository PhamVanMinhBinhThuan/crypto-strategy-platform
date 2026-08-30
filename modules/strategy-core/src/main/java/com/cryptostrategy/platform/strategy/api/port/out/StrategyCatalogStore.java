package com.cryptostrategy.platform.strategy.api.port.out;
import com.cryptostrategy.platform.strategy.api.model.StrategyDescriptor;
public interface StrategyCatalogStore { void registerOrVerify(StrategyDescriptor descriptor); }

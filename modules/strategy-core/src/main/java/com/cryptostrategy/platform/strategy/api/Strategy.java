package com.cryptostrategy.platform.strategy.api;
import com.cryptostrategy.platform.strategy.api.model.StrategyContext;
import com.cryptostrategy.platform.strategy.api.model.StrategyDecision;
@FunctionalInterface public interface Strategy { StrategyDecision evaluate(StrategyContext context); }

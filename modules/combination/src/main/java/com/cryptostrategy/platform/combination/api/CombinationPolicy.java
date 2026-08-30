package com.cryptostrategy.platform.combination.api;
import com.cryptostrategy.platform.strategy.api.model.StrategyDecision;
import com.cryptostrategy.platform.strategy.api.model.StrategySignal;
import java.util.List;
public interface CombinationPolicy { CombinationPolicyReference reference(); StrategySignal combine(List<StrategyDecision> decisions); }

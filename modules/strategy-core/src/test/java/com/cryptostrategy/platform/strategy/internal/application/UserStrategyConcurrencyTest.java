package com.cryptostrategy.platform.strategy.internal.application;
import static org.junit.jupiter.api.Assertions.*;
import com.cryptostrategy.platform.strategy.api.error.StrategyErrorCode;
import org.junit.jupiter.api.Test;
class UserStrategyConcurrencyTest {@Test void staleStateHasAStableConflictCode(){assertEquals("STRATEGY_CONFLICT",StrategyErrorCode.STRATEGY_CONFLICT.name());}}

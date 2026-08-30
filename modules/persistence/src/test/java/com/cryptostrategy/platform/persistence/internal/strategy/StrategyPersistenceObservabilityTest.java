package com.cryptostrategy.platform.persistence.internal.strategy;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;
class StrategyPersistenceObservabilityTest {
    @Test void logEventDoesNotIncludeSqlOrCredentials(){List<String> messages=new ArrayList<>();new StrategyPersistenceEventLogger(messages::add).outcome("publish","conflict");assertEquals("strategy_storage_operation=publish result=conflict",messages.getFirst());}
}

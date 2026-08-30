package com.cryptostrategy.platform.strategy.api.model;
import static org.junit.jupiter.api.Assertions.*;
import com.cryptostrategy.platform.domain.api.identity.UlidIdentifier;
import org.junit.jupiter.api.Test;
class StrategyIdentityTest {
    private static final String ID="01J00000000000000000000000";
    @Test void strategyIdsAreTypedCanonicalUlids(){assertInstanceOf(UlidIdentifier.class,new StrategyVersionId(ID));assertInstanceOf(UlidIdentifier.class,new UserStrategyId(ID));assertInstanceOf(UlidIdentifier.class,new UserStrategyVersionId(ID));assertThrows(IllegalArgumentException.class,()->new StrategyVersionId("81J00000000000000000000000"));}
}

package com.cryptostrategy.platform.strategy.internal.application;
import static org.junit.jupiter.api.Assertions.*;
import com.cryptostrategy.platform.strategy.api.model.UserStrategyId;
import java.util.*;
import org.junit.jupiter.api.Test;
class StrategyObservabilityTest {
    @Test void logsOnlyStableNonSensitiveFields(){List<String> messages=new ArrayList<>();new StrategyEventLogger(messages::add).lifecycle("published",new UserStrategyId("01J00000000000000000000000"),"ok password=secret");assertEquals(1,messages.size());assertFalse(messages.getFirst().contains("password="));}
}

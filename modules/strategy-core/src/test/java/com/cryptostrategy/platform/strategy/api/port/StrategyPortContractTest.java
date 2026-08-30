package com.cryptostrategy.platform.strategy.api.port;
import static org.junit.jupiter.api.Assertions.*;
import com.cryptostrategy.platform.strategy.api.model.user.query.UsableStrategyPageRequest;
import com.cryptostrategy.platform.strategy.api.port.in.ListUsableStrategiesUseCase;
import com.cryptostrategy.platform.strategy.api.port.out.UserStrategyStore;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
class StrategyPortContractTest {@Test void usableListingIsCombinedAndBounded(){assertTrue(ListUsableStrategiesUseCase.class.isInterface());assertEquals(20,UsableStrategyPageRequest.defaults().systemPageSize());assertThrows(IllegalArgumentException.class,()->new UsableStrategyPageRequest(101,null,20,null));assertFalse(Arrays.stream(UserStrategyStore.class.getMethods()).anyMatch(method->method.getName().equals("findById")));}}

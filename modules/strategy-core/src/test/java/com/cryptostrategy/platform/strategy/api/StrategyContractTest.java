package com.cryptostrategy.platform.strategy.api;
import static org.junit.jupiter.api.Assertions.*;
import com.cryptostrategy.platform.strategy.api.error.*;
import com.cryptostrategy.platform.strategy.api.model.*;
import java.util.*;
import org.junit.jupiter.api.Test;
class StrategyContractTest {@Test void insufficientInputIsAnErrorNotHold(){Strategy strategy=context->{throw new StrategyException(StrategyErrorCode.INSUFFICIENT_DATA,"warm-up");};StrategyException error=assertThrows(StrategyException.class,()->strategy.evaluate(null));assertEquals(StrategyErrorCode.INSUFFICIENT_DATA,error.code());assertNotEquals(StrategySignal.HOLD,error.code());}}

package com.cryptostrategy.platform.persistence.internal.strategy;
import static org.junit.jupiter.api.Assertions.*;
import com.cryptostrategy.platform.strategy.api.model.parameter.*;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;
class JdbcStrategyCatalogStoreTest {@Test void parameterJsonIsDeterministicAndRoundTripsExactValues(){StrategyJsonMapper mapper=new StrategyJsonMapper();StrategyParameterSet first=StrategyParameterSet.of(Map.of("b",new StrategyParameterValue.DecimalValue(new BigDecimal("1.00")),"a",new StrategyParameterValue.IntegerValue(2)));StrategyParameterSet second=mapper.readParameters(mapper.parameters(first));assertEquals(first,second);assertTrue(StrategySql.FIND_CATALOG.contains("plugin_id=? and version=?"));}}

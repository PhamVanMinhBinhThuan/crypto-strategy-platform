package com.cryptostrategy.platform.strategy.internal.parameter;
import static org.junit.jupiter.api.Assertions.*;
import com.cryptostrategy.platform.strategy.api.error.StrategyException;
import com.cryptostrategy.platform.strategy.api.model.parameter.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
class StrategyParameterValidatorTest {
    private static ParameterDefinition integer(String name,long value){return new ParameterDefinition(name,ParameterType.INTEGER,true,Optional.of(new StrategyParameterValue.IntegerValue(value)),Optional.of(BigDecimal.ONE),Optional.of(BigDecimal.valueOf(100)),Set.of(),name);}
    @Test void resolvesDefaultsAndValidatesCrossFieldRule(){StrategyParameterSchema schema=new StrategyParameterSchema(List.of(integer("fastPeriod",5),integer("slowPeriod",25)),List.of(new CrossParameterConstraint("fastPeriod","slowPeriod")));StrategyParameterValidator validator=new StrategyParameterValidator();StrategyParameterSet resolved=validator.resolve(schema,Map.of());assertEquals("5",resolved.require("fastPeriod").canonicalText());assertThrows(StrategyException.class,()->validator.resolve(schema,Map.of("fastPeriod",new StrategyParameterValue.IntegerValue(30))));assertThrows(StrategyException.class,()->validator.resolve(schema,Map.of("unknown",new StrategyParameterValue.IntegerValue(1))));}
}

package com.cryptostrategy.platform.strategy.api.model;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
class StrategyValueObjectTest {@Test void slugsVersionsAndEnumsAreStable(){assertEquals("ma-crossover",new StrategyPluginId("ma-crossover").value());assertThrows(IllegalArgumentException.class,()->new StrategyPluginId("MA"));assertEquals("1.2.3",SemanticVersion.parse("1.2.3").toString());assertEquals(3,StrategySignal.values().length);}}

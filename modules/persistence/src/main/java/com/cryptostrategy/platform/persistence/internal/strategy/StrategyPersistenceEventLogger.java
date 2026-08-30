package com.cryptostrategy.platform.persistence.internal.strategy;
import java.util.Objects;
import java.util.function.Consumer;
public final class StrategyPersistenceEventLogger {
    private final Consumer<String> sink;
    public StrategyPersistenceEventLogger(Consumer<String> sink){this.sink=Objects.requireNonNull(sink);}
    public void outcome(String operation,String result){sink.accept("strategy_storage_operation="+safe(operation)+" result="+safe(result));}
    private static String safe(String value){return value.replaceAll("[^A-Za-z0-9_-]","_");}
}

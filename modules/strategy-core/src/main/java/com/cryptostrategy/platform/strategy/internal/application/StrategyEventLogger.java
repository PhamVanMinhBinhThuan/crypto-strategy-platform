package com.cryptostrategy.platform.strategy.internal.application;
import com.cryptostrategy.platform.strategy.api.model.UserStrategyId;
import java.util.Objects;
import java.util.function.Consumer;
public final class StrategyEventLogger {
    private final Consumer<String> sink;
    public StrategyEventLogger(Consumer<String> sink){this.sink=Objects.requireNonNull(sink);}
    public void lifecycle(String event,UserStrategyId id,String outcome){sink.accept("strategy_event="+safe(event)+" strategy_id="+id.value()+" outcome="+safe(outcome));}
    private static String safe(String value){return value.replaceAll("[^A-Za-z0-9_-]","_");}
}

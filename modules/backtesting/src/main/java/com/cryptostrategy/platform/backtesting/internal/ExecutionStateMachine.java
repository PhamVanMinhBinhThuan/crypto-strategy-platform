package com.cryptostrategy.platform.backtesting.internal;

import com.cryptostrategy.platform.strategy.api.model.StrategySignal;

/** Long-only state: invalid BUY/SELL transitions are deterministic no-ops. */
final class ExecutionStateMachine {
    private Position position;
    private StrategySignal pending = StrategySignal.HOLD;
    Position position() { return position; }
    StrategySignal pending() { return pending; }
    boolean mayOpen() { return position == null && pending == StrategySignal.BUY; }
    boolean mayClose() { return position != null && pending == StrategySignal.SELL; }
    void opened(Position value) { if (position != null) throw new IllegalStateException("Position already open"); position = value; }
    Position closed() { if (position == null) throw new IllegalStateException("No open position"); Position old=position;position=null;return old; }
    void decide(StrategySignal signal) { pending = signal == null ? StrategySignal.HOLD : signal; }
}

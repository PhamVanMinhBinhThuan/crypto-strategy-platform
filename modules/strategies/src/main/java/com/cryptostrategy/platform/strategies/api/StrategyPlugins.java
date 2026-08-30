package com.cryptostrategy.platform.strategies.api;

import com.cryptostrategy.platform.strategies.internal.ma.MovingAverageCrossoverPlugin;
import com.cryptostrategy.platform.strategy.api.StrategyPlugin;
import java.util.List;

public final class StrategyPlugins {
    private StrategyPlugins() { }
    public static List<StrategyPlugin> trusted() { return List.of(new MovingAverageCrossoverPlugin()); }
}

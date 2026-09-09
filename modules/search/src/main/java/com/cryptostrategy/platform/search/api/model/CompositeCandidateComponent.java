package com.cryptostrategy.platform.search.api.model;

import com.cryptostrategy.platform.strategy.api.model.StrategyReference;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterSet;
import java.util.Objects;

public record CompositeCandidateComponent(
        StrategyReference strategy,
        StrategyParameterSet parameters) implements Comparable<CompositeCandidateComponent> {
    public CompositeCandidateComponent {
        Objects.requireNonNull(strategy, "strategy");
        Objects.requireNonNull(parameters, "parameters");
    }

    @Override
    public int compareTo(CompositeCandidateComponent other) {
        return strategy.compareTo(other.strategy);
    }
}

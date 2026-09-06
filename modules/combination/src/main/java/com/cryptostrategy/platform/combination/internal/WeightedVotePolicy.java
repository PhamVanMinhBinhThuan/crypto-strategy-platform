package com.cryptostrategy.platform.combination.internal;

import com.cryptostrategy.platform.combination.api.CombinationPolicy;
import com.cryptostrategy.platform.combination.api.CombinationPolicyReference;
import com.cryptostrategy.platform.strategy.api.model.CombinationPolicyId;
import com.cryptostrategy.platform.strategy.api.model.SemanticVersion;
import com.cryptostrategy.platform.strategy.api.model.StrategyDecision;
import com.cryptostrategy.platform.strategy.api.model.StrategySignal;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterSet;
import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class WeightedVotePolicy implements CombinationPolicy {
    private static final CombinationPolicyReference REFERENCE = new CombinationPolicyReference(
            new CombinationPolicyId("weighted-vote"), new SemanticVersion(1, 0, 0));

    @Override
    public CombinationPolicyReference reference() {
        return REFERENCE;
    }

    @Override
    public StrategySignal combine(
            List<StrategyDecision> decisions, StrategyParameterSet parameters) {
        if (decisions.size() < 2) {
            throw new IllegalArgumentException("Weighted vote needs two decisions");
        }
        Map<StrategySignal, BigDecimal> scores = new EnumMap<>(StrategySignal.class);
        for (StrategySignal signal : StrategySignal.values()) {
            scores.put(signal, BigDecimal.ZERO);
        }
        for (StrategyDecision decision : decisions) {
            String key = "weight." + decision.strategyReference().strategyVersionId().value();
            BigDecimal weight;
            try {
                weight = new BigDecimal(parameters.require(key).canonicalText());
            } catch (RuntimeException exception) {
                throw new IllegalArgumentException("Missing or invalid weight for component", exception);
            }
            if (weight.signum() <= 0) {
                throw new IllegalArgumentException("Component weight must be positive");
            }
            scores.compute(decision.signal(), (signal, current) -> current.add(weight));
        }
        BigDecimal max = scores.values().stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        List<StrategySignal> winners = scores.entrySet().stream()
                .filter(entry -> entry.getValue().compareTo(max) == 0)
                .map(Map.Entry::getKey)
                .toList();
        return winners.size() == 1 ? winners.getFirst() : StrategySignal.HOLD;
    }
}

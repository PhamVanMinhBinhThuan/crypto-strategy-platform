package com.cryptostrategy.platform.combination.internal;
import com.cryptostrategy.platform.combination.api.CombinationPolicy;
import com.cryptostrategy.platform.strategy.api.Strategy;
import com.cryptostrategy.platform.strategy.api.model.StrategyContext;
import com.cryptostrategy.platform.strategy.api.model.StrategyDecision;
import com.cryptostrategy.platform.strategy.api.model.StrategyEvidenceValue;
import com.cryptostrategy.platform.strategy.api.model.StrategyReference;
import java.util.List;
import java.util.Map;
import java.util.Objects;
public final class CompositeStrategy implements Strategy {
    private final StrategyReference reference; private final CombinationPolicy policy; private final List<Strategy> components;
    public CompositeStrategy(StrategyReference reference,CombinationPolicy policy,List<Strategy> components){this.reference=Objects.requireNonNull(reference);this.policy=Objects.requireNonNull(policy);this.components=List.copyOf(components);if(components.size()<2)throw new IllegalArgumentException("Composite needs two components");}
    @Override public StrategyDecision evaluate(StrategyContext context){List<StrategyDecision> decisions=components.stream().map(component->component.evaluate(context)).toList();return new StrategyDecision(policy.combine(decisions),context.evaluationTime(),reference,"COMPOSITE_"+policy.reference().policyId().value().toUpperCase().replace('-','_'),"Combined deterministic component decisions",Map.of("componentCount",new StrategyEvidenceValue.IntegerEvidence(decisions.size())));}
}

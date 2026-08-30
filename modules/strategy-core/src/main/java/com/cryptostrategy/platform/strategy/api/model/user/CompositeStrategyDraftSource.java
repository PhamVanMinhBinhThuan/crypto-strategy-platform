package com.cryptostrategy.platform.strategy.api.model.user;
import com.cryptostrategy.platform.strategy.api.model.CombinationPolicyId;
import com.cryptostrategy.platform.strategy.api.model.SemanticVersion;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterSet;
import java.util.List;
import java.util.Objects;
public record CompositeStrategyDraftSource(CombinationPolicyId policyId, SemanticVersion policyVersion,
        StrategyParameterSet policyParameters, List<UserStrategyComponent> components) implements StrategyDraftSource {
    public CompositeStrategyDraftSource {
        Objects.requireNonNull(policyId); Objects.requireNonNull(policyVersion); Objects.requireNonNull(policyParameters); Objects.requireNonNull(components);
        components = components.stream().sorted().toList();
        if (components.size() < 2 || components.stream().map(UserStrategyComponent::strategyReference).distinct().count() != components.size()) throw new IllegalArgumentException("Composite needs distinct system components");
    }
}

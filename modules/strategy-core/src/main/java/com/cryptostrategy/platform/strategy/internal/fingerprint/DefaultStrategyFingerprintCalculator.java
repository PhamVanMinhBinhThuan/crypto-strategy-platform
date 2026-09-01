package com.cryptostrategy.platform.strategy.internal.fingerprint;

import com.cryptostrategy.platform.strategy.api.port.in.StrategyFingerprintCalculator;

public final class DefaultStrategyFingerprintCalculator implements StrategyFingerprintCalculator {
    private final StrategyFingerprintV1 delegate = new StrategyFingerprintV1();

    @Override public String single(
            com.cryptostrategy.platform.strategy.api.model.StrategyReference reference,
            com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterSet parameters) {
        return delegate.single(reference, parameters);
    }

    @Override public String composite(
            com.cryptostrategy.platform.strategy.api.model.CombinationPolicyId policyId,
            com.cryptostrategy.platform.strategy.api.model.SemanticVersion policyVersion,
            com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterSet policyParameters,
            java.util.List<Component> components) {
        var encoder = new CanonicalStrategyEncoder();
        var encoded = components.stream().map(value -> encoder.encodeSingle(value.reference(), value.parameters())).toList();
        String policy = policyId + "@" + policyVersion;
        return delegate.composite(policy, encoded);
    }
}

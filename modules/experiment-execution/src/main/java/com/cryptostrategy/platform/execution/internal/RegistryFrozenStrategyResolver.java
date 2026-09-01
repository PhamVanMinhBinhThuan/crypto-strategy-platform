package com.cryptostrategy.platform.execution.internal;

import com.cryptostrategy.platform.backtesting.api.error.BacktestErrorCode;
import com.cryptostrategy.platform.backtesting.api.error.BacktestException;
import com.cryptostrategy.platform.backtesting.api.port.out.FrozenStrategyResolver;
import com.cryptostrategy.platform.backtesting.api.port.out.ResolvedStrategy;
import com.cryptostrategy.platform.combination.api.CombinationPolicyReference;
import com.cryptostrategy.platform.combination.api.CompositeStrategyMaterializer;
import com.cryptostrategy.platform.experiment.api.provenance.StrategyProvenanceSnapshot;
import com.cryptostrategy.platform.strategy.api.Strategy;
import com.cryptostrategy.platform.strategy.api.model.StrategyPluginId;
import com.cryptostrategy.platform.strategy.api.model.StrategyReference;
import com.cryptostrategy.platform.strategy.api.port.in.StrategyFingerprintCalculator;
import com.cryptostrategy.platform.strategy.api.port.in.StrategyRegistry;
import java.util.Objects;

public final class RegistryFrozenStrategyResolver implements FrozenStrategyResolver {
    private final StrategyRegistry registry;
    private final StrategyFingerprintCalculator fingerprints;
    private final CompositeStrategyMaterializer composites;

    public RegistryFrozenStrategyResolver(StrategyRegistry registry, StrategyFingerprintCalculator fingerprints,
            CompositeStrategyMaterializer composites) {
        this.registry = Objects.requireNonNull(registry);
        this.fingerprints = Objects.requireNonNull(fingerprints);
        this.composites = Objects.requireNonNull(composites);
    }

    @Override public ResolvedStrategy resolve(StrategyProvenanceSnapshot provenance) {
        if (provenance.singleStrategy().isPresent()) {
            StrategyReference reference = provenance.singleStrategy().orElseThrow();
            String actual = fingerprints.single(reference, provenance.parameters());
            requireFingerprint(provenance.strategyFingerprint(), actual);
            Strategy strategy = registry.create(reference.pluginId(), reference.implementationVersion(),
                    provenance.parameters().values());
            int lookback = registry.requiredLookback(reference.pluginId(), reference.implementationVersion(),
                    provenance.parameters().values());
            return new ResolvedStrategy(strategy, lookback, actual);
        }
        var policyId = provenance.compositePolicyId().orElseThrow();
        var policyVersion = provenance.compositePolicyVersion().orElseThrow();
        var fpComponents = provenance.components().stream()
                .map(value -> new StrategyFingerprintCalculator.Component(value.strategyReference(), value.parameters()))
                .toList();
        String actual = fingerprints.composite(policyId, policyVersion, provenance.parameters(), fpComponents);
        requireFingerprint(provenance.strategyFingerprint(), actual);
        var resolved = provenance.components().stream().map(value -> registry.create(
                value.strategyReference().pluginId(), value.strategyReference().implementationVersion(),
                value.parameters().values())).toList();
        int lookback = provenance.components().stream().mapToInt(value -> registry.requiredLookback(
                value.strategyReference().pluginId(), value.strategyReference().implementationVersion(),
                value.parameters().values())).max().orElseThrow();
        StrategyReference compositeReference = new StrategyReference(
                provenance.components().getFirst().strategyReference().strategyVersionId(),
                new StrategyPluginId("composite"), policyVersion);
        Strategy strategy = composites.materialize(compositeReference,
                new CombinationPolicyReference(policyId, policyVersion), resolved);
        return new ResolvedStrategy(strategy, lookback, actual);
    }

    private static void requireFingerprint(String expected, String actual) {
        if (!expected.equals(actual)) throw new BacktestException(BacktestErrorCode.INVALID_LINEAGE,
                "Frozen Strategy fingerprint does not match resolved version and parameters");
    }
}

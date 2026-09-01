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

    @Override public ResolvedStrategy resolve(StrategyProvenanceSnapshot provenance, com.cryptostrategy.platform.experiment.api.CandidateDefinition candidate) {
        java.util.Map<String, com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterValue> overrides = parseCandidateParams(candidate.definition());
        if (provenance.singleStrategy().isPresent()) {
            StrategyReference reference = provenance.singleStrategy().orElseThrow();
            var merged = new java.util.HashMap<>(provenance.parameters().values());
            merged.putAll(overrides);
            var mergedSet = com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterSet.of(merged);
            String actual = fingerprints.single(reference, mergedSet);
            requireFingerprint(candidate.fingerprint(), actual);
            Strategy strategy = registry.create(reference.pluginId(), reference.implementationVersion(), merged);
            int lookback = registry.requiredLookback(reference.pluginId(), reference.implementationVersion(), merged);
            return new ResolvedStrategy(strategy, lookback, actual);
        }
        var policyId = provenance.compositePolicyId().orElseThrow();
        var policyVersion = provenance.compositePolicyVersion().orElseThrow();
        
        var policyMerged = new java.util.HashMap<>(provenance.parameters().values());
        policyMerged.putAll(overrides);
        var policySet = com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterSet.of(policyMerged);

        var fpComponents = new java.util.ArrayList<StrategyFingerprintCalculator.Component>();
        var resolved = new java.util.ArrayList<Strategy>();
        int lookback = 0;

        for (var component : provenance.components()) {
             var compMerged = new java.util.HashMap<>(component.parameters().values());
             compMerged.putAll(overrides);
             fpComponents.add(new StrategyFingerprintCalculator.Component(component.strategyReference(), com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterSet.of(compMerged)));
             resolved.add(registry.create(component.strategyReference().pluginId(), component.strategyReference().implementationVersion(), compMerged));
             int lb = registry.requiredLookback(component.strategyReference().pluginId(), component.strategyReference().implementationVersion(), compMerged);
             if (lb > lookback) lookback = lb;
        }

        String actual = fingerprints.composite(policyId, policyVersion, policySet, fpComponents);
        requireFingerprint(candidate.fingerprint(), actual);
        
        StrategyReference compositeReference = new StrategyReference(
                provenance.components().getFirst().strategyReference().strategyVersionId(),
                new StrategyPluginId("composite"), policyVersion);
        Strategy strategy = composites.materialize(compositeReference,
                new CombinationPolicyReference(policyId, policyVersion), resolved);
        return new ResolvedStrategy(strategy, lookback, actual);
    }

    private java.util.Map<String, com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterValue> parseCandidateParams(java.util.Map<String, Object> definition) {
        var result = new java.util.HashMap<String, com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterValue>();
        for (var entry : definition.entrySet()) {
            Object v = entry.getValue();
            if (v instanceof Integer i) result.put(entry.getKey(), new com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterValue.IntegerValue(i));
            else if (v instanceof Long l) result.put(entry.getKey(), new com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterValue.IntegerValue(l));
            else if (v instanceof Double d) result.put(entry.getKey(), new com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterValue.DecimalValue(java.math.BigDecimal.valueOf(d)));
            else if (v instanceof String s) result.put(entry.getKey(), new com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterValue.TextValue(s));
            else if (v instanceof Boolean b) result.put(entry.getKey(), new com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterValue.BooleanValue(b));
        }
        return result;
    }

    private static void requireFingerprint(String expected, String actual) {
        if (!expected.equals(actual)) throw new BacktestException(BacktestErrorCode.INVALID_LINEAGE,
                "Frozen Strategy fingerprint does not match resolved version and parameters");
    }
}

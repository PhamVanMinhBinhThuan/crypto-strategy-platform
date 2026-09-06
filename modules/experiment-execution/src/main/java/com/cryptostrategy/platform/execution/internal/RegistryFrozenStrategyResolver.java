package com.cryptostrategy.platform.execution.internal;

import com.cryptostrategy.platform.backtesting.api.error.BacktestErrorCode;
import com.cryptostrategy.platform.backtesting.api.error.BacktestException;
import com.cryptostrategy.platform.backtesting.api.port.out.FrozenStrategyResolver;
import com.cryptostrategy.platform.backtesting.api.port.out.ResolvedStrategy;
import com.cryptostrategy.platform.combination.api.CombinationPolicyReference;
import com.cryptostrategy.platform.combination.api.CompositeStrategyMaterializer;
import com.cryptostrategy.platform.experiment.api.provenance.StrategyProvenanceSnapshot;
import com.cryptostrategy.platform.search.api.SearchModuleFactory;
import com.cryptostrategy.platform.search.api.model.CompositeCandidateComponent;
import com.cryptostrategy.platform.search.api.model.SearchCombinationPolicy;
import com.cryptostrategy.platform.search.api.CompositeSearchCanonicalization;
import com.cryptostrategy.platform.strategy.api.Strategy;
import com.cryptostrategy.platform.strategy.api.model.SemanticVersion;
import com.cryptostrategy.platform.strategy.api.model.StrategyPluginId;
import com.cryptostrategy.platform.strategy.api.model.StrategyReference;
import com.cryptostrategy.platform.strategy.api.model.StrategyVersionId;
import com.cryptostrategy.platform.strategy.api.model.parameter.ParameterType;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterSet;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterValue;
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
        if (Integer.valueOf(2).equals(candidate.definition().get("schemaVersion"))) {
            return resolveCompositeCandidate(candidate);
        }
        java.util.Map<String, com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterValue> overrides = parseCandidateParams(candidate.definition());
        if (provenance.singleStrategy().isPresent()) {
            StrategyReference reference = provenance.singleStrategy().orElseThrow();
            var merged = new java.util.HashMap<>(provenance.parameters().values());
            merged.putAll(overrides);
            var mergedSet = com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterSet.of(merged);
            requireFingerprint(candidate.fingerprint(),
                    SearchModuleFactory.canonicalCandidateFingerprint(
                            com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterSet.of(overrides)));
            String actual = fingerprints.single(reference, mergedSet);
            Strategy strategy = registry.create(reference.pluginId(), reference.implementationVersion(), merged);
            int lookback = registry.requiredLookback(reference.pluginId(), reference.implementationVersion(), merged);
            return new ResolvedStrategy(strategy, lookback, actual);
        }
        var policyId = provenance.compositePolicyId().orElseThrow();
        var policyVersion = provenance.compositePolicyVersion().orElseThrow();
        
        var policyMerged = new java.util.HashMap<>(provenance.parameters().values());
        policyMerged.putAll(overrides);
        var policySet = com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterSet.of(policyMerged);
        requireFingerprint(candidate.fingerprint(),
                SearchModuleFactory.canonicalCandidateFingerprint(
                        com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterSet.of(overrides)));

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
        StrategyReference compositeReference = new StrategyReference(
                provenance.components().getFirst().strategyReference().strategyVersionId(),
                new StrategyPluginId("composite"), policyVersion);
        Strategy strategy = composites.materialize(compositeReference,
                new CombinationPolicyReference(policyId, policyVersion), resolved);
        return new ResolvedStrategy(strategy, lookback, actual);
    }

    @SuppressWarnings("unchecked")
    private ResolvedStrategy resolveCompositeCandidate(
            com.cryptostrategy.platform.experiment.api.CandidateDefinition candidate) {
        Object rawPolicy = candidate.definition().get("combinationPolicy");
        Object rawComponents = candidate.definition().get("components");
        if (!(rawPolicy instanceof java.util.Map<?, ?> policy)
                || !(rawComponents instanceof java.util.List<?> componentsRaw)) {
            throw new BacktestException(BacktestErrorCode.INVALID_LINEAGE,
                    "Composite candidate definition is incomplete");
        }
        SearchCombinationPolicy searchPolicy = new SearchCombinationPolicy(
                new com.cryptostrategy.platform.strategy.api.model.CombinationPolicyId(
                        String.valueOf(policy.get("policyId"))),
                SemanticVersion.parse(String.valueOf(policy.get("version"))),
                StrategyParameterSet.empty());
        var components = new java.util.ArrayList<CompositeCandidateComponent>();
        var strategies = new java.util.ArrayList<Strategy>();
        var fingerprintComponents = new java.util.ArrayList<StrategyFingerprintCalculator.Component>();
        int lookback = 0;
        for (Object value : componentsRaw) {
            if (!(value instanceof java.util.Map<?, ?> component)) {
                throw new BacktestException(BacktestErrorCode.INVALID_LINEAGE,
                        "Composite component definition is invalid");
            }
            StrategyReference reference = new StrategyReference(
                    new StrategyVersionId(String.valueOf(component.get("strategyVersionId"))),
                    new StrategyPluginId(String.valueOf(component.get("strategyId"))),
                    SemanticVersion.parse(String.valueOf(component.get("strategyVersion"))));
            StrategyParameterSet parameters = parseTypedParameters(component.get("parameters"));
            components.add(new CompositeCandidateComponent(reference, parameters));
            fingerprintComponents.add(new StrategyFingerprintCalculator.Component(reference, parameters));
            strategies.add(registry.create(reference.pluginId(), reference.implementationVersion(),
                    parameters.values()));
            lookback = Math.max(lookback, registry.requiredLookback(
                    reference.pluginId(), reference.implementationVersion(), parameters.values()));
        }
        requireFingerprint(candidate.fingerprint(),
                CompositeSearchCanonicalization.candidateFingerprint(components, searchPolicy));
        if (components.size() == 1) {
            CompositeCandidateComponent only = components.getFirst();
            return new ResolvedStrategy(strategies.getFirst(), lookback,
                    fingerprints.single(only.strategy(), only.parameters()));
        }
        String actual = fingerprints.composite(searchPolicy.policyId(), searchPolicy.version(),
                searchPolicy.parameters(), fingerprintComponents);
        StrategyReference compositeReference = new StrategyReference(
                components.getFirst().strategy().strategyVersionId(),
                new StrategyPluginId("composite"), searchPolicy.version());
        Strategy strategy = composites.materialize(compositeReference,
                new CombinationPolicyReference(searchPolicy.policyId(), searchPolicy.version()), strategies);
        return new ResolvedStrategy(strategy, lookback, actual);
    }

    private static StrategyParameterSet parseTypedParameters(Object raw) {
        if (!(raw instanceof java.util.Map<?, ?> values)) {
            throw new BacktestException(BacktestErrorCode.INVALID_LINEAGE,
                    "Composite component parameters are invalid");
        }
        var result = new java.util.TreeMap<String, StrategyParameterValue>();
        values.forEach((name, typedValue) -> {
            if (!(typedValue instanceof java.util.Map<?, ?> encoded)) {
                throw new BacktestException(BacktestErrorCode.INVALID_LINEAGE,
                        "Composite parameter value is invalid");
            }
            ParameterType type = ParameterType.valueOf(String.valueOf(encoded.get("type")));
            String value = String.valueOf(encoded.get("value"));
            result.put(String.valueOf(name), switch (type) {
                case INTEGER -> new StrategyParameterValue.IntegerValue(Long.parseLong(value));
                case DECIMAL -> new StrategyParameterValue.DecimalValue(new java.math.BigDecimal(value));
                case BOOLEAN -> new StrategyParameterValue.BooleanValue(Boolean.parseBoolean(value));
                case TEXT -> new StrategyParameterValue.TextValue(value);
                case ENUM -> new StrategyParameterValue.EnumValue(value);
            });
        });
        return StrategyParameterSet.of(result);
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

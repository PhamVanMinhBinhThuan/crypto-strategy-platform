package com.cryptostrategy.platform.experiment.api.provenance;

import com.cryptostrategy.platform.strategy.api.model.CombinationPolicyId;
import com.cryptostrategy.platform.strategy.api.model.SemanticVersion;
import com.cryptostrategy.platform.strategy.api.model.StrategyKind;
import com.cryptostrategy.platform.strategy.api.model.StrategyReference;
import com.cryptostrategy.platform.strategy.api.model.UserStrategyVersionId;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Immutable, typed Strategy evidence frozen into an Experiment Manifest. */
public record StrategyProvenanceSnapshot(
        StrategyKind kind,
        Optional<StrategyReference> singleStrategy,
        StrategyParameterSet parameters,
        Optional<CombinationPolicyId> compositePolicyId,
        Optional<SemanticVersion> compositePolicyVersion,
        List<StrategyComponentSnapshot> components,
        Optional<UserStrategyVersionId> sourceUserStrategyVersionId,
        String strategyFingerprint
) {
    public StrategyProvenanceSnapshot {
        Objects.requireNonNull(kind, "kind cannot be null");
        Objects.requireNonNull(singleStrategy, "singleStrategy cannot be null");
        Objects.requireNonNull(parameters, "parameters cannot be null");
        Objects.requireNonNull(compositePolicyId, "compositePolicyId cannot be null");
        Objects.requireNonNull(compositePolicyVersion, "compositePolicyVersion cannot be null");
        components = List.copyOf(Objects.requireNonNull(components, "components cannot be null"));
        Objects.requireNonNull(sourceUserStrategyVersionId, "sourceUserStrategyVersionId cannot be null");
        Objects.requireNonNull(strategyFingerprint, "strategyFingerprint cannot be null");
        if (!strategyFingerprint.matches("^strategy-v1:sha256:[0-9a-f]{64}$")) {
            throw new IllegalArgumentException("Invalid strategy-v1 fingerprint");
        }
        if (kind == StrategyKind.SINGLE) {
            if (singleStrategy.isEmpty() || compositePolicyId.isPresent()
                    || compositePolicyVersion.isPresent() || !components.isEmpty()) {
                throw new IllegalArgumentException("Single Strategy provenance shape is invalid");
            }
        } else if (singleStrategy.isPresent() || compositePolicyId.isEmpty()
                || compositePolicyVersion.isEmpty() || components.size() < 2) {
            throw new IllegalArgumentException("Composite Strategy provenance shape is invalid");
        }
    }

    public static StrategyProvenanceSnapshot single(
            StrategyReference reference,
            StrategyParameterSet parameters,
            Optional<UserStrategyVersionId> sourceVersionId,
            String fingerprint
    ) {
        return new StrategyProvenanceSnapshot(StrategyKind.SINGLE, Optional.of(reference), parameters,
                Optional.empty(), Optional.empty(), List.of(), sourceVersionId, fingerprint);
    }

    public static StrategyProvenanceSnapshot composite(
            CombinationPolicyId policyId,
            SemanticVersion policyVersion,
            StrategyParameterSet policyParameters,
            List<StrategyComponentSnapshot> components,
            Optional<UserStrategyVersionId> sourceVersionId,
            String fingerprint
    ) {
        return new StrategyProvenanceSnapshot(StrategyKind.COMPOSITE, Optional.empty(), policyParameters,
                Optional.of(policyId), Optional.of(policyVersion), components, sourceVersionId, fingerprint);
    }
}

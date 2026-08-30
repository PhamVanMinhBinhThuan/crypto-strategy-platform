package com.cryptostrategy.platform.experiment.api.provenance;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record StrategyProvenanceSnapshot(
        String strategyKind,
        String strategyRefId,
        String strategyVersion,
        Map<String, Object> parameters,
        String compositePolicyId,
        List<StrategyComponentSnapshot> components,
        String sourceUserStrategyVersionId
) {
    public StrategyProvenanceSnapshot {
        Objects.requireNonNull(strategyKind, "strategyKind cannot be null");
        Objects.requireNonNull(strategyRefId, "strategyRefId cannot be null");
        Objects.requireNonNull(strategyVersion, "strategyVersion cannot be null");
        parameters = parameters != null ? Map.copyOf(parameters) : Map.of();
        components = components != null ? List.copyOf(components) : List.of();
    }

    public static StrategyProvenanceSnapshot single(
            String strategyRefId,
            String strategyVersion,
            Map<String, Object> parameters,
            String sourceUserStrategyVersionId
    ) {
        return new StrategyProvenanceSnapshot(
                "SINGLE",
                strategyRefId,
                strategyVersion,
                parameters,
                null,
                List.of(),
                sourceUserStrategyVersionId
        );
    }

    public static StrategyProvenanceSnapshot composite(
            String compositeId,
            String version,
            String compositePolicyId,
            Map<String, Object> parameters,
            List<StrategyComponentSnapshot> components,
            String sourceUserStrategyVersionId
    ) {
        return new StrategyProvenanceSnapshot(
                "COMPOSITE",
                compositeId,
                version,
                parameters,
                compositePolicyId,
                components,
                sourceUserStrategyVersionId
        );
    }
}

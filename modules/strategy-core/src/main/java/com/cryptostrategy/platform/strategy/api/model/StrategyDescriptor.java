package com.cryptostrategy.platform.strategy.api.model;

import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterSchema;
import java.util.Objects;
import java.util.Set;

public record StrategyDescriptor(StrategyReference reference, String contractVersion, String displayName,
        String description, String category, Set<StrategySignal> supportedSignals, int requiredLookback,
        StrategyParameterSchema parameterSchema, String descriptorFingerprint) implements Comparable<StrategyDescriptor> {
    public StrategyDescriptor {
        Objects.requireNonNull(reference); Objects.requireNonNull(contractVersion); Objects.requireNonNull(displayName);
        Objects.requireNonNull(description); Objects.requireNonNull(category); Objects.requireNonNull(supportedSignals);
        Objects.requireNonNull(parameterSchema); Objects.requireNonNull(descriptorFingerprint);
        supportedSignals = Set.copyOf(supportedSignals);
        if (contractVersion.isBlank() || displayName.isBlank() || category.isBlank() || descriptorFingerprint.isBlank()) throw new IllegalArgumentException("Descriptor metadata is blank");
        if (supportedSignals.isEmpty() || requiredLookback < 1) throw new IllegalArgumentException("Invalid Strategy capability");
    }
    @Override public int compareTo(StrategyDescriptor other) { return reference.compareTo(other.reference); }
}

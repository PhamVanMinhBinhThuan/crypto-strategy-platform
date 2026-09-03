package com.cryptostrategy.platform.search.api.model;

import com.cryptostrategy.platform.strategy.api.model.parameter.ParameterType;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

public record GeneratorDescriptor(
        GeneratorId generatorId,
        GeneratorVersion generatorVersion,
        String stateContractVersion,
        Set<ParameterType> supportedParameterKinds,
        String descriptorFingerprint
) {
    public GeneratorDescriptor {
        Objects.requireNonNull(generatorId, "generatorId");
        Objects.requireNonNull(generatorVersion, "generatorVersion");
        stateContractVersion = requireText(stateContractVersion, "stateContractVersion");
        descriptorFingerprint = requireText(descriptorFingerprint, "descriptorFingerprint");
        Objects.requireNonNull(supportedParameterKinds, "supportedParameterKinds");
        if (supportedParameterKinds.isEmpty()) {
            throw new IllegalArgumentException("supportedParameterKinds must not be empty");
        }
        supportedParameterKinds = Collections.unmodifiableSet(EnumSet.copyOf(supportedParameterKinds));
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}

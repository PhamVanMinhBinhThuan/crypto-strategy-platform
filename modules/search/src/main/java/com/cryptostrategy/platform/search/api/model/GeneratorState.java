package com.cryptostrategy.platform.search.api.model;

import java.util.Objects;

public record GeneratorState(
        String contractVersion,
        String canonicalState,
        String fingerprint
) {
    public GeneratorState {
        contractVersion = requireText(contractVersion, "contractVersion");
        canonicalState = requireText(canonicalState, "canonicalState");
        fingerprint = requireText(fingerprint, "fingerprint");
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}

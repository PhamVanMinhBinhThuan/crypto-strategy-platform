package com.cryptostrategy.platform.search.api.model;

import java.util.Objects;

public sealed interface GenerationOutcome permits GenerationOutcome.Generated,
        GenerationOutcome.Exhausted, GenerationOutcome.NoProgress, GenerationOutcome.Rejected {

    record Generated(GeneratedCandidate candidate, GeneratorState nextState) implements GenerationOutcome {
        public Generated {
            Objects.requireNonNull(candidate, "candidate");
            Objects.requireNonNull(nextState, "nextState");
        }
    }

    record Exhausted(GeneratorState finalState) implements GenerationOutcome {
        public Exhausted {
            Objects.requireNonNull(finalState, "finalState");
        }
    }

    record NoProgress(GeneratorState currentState, String reasonCode) implements GenerationOutcome {
        public NoProgress {
            Objects.requireNonNull(currentState, "currentState");
            reasonCode = requireCode(reasonCode);
        }
    }

    record Rejected(String reasonCode) implements GenerationOutcome {
        public Rejected {
            reasonCode = requireCode(reasonCode);
        }
    }

    private static String requireCode(String value) {
        Objects.requireNonNull(value, "reasonCode");
        if (!value.matches("^[A-Z][A-Z0-9_]*$")) {
            throw new IllegalArgumentException("reasonCode must be a stable uppercase code");
        }
        return value;
    }
}

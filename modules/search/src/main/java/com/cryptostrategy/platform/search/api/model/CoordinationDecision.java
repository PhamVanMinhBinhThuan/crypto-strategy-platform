package com.cryptostrategy.platform.search.api.model;

import java.time.Instant;
import java.util.Objects;

/** Append-only evidence for a durable coordination decision. */
public record CoordinationDecision(
        CoordinationDecisionId decisionId,
        SearchRunId searchRunId,
        long sequence,
        CoordinationDecisionType type,
        SearchCandidateId candidateId,
        SearchJobId backtestJobId,
        String candidateFingerprint,
        String stateBeforeFingerprint,
        String stateAfterFingerprint,
        String reasonCode,
        Instant decidedAt
) {
    public CoordinationDecision {
        Objects.requireNonNull(decisionId, "decisionId");
        Objects.requireNonNull(searchRunId, "searchRunId");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(decidedAt, "decidedAt");
        if (sequence < 0) {
            throw new IllegalArgumentException("sequence must be non-negative");
        }
        stateBeforeFingerprint = requireText(stateBeforeFingerprint, "stateBeforeFingerprint");
        stateAfterFingerprint = requireText(stateAfterFingerprint, "stateAfterFingerprint");
        reasonCode = requireCode(reasonCode);

        if (type == CoordinationDecisionType.ALLOCATED) {
            Objects.requireNonNull(candidateId, "candidateId");
            Objects.requireNonNull(backtestJobId, "backtestJobId");
            candidateFingerprint = requireText(candidateFingerprint, "candidateFingerprint");
        } else {
            if (candidateId != null || backtestJobId != null) {
                throw new IllegalArgumentException("candidateId/backtestJobId are only valid for ALLOCATED");
            }
            if (type == CoordinationDecisionType.DUPLICATE_SKIPPED) {
                candidateFingerprint = requireText(candidateFingerprint, "candidateFingerprint");
            }
        }
    }

    private static String requireCode(String value) {
        value = requireText(value, "reasonCode");
        if (!value.matches("^[A-Z][A-Z0-9_]*$")) {
            throw new IllegalArgumentException("reasonCode must be a stable uppercase code");
        }
        return value;
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}

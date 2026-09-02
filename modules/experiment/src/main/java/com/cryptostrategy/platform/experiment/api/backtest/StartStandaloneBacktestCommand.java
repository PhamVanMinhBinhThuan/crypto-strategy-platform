package com.cryptostrategy.platform.experiment.api.backtest;

import com.cryptostrategy.platform.experiment.api.provenance.DatasetProvenanceSnapshot;
import com.cryptostrategy.platform.experiment.api.provenance.StrategyProvenanceSnapshot;
import java.util.Map;
import java.util.Objects;

/** Fully resolved immutable input accepted by the F-005 ownership boundary. */
public record StartStandaloneBacktestCommand(
        String idempotencyKey,
        String canonicalRequestHash,
        DatasetProvenanceSnapshot datasetProvenance,
        StrategyProvenanceSnapshot strategyProvenance,
        Map<String, Object> backtestConfig,
        Map<String, Object> evaluationConfig,
        String softwareVersion,
        String gitCommit,
        String correlationId) {
    private static final int MAX_IDEMPOTENCY_KEY_LENGTH = 255;
    private static final int MAX_CORRELATION_ID_LENGTH = 128;

    public StartStandaloneBacktestCommand {
        idempotencyKey = requireText(idempotencyKey, "idempotencyKey");
        if (idempotencyKey.length() > MAX_IDEMPOTENCY_KEY_LENGTH) {
            throw new IllegalArgumentException("idempotencyKey exceeds 255 characters");
        }
        canonicalRequestHash = requireText(canonicalRequestHash, "canonicalRequestHash");
        if (!canonicalRequestHash.matches("^sha256:[0-9a-f]{64}$")) {
            throw new IllegalArgumentException("canonicalRequestHash must use SHA-256");
        }
        Objects.requireNonNull(datasetProvenance, "datasetProvenance cannot be null");
        Objects.requireNonNull(strategyProvenance, "strategyProvenance cannot be null");
        backtestConfig = Map.copyOf(Objects.requireNonNull(backtestConfig, "backtestConfig"));
        evaluationConfig = Map.copyOf(Objects.requireNonNull(evaluationConfig, "evaluationConfig"));
        softwareVersion = requireText(softwareVersion, "softwareVersion");
        gitCommit = requireText(gitCommit, "gitCommit");
        correlationId = requireText(correlationId, "correlationId");
        if (correlationId.length() > MAX_CORRELATION_ID_LENGTH
                || correlationId.chars().anyMatch(value -> Character.isISOControl((char) value))) {
            throw new IllegalArgumentException("correlationId is invalid");
        }
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " cannot be blank");
        }
        return value;
    }
}

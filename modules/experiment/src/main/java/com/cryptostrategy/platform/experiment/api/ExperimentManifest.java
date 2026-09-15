package com.cryptostrategy.platform.experiment.api;

import com.cryptostrategy.platform.experiment.api.provenance.DatasetProvenanceSnapshot;
import com.cryptostrategy.platform.experiment.api.provenance.SentimentProvenanceSnapshot;
import com.cryptostrategy.platform.experiment.api.provenance.StrategyProvenanceSnapshot;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record ExperimentManifest(
        ExperimentId experimentId,
        String manifestVersion,
        DatasetProvenanceSnapshot datasetProvenance,
        StrategyProvenanceSnapshot strategyProvenance,
        Map<String, Object> backtestConfig,
        Map<String, Object> searchConfig,
        Map<String, Object> evaluationConfig,
        Map<String, Object> sentimentConfig,
        String softwareVersion,
        String gitCommit,
        String fingerprint,
        Instant createdAt
) {
    public ExperimentManifest {
        Objects.requireNonNull(experimentId, "experimentId cannot be null");
        Objects.requireNonNull(manifestVersion, "manifestVersion cannot be null");
        Objects.requireNonNull(datasetProvenance, "datasetProvenance cannot be null");
        Objects.requireNonNull(strategyProvenance, "strategyProvenance cannot be null");
        backtestConfig = backtestConfig != null ? Map.copyOf(backtestConfig) : Map.of();
        searchConfig = searchConfig != null ? Map.copyOf(searchConfig) : Map.of();
        evaluationConfig = evaluationConfig != null ? Map.copyOf(evaluationConfig) : Map.of();
        sentimentConfig = sentimentConfig != null ? Map.copyOf(sentimentConfig) : null;
        Objects.requireNonNull(softwareVersion, "softwareVersion cannot be null");
        Objects.requireNonNull(gitCommit, "gitCommit cannot be null");
        Objects.requireNonNull(createdAt, "createdAt cannot be null");
    }

    public ExperimentManifest withFingerprint(String calculatedFingerprint) {
        return new ExperimentManifest(
                experimentId,
                manifestVersion,
                datasetProvenance,
                strategyProvenance,
                backtestConfig,
                searchConfig,
                evaluationConfig,
                sentimentConfig,
                softwareVersion,
                gitCommit,
                calculatedFingerprint,
                createdAt
        );
    }

    /** Typed F016 view over the backward-compatible sentimentConfig storage field. */
    public Optional<SentimentProvenanceSnapshot> sentimentProvenance() {
        return SentimentProvenanceSnapshot.fromConfig(sentimentConfig);
    }

    /**
     * Returns a draft copy containing canonical Sentiment provenance. The old fingerprint is
     * intentionally cleared because the Manifest content has changed and must be frozen again.
     */
    public ExperimentManifest withSentimentProvenance(SentimentProvenanceSnapshot provenance) {
        Objects.requireNonNull(provenance, "provenance cannot be null");
        return new ExperimentManifest(
                experimentId,
                manifestVersion,
                datasetProvenance,
                strategyProvenance,
                backtestConfig,
                searchConfig,
                evaluationConfig,
                provenance.toConfig(),
                softwareVersion,
                gitCommit,
                null,
                createdAt
        );
    }
}

package com.cryptostrategy.platform.experiment.api.provenance;

import com.cryptostrategy.platform.domain.api.market.AssetId;
import com.cryptostrategy.platform.strategy.api.model.StrategyInputSnapshotId;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable reference to the Sentiment input frozen for an Experiment.
 *
 * <p>The Strategy identity and its exact parameters remain in the enclosing
 * {@link StrategyProvenanceSnapshot}; this value captures the supplemental
 * Sentiment snapshot and model release used by that Strategy.</p>
 */
public record SentimentProvenanceSnapshot(
        StrategyInputSnapshotId snapshotId,
        String snapshotSchemaVersion,
        String snapshotFingerprint,
        AssetId assetId,
        Instant publicationCutoff,
        String modelName,
        String modelVersion,
        String preprocessingVersion,
        String inferenceContractVersion,
        int articleCount
) {
    public static final String PROVENANCE_VERSION = "sentiment-provenance-v1";
    private static final String SHA_256_PATTERN = "^sha256:[0-9a-f]{64}$";

    public SentimentProvenanceSnapshot {
        Objects.requireNonNull(snapshotId, "snapshotId cannot be null");
        snapshotSchemaVersion = required(snapshotSchemaVersion, "snapshotSchemaVersion");
        snapshotFingerprint = required(snapshotFingerprint, "snapshotFingerprint");
        if (!snapshotFingerprint.matches(SHA_256_PATTERN)) {
            throw new IllegalArgumentException("snapshotFingerprint must be a canonical SHA-256 value");
        }
        Objects.requireNonNull(assetId, "assetId cannot be null");
        Objects.requireNonNull(publicationCutoff, "publicationCutoff cannot be null");
        modelName = required(modelName, "modelName");
        modelVersion = required(modelVersion, "modelVersion");
        preprocessingVersion = required(preprocessingVersion, "preprocessingVersion");
        inferenceContractVersion = required(inferenceContractVersion, "inferenceContractVersion");
        if (articleCount < 0) {
            throw new IllegalArgumentException("articleCount cannot be negative");
        }
    }

    /** Returns the stable representation stored in the existing Manifest sentimentConfig column. */
    public Map<String, Object> toConfig() {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("provenanceVersion", PROVENANCE_VERSION);
        config.put("snapshotId", snapshotId.value());
        config.put("schemaVersion", snapshotSchemaVersion);
        config.put("snapshotFingerprint", snapshotFingerprint);
        config.put("assetId", assetId.value());
        config.put("publicationCutoff", publicationCutoff.toString());
        config.put("modelName", modelName);
        config.put("modelVersion", modelVersion);
        config.put("preprocessingVersion", preprocessingVersion);
        config.put("inferenceContractVersion", inferenceContractVersion);
        config.put("articleCount", articleCount);
        return Map.copyOf(config);
    }

    /**
     * Reads the versioned representation while treating pre-F016 maps as legacy configuration.
     */
    public static Optional<SentimentProvenanceSnapshot> fromConfig(Map<String, Object> config) {
        if (config == null || !config.containsKey("provenanceVersion")) {
            return Optional.empty();
        }
        String provenanceVersion = text(config, "provenanceVersion");
        if (!PROVENANCE_VERSION.equals(provenanceVersion)) {
            throw new IllegalArgumentException("Unsupported Sentiment provenance version: " + provenanceVersion);
        }
        return Optional.of(new SentimentProvenanceSnapshot(
                new StrategyInputSnapshotId(text(config, "snapshotId")),
                text(config, "schemaVersion"),
                text(config, "snapshotFingerprint"),
                new AssetId(text(config, "assetId")),
                instant(config, "publicationCutoff"),
                text(config, "modelName"),
                text(config, "modelVersion"),
                text(config, "preprocessingVersion"),
                text(config, "inferenceContractVersion"),
                integer(config, "articleCount")
        ));
    }

    private static String text(Map<String, Object> config, String key) {
        Object value = config.get(key);
        if (!(value instanceof String text)) {
            throw new IllegalArgumentException(key + " must be a string");
        }
        return text;
    }

    private static Instant instant(Map<String, Object> config, String key) {
        try {
            return Instant.parse(text(config, key));
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(key + " must be an ISO-8601 instant", exception);
        }
    }

    private static int integer(Map<String, Object> config, String key) {
        Object value = config.get(key);
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException(key + " must be a number");
        }
        long parsed = number.longValue();
        if (parsed < Integer.MIN_VALUE || parsed > Integer.MAX_VALUE
                || number.doubleValue() != (double) parsed) {
            throw new IllegalArgumentException(key + " must be an integer");
        }
        return (int) parsed;
    }

    private static String required(String value, String name) {
        String normalized = Objects.requireNonNull(value, name + " cannot be null").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(name + " cannot be blank");
        }
        return normalized;
    }
}

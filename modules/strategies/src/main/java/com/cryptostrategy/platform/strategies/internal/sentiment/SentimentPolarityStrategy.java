package com.cryptostrategy.platform.strategies.internal.sentiment;

import com.cryptostrategy.platform.strategy.api.Strategy;
import com.cryptostrategy.platform.strategy.api.model.StrategyContext;
import com.cryptostrategy.platform.strategy.api.model.StrategyDecision;
import com.cryptostrategy.platform.strategy.api.model.StrategyEvidenceValue;
import com.cryptostrategy.platform.strategy.api.model.StrategyObservation;
import com.cryptostrategy.platform.strategy.api.model.StrategyReference;
import com.cryptostrategy.platform.strategy.api.model.StrategySignal;
import com.cryptostrategy.platform.strategy.api.model.StrategySupplementalInput;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class SentimentPolarityStrategy implements Strategy {
    private static final int SCORE_SCALE = 10;

    private final StrategyReference reference;
    private final int lookbackHours;
    private final int minimumArticles;
    private final BigDecimal buyThreshold;
    private final BigDecimal sellThreshold;

    public SentimentPolarityStrategy(StrategyReference reference, int lookbackHours,
            int minimumArticles, BigDecimal buyThreshold, BigDecimal sellThreshold) {
        this.reference = Objects.requireNonNull(reference, "reference");
        this.buyThreshold = Objects.requireNonNull(buyThreshold, "buyThreshold");
        this.sellThreshold = Objects.requireNonNull(sellThreshold, "sellThreshold");
        if (lookbackHours < 1 || minimumArticles < 1) {
            throw new IllegalArgumentException("Lookback and minimumArticles must be positive");
        }
        if (buyThreshold.signum() <= 0 || buyThreshold.compareTo(BigDecimal.ONE) > 0
                || sellThreshold.signum() >= 0 || sellThreshold.compareTo(BigDecimal.ONE.negate()) < 0
                || sellThreshold.compareTo(buyThreshold) >= 0) {
            throw new IllegalArgumentException("Expected -1 <= sellThreshold < 0 < buyThreshold <= 1");
        }
        this.lookbackHours = lookbackHours;
        this.minimumArticles = minimumArticles;
    }

    @Override
    public StrategyDecision evaluate(StrategyContext context) {
        Objects.requireNonNull(context, "context");
        StrategySupplementalInput input = context.requireSupplementalInput(
                SentimentPolarityPlugin.INPUT_TYPE);
        if (!SentimentPolarityPlugin.INPUT_SCHEMA_VERSION.equals(input.schemaVersion())) {
            throw new IllegalArgumentException("Unsupported Sentiment input schema: " + input.schemaVersion());
        }
        String modelVersion = requiredMetadata(input, "modelVersion");
        Instant windowStart = context.evaluationTime().minus(Duration.ofHours(lookbackHours));
        List<StrategyObservation> eligible = input.observations().stream()
                .filter(value -> !value.occurredAt().isBefore(windowStart))
                .filter(value -> !value.occurredAt().isAfter(context.evaluationTime()))
                .toList();

        Map<String, StrategyEvidenceValue> evidence = baseEvidence(input, modelVersion, eligible);
        if (eligible.size() < minimumArticles) {
            return decision(context, StrategySignal.HOLD, "INSUFFICIENT_SENTIMENT_ARTICLES",
                    "Not enough eligible News sentiment observations", evidence);
        }

        BigDecimal weightedPolarity = BigDecimal.ZERO;
        BigDecimal totalWeight = BigDecimal.ZERO;
        for (StrategyObservation observation : eligible) {
            if (observation.value().compareTo(BigDecimal.ONE.negate()) < 0
                    || observation.value().compareTo(BigDecimal.ONE) > 0) {
                throw new IllegalArgumentException("Sentiment polarity is outside [-1, 1]");
            }
            weightedPolarity = weightedPolarity.add(observation.value().multiply(observation.weight()));
            totalWeight = totalWeight.add(observation.weight());
        }
        if (totalWeight.signum() == 0) {
            return decision(context, StrategySignal.HOLD, "ZERO_SENTIMENT_WEIGHT",
                    "Eligible News has zero total confidence", evidence);
        }

        BigDecimal score = weightedPolarity.divide(totalWeight, SCORE_SCALE, RoundingMode.HALF_EVEN);
        var withScore = new LinkedHashMap<>(evidence);
        withScore.put("sentimentScore", new StrategyEvidenceValue.DecimalEvidence(score));
        if (score.compareTo(buyThreshold) >= 0) {
            return decision(context, StrategySignal.BUY, "SENTIMENT_BUY_THRESHOLD",
                    "Weighted sentiment reached the configured BUY threshold", withScore);
        }
        if (score.compareTo(sellThreshold) <= 0) {
            return decision(context, StrategySignal.SELL, "SENTIMENT_SELL_THRESHOLD",
                    "Weighted sentiment reached the configured SELL threshold", withScore);
        }
        return decision(context, StrategySignal.HOLD, "SENTIMENT_BETWEEN_THRESHOLDS",
                "Weighted sentiment is between the configured thresholds", withScore);
    }

    private Map<String, StrategyEvidenceValue> baseEvidence(StrategySupplementalInput input,
            String modelVersion, List<StrategyObservation> eligible) {
        return Map.of(
                "buyThreshold", new StrategyEvidenceValue.DecimalEvidence(buyThreshold),
                "eligibleArticleCount", new StrategyEvidenceValue.IntegerEvidence(eligible.size()),
                "lookbackHours", new StrategyEvidenceValue.IntegerEvidence(lookbackHours),
                "modelVersion", new StrategyEvidenceValue.TextEvidence(modelVersion),
                "sellThreshold", new StrategyEvidenceValue.DecimalEvidence(sellThreshold),
                "sentimentEvidenceFingerprint", new StrategyEvidenceValue.TextEvidence(fingerprint(eligible)),
                "sentimentSnapshotFingerprint", new StrategyEvidenceValue.TextEvidence(input.snapshotFingerprint()),
                "sentimentSnapshotId", new StrategyEvidenceValue.TextEvidence(input.snapshotId().value()));
    }

    private StrategyDecision decision(StrategyContext context, StrategySignal signal,
            String reasonCode, String reason, Map<String, StrategyEvidenceValue> evidence) {
        return new StrategyDecision(signal, context.evaluationTime(), reference, reasonCode, reason, evidence);
    }

    private static String requiredMetadata(StrategySupplementalInput input, String name) {
        String value = input.metadata().get(name);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Missing input metadata: " + name);
        return value;
    }

    private static String fingerprint(List<StrategyObservation> observations) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update("sentiment-evidence-v1\n".getBytes(StandardCharsets.UTF_8));
            for (StrategyObservation value : observations) {
                String canonical = value.identity().value() + "|" + value.sourceReference() + "|"
                        + value.occurredAt() + "|" + value.value().toPlainString() + "|"
                        + value.weight().toPlainString() + "\n";
                digest.update(canonical.getBytes(StandardCharsets.UTF_8));
            }
            return "sha256:" + java.util.HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is unavailable", error);
        }
    }
}

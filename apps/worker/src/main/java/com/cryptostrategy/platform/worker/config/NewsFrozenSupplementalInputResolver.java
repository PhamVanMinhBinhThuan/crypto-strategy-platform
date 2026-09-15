package com.cryptostrategy.platform.worker.config;

import com.cryptostrategy.platform.backtesting.api.port.out.FrozenSupplementalInputResolver;
import com.cryptostrategy.platform.experiment.api.ExperimentManifest;
import com.cryptostrategy.platform.experiment.api.provenance.SentimentProvenanceSnapshot;
import com.cryptostrategy.platform.news.api.model.SentimentSnapshot;
import com.cryptostrategy.platform.news.api.model.SentimentSnapshotId;
import com.cryptostrategy.platform.news.api.port.in.GetSentimentSnapshotUseCase;
import com.cryptostrategy.platform.strategy.api.model.StrategyInputSnapshotId;
import com.cryptostrategy.platform.strategy.api.model.StrategyObservation;
import com.cryptostrategy.platform.strategy.api.model.StrategyObservationId;
import com.cryptostrategy.platform.strategy.api.model.StrategySupplementalInput;
import java.util.Map;
import java.util.Objects;

/** Loads only the immutable News snapshot referenced by a frozen Experiment manifest. */
public final class NewsFrozenSupplementalInputResolver implements FrozenSupplementalInputResolver {
    static final String INPUT_TYPE = "sentiment-polarity";

    private final GetSentimentSnapshotUseCase snapshots;

    public NewsFrozenSupplementalInputResolver(GetSentimentSnapshotUseCase snapshots) {
        this.snapshots = Objects.requireNonNull(snapshots, "snapshots");
    }

    @Override
    public Map<String, StrategySupplementalInput> resolve(ExperimentManifest manifest) {
        Objects.requireNonNull(manifest, "manifest");
        return manifest.sentimentProvenance()
                .map(this::resolve)
                .map(input -> Map.of(INPUT_TYPE, input))
                .orElseGet(Map::of);
    }

    private StrategySupplementalInput resolve(SentimentProvenanceSnapshot provenance) {
        SentimentSnapshot snapshot = snapshots.get(
                new SentimentSnapshotId(provenance.snapshotId().value()));
        verify(snapshot, provenance);
        return new StrategySupplementalInput(
                INPUT_TYPE,
                snapshot.schemaVersion(),
                new StrategyInputSnapshotId(snapshot.snapshotId().value()),
                snapshot.fingerprint(),
                Map.of(
                        "assetId", snapshot.assetId().value(),
                        "inferenceContractVersion", snapshot.release().contractVersion(),
                        "modelName", snapshot.release().modelName(),
                        "modelVersion", snapshot.release().modelVersion(),
                        "preprocessingVersion", snapshot.release().preprocessingVersion(),
                        "publicationCutoff", snapshot.publicationCutoff().toString()),
                snapshot.observations().stream()
                        .map(observation -> new StrategyObservation(
                                new StrategyObservationId(observation.newsId().value()),
                                observation.sentimentResultId().value(),
                                observation.publishedAt(),
                                observation.polarityScore(),
                                observation.confidence(),
                                Map.of(
                                        "analyzedAt", observation.analyzedAt().toString(),
                                        "assetId", observation.assetId().value(),
                                        "contentHash", observation.contentHash().value())))
                        .toList());
    }

    private static void verify(
            SentimentSnapshot snapshot, SentimentProvenanceSnapshot provenance) {
        boolean matches = snapshot.snapshotId().value().equals(provenance.snapshotId().value())
                && snapshot.schemaVersion().equals(provenance.snapshotSchemaVersion())
                && snapshot.fingerprint().equals(provenance.snapshotFingerprint())
                && snapshot.assetId().equals(provenance.assetId())
                && snapshot.publicationCutoff().equals(provenance.publicationCutoff())
                && snapshot.release().modelName().equals(provenance.modelName())
                && snapshot.release().modelVersion().equals(provenance.modelVersion())
                && snapshot.release().preprocessingVersion().equals(provenance.preprocessingVersion())
                && snapshot.release().contractVersion().equals(provenance.inferenceContractVersion())
                && snapshot.observationCount() == provenance.articleCount();
        if (!matches) {
            throw new IllegalStateException(
                    "Stored Sentiment snapshot does not match frozen experiment provenance");
        }
    }
}

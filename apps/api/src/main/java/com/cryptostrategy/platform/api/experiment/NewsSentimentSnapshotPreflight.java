package com.cryptostrategy.platform.api.experiment;

import com.cryptostrategy.platform.execution.api.port.out.SentimentSnapshotPreflight;
import com.cryptostrategy.platform.experiment.api.provenance.SentimentProvenanceSnapshot;
import com.cryptostrategy.platform.news.api.model.SentimentModelRelease;
import com.cryptostrategy.platform.news.api.model.SentimentSnapshot;
import com.cryptostrategy.platform.news.api.model.SentimentSnapshotId;
import com.cryptostrategy.platform.news.api.port.in.CreateSentimentSnapshotUseCase;
import com.cryptostrategy.platform.news.api.port.in.GetSentimentSnapshotUseCase;
import com.cryptostrategy.platform.strategy.api.model.StrategyInputSnapshotId;
import java.util.Objects;

/** Maps the News-owned snapshot use cases to Experiment Execution's preflight boundary. */
public final class NewsSentimentSnapshotPreflight implements SentimentSnapshotPreflight {
    private final CreateSentimentSnapshotUseCase createSnapshots;
    private final GetSentimentSnapshotUseCase getSnapshots;
    private final SentimentModelRelease activeRelease;

    public NewsSentimentSnapshotPreflight(
            CreateSentimentSnapshotUseCase createSnapshots,
            GetSentimentSnapshotUseCase getSnapshots,
            SentimentModelRelease activeRelease) {
        this.createSnapshots = Objects.requireNonNull(createSnapshots, "createSnapshots");
        this.getSnapshots = Objects.requireNonNull(getSnapshots, "getSnapshots");
        this.activeRelease = Objects.requireNonNull(activeRelease, "activeRelease");
    }

    @Override
    public SentimentProvenanceSnapshot freeze(FreezeRequest request) {
        Objects.requireNonNull(request, "request");
        SentimentSnapshot snapshot = createSnapshots.create(
                new CreateSentimentSnapshotUseCase.Command(
                        request.assetId(), request.publicationCutoff(), activeRelease));
        return toProvenance(snapshot);
    }

    @Override
    public void verify(SentimentProvenanceSnapshot provenance) {
        Objects.requireNonNull(provenance, "provenance");
        SentimentSnapshot snapshot = getSnapshots.get(
                new SentimentSnapshotId(provenance.snapshotId().value()));
        if (!provenance.equals(toProvenance(snapshot))) {
            throw new IllegalStateException(
                    "Stored Sentiment snapshot does not match frozen experiment provenance");
        }
    }

    static SentimentProvenanceSnapshot toProvenance(SentimentSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        return new SentimentProvenanceSnapshot(
                new StrategyInputSnapshotId(snapshot.snapshotId().value()),
                snapshot.schemaVersion(),
                snapshot.fingerprint(),
                snapshot.assetId(),
                snapshot.publicationCutoff(),
                snapshot.release().modelName(),
                snapshot.release().modelVersion(),
                snapshot.release().preprocessingVersion(),
                snapshot.release().contractVersion(),
                snapshot.observationCount());
    }
}

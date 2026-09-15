package com.cryptostrategy.platform.news.api.port.in;

import com.cryptostrategy.platform.domain.api.market.AssetId;
import com.cryptostrategy.platform.news.api.model.SentimentModelRelease;
import com.cryptostrategy.platform.news.api.model.SentimentSnapshot;
import java.time.Instant;
import java.util.Objects;

public interface CreateSentimentSnapshotUseCase {
    SentimentSnapshot create(Command command);

    record Command(AssetId assetId, Instant publicationCutoff, SentimentModelRelease release) {
        public Command {
            Objects.requireNonNull(assetId); Objects.requireNonNull(publicationCutoff);
            Objects.requireNonNull(release);
        }
    }
}

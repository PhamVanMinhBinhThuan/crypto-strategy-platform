package com.cryptostrategy.platform.news.api.port.out;

import com.cryptostrategy.platform.domain.api.market.AssetId;
import com.cryptostrategy.platform.news.api.model.SentimentModelRelease;
import com.cryptostrategy.platform.news.api.model.SentimentSnapshotObservation;
import java.time.Instant;
import java.util.List;

public interface SentimentSnapshotSource {
    List<SentimentSnapshotObservation> findForSnapshot(
            AssetId assetId, Instant publicationCutoff, SentimentModelRelease release);
}

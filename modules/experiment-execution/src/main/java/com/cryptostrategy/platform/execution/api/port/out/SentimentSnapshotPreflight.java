package com.cryptostrategy.platform.execution.api.port.out;

import com.cryptostrategy.platform.domain.api.market.AssetId;
import com.cryptostrategy.platform.experiment.api.provenance.SentimentProvenanceSnapshot;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Cross-capability boundary used to freeze new Sentiment input or verify an existing frozen input.
 */
public interface SentimentSnapshotPreflight {
    SentimentProvenanceSnapshot freeze(FreezeRequest request);

    void verify(SentimentProvenanceSnapshot provenance);

    static SentimentSnapshotPreflight unavailable() {
        return new SentimentSnapshotPreflight() {
            @Override
            public SentimentProvenanceSnapshot freeze(FreezeRequest request) {
                throw new IllegalStateException("Sentiment snapshot preflight is not configured");
            }

            @Override
            public void verify(SentimentProvenanceSnapshot provenance) {
                throw new IllegalStateException("Sentiment snapshot resolver is not configured");
            }
        };
    }

    record FreezeRequest(UUID ownerUserId, AssetId assetId, Instant publicationCutoff) {
        public FreezeRequest {
            Objects.requireNonNull(ownerUserId, "ownerUserId");
            Objects.requireNonNull(assetId, "assetId");
            Objects.requireNonNull(publicationCutoff, "publicationCutoff");
        }
    }
}

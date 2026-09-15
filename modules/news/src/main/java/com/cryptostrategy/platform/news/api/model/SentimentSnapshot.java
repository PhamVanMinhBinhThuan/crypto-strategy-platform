package com.cryptostrategy.platform.news.api.model;

import com.cryptostrategy.platform.domain.api.market.AssetId;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public record SentimentSnapshot(
        SentimentSnapshotId snapshotId,
        String schemaVersion,
        AssetId assetId,
        SentimentModelRelease release,
        Instant publicationCutoff,
        List<SentimentSnapshotObservation> observations,
        String fingerprint,
        Instant createdAt
) {
    public static final String SCHEMA_VERSION = "sentiment-snapshot-v1";

    public SentimentSnapshot {
        Objects.requireNonNull(snapshotId); schemaVersion = required(schemaVersion, "schemaVersion");
        Objects.requireNonNull(assetId); Objects.requireNonNull(release);
        Objects.requireNonNull(publicationCutoff); fingerprint = required(fingerprint, "fingerprint");
        Objects.requireNonNull(createdAt);
        observations = Objects.requireNonNull(observations).stream().sorted().toList();
        var identities = new HashSet<NewsId>();
        for (var observation : observations) {
            if (!assetId.equals(observation.assetId())) throw new IllegalArgumentException("Mixed snapshot asset");
            if (observation.publishedAt().isAfter(publicationCutoff)) throw new IllegalArgumentException("Future News in snapshot");
            if (!identities.add(observation.newsId())) throw new IllegalArgumentException("Duplicate News identity");
        }
    }

    public int observationCount() { return observations.size(); }

    private static String required(String value, String name) {
        value = Objects.requireNonNull(value, name).trim();
        if (value.isEmpty()) throw new IllegalArgumentException(name + " is blank");
        return value;
    }
}

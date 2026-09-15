package com.cryptostrategy.platform.news.internal.application;

import com.cryptostrategy.platform.news.api.error.NewsErrorCode;
import com.cryptostrategy.platform.news.api.error.NewsException;
import com.cryptostrategy.platform.news.api.model.SentimentSnapshot;
import com.cryptostrategy.platform.news.api.model.SentimentSnapshotId;
import com.cryptostrategy.platform.news.api.model.SentimentSnapshotObservation;
import com.cryptostrategy.platform.news.api.port.in.CreateSentimentSnapshotUseCase;
import com.cryptostrategy.platform.news.api.port.in.GetSentimentSnapshotUseCase;
import com.cryptostrategy.platform.news.api.port.out.SentimentSnapshotSource;
import com.cryptostrategy.platform.news.api.port.out.SentimentSnapshotStore;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

public final class SentimentSnapshotService
        implements CreateSentimentSnapshotUseCase, GetSentimentSnapshotUseCase {
    private final SentimentSnapshotSource source;
    private final SentimentSnapshotStore store;
    private final Clock clock;

    public SentimentSnapshotService(SentimentSnapshotSource source,
            SentimentSnapshotStore store, Clock clock) {
        this.source = Objects.requireNonNull(source);
        this.store = Objects.requireNonNull(store);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public SentimentSnapshot create(Command command) {
        Objects.requireNonNull(command);
        List<SentimentSnapshotObservation> observations = source.findForSnapshot(
                command.assetId(), command.publicationCutoff(), command.release()).stream()
                .filter(value -> !value.publishedAt().isAfter(command.publicationCutoff()))
                .sorted()
                .toList();
        if (observations.isEmpty()) {
            throw new NewsException(NewsErrorCode.SENTIMENT_SNAPSHOT_UNAVAILABLE,
                    "No analyzed Sentiment is available for the selected asset and Dataset range");
        }
        String fingerprint = fingerprint(command, observations);
        var snapshot = new SentimentSnapshot(SentimentSnapshotId.generate(),
                SentimentSnapshot.SCHEMA_VERSION, command.assetId(), command.release(),
                command.publicationCutoff(), observations, fingerprint, clock.instant());
        return store.save(snapshot);
    }

    @Override
    public SentimentSnapshot get(SentimentSnapshotId snapshotId) {
        return store.find(Objects.requireNonNull(snapshotId)).orElseThrow(() ->
                new NewsException(NewsErrorCode.SENTIMENT_SNAPSHOT_UNAVAILABLE,
                        "Sentiment snapshot is unavailable"));
    }

    private static String fingerprint(Command command,
            List<SentimentSnapshotObservation> observations) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            append(digest, SentimentSnapshot.SCHEMA_VERSION);
            append(digest, command.assetId().value());
            append(digest, command.publicationCutoff().toString());
            append(digest, command.release().modelName());
            append(digest, command.release().modelVersion());
            append(digest, command.release().preprocessingVersion());
            append(digest, command.release().contractVersion());
            for (var value : observations) {
                append(digest, value.newsId().value());
                append(digest, value.sentimentResultId().value());
                append(digest, value.publishedAt().toString());
                append(digest, value.contentHash().value());
                append(digest, value.polarityScore().toPlainString());
                append(digest, value.confidence().toPlainString());
            }
            return "sha256:" + HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is unavailable", error);
        }
    }

    private static void append(MessageDigest digest, String value) {
        digest.update(value.getBytes(StandardCharsets.UTF_8));
        digest.update((byte) '\n');
    }
}

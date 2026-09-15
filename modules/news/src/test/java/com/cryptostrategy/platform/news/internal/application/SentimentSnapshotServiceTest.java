package com.cryptostrategy.platform.news.internal.application;

import static org.junit.jupiter.api.Assertions.*;

import com.cryptostrategy.platform.domain.api.market.AssetId;
import com.cryptostrategy.platform.news.api.error.NewsErrorCode;
import com.cryptostrategy.platform.news.api.error.NewsException;
import com.cryptostrategy.platform.news.api.model.ContentHash;
import com.cryptostrategy.platform.news.api.model.NewsId;
import com.cryptostrategy.platform.news.api.model.SentimentModelRelease;
import com.cryptostrategy.platform.news.api.model.SentimentResultId;
import com.cryptostrategy.platform.news.api.model.SentimentSnapshot;
import com.cryptostrategy.platform.news.api.model.SentimentSnapshotId;
import com.cryptostrategy.platform.news.api.model.SentimentSnapshotObservation;
import com.cryptostrategy.platform.news.api.port.in.CreateSentimentSnapshotUseCase;
import com.cryptostrategy.platform.news.api.port.out.SentimentSnapshotStore;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SentimentSnapshotServiceTest {
    private static final AssetId BTC = new AssetId("01J00000000000000000000001");
    private static final Instant CUTOFF = Instant.parse("2026-09-09T12:00:00Z");
    private static final SentimentModelRelease RELEASE = new SentimentModelRelease(
            "multichannel-english-1", "multichannel-english", "news-text-v1", "sentiment-v1");

    @Test
    void freezesCanonicalObservationsAndExcludesFutureNews() {
        var store = new InMemoryStore();
        var service = new SentimentSnapshotService((asset, cutoff, release) -> List.of(
                observation("02", "2026-09-09T11:00:00Z", "0.4"),
                observation("01", "2026-09-09T10:00:00Z", "-0.2"),
                observation("03", "2026-09-09T12:00:01Z", "1")), store,
                Clock.fixed(CUTOFF.plusSeconds(10), ZoneOffset.UTC));

        SentimentSnapshot snapshot = service.create(command());

        assertEquals(2, snapshot.observationCount());
        assertEquals("01J00000000000000000000001",
                snapshot.observations().getFirst().newsId().value());
        assertTrue(snapshot.fingerprint().matches("sha256:[0-9a-f]{64}"));
        assertEquals(snapshot, service.get(snapshot.snapshotId()));
    }

    @Test
    void sameEvidenceProducesSameFingerprintIndependentOfSourceOrder() {
        var first = create(List.of(
                observation("02", "2026-09-09T11:00:00Z", "0.4"),
                observation("01", "2026-09-09T10:00:00Z", "-0.2")));
        var second = create(List.of(
                observation("01", "2026-09-09T10:00:00Z", "-0.2"),
                observation("02", "2026-09-09T11:00:00Z", "0.4")));

        assertEquals(first.fingerprint(), second.fingerprint());
    }

    @Test
    void refusesAnewSnapshotWhenNoPersistedAnalysisExists() {
        var service = new SentimentSnapshotService((asset, cutoff, release) -> List.of(),
                new InMemoryStore(), Clock.systemUTC());

        NewsException error = assertThrows(NewsException.class, () -> service.create(command()));

        assertEquals(NewsErrorCode.SENTIMENT_SNAPSHOT_UNAVAILABLE, error.code());
    }

    private static SentimentSnapshot create(List<SentimentSnapshotObservation> observations) {
        return new SentimentSnapshotService((asset, cutoff, release) -> observations,
                new InMemoryStore(), Clock.fixed(CUTOFF, ZoneOffset.UTC)).create(command());
    }

    private static CreateSentimentSnapshotUseCase.Command command() {
        return new CreateSentimentSnapshotUseCase.Command(BTC, CUTOFF, RELEASE);
    }

    private static SentimentSnapshotObservation observation(String suffix, String publishedAt,
            String polarity) {
        return new SentimentSnapshotObservation(
                new NewsId("01J000000000000000000000" + suffix),
                new SentimentResultId("01J000000000000000000001" + suffix),
                BTC, Instant.parse(publishedAt), Instant.parse("2026-09-09T12:01:00Z"),
                new ContentHash("sha256:" + suffix.repeat(32)), new BigDecimal(polarity),
                new BigDecimal("0.8"));
    }

    private static final class InMemoryStore implements SentimentSnapshotStore {
        private final Map<SentimentSnapshotId, SentimentSnapshot> values = new HashMap<>();
        @Override public SentimentSnapshot save(SentimentSnapshot snapshot) {
            values.put(snapshot.snapshotId(), snapshot); return snapshot;
        }
        @Override public Optional<SentimentSnapshot> find(SentimentSnapshotId snapshotId) {
            return Optional.ofNullable(values.get(snapshotId));
        }
    }
}

package com.cryptostrategy.platform.api.news;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.cryptostrategy.platform.api.transport.PageRequestMapper;
import com.cryptostrategy.platform.marketdata.api.port.in.GetTradingPairUseCase;
import com.cryptostrategy.platform.news.api.NewsModuleFactory;
import com.cryptostrategy.platform.news.api.NewsNormalizationPolicy;
import com.cryptostrategy.platform.news.api.model.AnalysisLease;
import com.cryptostrategy.platform.news.api.model.AnalysisStatus;
import com.cryptostrategy.platform.news.api.model.CanonicalNewsUrl;
import com.cryptostrategy.platform.news.api.model.ContentHash;
import com.cryptostrategy.platform.news.api.model.LanguageCode;
import com.cryptostrategy.platform.news.api.model.NewsId;
import com.cryptostrategy.platform.news.api.model.NewsItem;
import com.cryptostrategy.platform.news.api.model.NewsSource;
import com.cryptostrategy.platform.news.api.model.SentimentLabel;
import com.cryptostrategy.platform.news.api.model.SentimentModelRelease;
import com.cryptostrategy.platform.news.api.model.SentimentResult;
import com.cryptostrategy.platform.news.api.model.SentimentResultId;
import com.cryptostrategy.platform.news.api.port.in.ListNewsUseCase;
import com.cryptostrategy.platform.news.api.port.in.NewsAnalysisUseCase;
import com.cryptostrategy.platform.news.api.port.out.AnalysisWorkStore;
import com.cryptostrategy.platform.news.api.port.out.NewsQueryPort;
import com.cryptostrategy.platform.news.api.port.out.SentimentModelReleaseStore;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

class NewsDegradedIsolationTest {
    private static final Instant NOW = Instant.parse("2026-09-04T04:00:00Z");
    private static final NewsId NEWS_ID = new NewsId("01J00000000000000000000001");
    private static final String FIRST_LEASE = "01J00000000000000000000002";
    private static final String RETRY_LEASE = "01J00000000000000000000003";
    private static final ContentHash HASH = new ContentHash("sha256:" + "a".repeat(64));
    private static final SentimentModelRelease RELEASE =
            new SentimentModelRelease("1.0.0", "multichannel-english", "whitespace-en-1", "sentiment-v1");

    @Test
    void transientSentimentFailureStaysReadableThenRecoversAfterTheDurableRetryBoundary() {
        GetTradingPairUseCase pairs = mock(GetTradingPairUseCase.class);
        var state = new RecoverableNewsState();
        var components = NewsModuleFactory.create(
                new NewsModuleFactory.Dependencies(
                        List.of(),
                        item -> com.cryptostrategy.platform.news.api.port.out.NewsItemStore.SaveOutcome.INSERTED,
                        state,
                        state,
                        newsId -> Optional.empty(),
                        symbols -> Map.of(),
                        request -> CompletableFuture.failedFuture(new AssertionError("inference is driven by the worker")),
                        new NoOpReleaseStore(),
                        unusedNormalization(),
                        Clock.fixed(NOW, ZoneOffset.UTC)),
                new NewsModuleFactory.Settings(
                        RELEASE,
                        new NewsModuleFactory.AnalysisPolicy(
                                Duration.ofMinutes(2),
                                10,
                                3,
                                List.of(Duration.ofSeconds(5), Duration.ofSeconds(30)))));
        var controller = new NewsController(
                components.queries(),
                new NewsQueryMapper(pairs, new PageRequestMapper()));

        components.analysis().fail(new NewsAnalysisUseCase.Fail(NEWS_ID, FIRST_LEASE, true, 1));

        var degraded = controller.list(null, Set.of(), null, 20).items().getFirst();
        assertThat(degraded.analysisStatus()).isEqualTo("FAILED_RETRYABLE");
        assertThat(degraded.sentiment()).isEmpty();
        assertThat(components.analysis().acquire(acquireAt(NOW.plusSeconds(4)))).isEmpty();

        var retry = components.analysis().acquire(acquireAt(NOW.plusSeconds(5))).getFirst();
        assertThat(retry.analysisStatus()).isEqualTo(AnalysisStatus.ANALYZING);
        assertThat(components.analysis().startAttempt(new NewsAnalysisUseCase.StartAttempt(
                        NEWS_ID,
                        RETRY_LEASE,
                        HASH,
                        RELEASE.modelVersion())))
                .isTrue();
        components.analysis().complete(new NewsAnalysisUseCase.Complete(
                NEWS_ID,
                RETRY_LEASE,
                new SentimentResult(
                        SentimentResultId.generate(),
                        NEWS_ID,
                        HASH,
                        LanguageCode.ENGLISH,
                        RELEASE,
                        SentimentLabel.POSITIVE,
                        new BigDecimal("0.91"),
                        new BigDecimal("0.72"),
                        NOW.plusSeconds(6))));

        var recovered = controller.list(null, Set.of(), null, 20).items().getFirst();
        assertThat(recovered.analysisStatus()).isEqualTo("ANALYZED");
        assertThat(recovered.sentiment()).hasValueSatisfying(sentiment -> {
            assertThat(sentiment.label()).isEqualTo("POSITIVE");
            assertThat(sentiment.confidence()).isEqualTo("0.91");
            assertThat(sentiment.polarityScore()).isEqualTo("0.72");
        });
        verifyNoInteractions(pairs);
    }

    private static NewsAnalysisUseCase.Acquire acquireAt(Instant now) {
        return new NewsAnalysisUseCase.Acquire("news-worker", now, Duration.ofMinutes(2), 10);
    }

    private static NewsNormalizationPolicy unusedNormalization() {
        return (url, title, content, language) -> {
            throw new AssertionError("normalization is not part of analysis recovery");
        };
    }

    private static final class NoOpReleaseStore implements SentimentModelReleaseStore {
        @Override
        public void registerOrVerify(SentimentModelRelease release) {}

        @Override
        public void activateForEnglish(String modelVersion) {}
    }

    private static final class RecoverableNewsState implements AnalysisWorkStore, NewsQueryPort {
        private AnalysisStatus status = AnalysisStatus.ANALYZING;
        private int attempts = 1;
        private Instant nextEligibleAt;
        private SentimentResult result;

        @Override
        public List<NewsItem> claim(String owner, Instant now, Duration leaseDuration, int limit) {
            if (status != AnalysisStatus.FAILED_RETRYABLE || now.isBefore(nextEligibleAt)) {
                return List.of();
            }
            status = AnalysisStatus.ANALYZING;
            nextEligibleAt = null;
            return List.of(new NewsItem(
                    NEWS_ID,
                    "News remains available",
                    "Sentiment can recover independently",
                    NOW,
                    NOW,
                    HASH,
                    status,
                    new NewsSource("fixture-provider"),
                    new CanonicalNewsUrl("https://example.test/degraded"),
                    LanguageCode.ENGLISH,
                    Optional.empty(),
                    Optional.of(RELEASE.modelVersion()),
                    Optional.of(new AnalysisLease(
                            owner,
                            RETRY_LEASE,
                            now.plus(leaseDuration),
                            attempts,
                            RELEASE.modelVersion())),
                    Optional.empty(),
                    attempts,
                    List.of()));
        }

        @Override
        public boolean reserveAttempt(NewsId newsId, String leaseToken, ContentHash hash, String modelVersion) {
            if (status != AnalysisStatus.ANALYZING
                    || !NEWS_ID.equals(newsId)
                    || !RETRY_LEASE.equals(leaseToken)
                    || !HASH.equals(hash)
                    || !RELEASE.modelVersion().equals(modelVersion)) {
                return false;
            }
            attempts++;
            return true;
        }

        @Override
        public void defer(NewsId newsId, String leaseToken, Instant eligibleAt) {
            transitionToRetry(newsId, leaseToken, eligibleAt);
        }

        @Override
        public void fail(NewsId newsId, String leaseToken, boolean retryable, Instant eligibleAt) {
            assertThat(newsId).isEqualTo(NEWS_ID);
            if (retryable) {
                transitionToRetry(newsId, leaseToken, eligibleAt);
            } else {
                status = AnalysisStatus.FAILED;
                nextEligibleAt = null;
            }
        }

        @Override
        public void complete(NewsId newsId, String leaseToken, SentimentResult completed) {
            assertThat(newsId).isEqualTo(NEWS_ID);
            assertThat(leaseToken).isEqualTo(RETRY_LEASE);
            result = completed;
            status = AnalysisStatus.ANALYZED;
            nextEligibleAt = null;
        }

        @Override
        public ListNewsUseCase.Page list(ListNewsUseCase.Query query) {
            Optional<String> label = result == null ? Optional.empty() : Optional.of(result.label().name());
            Optional<BigDecimal> confidence = result == null ? Optional.empty() : Optional.of(result.confidence());
            Optional<BigDecimal> polarity = result == null ? Optional.empty() : Optional.of(result.polarityScore());
            return new ListNewsUseCase.Page(
                    List.of(new ListNewsUseCase.Item(
                            NEWS_ID,
                            "News remains available",
                            "fixture-provider",
                            "https://example.test/degraded",
                            NOW,
                            status,
                            List.of(),
                            label,
                            confidence,
                            polarity)),
                    Optional.empty());
        }

        private void transitionToRetry(NewsId newsId, String leaseToken, Instant eligibleAt) {
            assertThat(newsId).isEqualTo(NEWS_ID);
            assertThat(leaseToken).isIn(FIRST_LEASE, RETRY_LEASE);
            status = AnalysisStatus.FAILED_RETRYABLE;
            nextEligibleAt = eligibleAt;
        }
    }
}

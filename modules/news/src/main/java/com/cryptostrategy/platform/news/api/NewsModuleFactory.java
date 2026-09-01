package com.cryptostrategy.platform.news.api;

import com.cryptostrategy.platform.news.api.model.SentimentModelRelease;
import com.cryptostrategy.platform.news.api.port.in.CollectNewsUseCase;
import com.cryptostrategy.platform.news.api.port.in.GetSentimentAuditUseCase;
import com.cryptostrategy.platform.news.api.port.in.ListNewsUseCase;
import com.cryptostrategy.platform.news.api.port.in.NewsAnalysisUseCase;
import com.cryptostrategy.platform.news.api.port.out.AnalysisWorkStore;
import com.cryptostrategy.platform.news.api.port.out.AssetResolver;
import com.cryptostrategy.platform.news.api.port.out.NewsItemStore;
import com.cryptostrategy.platform.news.api.port.out.NewsProvider;
import com.cryptostrategy.platform.news.api.port.out.NewsQueryPort;
import com.cryptostrategy.platform.news.api.port.out.SentimentAuditStore;
import com.cryptostrategy.platform.news.api.port.out.SentimentInferencePort;
import com.cryptostrategy.platform.news.api.port.out.SentimentModelReleaseStore;
import com.cryptostrategy.platform.news.internal.application.NewsAnalysisService;
import com.cryptostrategy.platform.news.internal.application.NewsAuditService;
import com.cryptostrategy.platform.news.internal.application.NewsCollectionService;
import com.cryptostrategy.platform.news.internal.application.NewsQueryService;
import com.cryptostrategy.platform.news.internal.normalization.CanonicalNewsNormalizer;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

/** Composition root for the framework-independent News capability. */
public final class NewsModuleFactory {
    private NewsModuleFactory() {}

    public static NewsNormalizationPolicy canonicalNormalizationV1(){return new CanonicalNewsNormalizer();}
    public static ListNewsUseCase queryUseCase(NewsQueryPort queries){return new NewsQueryService(queries);}
    public static GetSentimentAuditUseCase auditUseCase(SentimentAuditStore audit){return new NewsAuditService(audit);}

    public static Components create(Dependencies dependencies, Settings settings) {
        Objects.requireNonNull(dependencies, "dependencies");
        Objects.requireNonNull(settings, "settings");
        dependencies.releases().registerOrVerify(settings.activeRelease());
        dependencies.releases().activateForEnglish(settings.activeRelease().modelVersion());

        CollectNewsUseCase collection = new NewsCollectionService(
                dependencies.providers(), dependencies.items(), dependencies.assets(),
                dependencies.normalization(), dependencies.clock(),
                java.util.Optional.of(settings.activeRelease().modelVersion()));
        NewsAnalysisUseCase analysis = new NewsAnalysisService(
                dependencies.analysisWork(), dependencies.inference(), dependencies.clock(), settings.analysis());
        ListNewsUseCase queries = new NewsQueryService(dependencies.queries());
        GetSentimentAuditUseCase audit = new NewsAuditService(dependencies.audit());
        return new Components(collection, analysis, queries, audit);
    }

    public record Dependencies(
            List<NewsProvider> providers,
            NewsItemStore items,
            AnalysisWorkStore analysisWork,
            NewsQueryPort queries,
            SentimentAuditStore audit,
            AssetResolver assets,
            SentimentInferencePort inference,
            SentimentModelReleaseStore releases,
            NewsNormalizationPolicy normalization,
            Clock clock) {
        public Dependencies {
            providers = List.copyOf(Objects.requireNonNull(providers, "providers"));
            if (providers.stream().anyMatch(Objects::isNull)) throw new NullPointerException("provider");
            Objects.requireNonNull(items, "items");
            Objects.requireNonNull(analysisWork, "analysisWork");
            Objects.requireNonNull(queries, "queries");
            Objects.requireNonNull(audit, "audit");
            Objects.requireNonNull(assets, "assets");
            Objects.requireNonNull(inference, "inference");
            Objects.requireNonNull(releases, "releases");
            Objects.requireNonNull(normalization, "normalization");
            Objects.requireNonNull(clock, "clock");
        }
    }

    public record Settings(SentimentModelRelease activeRelease, AnalysisPolicy analysis) {
        public Settings {
            Objects.requireNonNull(activeRelease, "activeRelease");
            Objects.requireNonNull(analysis, "analysis");
        }
    }

    public record AnalysisPolicy(Duration leaseDuration, int claimBatch, int maxAttempts, List<Duration> retryDelays) {
        public AnalysisPolicy {
            Objects.requireNonNull(leaseDuration, "leaseDuration");
            if (leaseDuration.isZero() || leaseDuration.isNegative()) throw new IllegalArgumentException("leaseDuration must be positive");
            if (claimBatch < 1 || claimBatch > 25) throw new IllegalArgumentException("claimBatch must be between 1 and 25");
            if (maxAttempts < 1) throw new IllegalArgumentException("maxAttempts must be positive");
            retryDelays = List.copyOf(Objects.requireNonNull(retryDelays, "retryDelays"));
            if (retryDelays.isEmpty() || retryDelays.stream().anyMatch(delay -> delay == null || delay.isNegative())) {
                throw new IllegalArgumentException("retryDelays must contain non-negative values");
            }
        }
    }

    public record Components(
            CollectNewsUseCase collection,
            NewsAnalysisUseCase analysis,
            ListNewsUseCase queries,
            GetSentimentAuditUseCase audit) {
        public Components {
            Objects.requireNonNull(collection, "collection");
            Objects.requireNonNull(analysis, "analysis");
            Objects.requireNonNull(queries, "queries");
            Objects.requireNonNull(audit, "audit");
        }
    }
}

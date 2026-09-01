package com.cryptostrategy.platform.news.internal.application;

import com.cryptostrategy.platform.news.api.NewsNormalizationPolicy;
import com.cryptostrategy.platform.news.api.model.*;
import com.cryptostrategy.platform.news.api.port.in.CollectNewsUseCase;
import com.cryptostrategy.platform.news.api.port.out.*;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.*;

public final class NewsCollectionService implements CollectNewsUseCase {
    private final List<NewsProvider> providers; private final NewsItemStore store;
    private final AssetResolver assets; private final NewsNormalizationPolicy normalizer; private final Clock clock;
    private final Optional<String> activeModelVersion;
    public NewsCollectionService(List<NewsProvider> providers, NewsItemStore store, AssetResolver assets,
            NewsNormalizationPolicy normalizer, Clock clock, Optional<String> activeModelVersion) {
        this.providers = List.copyOf(providers); this.store = Objects.requireNonNull(store); this.assets = Objects.requireNonNull(assets);
        this.normalizer = Objects.requireNonNull(normalizer); this.clock = Objects.requireNonNull(clock);
        this.activeModelVersion = activeModelVersion == null ? Optional.empty() : activeModelVersion;
    }
    @Override public List<CollectionOutcome> collectSince(Instant since) {
        var outcomes = new ArrayList<CollectionOutcome>();
        for (var provider : providers) {
            List<ProviderNewsItem> fetched;
            try { fetched = provider.fetchSince(since); }
            catch (RuntimeException error) { outcomes.add(new CollectionOutcome(provider.source().value(), "", Status.PROVIDER_FAILED, error.getClass().getSimpleName())); continue; }
            for (var raw : fetched) {
                try {
                    var normalized = normalizer.normalize(raw.url(), raw.titleHtml(), raw.contentHtml(), raw.language());
                    var resolved = assets.resolveSymbols(new LinkedHashSet<>(raw.assetSymbols()));
                    var related = resolved.values().stream().distinct().map(id -> new RelatedNewsAsset(id, Optional.<BigDecimal>empty())).toList();
                    Optional<String> target = normalized.language().equals(LanguageCode.ENGLISH) ? activeModelVersion : Optional.empty();
                    var item = new NewsItem(NewsId.generate(), normalized.title(), normalized.content(), raw.publishedAt(), clock.instant(),
                            normalized.contentHash(), AnalysisStatus.PENDING, provider.source(), normalized.url(), normalized.language(),
                            raw.sourceItemId(), target, Optional.empty(), Optional.empty(), 0, related);
                    var result = store.saveIfAbsent(item);
                    outcomes.add(new CollectionOutcome(provider.source().value(), normalized.url().toString(),
                            result == NewsItemStore.SaveOutcome.INSERTED ? Status.ACCEPTED : result == NewsItemStore.SaveOutcome.ALREADY_PRESENT ? Status.DUPLICATE : Status.REJECTED,
                            result.name()));
                } catch (RuntimeException error) { outcomes.add(new CollectionOutcome(provider.source().value(), raw.url(), Status.REJECTED, error.getClass().getSimpleName())); }
            }
        }
        return List.copyOf(outcomes);
    }
}

package com.cryptostrategy.platform.worker.config;

import com.cryptostrategy.platform.news.api.NewsModuleFactory;
import com.cryptostrategy.platform.news.api.NewsProviderFactory;
import com.cryptostrategy.platform.news.api.model.*;
import com.cryptostrategy.platform.news.api.port.out.*;
import com.cryptostrategy.platform.marketdata.api.port.out.MarketReferenceDataStore;
import com.cryptostrategy.platform.domain.api.market.AssetSymbol;
import com.cryptostrategy.platform.persistence.api.NewsPersistenceFactory;
import com.cryptostrategy.platform.worker.news.analysis.NewsAnalysisCoordinator;
import com.cryptostrategy.platform.worker.news.sentiment.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.*;
import io.github.resilience4j.timelimiter.*;
import java.net.http.HttpClient;
import java.time.Clock;
import java.util.concurrent.Executors;
import java.time.Duration;
import java.util.*;
import io.micrometer.core.instrument.MeterRegistry;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableConfigurationProperties(NewsWorkerProperties.class)
@ConditionalOnProperty(name="news.enabled",havingValue="true")
@EnableScheduling
public class NewsWorkerConfiguration {
    @Bean NewsPersistenceFactory.Components newsPersistence(DataSource source){return NewsPersistenceFactory.create(source);}
    @Bean HttpClient sentimentHttpClient(NewsWorkerProperties properties){return HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).connectTimeout(properties.sentiment().connectTimeout()).build();}
    @Bean HttpSentimentInferenceAdapter sentimentInference(HttpClient client,NewsWorkerProperties p,ObjectMapper json){return new HttpSentimentInferenceAdapter(client,p.sentiment().endpoint(),p.sentiment().serviceToken(),json,new SentimentContractMapper());}
    @Bean SentimentClientGuard sentimentGuard(NewsWorkerProperties p){
        var s=p.sentiment();
        var cb=CircuitBreaker.of("sentiment",CircuitBreakerConfig.custom().failureRateThreshold(s.failureRateThreshold()).slidingWindowSize(s.slidingWindowSize()).minimumNumberOfCalls(s.slidingWindowSize()).waitDurationInOpenState(s.openStateDuration()).build());
        var tl=TimeLimiter.of(TimeLimiterConfig.custom().timeoutDuration(s.responseTimeout()).cancelRunningFuture(true).build());
        return new SentimentClientGuard(cb,tl,Executors.newSingleThreadScheduledExecutor(),s.maxConcurrency());
    }
    @Bean Clock newsClock(){return Clock.systemUTC();}
    @Bean SentimentModelRelease activeSentimentRelease(NewsWorkerProperties properties){
        var r=properties.release();
        return new SentimentModelRelease(r.modelVersion(),r.modelName(),r.preprocessingVersion(),r.contractVersion());
    }
    @Bean NewsModuleFactory.Components newsModule(List<NewsProvider> providers,NewsPersistenceFactory.Components persistence,
            AssetResolver assets,HttpSentimentInferenceAdapter inference,Clock clock,SentimentModelRelease release,NewsWorkerProperties properties){
        var dependencies=new NewsModuleFactory.Dependencies(providers,persistence.items(),persistence.work(),persistence.queries(),persistence.audit(),
                assets,inference,persistence.releases(),NewsModuleFactory.canonicalNormalizationV1(),clock);
        var analysis=properties.analysis();
        var settings=new NewsModuleFactory.Settings(release,new NewsModuleFactory.AnalysisPolicy(
                analysis.leaseDuration(),analysis.claimBatch(),analysis.maxAttempts(),analysis.retryDelays()));
        return NewsModuleFactory.create(dependencies,settings);
    }
    @Bean com.cryptostrategy.platform.worker.news.analysis.NewsAnalysisObservability newsAnalysisObservability(MeterRegistry meters){return new com.cryptostrategy.platform.worker.news.analysis.NewsAnalysisObservability(meters);}
    @Bean NewsAnalysisCoordinator newsAnalysisCoordinator(NewsModuleFactory.Components news,SentimentClientGuard guard,NewsWorkerProperties properties,SentimentModelRelease release,Clock clock,com.cryptostrategy.platform.worker.news.analysis.NewsAnalysisObservability observability){
        return new NewsAnalysisCoordinator(news.analysis(),guard,properties.analysis(),release,clock,observability);
    }
    @Bean SentimentReadinessProbe sentimentReadinessProbe(HttpClient client,NewsWorkerProperties properties,ObjectMapper json){return new SentimentReadinessProbe(client,properties.sentiment().endpoint(),json,properties.release().modelVersion());}
    @Bean com.cryptostrategy.platform.worker.news.analysis.NewsAnalysisScheduler newsAnalysisScheduler(NewsAnalysisCoordinator coordinator,SentimentReadinessProbe readiness){return new com.cryptostrategy.platform.worker.news.analysis.NewsAnalysisScheduler(coordinator,readiness);}
    @Bean MarketReferenceDataStore newsAssetReferences(DataSource source){return com.cryptostrategy.platform.persistence.api.MarketDataPersistenceFactory.create(source).references();}
    @Bean AssetResolver newsAssetResolver(MarketReferenceDataStore references){return symbols->{var result=new LinkedHashMap<String,com.cryptostrategy.platform.domain.api.market.AssetId>();for(String symbol:symbols){String canonical=symbol.toUpperCase(Locale.ROOT);references.findAsset(new AssetSymbol(canonical)).ifPresent(asset->result.put(canonical,asset.assetId()));}return Map.copyOf(result);};}
    @Bean List<NewsProvider> newsProviders(NewsWorkerProperties properties,HttpClient http){return properties.collection().providers().stream().map(p->NewsProviderFactory.rss(p.source(),p.url(),p.language(),p.assetSymbols(),http,Duration.ofSeconds(10))).toList();}
    @Bean com.cryptostrategy.platform.worker.news.collection.NewsCollectionScheduler newsCollectionScheduler(NewsModuleFactory.Components news,Clock clock,NewsWorkerProperties properties,MeterRegistry meters){return new com.cryptostrategy.platform.worker.news.collection.NewsCollectionScheduler(news.collection(),clock,properties.collection().lookback(),meters);}
}

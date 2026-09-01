package com.cryptostrategy.platform.worker.config;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("news")
public record NewsWorkerProperties(boolean enabled, Sentiment sentiment, Analysis analysis, Release release, CollectionSettings collection) {
    public NewsWorkerProperties { if(sentiment==null||analysis==null||release==null||collection==null) throw new IllegalArgumentException("News configuration is required"); }
    public record Sentiment(URI endpoint,String serviceToken,Duration connectTimeout,Duration responseTimeout,
            int maxConcurrency,float failureRateThreshold,int slidingWindowSize,Duration openStateDuration) {
        public Sentiment {
            if(endpoint==null || !List.of("http","https").contains(endpoint.getScheme())) throw new IllegalArgumentException("Valid sentiment endpoint required");
            if(serviceToken==null||serviceToken.length()<16) throw new IllegalArgumentException("Sentiment service token required");
            if(!Duration.ofSeconds(2).equals(connectTimeout)||!Duration.ofSeconds(30).equals(responseTimeout)) throw new IllegalArgumentException("F-008 timeouts must be 2s/30s");
            if(maxConcurrency<1||failureRateThreshold!=50f||slidingWindowSize!=10||!Duration.ofSeconds(30).equals(openStateDuration)) throw new IllegalArgumentException("Invalid process-local resilience settings");
        }
    }
    public record Analysis(Duration leaseDuration,int claimBatch,int maxAttempts,List<Duration> retryDelays) {
        public Analysis {
            retryDelays=List.copyOf(retryDelays);
            if(!Duration.ofSeconds(120).equals(leaseDuration)||claimBatch<1||claimBatch>25||maxAttempts!=3||!retryDelays.equals(List.of(Duration.ofSeconds(5),Duration.ofSeconds(30))))
                throw new IllegalArgumentException("Invalid persisted analysis policy");
        }
    }
    public record Release(String modelName,String modelVersion,String preprocessingVersion,String contractVersion) {
        public Release { if(java.util.stream.Stream.of(modelName,modelVersion,preprocessingVersion,contractVersion).anyMatch(v->v==null||v.isBlank())) throw new IllegalArgumentException("Complete release identity required"); }
    }
    public record CollectionSettings(Duration lookback,List<Provider> providers) {
        public CollectionSettings { providers=List.copyOf(providers==null?List.of():providers);if(lookback==null||lookback.isNegative()||lookback.isZero())throw new IllegalArgumentException("Collection lookback required"); }
    }
    public record Provider(String source,URI url,String language,List<String> assetSymbols) {
        public Provider { assetSymbols=List.copyOf(assetSymbols==null?List.of():assetSymbols);if(source==null||source.isBlank()||url==null||language==null||language.isBlank())throw new IllegalArgumentException("Complete provider configuration required"); }
    }
}

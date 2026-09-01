package com.cryptostrategy.platform.worker.config;

import static org.junit.jupiter.api.Assertions.*;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class NewsWorkerPropertiesTest {
    @Test void accepts_only_the_approved_process_local_and_persisted_defaults(){
        var sentiment=new NewsWorkerProperties.Sentiment(URI.create("http://sentiment:8000"),"service-token-1234",Duration.ofSeconds(2),Duration.ofSeconds(30),4,50f,10,Duration.ofSeconds(30));
        var analysis=new NewsWorkerProperties.Analysis(Duration.ofSeconds(120),25,3,List.of(Duration.ofSeconds(5),Duration.ofSeconds(30)));
        assertDoesNotThrow(()->new NewsWorkerProperties(true,sentiment,analysis,new NewsWorkerProperties.Release("model","v1","prep","sentiment-v1"),new NewsWorkerProperties.CollectionSettings(Duration.ofHours(24),List.of())));
        assertEquals(4, sentiment.maxConcurrency());
        assertEquals(50f, sentiment.failureRateThreshold());
        assertEquals(10, sentiment.slidingWindowSize());
        assertEquals(3, analysis.maxAttempts());
        assertEquals(Duration.ofSeconds(120), analysis.leaseDuration());
        assertEquals(List.of(Duration.ofSeconds(5), Duration.ofSeconds(30)), analysis.retryDelays());
        assertThrows(IllegalArgumentException.class,()->new NewsWorkerProperties.Analysis(Duration.ofSeconds(120),25,3,List.of(Duration.ofSeconds(1))));
        assertThrows(IllegalArgumentException.class,()->new NewsWorkerProperties.Sentiment(URI.create("http://sentiment"),"service-token-1234",Duration.ofSeconds(2),Duration.ofSeconds(29),4,50f,10,Duration.ofSeconds(30)));
        assertThrows(IllegalArgumentException.class,()->new NewsWorkerProperties.Sentiment(URI.create("ftp://sentiment"),"service-token-1234",Duration.ofSeconds(2),Duration.ofSeconds(30),4,50f,10,Duration.ofSeconds(30)));
        assertThrows(IllegalArgumentException.class,()->new NewsWorkerProperties.Sentiment(URI.create("http://sentiment"),"short",Duration.ofSeconds(2),Duration.ofSeconds(30),4,50f,10,Duration.ofSeconds(30)));
        assertThrows(IllegalArgumentException.class,()->new NewsWorkerProperties.Sentiment(URI.create("http://sentiment"),"service-token-1234",Duration.ofSeconds(3),Duration.ofSeconds(30),4,50f,10,Duration.ofSeconds(30)));
        assertThrows(IllegalArgumentException.class,()->new NewsWorkerProperties.Sentiment(URI.create("http://sentiment"),"service-token-1234",Duration.ofSeconds(2),Duration.ofSeconds(30),0,50f,10,Duration.ofSeconds(30)));
        assertThrows(IllegalArgumentException.class,()->new NewsWorkerProperties.Analysis(Duration.ofSeconds(119),25,3,List.of(Duration.ofSeconds(5),Duration.ofSeconds(30))));
        assertThrows(IllegalArgumentException.class,()->new NewsWorkerProperties.Analysis(Duration.ofSeconds(120),26,3,List.of(Duration.ofSeconds(5),Duration.ofSeconds(30))));
        assertThrows(IllegalArgumentException.class,()->new NewsWorkerProperties.Analysis(Duration.ofSeconds(120),25,4,List.of(Duration.ofSeconds(5),Duration.ofSeconds(30))));
    }
}

package com.cryptostrategy.platform.worker.news.analysis;

import com.cryptostrategy.platform.worker.news.sentiment.SentimentReadinessProbe;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;
import org.springframework.scheduling.annotation.Scheduled;

public final class NewsAnalysisScheduler {
    private final NewsAnalysisCoordinator coordinator; private final Supplier<CompletionStage<Boolean>> readiness;
    private final AtomicBoolean probing=new AtomicBoolean(); private final String owner="worker-"+UUID.randomUUID();
    public NewsAnalysisScheduler(NewsAnalysisCoordinator coordinator,SentimentReadinessProbe readiness){this(coordinator,readiness::ready);}
    NewsAnalysisScheduler(NewsAnalysisCoordinator coordinator,Supplier<CompletionStage<Boolean>> readiness){this.coordinator=coordinator;this.readiness=readiness;}
    @Scheduled(fixedDelayString="${news.analysis.poll-delay:1s}")
    public void poll(){
        if(!probing.compareAndSet(false,true))return;
        readiness.get().whenComplete((ready,error)->{try{if(error==null&&Boolean.TRUE.equals(ready))coordinator.poll(owner);}finally{probing.set(false);}});
    }
}

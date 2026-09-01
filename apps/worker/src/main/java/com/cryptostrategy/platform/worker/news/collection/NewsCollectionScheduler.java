package com.cryptostrategy.platform.worker.news.collection;

import com.cryptostrategy.platform.news.api.port.in.CollectNewsUseCase;
import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

public final class NewsCollectionScheduler {
    private static final Logger LOG=LoggerFactory.getLogger(NewsCollectionScheduler.class);
    private final CollectNewsUseCase collector; private final Clock clock; private final Duration lookback; private final MeterRegistry meters; private final AtomicBoolean running=new AtomicBoolean();
    public NewsCollectionScheduler(CollectNewsUseCase collector,Clock clock,Duration lookback,MeterRegistry meters){this.collector=collector;this.clock=clock;this.lookback=lookback;this.meters=meters;}
    @Scheduled(fixedDelayString="${news.collection.poll-delay:60s}")
    public void collect(){if(!running.compareAndSet(false,true))return;try{for(var outcome:collector.collectSince(clock.instant().minus(lookback))){
        Counter.builder("news.collection.outcomes").tag("status",outcome.status().name()).register(meters).increment();
        LOG.info("News collection outcome provider={} status={} reason={}",outcome.provider(),outcome.status(),outcome.reason());
    }}finally{running.set(false);}}
}

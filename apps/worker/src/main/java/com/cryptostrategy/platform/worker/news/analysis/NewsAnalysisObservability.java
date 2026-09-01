package com.cryptostrategy.platform.worker.news.analysis;

import com.cryptostrategy.platform.news.api.model.NewsId;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Safe process-local telemetry; article bodies, URLs and credentials are never accepted. */
public final class NewsAnalysisObservability {
    private static final Logger LOG=LoggerFactory.getLogger(NewsAnalysisObservability.class);
    private final MeterRegistry meters;
    public NewsAnalysisObservability(MeterRegistry meters){this.meters=meters;}
    public void claimed(int count){Counter.builder("news.analysis.claimed").register(meters).increment(count);}
    public void event(NewsId newsId,String leaseToken,String outcome){
        Counter.builder("news.analysis.outcomes").tag("outcome",outcome).register(meters).increment();
        LOG.info("News analysis lifecycle newsId={} leaseToken={} outcome={}",newsId.value(),leaseToken,outcome);
    }
}

package com.cryptostrategy.platform.worker.news.analysis;

import com.cryptostrategy.platform.news.api.model.*;
import com.cryptostrategy.platform.news.api.port.in.NewsAnalysisUseCase;
import com.cryptostrategy.platform.worker.config.NewsWorkerProperties;
import com.cryptostrategy.platform.worker.news.sentiment.*;
import java.time.Clock;
import java.util.concurrent.*;

public final class NewsAnalysisCoordinator {
    private final NewsAnalysisUseCase analysis; private final SentimentClientGuard guard; private final NewsWorkerProperties.Analysis policy; private final SentimentModelRelease release; private final Clock clock; private final NewsAnalysisObservability observability;
    public NewsAnalysisCoordinator(NewsAnalysisUseCase analysis,SentimentClientGuard guard,NewsWorkerProperties.Analysis policy,SentimentModelRelease release,Clock clock,NewsAnalysisObservability observability){this.analysis=analysis;this.guard=guard;this.policy=policy;this.release=release;this.clock=clock;this.observability=observability;}
    public void poll(String owner) {
        var command=new NewsAnalysisUseCase.Acquire(owner,clock.instant(),policy.leaseDuration(),policy.claimBatch());
        var claimed=analysis.acquire(command);observability.claimed(claimed.size());
        for(var item:claimed) dispatch(item);
    }
    private void dispatch(NewsItem item) {
        var lease=item.lease().orElseThrow();
        if(!lease.targetModelVersion().equals(release.modelVersion())) { analysis.fail(new NewsAnalysisUseCase.Fail(item.newsId(),lease.token(),false,Math.max(1,item.attemptCount())));observability.event(item.newsId(),lease.token(),"incompatible_release"); return; }
        var request=new SentimentAnalysisRequest(com.cryptostrategy.platform.domain.api.identity.Ulids.generate(),item.newsId(),item.title(),item.content(),item.language(),item.contentHash(),release);
        guard.execute(()->{
            if(!analysis.startAttempt(new NewsAnalysisUseCase.StartAttempt(item.newsId(),lease.token(),item.contentHash(),release.modelVersion()))) {observability.event(item.newsId(),lease.token(),"stale_start");return CompletableFuture.failedFuture(new SentimentClientGuard.UnusedPermitException("Stale lease"));}
            observability.event(item.newsId(),lease.token(),"attempt_reserved");
            return analysis.analyze(request);
        }).whenComplete((outcome,error)->{
            Throwable cause=unwrap(error);
            if(cause==null){try{analysis.complete(new NewsAnalysisUseCase.Complete(item.newsId(),lease.token(),outcome.toResult(SentimentResultId.generate())));observability.event(item.newsId(),lease.token(),"completed");}catch(RuntimeException persistence){observability.event(item.newsId(),lease.token(),"completion_persistence_failed");}return;}
            if(cause instanceof SentimentClientGuard.DispatchDeferredException){analysis.defer(new NewsAnalysisUseCase.Defer(item.newsId(),lease.token(),policy.retryDelays().getFirst()));observability.event(item.newsId(),lease.token(),"deferred");return;}
            if(cause instanceof SentimentClientGuard.UnusedPermitException)return;
            int consumed=item.attemptCount()+1;
            boolean transientFailure=!(cause instanceof SentimentClientException client)||client.retryable();
            analysis.fail(new NewsAnalysisUseCase.Fail(item.newsId(),lease.token(),transientFailure,consumed));
            observability.event(item.newsId(),lease.token(),transientFailure?"failed_transient":"failed_permanent");
        });
    }
    private static Throwable unwrap(Throwable value){while(value instanceof CompletionException||value instanceof ExecutionException)value=value.getCause();return value;}
}

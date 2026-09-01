package com.cryptostrategy.platform.worker.news.sentiment;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.timelimiter.TimeLimiter;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public final class SentimentClientGuard implements AutoCloseable {
    private final CircuitBreaker circuit; private final TimeLimiter limiter; private final ScheduledExecutorService scheduler; private final Semaphore concurrency;
    public SentimentClientGuard(CircuitBreaker circuit,TimeLimiter limiter,ScheduledExecutorService scheduler,int maxConcurrency){this.circuit=circuit;this.limiter=limiter;this.scheduler=scheduler;this.concurrency=new Semaphore(maxConcurrency);}
    public <T> CompletionStage<T> execute(Supplier<? extends CompletionStage<T>> dispatch) {
        if(!concurrency.tryAcquire()) return CompletableFuture.failedFuture(new DispatchDeferredException("Process-local concurrency full"));
        if(!circuit.tryAcquirePermission()){concurrency.release();return CompletableFuture.failedFuture(new DispatchDeferredException("Circuit open"));}
        final CompletionStage<T> stage;
        try { stage=TimeLimiter.decorateCompletionStage(limiter,scheduler,dispatch).get(); }
        catch(Throwable error){circuit.releasePermission();concurrency.release();return CompletableFuture.failedFuture(error);}
        var recorded=new AtomicBoolean();
        return stage.whenComplete((value,error)->{
            if(!recorded.compareAndSet(false,true))return;
            concurrency.release();
            long duration=0;
            Throwable cause=unwrap(error);
            if(cause==null)circuit.onSuccess(duration,TimeUnit.NANOSECONDS);
            else if(cause instanceof UnusedPermitException)circuit.releasePermission();
            else if(cause instanceof SentimentClientException client&&!client.countsTowardCircuit())circuit.releasePermission();
            else circuit.onError(duration,TimeUnit.NANOSECONDS,cause);
        });
    }
    private static Throwable unwrap(Throwable value){while(value instanceof CompletionException||value instanceof ExecutionException)value=value.getCause();return value;}
    @Override public void close(){scheduler.shutdownNow();}
    public static final class DispatchDeferredException extends RuntimeException { public DispatchDeferredException(String message){super(message);} }
    public static final class UnusedPermitException extends RuntimeException { public UnusedPermitException(String message){super(message);} }
}

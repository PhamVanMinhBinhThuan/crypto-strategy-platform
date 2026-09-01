package com.cryptostrategy.platform.worker.engine;

import com.cryptostrategy.platform.worker.config.WorkerProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Component
public class BacktestExecutionPipeline {

    private static final Logger log = LoggerFactory.getLogger(BacktestExecutionPipeline.class);

    private final ExecutorService executor;
    private final Semaphore concurrencyLimiter;
    private final WorkerProperties workerProperties;

    public BacktestExecutionPipeline(WorkerProperties workerProperties) {
        this.workerProperties = Objects.requireNonNull(workerProperties, "workerProperties cannot be null");
        int concurrency = workerProperties.concurrency().backtest();
        this.executor = Executors.newFixedThreadPool(concurrency);
        this.concurrencyLimiter = new Semaphore(concurrency);
    }

    public Future<?> executeAsync(Runnable task) {
        return executor.submit(() -> {
            try {
                concurrencyLimiter.acquire();
                task.run();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Task interrupted in execution pipeline");
            } finally {
                concurrencyLimiter.release();
            }
        });
    }

    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(workerProperties.execution().gracefulShutdownTimeout().toMillis(), TimeUnit.MILLISECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}

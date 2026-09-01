package com.cryptostrategy.platform.worker.engine;

import com.cryptostrategy.platform.worker.config.WorkerProperties;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class BacktestExecutionPipelineTest {

    @Test
    void executesSubmittedTasksConcurrently() throws Exception {
        WorkerProperties workerProperties = new WorkerProperties(null, null, null, null, null, null, null, null);
        BacktestExecutionPipeline pipeline = new BacktestExecutionPipeline(workerProperties);

        AtomicBoolean executed = new AtomicBoolean(false);
        CountDownLatch latch = new CountDownLatch(1);

        Future<?> future = pipeline.executeAsync(() -> {
            executed.set(true);
            latch.countDown();
        });

        boolean completed = latch.await(2, TimeUnit.SECONDS);
        assertThat(completed).isTrue();
        assertThat(executed.get()).isTrue();
        pipeline.shutdown();
    }
}

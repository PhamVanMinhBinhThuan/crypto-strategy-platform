package com.cryptostrategy.platform.execution.api.port.in;

/** Public recovery boundary cho durable reproduction verification. */
public interface SearchReproductionVerificationUseCase {
    void reconcile(int batchSize);
}

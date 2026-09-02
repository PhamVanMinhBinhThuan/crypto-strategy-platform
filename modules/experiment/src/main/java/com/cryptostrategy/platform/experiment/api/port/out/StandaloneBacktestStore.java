package com.cryptostrategy.platform.experiment.api.port.out;

import com.cryptostrategy.platform.experiment.api.CandidateDefinition;
import com.cryptostrategy.platform.experiment.api.Experiment;
import com.cryptostrategy.platform.experiment.api.ExperimentManifest;
import com.cryptostrategy.platform.experiment.api.backtest.StandaloneBacktest;
import com.cryptostrategy.platform.experiment.api.backtest.StandaloneBacktestAcceptance;
import com.cryptostrategy.platform.experiment.api.job.Job;
import com.cryptostrategy.platform.experiment.api.outbox.OutboxEvent;
import java.time.Instant;
import java.util.UUID;

/** Atomic acceptance boundary owned by F-005. */
public interface StandaloneBacktestStore {
    StandaloneBacktestAcceptance accept(
            UUID ownerUserId,
            String operation,
            String idempotencyKey,
            String requestHash,
            Instant receiptExpiresAt,
            StandaloneBacktest backtest,
            Experiment experiment,
            ExperimentManifest manifest,
            CandidateDefinition candidate,
            Job job,
            OutboxEvent outboxEvent);
}

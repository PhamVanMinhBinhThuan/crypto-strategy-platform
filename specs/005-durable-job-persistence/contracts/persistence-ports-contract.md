# Contract: Persistence Output Ports & Store Interfaces

**Feature:** F-005 Experiment Persistence and Ownership  
**Status:** Canonical Design Contract  
**Date:** 2026-08-30  

This document defines the persistence output port interfaces exposed by `modules/experiment` and implemented by `modules/persistence`. All interfaces enforce authenticated owner predicates at their boundary.

---

## 1. Experiment Persistence Port

```java
package com.cryptostrategy.platform.experiment.api.port.out;

import com.cryptostrategy.platform.experiment.api.CandidateDefinition;
import com.cryptostrategy.platform.experiment.api.CandidateId;
import com.cryptostrategy.platform.experiment.api.Experiment;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.ExperimentManifest;
import com.cryptostrategy.platform.experiment.api.ExperimentStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExperimentStore {
    /** Inserts a new Experiment with its Manifest in CREATED status. */
    void insert(Experiment experiment);

    /** Updates Experiment and Manifest during mutable CREATED phase. */
    void updateCreatedConfiguration(UUID ownerUserId, ExperimentId experimentId, String name, ExperimentManifest manifest);

    /** Transitions Experiment from CREATED to QUEUED, freezing the Manifest. */
    void freezeAndQueue(UUID ownerUserId, ExperimentId experimentId, String fingerprint);

    /** Updates Experiment runtime status and lifecycle timestamps. */
    void updateStatus(UUID ownerUserId, ExperimentId experimentId, ExperimentStatus expectedStatus, ExperimentStatus newStatus);

    /** Finds Experiment by ID ensuring owner match. */
    Optional<Experiment> findById(UUID ownerUserId, ExperimentId experimentId);

    /** Lists Experiments for an owner with pagination. */
    List<Experiment> listByOwner(UUID ownerUserId, int offset, int limit);

    /** Appends an immutable Candidate definition under a QUEUED experiment. */
    void insertCandidate(UUID ownerUserId, CandidateDefinition candidate);

    /** Queries a Candidate definition ensuring owner match. */
    Optional<CandidateDefinition> findCandidateById(UUID ownerUserId, CandidateId candidateId);

    /** Lists Candidates for an Experiment in generation order. */
    List<CandidateDefinition> listCandidatesByExperiment(UUID ownerUserId, ExperimentId experimentId);
}
```

---

## 2. Job Persistence Port

```java
package com.cryptostrategy.platform.experiment.api.port.out;

import com.cryptostrategy.platform.experiment.api.CandidateId;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.job.Job;
import com.cryptostrategy.platform.experiment.api.job.JobId;
import com.cryptostrategy.platform.experiment.api.job.JobStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobStore {
    /** Inserts a new durable Job.
     *  SEARCH jobs have no Candidate; BACKTEST jobs enforce one logical Job per Candidate.
     */
    void insert(Job job);

    /** Finds Job by ID ensuring owner match through Experiment. */
    Optional<Job> findById(UUID ownerUserId, JobId jobId);

    /** Finds Backtest Job for a Candidate. */
    Optional<Job> findByCandidateId(UUID ownerUserId, CandidateId candidateId);

    /** Atomically transitions Job status with optimistic/status check. */
    void transitionStatus(UUID ownerUserId, JobId jobId, JobStatus expectedStatus, JobStatus newStatus);

    /** Updates Job progress counters and best score without emitting Outbox events. */
    void updateProgress(UUID ownerUserId, JobId jobId, int completedWork, int failedWork, Double bestScore);

    /** Schedules next retry timestamp and transitions Job to RETRY_SCHEDULED. */
    void scheduleRetry(UUID ownerUserId, JobId jobId, Instant nextRetryAt, String failureCode, String failureMessage);

    /** Transitions Job from RETRY_SCHEDULED to QUEUED when retry is ready. */
    void requeueRetry(UUID ownerUserId, JobId jobId);

    /** Lists active Jobs for recovery scanning. */
    List<Job> listUnfinishedJobs(int limit);
}
```

---

## 3. Execution Attempt Persistence Port

```java
package com.cryptostrategy.platform.experiment.api.port.out;

import com.cryptostrategy.platform.experiment.api.job.AttemptId;
import com.cryptostrategy.platform.experiment.api.job.ExecutionAttempt;
import com.cryptostrategy.platform.experiment.api.job.JobId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExecutionAttemptStore {
    /**
     * Atomically starts the next Backtest try.
     *
     * Implementation requirements:
     * - lock the parent Job row with SELECT ... FOR UPDATE;
     * - verify the Job is dispatch-ready;
     * - read previous MAX(attempt_no) after the Job lock is held;
     * - assign attempt_no = previous_max + 1;
     * - transition Job QUEUED -> RUNNING when required;
     * - insert the new Attempt and mark it RUNNING in the same transaction.
     *
     * UNIQUE(job_id, attempt_no) remains the final collision guard.
     */
    ExecutionAttempt startNextAttempt(
        UUID ownerUserId,
        JobId jobId,
        String workerId,
        Instant startTime
    );

    /**
     * Finalizes an Attempt and its coupled Job outcome atomically where required.
     * SUCCEEDED Attempt -> Job SUCCEEDED.
     * Retryable FAILED Attempt -> Job RETRY_SCHEDULED.
     * Terminal FAILED Attempt -> Job FAILED.
     * CANCELLED Attempt acknowledging CANCEL_REQUESTED -> Job CANCELLED.
     */
    void finalizeAttemptWithJobOutcome(
        UUID ownerUserId,
        AttemptId attemptId,
        ExecutionAttempt attempt
    );

    /** Lists all Execution Attempts for a Job ordered by attempt_no ASC. */
    List<ExecutionAttempt> listByJobId(UUID ownerUserId, JobId jobId);

    /** Finds Execution Attempt by ID ensuring owner match. */
    Optional<ExecutionAttempt> findById(UUID ownerUserId, AttemptId attemptId);
}
```

---

## 4. Idempotency & Outbox Persistence Ports

```java
package com.cryptostrategy.platform.experiment.api.port.out;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface IdempotencyStore {
    enum ClaimStatus {
        ACQUIRED,
        IN_PROGRESS_REPLAY,
        COMPLETED_REPLAY,
        CONFLICT
    }

    record IdempotencyOutcome(
        String outcomeCode,
        String responseBodyJson,
        Instant expiresAt
    ) {}

    record ClaimResult(
        ClaimStatus status,
        Optional<IdempotencyOutcome> existingOutcome
    ) {}

    /**
     * Atomically claims (owner, operationScope, idempotencyKey) for one logical request.
     *
     * - no existing record -> create IN_PROGRESS row and return ACQUIRED;
     * - same key + same requestHash -> return IN_PROGRESS_REPLAY or COMPLETED_REPLAY;
     * - same key + different requestHash -> return CONFLICT;
     * - exactly one concurrent first caller may receive ACQUIRED.
     */
    ClaimResult claim(
        UUID ownerUserId,
        String operationScope,
        String idempotencyKey,
        String requestHash,
        Instant expiresAt
    );

    /** Completes a previously acquired idempotency record with the canonical application outcome. */
    void complete(
        UUID ownerUserId,
        String operationScope,
        String idempotencyKey,
        String requestHash,
        IdempotencyOutcome outcome
    );

    /** Retrieves the current persisted outcome/state for replay resolution. */
    Optional<IdempotencyOutcome> getOutcome(
        UUID ownerUserId,
        String operationScope,
        String idempotencyKey
    );
}

public interface OutboxStore {
    record OutboxEntry(
        String eventId,
        String messageId,
        String aggregateType,
        String aggregateId,
        String eventType,
        String eventVersion,
        String payloadJson,
        String headersJson,
        Instant occurredAt
    ) {}

    /** Inserts Outbox row within current business transaction. */
    void insert(OutboxEntry entry);
}
```

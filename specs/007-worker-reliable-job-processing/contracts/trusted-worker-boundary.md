# Contract: Trusted Worker Application Boundary (`modules/experiment`)

**Owner**: F-005  
**Public package**: `com.cryptostrategy.platform.experiment.api.port.in`  
**Authorized consumer**: trusted `apps/worker` runtime only

This boundary derives authorization from durable Job/Experiment state. Redis never supplies authorization authority.

---

## 1. Command/Read Facade

```java
package com.cryptostrategy.platform.experiment.api.port.in;

import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.ExperimentStatus;
import com.cryptostrategy.platform.experiment.api.execution.FrozenBacktestExecution;
import com.cryptostrategy.platform.experiment.api.job.*;
import java.math.BigDecimal;
import java.time.Instant;

public interface TrustedWorkerExperimentUseCase {

    ExecutionAttempt startNextAttempt(JobId jobId, WorkerId workerId);

    FrozenBacktestExecution getFrozenExecution(JobId jobId);

    void finalizeSuccess(JobId jobId, AttemptId attemptId);

    void finalizeFailure(
        JobId jobId,
        AttemptId attemptId,
        String failureCode,
        String failureMessage,
        FailureClassification classification,
        Instant nextRetryAt
    );

    void finalizeCancelled(JobId jobId, AttemptId attemptId);

    boolean isCancelRequested(JobId jobId);

    void requeueDueRetry(JobId jobId);

    Job getJob(JobId jobId);

    ExperimentStatus getExperimentStatus(ExperimentId experimentId);

    /**
     * Idempotently resolves terminal work progress for a Backtest Job.
     * This is SET semantics, not a blind increment.
     */
    void recordTerminalProgress(
        JobId jobId,
        TerminalWorkOutcome outcome,
        BigDecimal score
    );
}
```

Conceptual terminal outcomes:

```text
SUCCEEDED
FAILED
```

A retryable failure does not resolve terminal failed work.

For a one-unit Backtest Job:
- success -> `completed_work=1, failed_work=0`;
- terminal failure -> `completed_work=0, failed_work=1`.

Repeated calls with the same terminal outcome are idempotent.

---

## 2. Recovery Query Boundary

```java
package com.cryptostrategy.platform.experiment.api.port.in;

import java.time.Instant;
import java.util.List;

public interface TrustedWorkerRecoveryQueryUseCase {

    List<RecoverableQueuedJob> findRecoverableQueuedJobs(
        Instant olderThan,
        int limit
    );

    List<DueRetryJob> findDueRetries(
        Instant dueAtOrBefore,
        int limit
    );

    List<StaleRunningAttempt> findStaleRunningAttempts(
        Instant startedBefore,
        int limit
    );
}
```

The concrete record names may follow repository naming conventions during implementation. The semantic requirement is fixed: bounded recovery discovery is capability-owned and Worker-safe.

---

## 3. Security Invariants

1. Implementation resolves `Job -> Experiment -> owner_user_id` internally.
2. Candidate/Attempt parent relationships are revalidated.
3. `apps/worker` is the only runtime composition authorized to bind this facade.
4. Public REST/API/F-009 must not expose it as an authorization bypass.
5. Worker must not import F-005 stores or persistence internals.
6. Trusted recovery queries return only fields needed for orchestration, not persistence row models.

---

## 4. Progress Ownership

Backtest completion owns candidate terminal completion. Ranking Handler must not call `recordTerminalProgress` for the same successful Candidate.

Leaderboard progress notifications may reference F-006 Revision state but must not double-count Backtest work.

# Research & Architecture Decisions: F-007 Worker and Reliable Job Processing

**Feature**: F-007  
**Status**: Planning Complete — Ready for Checklist  
**Date**: 2026-09-01

---

## 1. Trusted Worker Authorization Boundary

### Decision

Use an F-005-owned trusted Worker/system facade. `apps/worker` supplies only resource/routing identities. The facade resolves:

```text
Job -> Experiment -> owner_user_id
```

and validates:

```text
Candidate -> Experiment
Attempt -> Job
```

before delegating to the canonical F-005 state machine.

### Rationale

Redis data is not authorization authority. This preserves user-facing owner predicates while allowing trusted background execution.

### Rejected

- Put `ownerUserId` in Redis and trust it.
- Let Worker call F-005 repositories/JDBC directly.
- Reuse public API authorization shortcuts.

---

## 2. Worker-Compatible F-005/F-006 Completion Ordering

### Finding

The current repository has a real integration mismatch:

- F-005 success finalization transitions Attempt and Job to `SUCCEEDED`.
- F-006 Backtest Result persistence verifies the referenced Attempt is already `SUCCEEDED`.
- Existing `RunBacktestUseCase` both computes and persists the Result.

Calling F-006 while Attempt is RUNNING fails lineage validation. Finalizing F-005 success before calling F-006 makes a computation/persistence failure capable of leaving a false `SUCCEEDED` Job.

### Decision

Introduce a capability-owned prepare/commit seam:

1. F-006 `PrepareBacktestUseCase` performs deterministic computation without Result persistence.
2. `modules/experiment-execution` owns `CompleteBacktestAttemptUseCase`.
3. Completion executes in a short shared PostgreSQL transaction:
   - finalize F-005 Attempt/Job success;
   - commit prepared Backtest Result through F-006;
   - persist Evaluation through F-006;
   - record terminal progress idempotently through F-005.
4. Failure rolls back all completion writes.

The existing F-006 deterministic algorithm and lineage checks remain unchanged. Only the integration seam is split.

### Rationale

This avoids:
- long DB transactions around Backtest computation;
- false durable success;
- Result rows linked to non-successful Attempts;
- Worker importing F-006 internals.

---

## 3. Outbox Multi-Publisher Concurrency

### Decision

Do **not** rely on `FOR UPDATE SKIP LOCKED` as an exclusive claim across Redis publication.

Use duplicate-tolerant scan + conditional completion updates:

```sql
SELECT ...
FROM platform.outbox_event
WHERE published_at IS NULL
ORDER BY occurred_at
LIMIT :batchSize;
```

Multiple publishers may publish the same event. This is acceptable under at-least-once semantics.

### Success/failure semantics

- every physical publish attempt increments `publish_attempts`;
- a successful publisher sets `published_at` if still null and clears `last_error`;
- a late failed publisher must not overwrite another publisher's success;
- suppression marks the row completed with an auditable `SUPPRESSED_*` reason but does not count as a physical publish attempt.

### Rationale

A row lock held in a short transaction disappears before network I/O. Holding a transaction open across Redis I/O is undesirable. Duplicate-safe publication is simpler and consistent with the spec.

---

## 4. Dual-Layer Consumer Idempotency

### Decision

Use:
1. `platform.processed_message` completed marker keyed by `(consumer_name, message_id)`;
2. durable domain guards specific to each consumer.

Backtest Worker:
- only QUEUED Job can start a new Attempt;
- F-005 atomic transition and unique `(job_id, attempt_number)` prevent concurrent starts;
- terminal Job deliveries are acknowledged without new execution.

Ranking Handler:
- `processed_message` protects completed message handling;
- F-006 durable projection/fingerprint semantics protect concurrent duplicate ranking.

`insertIfAbsent` is preferred for the completed marker. A non-atomic read-then-insert is acceptable only because domain guards remain authoritative.

---

## 5. Recovery Query Boundaries

### Decision

`apps/worker` schedules recovery but does not query capability tables directly.

F-005 trusted recovery queries expose bounded:
- recoverable QUEUED jobs;
- due RETRY_SCHEDULED jobs;
- stale RUNNING attempts;
- current Job/Experiment state needed for validation.

F-006 exposes a Leaderboard reconciliation use case which owns Evaluation/Leaderboard reads.

### Rationale

This preserves modular boundaries and keeps SQL/persistence models out of the Worker runtime.

---

## 6. Redis Stream Client

### Decision

Use Spring Data Redis/Lettuce Consumer Groups with explicit acknowledgement.

Requirements:
- manual `XACK`;
- pending/reclaim support;
- configurable bounded read/prefetch/in-flight concurrency;
- no exactly-once claims.

No specific worker count or latency target is introduced in planning.

---

## 7. Redis Loss Recovery

### Decision

Use durable-state-driven recovery:

1. **Outbox retry** for unpublished rows.
2. **Queue Reconciler** for durable BACKTEST Jobs still QUEUED beyond the grace period even when their original Outbox row was already marked published.
3. **Stale Attempt Reconciler** for orphaned RUNNING Attempts.
4. **Retry Orchestrator** for due RETRY_SCHEDULED Jobs.
5. **Leaderboard Reconciler** for lost `candidate.evaluated.v1`.
6. **Dead-letter regeneration** may be performed from durable FAILED Job state because DLQ is diagnostic only.

No new Job/Candidate/Experiment identity is created during recovery.

---

## 8. Leaderboard Reconciliation Definition

### Decision

Do not define “unprojected Evaluation” as “Evaluation absent from leaderboard_entry”.

A valid Evaluation may never be Top-K.

Reconciliation instead recomputes deterministic Top-K from the durable leaderboard-eligible Evaluation set and delegates to F-006 idempotent projection. If the projection fingerprint equals the latest Revision, the operation is a no-op.

### Rationale

This remains correct for:
- evaluations outside Top-K;
- dropped candidate-evaluated messages;
- duplicate messages;
- Redis total loss.

Repeated no-op reconciliation is acceptable and does not require a new projection-marker table for correctness.

---

## 9. Progress Ownership and Transport

### Decision

Backtest terminal completion owns candidate completion counters exactly once.

- success -> idempotent terminal success progress;
- terminal failure -> idempotent terminal failure progress;
- retryable failure -> no terminal failed count;
- Ranking Handler never increments candidate completion.

Per-Experiment counts are derived from durable Backtest Job states unless a future Search-owned aggregate exists.

For cross-process notification, use transient:

```text
progress.events.v1
```

rather than Spring `ApplicationEvent`.

### Rationale

`apps/worker` and future F-009 are separate JVM processes. JVM-local events cannot serve as the integration boundary.

---

## 10. Outbox Lifecycle Routing

### Decision

Use explicit event-specific routing.

- BACKTEST `JobQueued` -> `backtest.jobs.v1`
- SEARCH `JobQueued` -> `lifecycle.events.v1` notification only
- `ExperimentQueued` -> `lifecycle.events.v1`
- `ExperimentStopRequested` -> `lifecycle.events.v1`
- `JobCancelRequested` -> `lifecycle.events.v1`
- `JobCancelled` -> `lifecycle.events.v1`

`search.requests.v1` remains reserved and is not used to start Search execution in F-007.

### Rationale

All F-005 Outbox events are handled; none is silently discarded. Work dispatch remains separated from lifecycle notification.

---

## 11. Candidate-Evaluated Message Authority

### Decision

`overallScore` may remain in `candidate.evaluated.v1` as a diagnostic fast-path hint, but it is not business truth.

Ranking loads the canonical `EvaluationResult` using `evaluationResultId` through an F-006-owned public boundary and uses the durable score/fingerprint for projection.

### Rationale

Redis may be stale, duplicated, or lost. Business ranking must not depend on transient payload values.

---

## 12. Message Compatibility

### Decision

- breaking changes -> new `messageVersion` / stream version;
- consumers ignore unknown optional properties;
- JSON payload schemas allow additional optional properties;
- Java DTOs are configured to ignore unknown properties.

### Rationale

This makes the stated forward-compatible policy match the actual schema behavior.

---

## 13. Database Migration Decision

### Decision

The corrected F-007 design does not require a new table/column solely for publication claims, reconciliation markers, or progress transport.

At implementation time, inspect the migration directory first. If an actual incompatibility is found, create a new forward-only migration. Never edit applied migrations.

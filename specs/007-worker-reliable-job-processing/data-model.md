# Data Model: F-007 Worker and Reliable Job Processing

**Feature**: F-007 — Worker and Reliable Job Processing  
**Branch**: `feature/007-worker-reliable-job-processing`  
**Status**: Planning Complete — Ready for Checklist

---

## 1. Durable vs Transient Boundaries

| Entity / State | Tier | Storage | Owner |
|---|---|---|---|
| Experiment / Manifest | Durable truth | PostgreSQL | F-005 |
| Candidate / Job / ExecutionAttempt | Durable truth | PostgreSQL | F-005 |
| BacktestResult / Trade | Durable truth | PostgreSQL | F-006 Backtesting |
| EvaluationResult | Durable truth | PostgreSQL | F-006 Evaluation |
| LeaderboardRevision / Entry | Durable truth | PostgreSQL | F-006 Leaderboard |
| Outbox write intent | Durable truth | `platform.outbox_event` | F-005 write side |
| Outbox publication metadata | Durable publication state | same Outbox row | F-007 publication port |
| ProcessedMessage | Durable completed marker | `platform.processed_message` | F-007 persistence port |
| Work/ranking/lifecycle/progress streams | Transient delivery | Redis Streams | F-007 runtime |
| Public WebSocket subscription state | Future | F-009 | out of scope |

Redis is never the sole source of business truth.

---

## 2. Redis Message Envelope

```json
{
  "messageId": "01J7K8M9N0P1Q2R3S4T5U6V7W8",
  "messageVersion": 1,
  "messageType": "BACKTEST_JOB",
  "occurredAt": "2026-09-01T10:15:30.123456Z",
  "correlationId": "01J7K8M9N0P1Q2R3S4T5U6V7W0",
  "payload": {}
}
```

Rules:
- `messageId`: stable ULID for deduplication.
- `messageVersion`: positive integer.
- `messageType`: uppercase snake case.
- `occurredAt`: UTC timestamp.
- `correlationId`: nonblank trace ID.
- unknown optional JSON properties are ignored by compatible consumers.
- required field removal/rename/type change is breaking.

---

## 3. Stream Payloads

### 3.1 `backtest.jobs.v1`

```json
{
  "experimentId": "...",
  "jobId": "...",
  "candidateId": "..."
}
```

Routing only. No `ownerUserId`, Candle data, Strategy implementation, parameters, or credentials.

### 3.2 `candidate.evaluated.v1`

```json
{
  "experimentId": "...",
  "jobId": "...",
  "candidateId": "...",
  "backtestResultId": "...",
  "evaluationResultId": "...",
  "overallScore": 0.0
}
```

`overallScore` is non-authoritative. Ranking loads the canonical EvaluationResult by `evaluationResultId`.

### 3.3 `jobs.dead-letter.v1`

Diagnostic projection only. Durable Job `FAILED` state remains authoritative.

### 3.4 `progress.events.v1`

```json
{
  "experimentId": "...",
  "jobId": "...",
  "completedWork": 1,
  "failedWork": 0,
  "totalWork": 1,
  "bestScore": 0.81,
  "leaderboardRevisionId": null,
  "eventType": "BACKTEST_COMPLETED"
}
```

Transient cross-process notification. F-009 later consumes it.

### 3.5 `lifecycle.events.v1`

```json
{
  "aggregateType": "JOB",
  "aggregateId": "...",
  "experimentId": "...",
  "jobId": "...",
  "candidateId": null,
  "lifecycleEventType": "JOB_CANCELLED"
}
```

Transient lifecycle notification for non-work F-005 Outbox events. F-007 does not implement the eventual public/UI consumer.

### 3.6 `search.requests.v1`

Reserved schema only. No F-007 consumer or Search runtime.

---

## 4. Trusted Worker APIs

### 4.1 Command facade

Conceptual operations:

```java
ExecutionAttempt startNextAttempt(JobId jobId, WorkerId workerId);
FrozenBacktestExecution getFrozenExecution(JobId jobId);
void finalizeFailure(...);
void finalizeCancelled(...);
boolean isCancelRequested(JobId jobId);
void requeueDueRetry(JobId jobId);
Job getJob(JobId jobId);
ExperimentStatus getExperimentStatus(ExperimentId experimentId);
void recordTerminalProgress(JobId jobId, TerminalOutcome outcome, BigDecimal score);
```

`recordTerminalProgress` is idempotent and **sets** the durable terminal work state. It does not blindly increment counters.

### 4.2 Recovery query facade

```java
List<RecoverableQueuedJob> findRecoverableQueuedJobs(Instant olderThan, int limit);
List<DueRetryJob> findDueRetries(Instant dueAtOrBefore, int limit);
List<StaleRunningAttempt> findStaleRunningAttempts(Instant startedBefore, int limit);
```

Worker never accesses F-005 persistence directly.

---

## 5. Worker-Compatible Backtest Completion

### 5.1 Prepared outcome

F-006 adds an immutable prepared computation model, conceptually:

```java
PreparedBacktestOutcome {
  experimentId
  candidateId
  jobId
  attemptId
  computedResult
}
```

It contains the deterministic Backtest output required for persistence but is not itself durable truth.

### 5.2 Completion transaction

`modules/experiment-execution` exposes a completion use case whose short PostgreSQL transaction performs:

```text
F-005 Attempt/Job -> SUCCEEDED
+
F-006 BacktestResult + Trades persist
+
F-006 EvaluationResult persist
+
F-005 terminal progress set
```

All commit or all rollback.

This is an application transaction boundary, not a new database entity.

---

## 6. Processed Message

Existing table:

```text
platform.processed_message
PK (consumer_name, message_id)
processed_at
expires_at
```

F-007 treats it as a **completed marker**, not a distributed lock.

Preferred write semantic:

```text
insertIfAbsent(consumerName, messageId, processedAt, expiresAt)
```

Domain state remains the concurrency authority.

---

## 7. Outbox Publication State

Existing Outbox columns are reused.

F-007 publication semantics:

- `published_at IS NULL`: still eligible for publication.
- `publish_attempts`: number of actual Redis publish attempts.
- `last_error`: safe last failure diagnostic or explicit `SUPPRESSED_*` audit reason.
- suppression marks the row completed without incrementing physical publish attempts.

No lease/claim column is introduced by this plan.

---

## 8. Progress Invariant

For one Backtest Job:

```text
total_work = 1
```

Terminal states resolve progress idempotently:

```text
SUCCEEDED -> completed_work=1, failed_work=0
FAILED    -> completed_work=0, failed_work=1
CANCELLED -> no successful/failed business outcome unless the F-005 contract explicitly defines otherwise
```

Retryable Attempt failure does not increment terminal failed work.

Ranking never increments these counters.

Per-Experiment counts are derived from durable child Backtest Jobs unless a future Search feature owns a dedicated aggregate progress model.

---

## 9. Leaderboard Reconciliation Model

No “projected” flag is inferred from Top-K membership.

F-006 reconciliation:
- loads all durable leaderboard-eligible Evaluations for a bounded Experiment;
- deterministically recomputes Top-K;
- saves a new Revision only if the fingerprint changes;
- otherwise returns/no-ops against the latest Revision.

An Evaluation outside Top-K is still considered safely reconciled by this rule.

---

## 10. Database Change Assessment

No new F-007 schema object is required by the corrected design.

Implementation must inspect current migrations before coding. If a real incompatibility appears, create a new forward-only migration; never edit an applied migration.

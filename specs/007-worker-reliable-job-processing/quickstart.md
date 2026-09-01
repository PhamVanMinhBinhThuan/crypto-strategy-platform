# Quickstart & Validation Guide: F-007 Worker and Reliable Job Processing

**Feature**: F-007  
**Status**: Planning Complete — Validation Plan

---

## 1. Prerequisites

- Java 21.
- Local PostgreSQL/Supabase development database.
- Local Redis.
- Current F-005 and F-006 code/migrations present.
- F-007 trusted Worker facade and worker-compatible F-006 prepare/commit seam implemented before end-to-end tests.

Do not apply shared/remote database migrations implicitly during verification.

---

## 2. Environment Configuration

Configure, at minimum:

```text
DATABASE_URL
DATABASE_USERNAME
DATABASE_PASSWORD

SPRING_DATA_REDIS_HOST
SPRING_DATA_REDIS_PORT

WORKER_CONCURRENCY_BACKTEST
WORKER_PREFETCH
WORKER_MAX_IN_FLIGHT

WORKER_RETRY_MAX_ATTEMPTS
WORKER_RETRY_BASE_DELAY
WORKER_RETRY_MULTIPLIER
WORKER_RETRY_MAX_DELAY
WORKER_RETRY_JITTER

WORKER_EXECUTION_TIMEOUT
WORKER_RECOVERY_GRACE
WORKER_RECONCILIATION_GRACE
WORKER_PROCESSED_MESSAGE_TTL
WORKER_OUTBOX_SCAN_INTERVAL
```

Values are environment configuration. The planning artifacts intentionally do not define hard-coded production defaults.

---

## 3. Architecture Verification

Run architecture tests and verify:

- `apps/worker` imports no `com.cryptostrategy.platform.*.internal..`.
- Worker has no raw JDBC/SQL.
- public API/F-009 cannot import the trusted Worker facade.
- Outbox/processed-message JDBC implementations are internal to `modules/persistence`.
- Leaderboard reconciliation queries remain inside F-006.
- Backtest prepare/commit logic remains inside F-006 / `modules/experiment-execution`, not Worker.

Example:

```powershell
./gradlew :architecture-tests:test
```

---

## 4. Scenario A — Happy Path

1. Create/freeze Experiment and durable Backtest Job.
2. Confirm F-005 creates `JobQueued` Outbox row.
3. Outbox Publisher sends BACKTEST Job to `backtest.jobs.v1`.
4. Worker:
   - checks processed marker;
   - starts Attempt;
   - loads frozen execution;
   - prepares Backtest outside DB completion transaction;
   - calls `CompleteBacktestAttemptUseCase`.
5. Completion transaction commits:
   - Attempt/Job `SUCCEEDED`;
   - exactly one BacktestResult;
   - exactly one canonical EvaluationResult;
   - terminal Backtest progress recorded once.
6. Worker sends `candidate.evaluated.v1` and `BACKTEST_COMPLETED` progress notification.
7. Ranking Handler projects Top-K from durable Evaluation state.
8. Processed markers are inserted before XACK.

Expected: no duplicate durable effects.

---

## 5. Scenario B — Duplicate Backtest Delivery

Publish the same Backtest message twice using the same `messageId`.

Expected:
- at most one new Attempt starts;
- one canonical Backtest Result;
- one canonical Evaluation;
- terminal progress is not incremented twice;
- duplicate delivery is acknowledged safely.

---

## 6. Scenario C — Crash During Completion Transaction

Inject failure after F-005 success update but before F-006 Result/Evaluation commit **inside the shared completion transaction**.

Expected:
- whole transaction rolls back;
- Job/Attempt are not durably left SUCCEEDED;
- no partial Result/Evaluation remains;
- stale recovery can process the RUNNING Attempt normally.

This specifically verifies the F-005/F-006 success-order fix.

---

## 7. Scenario D — Crash After Completion Commit Before Processed Marker

Commit Attempt/Job/Result/Evaluation/progress, then crash before:
- `candidate.evaluated.v1`,
- processed marker,
- XACK.

Expected after redelivery:
- Backtest is not executed again;
- no duplicate Result/Evaluation;
- processed marker is repaired where safe;
- message is acknowledged;
- Leaderboard Reconciler repairs any missing ranking fast path.

---

## 8. Scenario E — Publisher Crash After Redis Accepts Message

Publish Outbox event to Redis, crash before setting `published_at`.

Expected:
- row remains eligible;
- another publisher may publish duplicate;
- consumer dedup/domain guard prevents duplicate business effect;
- `publish_attempts` records physical attempts;
- final successful publisher marks completion.

---

## 9. Scenario F — Concurrent Outbox Publishers

Run multiple publisher instances against the same unpublished row.

Expected:
- duplicates are allowed;
- no publisher assumes exclusive claim;
- a late failure never overwrites an already-recorded success;
- no lost event;
- no duplicate durable business outcome downstream.

---

## 10. Scenario G — Outbox Suppression

Create stale events:

- BACKTEST `JobQueued`, then cancel Job before publication;
- `JobCancelRequested`, then complete cancellation;
- `JobCancelled` with durable Job already CANCELLED.

Expected:
- stale `JobQueued` is suppressed/audited;
- stale `JobCancelRequested` may be suppressed/audited;
- `JobCancelled` is still published to `lifecycle.events.v1`;
- suppression does not increment physical `publish_attempts`.

---

## 11. Scenario H — Redis Total Loss

Prepare:
- unpublished Outbox rows;
- already-published durable QUEUED Backtest Jobs;
- RUNNING Attempts;
- RETRY_SCHEDULED Jobs.

Flush Redis and restart Worker.

Expected:
- unpublished Outbox rows retry;
- Queue Reconciler redispatches durable QUEUED Backtest Jobs;
- Stale Attempt Reconciler resolves orphaned RUNNING work;
- due retries requeue with same Job IDs;
- no new Experiment/Candidate/Job IDs are created.

---

## 12. Scenario I — Retry vs Cancellation Race

Race:
- due `RETRY_SCHEDULED -> QUEUED`;
- cancellation of the same Job.

Expected:
- F-005 durable locking selects one legal outcome;
- if cancellation wins, no new Attempt starts;
- stale JobQueued publication is suppressed if Job becomes CANCELLED.

---

## 13. Scenario J — Lost Candidate-Evaluated Message

Persist successful Evaluation but drop `candidate.evaluated.v1`.

Expected:
- Leaderboard Reconciler loads durable eligible Evaluations;
- recomputes deterministic Top-K;
- ranking becomes correct without requiring the lost Redis message.

---

## 14. Scenario K — Evaluation Outside Top-K

Create a valid leaderboard-eligible Evaluation whose score is below Top-K cutoff.

Expected:
- it may have no `leaderboard_entry`;
- reconciler does not classify that absence as a processing failure;
- recomputation is a safe no-op if Top-K fingerprint does not change;
- no infinite creation of duplicate revisions.

---

## 15. Scenario L — Ranking Message Score Tampering

Publish `candidate.evaluated.v1` with:
- correct `evaluationResultId`;
- deliberately wrong `overallScore`.

Expected:
- Ranking loads durable EvaluationResult;
- durable score wins;
- transient score is treated as hint/diagnostic only.

---

## 16. Scenario M — Progress Double-Count Prevention

Deliver:
- successful Backtest event;
- duplicate ranking event;
- duplicate backtest message.

Expected:
- Backtest terminal progress resolves exactly once;
- Ranking does not increment completion counters;
- `completed_work + failed_work <= total_work`.

---

## 17. Scenario N — Cross-JVM Progress Notification

Publish `BACKTEST_COMPLETED` / `LEADERBOARD_UPDATED` to `progress.events.v1`.

Expected:
- message is consumable by a separate process contract;
- no Spring JVM-local event is required;
- deleting the Redis progress stream does not change durable progress returned from PostgreSQL.

---

## 18. Scenario O — Message Compatibility

1. Send a supported version with an unknown optional property.
   - consumer ignores the property.
2. Send an unsupported `messageVersion`.
   - consumer rejects safely without an infinite retry loop.
3. Verify no sensitive stack trace/raw SQL detail is published.

---

## 19. Scenario P — Search Remains Deferred

Publish or create F-005 Search-related lifecycle Outbox state.

Expected:
- F-007 does not instantiate Search Coordinator or generator;
- SEARCH `JobQueued` is treated as lifecycle notification, not Backtest dispatch;
- no F-007 consumer group is created for `search.requests.v1`.

---

## 20. Full Test Suite

At minimum:

```powershell
./gradlew :modules:contracts:test `
  :modules:experiment:test `
  :modules:backtesting:test `
  :modules:evaluation:test `
  :modules:leaderboard:test `
  :modules:experiment-execution:test `
  :modules:persistence:test `
  :apps:worker:test `
  :architecture-tests:test
```

Run repository-wide `check` after targeted suites pass.

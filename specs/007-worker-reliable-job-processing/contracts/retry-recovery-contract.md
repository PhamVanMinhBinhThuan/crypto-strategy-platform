# Contract: Retry & Recovery Orchestration

**Orchestration owner**: `apps/worker`  
**Durable lifecycle owner**: F-005 (`modules/experiment`)  
**No direct SQL from Worker**

---

## 1. Failure Classification

Runtime failures are mapped to the five canonical F-005 classifications:

| Failure mode | Classification | Retryable? |
|---|---|---:|
| Worker execution timeout/crash | `WORKER_CRASHED` | yes |
| Transport/provider/database transient connectivity | `TRANSIENT_NETWORK_ERROR` | yes |
| Temporarily unavailable durable dataset input | `DATA_UNAVAILABLE_RETRY` | yes |
| Deterministic invalid strategy/business execution | `PERMANENT_LOGIC_ERROR` | no |
| Unclassified terminal runtime failure | `UNKNOWN_ERROR` | no |

The exact exception mapping is implemented/tested in Worker but classification values remain F-005-owned.

---

## 2. Backoff

Backoff is computed from environment-configured values:

```text
base delay
multiplier
maximum delay
jitter policy
maximum attempts
```

No production timing or retry number is hard-coded by this planning contract.

A typical implementation may use exponential backoff with jitter, but all parameters remain configurable.

---

## 3. Retry Lifecycle

1. Worker catches a retryable failure during Backtest preparation/execution.
2. Worker computes `nextRetryAt` from configured policy.
3. Worker calls trusted F-005 `finalizeFailure(...)`.
4. F-005 finalizes current Attempt as `FAILED`, sets parent Job to `RETRY_SCHEDULED`, and stores `next_retry_at`.
5. Retry Orchestrator calls the trusted recovery query:
   ```text
   findDueRetries(now, limit)
   ```
6. For each returned Job, it re-reads durable state and calls:
   ```text
   requeueDueRetry(jobId)
   ```
7. F-005 performs `RETRY_SCHEDULED -> QUEUED` and writes `JobQueued`.
8. Outbox Publisher later dispatches the durable event.

Worker does not query `experiment.job` directly.

---

## 4. Stale Attempt Recovery

The scheduled reconciler calls:

```text
findStaleRunningAttempts(startedBefore, limit)
```

through the F-005 trusted recovery boundary.

For each candidate:
- re-read current durable Job/Attempt state;
- if no longer RUNNING, do nothing;
- if still RUNNING and no completion transaction committed, finalize as `WORKER_CRASHED`;
- standard retry/terminal failure policy applies.

The corrected Worker success path commits F-005 success + F-006 Result/Evaluation atomically, so the reconciler does not have to guess whether F-006 durable output committed without F-005 success.

---

## 5. Queue Reconciliation

Queue Reconciler calls:

```text
findRecoverableQueuedJobs(olderThan, limit)
```

through F-005.

It redispatches only BACKTEST Jobs still durably `QUEUED` and preserves existing Job/Experiment/Candidate identities.

Redis inspection is not used as business truth.

---

## 6. Cancellation

Before requeue/start:
- re-read durable Job state;
- cancellation wins according to F-005 serialized state transitions;
- stale `JobQueued` publication is suppressed when the Job is no longer dispatchable.

No new Attempt is created for a Job that is already CANCELLED.

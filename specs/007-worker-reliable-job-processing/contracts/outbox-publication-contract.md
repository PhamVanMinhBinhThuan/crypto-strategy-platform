# Contract: Outbox Publication Port (`modules/persistence`)

**Owner**: F-007 publication side  
**Public package**: `com.cryptostrategy.platform.persistence.api.worker`  
**Consumer**: `apps/worker`  
**Table**: existing `platform.outbox_event`

The existing F-005 `OutboxStore` remains the write-side owner. This F-007 port only discovers and records publication outcomes.

---

## 1. Java Port

```java
package com.cryptostrategy.platform.persistence.api.worker;

import java.time.Instant;
import java.util.List;

public interface OutboxPublicationPort {

    List<OutboxEventRecord> listUnpublishedBatch(int batchSize);

    /**
     * Records one successful physical Redis publish attempt.
     * Must increment publish_attempts.
     * Must not overwrite an earlier published_at with a later timestamp.
     */
    void recordPublishSuccess(String outboxEventId, Instant publishedAt);

    /**
     * Records one failed physical Redis publish attempt.
     * Must increment publish_attempts only while the row is still unpublished.
     * Must never overwrite a success recorded concurrently.
     */
    void recordPublishFailure(String outboxEventId, String safeDiagnostic);

    /**
     * Completes a stale/non-dispatchable Outbox intent without Redis publication.
     * Does not increment publish_attempts.
     */
    void markSuppressed(String outboxEventId, Instant completedAt, String suppressionReason);
}
```

`OutboxEventRecord` is a publication DTO in the persistence public API. It must not expose JDBC types or internal row classes.

---

## 2. Discovery Semantics

```sql
SELECT ...
FROM platform.outbox_event
WHERE published_at IS NULL
ORDER BY occurred_at ASC
LIMIT :batch_size;
```

This is a **duplicate-tolerant scan**, not an exclusive claim.

Two publisher instances MAY read and publish the same row. F-007 guarantees at-least-once delivery plus idempotent downstream effects, not single physical publication.

### Why the contract does not rely on `FOR UPDATE SKIP LOCKED`

A PostgreSQL row lock ends with the transaction. Returning claimed rows from a short transaction and then performing Redis network I/O would no longer be protected by the lock. Holding a transaction open during Redis I/O is intentionally avoided.

No lease column is required by this plan.

---

## 3. Publication Outcome Semantics

### Success

Conceptually:

```sql
UPDATE platform.outbox_event
SET publish_attempts = publish_attempts + 1,
    published_at = COALESCE(published_at, :published_at),
    last_error = NULL
WHERE outbox_event_id = :id;
```

If another publisher already marked success, the later success still represents a physical publish attempt, so `publish_attempts` may increment while the original `published_at` remains unchanged.

### Failure

Conceptually:

```sql
UPDATE platform.outbox_event
SET publish_attempts = publish_attempts + 1,
    last_error = :safe_diagnostic
WHERE outbox_event_id = :id
  AND published_at IS NULL;
```

A late failure must not revert or annotate a row already completed successfully by another publisher.

### Suppression

Suppression performs no Redis publish and does not increment `publish_attempts`.

Using the existing schema, the adapter marks the row completed and stores an auditable safe reason such as:

```text
SUPPRESSED_JOB_NOT_DISPATCHABLE
SUPPRESSED_CANCEL_ALREADY_COMPLETED
SUPPRESSED_EXPERIMENT_TERMINAL
```

No stack trace, SQL text, credential, or user-sensitive data may be stored.

---

## 4. Event Routing

| Event | Route | Validation |
|---|---|---|
| `JobQueued` + BACKTEST | `backtest.jobs.v1` | publish only if Job remains `QUEUED` |
| `JobQueued` + SEARCH | `lifecycle.events.v1` | notification only; no Search runtime |
| `ExperimentQueued` | `lifecycle.events.v1` | event-specific lifecycle validation |
| `ExperimentStopRequested` | `lifecycle.events.v1` | event-specific lifecycle validation |
| `JobCancelRequested` | `lifecycle.events.v1` | may suppress if cancellation already completed |
| `JobCancelled` | `lifecycle.events.v1` | MUST NOT suppress merely because Job is `CANCELLED` |

`search.requests.v1` remains reserved and is not used to start Search execution in F-007.

Unknown Outbox event types are not silently discarded. They are recorded as unroutable operator-visible failures and remain unpublished until explicitly handled.

---

## 5. Aggregate-State Validation

Validation uses public F-005 trusted read boundaries, never direct SQL from `apps/worker`.

- `JobQueued`: current durable Job must be dispatchable.
- `JobCancelRequested`: stale request may be suppressed after durable cancellation completes.
- `JobCancelled`: durable `CANCELLED` is the expected state announced by the event.
- Experiment lifecycle events are suppressed only when genuinely stale for their meaning, not merely because the aggregate reached the state represented by the event.

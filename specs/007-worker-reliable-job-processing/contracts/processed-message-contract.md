# Contract: Processed Message Deduplication Port (`modules/persistence`)

**Public package**: `com.cryptostrategy.platform.persistence.api.worker`  
**Consumer**: `apps/worker`  
**Table**: existing `platform.processed_message`

`platform.processed_message` is a durable **completed marker**, not a distributed execution lock.

---

## 1. Java Port

```java
package com.cryptostrategy.platform.persistence.api.worker;

import java.time.Instant;

public interface ProcessedMessageStore {

    boolean isProcessed(String consumerName, String messageId, Instant now);

    /**
     * Inserts the completed marker if absent.
     * Returns true when inserted, false when a marker already exists.
     */
    boolean insertIfAbsent(
        String consumerName,
        String messageId,
        Instant processedAt,
        Instant expiresAt
    );
}
```

---

## 2. Table Contract

Existing columns:

```text
consumer_name
message_id
processed_at
expires_at
```

Primary key:

```text
(consumer_name, message_id)
```

The TTL is environment-configurable and must exceed the configured pending/reclaim/redelivery and operational recovery horizon.

Expired infrastructure markers do not override durable domain truth. A terminal Backtest Job or already-idempotently-projected Evaluation remains safe even when the old processed marker has expired.

---

## 3. Read

Conceptually:

```sql
SELECT 1
FROM platform.processed_message
WHERE consumer_name = :consumer_name
  AND message_id = :message_id
  AND expires_at > :now;
```

---

## 4. Insert

Use an insert-if-absent semantic:

```sql
INSERT INTO platform.processed_message(
    consumer_name, message_id, processed_at, expires_at
)
VALUES (:consumer_name, :message_id, :processed_at, :expires_at)
ON CONFLICT (consumer_name, message_id) DO NOTHING;
```

The affected-row count determines the return value.

---

## 5. Consumer Pattern

```text
receive
-> check marker
-> if marker exists: XACK
-> otherwise perform durable business effect guarded by F-005/F-006 state
-> insert completed marker
-> XACK
```

A read-then-insert race is not relied upon for business correctness. F-005 Job/Attempt state protects Backtest execution; F-006 deterministic/idempotent projection protects Ranking.

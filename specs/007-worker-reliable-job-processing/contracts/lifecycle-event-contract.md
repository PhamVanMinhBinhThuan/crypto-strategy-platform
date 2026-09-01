# Contract: Lifecycle Notification (`lifecycle.events.v1`)

**Stream**: `lifecycle.events.v1`  
**Producer**: F-007 Outbox Publisher  
**F-007 Consumer**: none  
**Future Consumers**: Search/F-009/control-plane capabilities as explicitly defined later  
**Durability**: transient notification; PostgreSQL aggregate state is authoritative

---

## 1. Purpose

This stream gives every non-work F-005 Outbox lifecycle event an explicit publication target without turning lifecycle notifications into Backtest or Search work-dispatch commands.

Supported logical notifications:

```text
EXPERIMENT_QUEUED
EXPERIMENT_STOP_REQUESTED
SEARCH_JOB_QUEUED
JOB_CANCEL_REQUESTED
JOB_CANCELLED
```

---

## 2. Payload Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "LifecycleNotificationPayload",
  "type": "object",
  "required": [
    "aggregateType",
    "aggregateId",
    "experimentId",
    "lifecycleEventType"
  ],
  "properties": {
    "aggregateType": {
      "type": "string",
      "enum": ["EXPERIMENT", "JOB"]
    },
    "aggregateId": {
      "type": "string",
      "pattern": "^[0-9A-HJKMNP-TV-Z]{26}$"
    },
    "experimentId": {
      "type": "string",
      "pattern": "^[0-9A-HJKMNP-TV-Z]{26}$"
    },
    "jobId": {
      "type": ["string", "null"]
    },
    "candidateId": {
      "type": ["string", "null"]
    },
    "lifecycleEventType": {
      "type": "string",
      "enum": [
        "EXPERIMENT_QUEUED",
        "EXPERIMENT_STOP_REQUESTED",
        "SEARCH_JOB_QUEUED",
        "JOB_CANCEL_REQUESTED",
        "JOB_CANCELLED"
      ]
    }
  },
  "additionalProperties": true
}
```

---

## 3. Invariants

- `JobCancelled` is not suppressed merely because the durable Job is already CANCELLED.
- `JobCancelRequested` may be suppressed when cancellation has already completed.
- SEARCH Job notification never starts Search runtime in F-007.
- Loss of this stream never changes durable lifecycle state.
- F-007 does not implement public WebSocket or browser handling for these notifications.

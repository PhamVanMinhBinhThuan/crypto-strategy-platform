# Shared Interaction and Async State Reference

Every data-driven business screen should intentionally design the states relevant to its released contract.

## Baseline async states

| State | Expected presentation |
|---|---|
| Initial loading | stable skeleton/spinner with page context preserved |
| Refreshing | keep authoritative previous snapshot visible when safe; show non-blocking refresh indicator |
| Empty | explain what is absent and provide the next valid action when one exists |
| Success | render authoritative data without fixture/mock badge in production |
| Validation error | field-level message plus summary when useful; preserve user input |
| Inaccessible / ownership-safe not found | one uniform safe state; do not reveal whether another owner has the resource |
| Conflict | explain current state and valid next action without inventing a transition |
| Retryable dependency/network error | preserve safe snapshot, show retry affordance when the request is safe to retry |
| Degraded | identify what is degraded while keeping independent capabilities usable |
| Terminal failure | show safe failure code/message and relevant recovery action supported by contract |

## Realtime states

| State | Expected presentation |
|---|---|
| Connecting | indicate realtime freshness is being established |
| Connected | no excessive animation; show connection only where operationally useful |
| Reconnecting | keep the last durable snapshot, mark freshness as stale/recovering |
| Disconnected | do not pretend live updates are current; durable REST reads remain the recovery path |
| Subscription error | isolate to the affected Experiment/Leaderboard subscription when protocol allows |
| Snapshot recovery | resubscribe/re-read authoritative snapshot according to F-009 sequencing rules |

## Durable Job / Experiment presentation

Render only statuses defined by the released public contract. Typical UI may need to distinguish:

- QUEUED
- RUNNING
- RETRY_SCHEDULED
- SUCCEEDED
- FAILED
- CANCEL_REQUESTED
- CANCELLED
- STOP_REQUESTED
- STOPPED

Do not infer a backend transition from animation alone.

## Command safety

State-changing POST retry behavior must follow F-009 idempotency rules. If the outcome is uncertain, do not silently create a new logical command with a new idempotency key.

## Accessibility

- Status changes should have accessible text.
- Dialog/form focus must be controlled.
- Keyboard users must reach primary actions and tables.
- Loading/error/status must not be communicated by animation or color alone.
- Respect reduced-motion preferences for non-essential progress animation where production styling supports it.

# Contract: Search Events v1

Mọi message dùng envelope hiện có với `messageId`, `messageType`, `messageVersion`, `occurredAt`,
`correlationId` và payload. Unknown optional payload fields được bỏ qua; breaking change dùng version mới.

## `search.requests.v1`

Consumer group mới: `search-coordinators` (không dùng `ranking-workers`).

Required payload giữ backward compatibility:

```json
{
  "searchJobId": "01J00000000000000000000001",
  "experimentId": "01J00000000000000000000002",
  "concurrencyHint": 4,
  "topKTarget": 10
}
```

`messageType = SEARCH_REQUEST`, `messageVersion = 1`. Runtime MUST validate typed ULIDs và positive
bounded hints; durable Manifest/Run mới là authority cho configuration.

## `candidate.evaluated.v1`

Search Coordinator subscribe bằng group riêng, dùng payload F-007 hiện hành gồm Experiment,
Candidate, Job, Backtest Result, Evaluation Result và score identities. Message chỉ là trigger;
Coordinator MUST reload authoritative state trước progress/next decision.

## Progress/lifecycle publication

Coordinator reuse `progress.events.v1` và `lifecycle.events.v1`; payload phải tương thích F-007/F-009.
Progress counters là totals authoritative của SEARCH Job, không phải delta từ message.

## Delivery semantics

- At-least-once; message ID dedupe là optimization, durable decision là correctness.
- ACK chỉ sau durable transition hoặc khi message được chứng minh irrelevant/terminal/malformed.
- Retry hữu hạn; malformed/unsupported/invariant failure đi dead-letter với redacted metadata.
- Consumer reclaim pending message sau configured idle time.
- Queue mất được repair từ Outbox/reconciler; không mất durable business outcome.

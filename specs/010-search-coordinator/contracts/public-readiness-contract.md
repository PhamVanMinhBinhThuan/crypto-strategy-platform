# Contract: Public Start/Reproduce Readiness

## Existing behavior

F-009 hiện expose schema nhưng Start/Reproduce Experiment trả stable
`503 DEPENDENCY_UNAVAILABLE`. Read/stop/cancel và standalone Backtest không bị ảnh hưởng.

## Start gate removal conditions

Chỉ thay Start gate bằng published command call khi tất cả điều kiện sau pass:

1. Start transaction atomic và idempotency replay/conflict có integration evidence.
2. Search request Outbox được publish và consumer group Coordinator xử lý được.
3. Finite Random Search chạy đến Result/Evaluation/Leaderboard terminal snapshot.
4. Duplicate/stale/out-of-order event và process restart không tạo business outcome trùng.
5. Stop race không dispatch Candidate sau durable stop.
6. Two-user ownership tests trả cùng inaccessible result như missing resource.
7. Error/log payload qua public boundary được redaction.

## Reproduce gate removal conditions

Reproduce gate độc lập và chỉ được gỡ sau khi toàn bộ Start conditions ở trên đã pass, đồng thời:

1. Initialization atomic giữ source bất biến và copy exact Candidate sequence.
2. Public request chỉ enqueue durable reproduction/verification work và trả `202`; API thread không
   chạy Backtest hoặc verification đồng bộ.
3. Terminal trigger/reconciler chuyển verification idempotently qua
   `PENDING -> RUNNING -> MATCHED|MISMATCHED|FAILED`, kể cả sau restart.
4. Match/mismatch evidence cho Trade sequence, metrics và fingerprints có integration evidence.

## Public compatibility

- Giữ request/response DTO, paths, 202, `Location`, idempotency header và error envelope F-009.
- Accepted Start/Reproduce trả Experiment ID + SEARCH Job ID + `QUEUED`.
- Validation dùng stable 4xx; dependency/runtime outage dùng retryable safe 5xx theo error catalog.
- Không expose generator internal state, Worker identity, queue offset, SQL hay exception detail.
- WebSocket không nhận command tạo/reproduce; nó chỉ phân phối progress/lifecycle và hướng client
  reconcile từ authorized REST snapshot.

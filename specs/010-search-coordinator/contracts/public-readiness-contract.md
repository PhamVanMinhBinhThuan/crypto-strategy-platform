# Contract: Public Start/Reproduce Readiness

## Existing behavior

F-009 hiện expose schema nhưng Start/Reproduce Experiment trả stable
`503 DEPENDENCY_UNAVAILABLE`. Read/stop/cancel và standalone Backtest không bị ảnh hưởng.

## Gate removal conditions

Chỉ thay gate bằng published command call khi tất cả điều kiện sau pass:

1. Start transaction atomic và idempotency replay/conflict có integration evidence.
2. Search request Outbox được publish và consumer group Coordinator xử lý được.
3. Finite Random Search chạy đến Result/Evaluation/Leaderboard terminal snapshot.
4. Duplicate/stale/out-of-order event và process restart không tạo business outcome trùng.
5. Stop race không dispatch Candidate sau durable stop.
6. Reproduction giữ source bất biến, copy exact Candidate sequence và có verification outcome.
7. Two-user ownership tests trả cùng inaccessible result như missing resource.
8. Error/log payload qua public boundary được redaction.

## Public compatibility

- Giữ request/response DTO, paths, 202, `Location`, idempotency header và error envelope F-009.
- Accepted Start/Reproduce trả Experiment ID + SEARCH Job ID + `QUEUED`.
- Validation dùng stable 4xx; dependency/runtime outage dùng retryable safe 5xx theo error catalog.
- Không expose generator internal state, Worker identity, queue offset, SQL hay exception detail.
- WebSocket không nhận command tạo/reproduce; nó chỉ phân phối progress/lifecycle và hướng client
  reconcile từ authorized REST snapshot.

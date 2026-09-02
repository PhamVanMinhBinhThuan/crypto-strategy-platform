# ADR-0016: Search Coordinator dùng durable decisions và Worker orchestration

**Status**: Accepted
**Date**: 2026-09-02  
**Owners**: Tiến Luật  
**Extends**: ADR-0006, ADR-0007, ADR-0009, ADR-0010 và ADR-0014

## Context

ADR-0010 đã đặt Strategy Generator contract trong module Search; ADR-0014 cho phép orchestration
xuyên capability ở application layer. F-007 cố ý chỉ reserve `search.requests.v1`, còn F-009 giữ
Start/Reproduce Experiment dưới readiness gate. F-010 cần quyết định nơi chạy Coordinator, nơi sở
hữu state, transaction boundary và cách nhiều logical subscribers dùng completion stream.

Nếu state chỉ nằm trong Redis hoặc process memory, restart/queue loss có thể làm mất generation
position hoặc dispatch trùng. Nếu Search ghi trực tiếp Experiment tables, module ownership bị phá.

## Decision

1. `modules/search` sở hữu pure generator contract/registry/Random generator và durable Search Run
   model/ports. Module không phụ thuộc framework, Experiment, Backtest, Evaluation hay Redis.
2. `modules/experiment-execution` là application orchestration boundary cho Start/Reproduce,
   allocation, progress và async verification; nó chỉ compose published Search/Experiment ports.
   `apps/worker` host delivery/scheduling và gọi public orchestration ports, không sở hữu state.
3. PostgreSQL giữ Search Run, versioned generator state và coordination decision; Redis Streams chỉ
   delivery và có thể rebuild từ Outbox/reconciliation.
4. `experiment-execution` định nghĩa composite transaction gateway; `modules/persistence` implement
   adapter atomically ghi next generator state, Candidate, logical Backtest Job, decision và Outbox.
   Đây không phải quyền cho Worker hoặc capability khác truy cập owner tables trực tiếp. Database
   version/fencing là correctness boundary; không dùng Redis lock.
5. Coordinator dùng consumer group riêng trên `candidate.evaluated.v1`; Ranking Handler tiếp tục
   group riêng vì một group Redis không fan-out cho nhiều capability.
6. Scheduling dùng bounded in-flight window. Không giữ DB transaction trong lúc chạy Backtest,
   chờ Redis hoặc thực thi generator ngoài proposal/revalidation boundary.
7. Reproduction reuse exact frozen Candidate sequence, tạo durable `PENDING` verification và trả
   async; terminal handler/reconciler so Trade/metrics/fingerprints rồi persist outcome idempotently.
8. F-009 gỡ Start gate sau US1 + US2 evidence; Reproduce gate độc lập chỉ gỡ thêm sau US3 evidence.
9. Dependency arrows chỉ cho phép `experiment-execution -> search`, `persistence -> search` và
   `worker -> search`; `search` không phụ thuộc ngược lại các module này.

## Alternatives Considered

- Coordinator trong `modules/search`: loại vì kéo Experiment/queue framework vào pure capability.
- Coordinator trong API: loại vì request process không phải durable runtime.
- State chỉ Redis hoặc seed-only: loại vì không đủ durable resume/version evolution.
- Generate toàn bộ upfront: loại vì unbounded queue và stop chậm.
- Dùng chung ranking consumer group: loại vì message sẽ bị chia thay vì fan-out.
- Chạy lại generator khi reproduction: loại vì artifact/version có thể không còn; exact Candidate
  evidence đã là nguồn mạnh hơn.

## Consequences

### Positive

- Deterministic resume và duplicate-safe allocation qua mọi crash boundary.
- Generator thay thế không ảnh hưởng downstream pipeline.
- Queue/cache mất không xóa durable progress hoặc publication intent.
- Public Start/Reproduce chỉ mở khi runtime thật sự sẵn sàng.

### Negative

- Cần Search-owned table, reconciliation loop và thêm consumer group.
- Atomic allocation chạm nhiều Experiment-owned records, cần port/adapter transaction được thiết kế
  cẩn thận và integration test concurrency.
- Reproduction cần bounded copy/dispatch của Candidate sequence.

## Validation

- Determinism và fixture-generator replaceability proof.
- Concurrent allocation/fencing và rollback không partial graph.
- Duplicate/stale/out-of-order completion không làm progress sai.
- Kill/restart và queue-loss recovery tới terminal state.
- Stop/allocate race không dispatch Candidate mới sau stop.
- Reproduction giữ source bất biến và so exact evidence.
- ArchUnit cấm Search framework/cross-internal dependency và Worker direct SQL.

## References

- [F-010 specification](../../specs/010-search-coordinator/spec.md)
- [F-010 plan](../../specs/010-search-coordinator/plan.md)
- [ADR-0010](0010-strategy-generator-contract.md)
- [ADR-0014](0014-experiment-execution-orchestrator.md)

## Supersession

- Supersedes: None
- Superseded by: None

## Amendment history

- 2026-09-02: Trước implementation, consistency analysis làm rõ orchestration thuộc
  `modules/experiment-execution`, persistence implement composite gateway, async verification và
  hai readiness gate độc lập. Không có code đã merge phụ thuộc wording cũ.

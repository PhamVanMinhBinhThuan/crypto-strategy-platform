# ADR-0015: Backtest đơn lẻ dùng aggregate single-run thuộc Experiment

**Status**: Proposed  
**Date**: 2026-09-02  
**Owners**: Tiến Luật  
**Extends**: ADR-0006, ADR-0009, ADR-0011 và ADR-0012

## Context

F-009 công bố `POST /api/v1/backtests` và trả `backtestId` cùng `jobId`. F-005 hiện
chỉ tạo Backtest Job cho một Candidate thuộc Experiment; F-006 cũng resolve execution
từ graph bất biến `Experiment -> Manifest -> Candidate -> Job -> Attempt`.

Dùng `CandidateId` làm `backtestId` sẽ làm sai nghĩa public contract. Tạo một parent
hoàn toàn mới cho Job lại buộc đổi queue contract, Attempt foreign key, worker recovery
và completion pipeline dù execution semantics không thay đổi. ADR-0011 đã yêu cầu một
Backtest đơn lẻ phải thuộc single-run Experiment hoặc aggregate tương đương có owner.

## Decision

F-005 sở hữu aggregate `StandaloneBacktest` với `BacktestId` riêng. Aggregate này là
projection bền vững của đúng một single-run Experiment, một Candidate thật và một
Backtest Job:

```text
StandaloneBacktest (BacktestId)
  -> Experiment + immutable Manifest
  -> exactly one Candidate Definition
  -> exactly one BACKTEST Job + Outbox event
```

`backtestId`, `experimentId`, `candidateId` và `jobId` là các identity riêng; API chỉ
công bố `backtestId` và `jobId` khi accept command. Candidate không bị đổi tên hoặc dùng
thay Backtest identity.

F-005 công bố owner-scoped input port để accept Backtest đã được freeze. `apps/api`
chỉ điều phối qua published boundary:

1. F-003 resolve Dataset snapshot bất biến.
2. F-004 resolve exact Strategy version/parameters và fingerprint.
3. F-006 parse/validate đầy đủ Backtest assumptions, không dùng hidden default.
4. F-005 canonicalize immutable Manifest/Candidate, rồi atomically ghi aggregate,
   Backtest Job, Outbox và completed idempotency receipt.

Idempotency scope là `owner + START_BACKTEST + key`. Transaction đầu tiên cấp toàn bộ
identity và hoàn tất receipt cùng business state. Request cạnh tranh cùng key chờ
transaction đầu, sau đó replay cùng outcome; payload khác trả conflict. Rollback không
để lại receipt `IN_PROGRESS` mồ côi hoặc partial aggregate.

Worker/queue tiếp tục dùng contract hiện tại chứa `experimentId`, `candidateId` và
`jobId`. Đây là execution identity nội bộ, không phải public Backtest identity.

Schema mới phải là forward migration, tạo bảng owner `experiment.standalone_backtest`
và constraints chứng minh các reference cùng single-run graph. Browser không có quyền
đọc/ghi bảng này trực tiếp.

## Alternatives Considered

- **Dùng CandidateId làm backtestId**: ít code nhưng làm sai identity và khiến public
  resource phụ thuộc implementation detail của Search.
- **Tạo Job parent Backtest hoàn toàn mới**: domain thuần hơn nhưng phải version lại
  queue/Attempt/worker pipeline trong khi graph execution hiện tại đã đủ invariant.
- **Không lưu Backtest resource, chỉ trả JobId hai lần**: không có durable resource
  identity để replay, authorization hoặc mở rộng read contract.
- **Tạo nhiều bước transaction rời ở apps/api**: có thể để lại Experiment/Candidate
  không có Job hoặc receipt không khớp khi process lỗi.

## Consequences

### Positive

- Public `BacktestId` có nghĩa và lifecycle riêng, không giả mạo Candidate identity.
- Ownership, provenance, worker execution và Result lineage tái sử dụng invariant đã
  được kiểm chứng của F-005/F-006.
- Command acceptance và idempotency có một transaction boundary duy nhất.
- Queue contract và worker recovery không cần breaking change.

### Negative

- Cần thêm một bảng mapping và published F-005 acceptance port.
- Một Backtest đơn lẻ vẫn có Experiment/Candidate backing graph nội bộ.
- ADR phải được chuyển sang `Accepted` trước khi implementation phụ thuộc được merge.

## Affected Components

- `modules/experiment`
- `modules/backtesting`
- `modules/persistence`
- `apps/api`
- `supabase/migrations`
- F-009 specification, plan, data model và tasks

## Validation

- Replay cùng request 100 lần tạo đúng một Backtest, Experiment, Candidate, Job và
  Outbox event; đổi payload cùng key trả conflict.
- Transaction failure không để lại partial graph hoặc receipt mồ côi.
- Cross-owner lookup/cancel không tìm thấy resource.
- Worker chạy Job bằng queue contract hiện tại và resolve đúng frozen Manifest.
- Static migration test kiểm tra owner schema, constraints và browser grants.
- ArchUnit xác nhận `apps/api` không import implementation/persistence nội bộ.

## Risks and Mitigations

- **Risk**: Single-run Experiment bị hiển thị như Search Experiment. — **Mitigation**:
  public read dùng `BacktestId`; bảng mapping phân biệt workload và UI không suy loại
  từ Candidate.
- **Risk**: Graph mapping trỏ chéo Experiment/Candidate/Job. — **Mitigation**: composite
  foreign keys và unique constraints được enforce trong forward migration.
- **Risk**: API validation và worker parser lệch nhau. — **Mitigation**: dùng cùng
  published F-006 configuration parser và contract fixture.

## References

- [F-009 specification](../../specs/009-public-api-realtime/spec.md)
- [F-009 plan](../../specs/009-public-api-realtime/plan.md)
- [ADR-0006: Queue và Worker](0006-queue-worker-backtesting.md)
- [ADR-0009: Reproducible Experiments](0009-reproducible-experiments.md)
- [ADR-0011: Supabase Auth và ownership](0011-supabase-auth-user-ownership.md)
- [ADR-0012: User Strategy và durable Job](0012-user-strategy-job-ownership.md)

## Supersession

- Supersedes: None
- Superseded by: None

# ADR-0012: CQRS-style Read Model nhưng không Event Sourcing trong MVP

**Status**: Proposed
**Date**: 2026-08-12
**Owners**: Tiến Luật

## Context

Leaderboard cần đọc Top-K nhanh và phát revision realtime, trong khi nguồn dữ liệu gốc là immutable Evaluation Results. Event-driven pipeline đã có CandidateEvaluated events, nhưng dùng full Event Sourcing sẽ yêu cầu event store, replay/upcaster và schema lifecycle vượt nhu cầu của nhóm.

## Drivers and Quality Scenarios

- [QA-05 Scale Backtest Workers](../architecture/quality-attributes.md#qa-05--scale-backtest-workers)
- [QA-07 Reproduce Experiment](../architecture/quality-attributes.md#qa-07--reproduce-experiment)
- [QA-08 Observe Running Experiment](../architecture/quality-attributes.md#qa-08--observe-running-experiment)

## Decision

- Command/write model lưu Experiment, Candidate, Result và Evaluation trong PostgreSQL qua owner ports.
- Leaderboard là **CQRS-style read model** được project từ Evaluation Results/CandidateEvaluated events, có revision và cache Redis tùy chọn.
- Read model có thể rebuild từ PostgreSQL source of truth; cache không quyết định business truth.
- MVP **không dùng Event Sourcing**: events phục vụ integration/notification, không phải nguồn duy nhất để dựng aggregate state.
- Transactional Outbox và idempotent consumers bảo vệ consistency giữa PostgreSQL và Redis.

## Alternatives Considered

- **CRUD query trực tiếp mọi lần**: đơn giản nhưng khó tối ưu Top-K/realtime independent read shape.
- **Full Event Sourcing**: audit/replay mạnh nhưng tăng event schema, upcasting, projection recovery và vận hành.
- **Redis-only Leaderboard**: nhanh nhưng mất provenance/audit khi cache mất.

## Consequences

### Positive

- Leaderboard query shape và cache thay đổi độc lập với write model.
- Top-K rebuild được và vẫn truy tới exact Evaluation/Experiment provenance.
- Tránh complexity của Event Sourcing trong MVP.

### Negative

- Read model eventual consistent và cần revision/idempotency.
- Phải duy trì projection/rebuild path cùng Outbox.

## Affected Components

- `modules/leaderboard`, `modules/evaluation`, `modules/persistence`
- Redis Streams/cache, PostgreSQL read model và WebSocket updates

## Validation Plan

- Xóa Redis/read cache và rebuild cùng Top-K/revision từ Evaluation Results.
- Delivery trùng không tạo entry hoặc tăng revision sai.
- Click Top-K truy được EvaluationResult, Candidate và immutable Experiment Manifest.

## Evidence

**Status**: Planned — chưa thu thập do chưa có implementation.

- AP-05, AP-07 và AP-08 trong [Architecture Evidence](../architecture/architecture-evidence.md).

## Risks and Mitigations

- **Risk**: Projection lag/stale UI — **Mitigation**: expose revision/lag và phát event sau durable update.
- **Risk**: Event Sourcing xuất hiện ngầm — **Mitigation**: PostgreSQL state/results luôn là source of truth và có recovery tests.

## References

- [Đề bài Crypto Strategy Lab — §21–24, §34–36 và §40](../Crypto%20Strategy%20Lab%20%E2%80%93%20%C4%90%E1%BB%93%20%C3%A1n%20cu%E1%BB%91i%20k%E1%BB%B3.pdf)
- [Slide kiến trúc — CQRS/Event Sourcing trade-off](../KienTrucDoAn_slide.pdf)
- [ADR-0006](0006-queue-worker-backtesting.md), [ADR-0007](0007-postgresql-redis-ownership.md)

## Supersession

- Supersedes: None
- Superseded by: None

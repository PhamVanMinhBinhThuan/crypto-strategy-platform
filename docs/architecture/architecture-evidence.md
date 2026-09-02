# Architecture Requirements and Evidence

**Status**: Planned — Chưa có implementation evidence

**Last Updated**: 2026-09-02

**Owner**: Văn Minh

Tài liệu này nối yêu cầu với view, quyết định và cách kiểm chứng. `Planned` nghĩa là test/demo đã được định nghĩa nhưng chưa có source code, log hoặc benchmark thật. Chỉ chuyển sang `Verified` khi evidence có thể xem lại được và gắn với commit/tag.

## Traceability Matrix

| Requirement/driver | Source | Architecture view | ADR | QA | Planned proof | Target | Evidence status |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Thêm Strategy mới với ảnh hưởng tối thiểu | Đề §11–§12, §32.1, §41; Slide Architecture Proof #1 | [Module View](module-view.md) | [0002](../adr/0002-module-boundaries.md), [0005](../adr/0005-strategy-plugin-registry.md) | QA-01 | Thêm MACD + ArchUnit/contract test | Không sửa Backtester/Evaluator/Leaderboard/UI | Planned — chưa có implementation |
| Thay Search Algorithm | Đề §16–§18, §32.6, §42; Slide Proof #2 | [Module View](module-view.md), [Search Flow](data-flows.md) | [0010](../adr/0010-strategy-generator-contract.md) | QA-02 | Thêm Fixture/Domain-guided Generator | Downstream contract không đổi | Planned — chưa có implementation |
| Thay Market Data Provider | Đề §4, §32.1, §40; Slide ATAM Scenario | [Context](system-context.md), [Market Flows](data-flows.md) | [0003](../adr/0003-market-data-adapter.md) | QA-03 | Chạy adapter contract suite | Frontend/public contract không đổi | Planned — chưa có implementation |
| Realtime và Binance recovery | Đề §4–§5, §32.3–§32.4; Slide Proof #3 | [Realtime Flow](data-flows.md) | [0003](../adr/0003-market-data-adapter.md), [0004](../adr/0004-websocket-realtime.md) | QA-04, QA-09 | Disconnect/gap test + four-chart latency | ≤30s recovery, no gap/duplicate; p95 ≤1s | Planned — chưa có implementation |
| Scale Search/Backtest | Đề §23–§24, §32.2, §32.5, §43; Slide Proof #3 | [Container](container-view.md), [Search Flow](data-flows.md), [Deployment](deployment-view.md) | [0006](../adr/0006-queue-worker-backtesting.md) | QA-05, QA-10 | 1-vs-3 Worker benchmark + API timing | ≥2× throughput; Start Search ≤2s | Planned — chưa có implementation |
| Stop, retry, duplicate và ordering | Đề §23–§24, §34, §40; Slide checklist | [Data Flows](data-flows.md), [Data Model](data-model-overview.md) | [0004](../adr/0004-websocket-realtime.md), [0006](../adr/0006-queue-worker-backtesting.md) | QA-04, QA-08, QA-10 | Stop/retry/reclaim/duplicate tests | No duplicate business effect; bounded loop | Planned — chưa có implementation |
| News/Sentiment failure isolation | Đề §27–§30, §40; Slide Proof #3 | [Container](container-view.md), [News Flow](data-flows.md) | [0008](../adr/0008-sentiment-service-boundary.md) | QA-06 | Kill/timeout Sentiment Service | Chart/technical Backtest continue; degraded ≤5s | Planned — chưa có implementation |
| Reproducible Experiment/provenance | Đề §35–§36, §40; Slide ATAM Scenario E | [Data Model](data-model-overview.md) | [0007](../adr/0007-postgresql-redis-ownership.md), [0009](../adr/0009-reproducible-experiments.md) | QA-07 | Reproduction comparison | Same Trade sequence, four metrics, fingerprint | Planned — chưa có implementation |
| Observability | Đề §32.7; Slide rubric | [Search Flow](data-flows.md), [Deployment](deployment-view.md) | [0006](../adr/0006-queue-worker-backtesting.md) | QA-08 | Progress/event/log trace | Visible ≤5s và correlation end-to-end | Planned — chưa có implementation |
| Architecture documentation | Đề §45; Slide C4/Dynamic/rubric | Toàn bộ `docs/architecture` | [ADR index](../adr/README.md) | QA-01–QA-10 | Link/name/diagram consistency review | Context/Container/Module/Dynamic views nhất quán | Planned — document review complete, runtime proof pending |

## Architecture Proof Packages

### Proof A — Extensibility

- Thêm `MACDStrategy` và plugin descriptor/schema.
- Chạy Strategy contract test và ArchUnit.
- Ghi diff theo module; thất bại nếu Backtester/Evaluator/Leaderboard/UI business logic phải đổi.

### Proof B — Replaceability

- Thêm Fixture hoặc Domain-guided Generator qua Generator Registry.
- Dùng cùng Candidate contract và pipeline.
- Chạy downstream regression; thất bại nếu queue/Backtest/Evaluation/Ranking contract phải đổi.

### Proof C — Scale and Failure

- Benchmark cùng workload với 1 và 3 Worker; ghi throughput, queue lag, DB pool và duplicate count.
- Tắt Sentiment Service; đo degraded signal và xác nhận Market/Backtest còn hoạt động.
- Ngắt Binance stream; đo reconnect, backfill, gap và duplicate.

### Proof D — Provenance

- Mở Top-K entry và export exact Experiment Manifest.
- Reproduce bằng frozen Dataset/Strategy/version/assumptions.
- So sánh fingerprint, Trade sequence và bốn metrics; giữ mismatch report nếu khác.

## ATAM-lite Risks and Trade-offs

| Decision | Sensitivity/trade-off point | Failure scenario | Evidence cần có |
| --- | --- | --- | --- |
| F-006 deterministic Backtest/Evaluation/Leaderboard | Frozen execution graph, batch size, assumptions, metric/ranking version và tie-break | Look-ahead, caller-injected provenance, liên kết chéo Experiment, kết quả không tái tạo được | Verified — End-to-end integration test (T075), DB unique/foreign key constraints, and ReproduceExperimentExecutionService ensure strict lineage and mismatch detection. |
| Modular Monolith | Boundary phụ thuộc build discipline | God Service/cross-module table access | ArchUnit + dependency review |
| Plugin/Registry | Contract/schema/version phải ổn định | Strategy mới làm downstream đổi | MACD change proof |
| Redis Queue/Worker | At-least-once và eventual consistency | Worker crash/duplicate/pending | Reclaim + idempotency test |
| Transactional Outbox | Publisher/cleanup tăng complexity | DB commit thành công, Redis lỗi | Redis outage/recovery test |
| Leaderboard projection | Read nhanh nhưng có thể stale | Revision cũ đến muộn/cache mất | Revision ordering + rebuild test |
| Sentiment boundary | Network/model startup/failure | Model down kéo thread/News chậm | Timeout/circuit-breaker test |
| Provider Adapter | Canonical model có thể mất field riêng | Binance schema/rate-limit/disconnect | Contract + recovery test |
| Không full CQRS/Event Sourcing | Ít complexity nhưng không generic replay | Cần temporal audit ngoài immutable records | Review driver trước ADR mới |

## Evidence Governance

- Evidence phải ghi commit/tag, môi trường, Dataset/config, thời điểm và lệnh/test đã chạy.
- Benchmark phải dùng cùng workload và công bố warm-up/concurrency/Worker count.
- Không dùng số minh họa trong PDF/slide làm kết quả của nhóm.
- Screenshot một mình không đủ cho performance/reliability; kèm log, metric hoặc assertion.
- Runtime evidence thuộc demo/test artifact; tài liệu này chỉ ghi link và kết luận ngắn sau khi có thật.

## F-009 Public API và Realtime evidence (2026-09-02)

- **Verified bằng automated tests**: public authentication/error/ownership/redaction,
  standalone Backtest atomic acceptance, immutable REST reads, deterministic pagination,
  realtime protocol/subscription/backpressure/snapshot recovery, workload stream mapping và
  News degraded isolation.
- **Contract evidence**: `docs/api/openapi.yaml`, `error-catalog.md`, `examples.md` và
  `websocket-events.md` được khóa bằng `DocumentationParityTest`.
- **Architecture evidence**: full architecture suite kiểm tra dependency matrix, internal
  package access, cycle, framework purity và canonical UUID/typed-ULID/exact-decimal/UTC boundary.
- **Chưa Verified runtime**: PostgreSQL/Supabase và Redis integration chưa chạy vì môi trường
  hiện tại không có database/Redis variables. Không dùng unit performance smoke để claim latency
  production hoặc multi-worker throughput.
- **Dependency gate**: Experiment start/reproduce trả stable `503 DEPENDENCY_UNAVAILABLE` cho
  đến khi Search Coordinator cung cấp published runtime boundary; read/stop và Job cancel vẫn ready.

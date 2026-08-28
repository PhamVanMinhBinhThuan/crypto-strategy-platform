# Implementation Roadmap

## Mục tiêu

Roadmap này chuyển các quyết định trong Architecture, ADR và database baseline thành
thứ tự triển khai cho nhóm bốn người. Đây là tài liệu điều phối; mỗi feature vẫn phải
đi qua Spec Kit trước khi code.

## Trạng thái hiện tại

- Architecture và ADR nền tảng: đã có.
- Database baseline: đã specification, planning, migration, verification và apply trên
  Supabase shared development.
- Java, Python và Web application: chưa khởi tạo.
- Redis/queue, Binance ingestion, Strategy, Backtest và Sentiment runtime: chưa implement.

## Phân công ownership chính

| Thành viên | Ownership chính | Vai trò tích hợp |
|---|---|---|
| **Luật** | Java Foundation, `apps/api`, authentication, public API và realtime | Tech lead, giữ build/module boundary và tích hợp các feature |
| **Nghi Văn** | Market Data, Dataset, Binance adapter và Java News collection | Cung cấp market/news fixture và provider adapter cho luồng E2E |
| **Văn Minh** | Strategy Registry, Strategies, Composite, Backtest và contract Job–Execution Attempt | Bảo đảm Strategy/Backtest deterministic; định nghĩa rõ lifecycle của một Job và các lần Worker thực thi |
| **Tiến** | Experiment persistence, ownership, Evaluation, Leaderboard và Worker reliability | Giữ transaction invariant, Outbox, idempotency và recovery; tích hợp Job–Attempt contract vào persistence/Worker |

Ownership chính không có nghĩa chỉ một người được review. Pull request của mỗi capability
cần ít nhất một người không phải owner review contract và dependency direction.

## Quy trình áp dụng cho mỗi feature

```text
specify → clarify → plan → tasks → analyze → implement → verify → review/merge
```

Không tạo một specification khổng lồ cho toàn hệ thống. Roadmap giữ bức tranh tổng thể;
mỗi feature bên dưới có specification và acceptance criteria riêng.

## Thứ tự feature

### F-002 — Java Backend Foundation

**Mục tiêu**: Tạo build và module boundary dùng chung để bốn người có thể làm song song.

**Phạm vi**:

- Java 21, Spring Boot 3 và một Gradle multi-module build.
- Khung `apps/api`, `apps/worker` và các `modules/*` đã được Architecture công bố.
- Convention cho ULID, exact decimal, UTC timestamp và error handling.
- Supabase PostgreSQL configuration không chứa secret.
- Supabase JWT authentication boundary cho `apps/api`.
- ArchUnit test bảo vệ dependency direction theo ADR-0001/0002.
- Health/readiness tối thiểu và test command dùng chung.

**Điều kiện hoàn thành**: Build/test toàn bộ Java module bằng một command; API khởi động,
validate JWT ở boundary và architecture test phát hiện dependency bị cấm.

**Phụ trách chính**: **Luật**. Nghi Văn, Văn Minh và Tiến review module API/port mà phần
việc của mình sẽ sử dụng.

### F-003 — Market Data and Dataset

**Phụ thuộc**: F-002.

**Phạm vi**:

- Domain type Asset, Trading Pair, Candle và Timeframe.
- Market Data provider port và Binance adapter.
- Chuẩn hóa provider response; chỉ nhận closed Candle.
- Candle/Dataset persistence port và PostgreSQL adapter.
- Dataset membership, checksum và deduplication tests.

**Phụ trách chính**: **Nghi Văn**.

### F-004 — Strategy Registry and Strategies

**Phụ thuộc**: F-002; có thể làm song song F-003.

**Phạm vi**:

- Strategy, StrategyContext, StrategyDecision và descriptor contract.
- Strategy Registry theo ADR-0005.
- Một Strategy mẫu deterministic và Composite Strategy contract.
- Strategy snapshot persistence port.
- Unit/architecture tests xác nhận Strategy không gọi Spring, database hoặc network.

**Phụ trách chính**: **Văn Minh**.

### F-005 — Experiment Persistence and Ownership

**Phụ thuộc**: F-002; contract tích hợp cuối cần F-003 và F-004.

**Phạm vi**:

- Experiment, Manifest, Candidate, Job, Execution Attempt và lifecycle application service.
- Ownership authorization theo Supabase user identity.
- Persistence adapter cho Experiment graph.
- Transaction-enforced invariant đã hoãn từ database baseline.
- Idempotency record và durable Outbox write.

**Cách chia**: **Văn Minh** định nghĩa contract và state machine
`Candidate → Job → Execution Attempt`, bao gồm retry/cancel và ranh giới giữa trạng thái
Job với trạng thái từng Attempt. **Tiến** phụ trách Experiment graph, ownership,
persistence adapter, transaction invariant, Idempotency và Outbox; đồng thời tích hợp
contract do Văn Minh định nghĩa. Tiến vẫn là owner tích hợp chính của F-005.

### F-006 — Backtest, Evaluation and Leaderboard

**Phụ thuộc**: F-003, F-004 và F-005.

**Phạm vi**:

- Backtest engine và assumptions có version.
- Trade/Backtest Result persistence.
- Evaluation metric version và deterministic scoring.
- Leaderboard revision/Top-K projection.
- Reproduction test từ frozen Dataset, Strategy và Manifest.

**Cách chia**: **Văn Minh** phụ trách Backtest/Strategy integration; **Tiến** phụ trách
Evaluation/Leaderboard persistence; **Nghi Văn** chuẩn bị Dataset fixture; **Luật** tích
hợp API.

### F-007 — Worker and Reliable Job Processing

**Phụ thuộc**: F-005 và F-006.

**Phạm vi**:

- Redis Streams contract có version.
- `apps/worker` orchestration.
- Bounded retry, processed-message deduplication và Outbox publisher.
- Recovery test khi mất transient queue/cache.
- Job progress event cho API/WebSocket boundary.

**Phụ trách chính**: **Tiến**; **Luật** phụ trách API/Worker composition; **Văn Minh**
review việc Worker hiện thực đúng Job–Execution Attempt contract đã chốt ở F-005.

### F-008 — News and Sentiment

**Phụ thuộc**: F-002; có thể bắt đầu song song sau khi Java foundation ổn định.

**Phạm vi**:

- Java News provider port, collection, normalization và deduplication.
- Python/FastAPI Sentiment Service theo ADR-0008.
- Versioned Java–Python request/response contract và shared fixtures.
- Sentiment Result persistence theo content hash/model version.
- Timeout, retry, degraded-state và retention metadata.

**Phụ trách chính**: **Nghi Văn** phụ trách News Java; **Văn Minh** phụ trách Python
Sentiment Service sau khi Strategy foundation ổn định. Tiến review persistence contract
và Luật review integration/security boundary.

### F-009 — Public API and Realtime

**Phụ thuộc**: F-003 đến F-008 theo endpoint tương ứng.

**Phạm vi**:

- Implement OpenAPI endpoints trong `apps/api`.
- Ownership/authorization test cho Experiment endpoint.
- Error catalog và idempotency behavior.
- WebSocket snapshot/event sequencing theo ADR-0004.
- Contract tests chống lệch OpenAPI/WebSocket docs.

**Phụ trách chính**: **Luật**; Nghi Văn, Văn Minh và Tiến cung cấp application service
của capability mình sở hữu.

### F-010 — Web MVP

**Phụ thuộc**: Các API cần cho demo trong F-009.

**Phạm vi**:

- Supabase login/session ở browser.
- Market, Experiment progress, Result, Leaderboard, News/Sentiment screens.
- API/WebSocket client; browser không truy cập business table trực tiếp.
- Loading/error/degraded state và demo flow.

**Cách chia**: **Luật** giữ API client/auth integration; **Nghi Văn** làm Market và News;
**Văn Minh** làm Strategy/Backtest Result; **Tiến** làm Experiment progress và
Leaderboard. Chỉ bắt đầu màn hình khi API contract tương ứng ổn định.

### F-011 — End-to-End Demo and Hardening

**Phụ thuộc**: F-003 đến F-010.

**Phạm vi**:

- Luồng ingest → dataset → experiment → worker → result → leaderboard.
- News/Sentiment failure isolation.
- Reproduction và queue/cache recovery evidence.
- Security/secret scan, performance smoke test và demo fallback.
- Đồng bộ README, Architecture Evidence và demo documents.

**Phụ trách**: Cả bốn thành viên; Luật điều phối luồng tích hợp, mỗi người chịu trách
nhiệm bằng chứng cho capability mình sở hữu.

## Kế hoạch làm song song

```text
F-002 Java Foundation
        │
        ├── F-003 Market/Data ──────────────┐
        ├── F-004 Strategy ─────────────────┤
        ├── F-005 Experiment/Persistence ───┼── F-006 Backtest/Evaluation
        └── F-008 News/Sentiment ───────────┘             │
                                                          ├── F-007 Worker
                                                          ├── F-009 API/Realtime
                                                          └── F-010 Web
                                                                  │
                                                                  └── F-011 E2E
```

## Việc cần làm ngay

1. Cả nhóm review roadmap và xác nhận phạm vi F-002.
2. Tạo Spec Kit feature `002-java-backend-foundation`.
3. Chạy `speckit-specify`, `speckit-clarify`, `speckit-plan`, `speckit-tasks` và
   `speckit-analyze` cho F-002.
4. Luật implement F-002; Nghi Văn, Văn Minh và Tiến review public boundary. Nghi Văn
   chuẩn bị description F-003; Văn Minh chuẩn bị F-004 và phần Job–Execution Attempt
   của F-005; Tiến chuẩn bị các phần Experiment persistence, ownership, Idempotency và
   Outbox còn lại của F-005.
5. Khi F-002 merge vào `main`, tạo ba feature branch từ cùng commit để bắt đầu làm
   song song.

## Quy tắc Git cho nhóm

- Mỗi Spec Kit feature có một feature branch và một owner chính.
- Không tạo bốn nhánh cùng sửa root Gradle/build structure trước khi F-002 merge.
- Capability owner định nghĩa port; persistence adapter implement port nhưng không làm
  capability import ngược adapter.
- Rebase/merge `main` trước integration review; không sửa migration đã apply.
- Pull request phải ghi ADR, module owner, contract, migration và test evidence bị ảnh hưởng.

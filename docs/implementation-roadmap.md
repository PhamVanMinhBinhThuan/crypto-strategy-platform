# Implementation Roadmap

## Mục tiêu

Roadmap này chuyển các quyết định trong Architecture, ADR và database baseline thành
thứ tự triển khai cho nhóm bốn người. Đây là tài liệu điều phối; mỗi feature vẫn phải
đi qua Spec Kit trước khi code.

## Trạng thái hiện tại

- F-003 đến F-013 đã cung cấp implementation nền cho Market/Dataset, Strategy, Experiment,
  Backtest/Evaluation/Leaderboard, Worker/Redis, News/Sentiment, public API/realtime và Web UI.
- F-014 đang hardening trên nhánh `feature/014-end-to-end-demo-hardening`: T001–T028, T030–T036,
  T038–T062 đã hoàn tất;
  automated Java/Web/Python gates không có failure, benchmark/secret scan/keyboard-responsive đạt.
- Controlled browser journeys có evidence nhưng không được dùng thay LIVE. PostgreSQL đã được migrate
  và reproduction dependency-backed đã pass; LIVE còn bị chặn bởi browser auth configuration/session
  và chưa có authenticated Sentiment isolation timeline; chi tiết trong `docs/demo/f014/release-checklist.md`.
- Candidate `0761a54b` đã qua clean-checkout quickstart và standalone performance target 2×; PostgreSQL
  reproduction được xác nhận ở `0d87e16b`. Chưa phải LIVE release vì T029/T037 còn operator/auth evidence.

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

### Database gate — User Strategy and Durable Job

Đây là schema increment dùng chung cho F-004 và F-005, không phải một business feature
độc lập. Nhánh `db-setup-v2` được giữ để review và sửa trước khi merge.

**Phạm vi đề xuất**:

- Tách Strategy plugin catalog dùng chung khỏi Strategy configuration thuộc từng user.
- Version hóa User Strategy và giữ provenance trong Experiment Manifest.
- Tạo durable Job phía trên Execution Attempt và backfill Attempt hiện có.
- Giữ Supabase Auth là nơi duy nhất quản lý password/session.

**Merge/apply gate**:

- **[I5]** Giữ nguyên `specs/002-java-backend-foundation` là feature F-002 chính thức.
- `specs/002-user-strategy-jobs` trên nhánh DB v2 chỉ là thư mục staging; trước khi
  merge phải chuyển đầy đủ requirement, plan, task, contract và test traceability sang
  `specs/004-user-strategy-library` và `specs/005-durable-job-persistence`.
- Chỉ xóa thư mục staging sau khi review xác nhận hai spec mới không mất requirement,
  acceptance scenario hoặc verification contract; đồng bộ `.specify/feature.json` theo
  feature đang thao tác và không để hai feature cùng số `002` trong `main`.
- **[G1]** ADR-0012 và các ADR nền liên quan phải được review/`Accepted` theo Constitution.
- **[I1]** Quyết định rõ database trigger hay application transaction; không âm thầm mâu thuẫn
  database baseline đã chốt.
- **[I2]** Đồng bộ Job status giữa database, ADR và OpenAPI (`SUCCEEDED`/`COMPLETED`).
- **[I3]** Chốt Search Job có Execution Attempt hay Attempt chỉ thuộc Backtest Job.
- **[U1]** Không tuyên bố DB v2 đã hỗ trợ Rule DSL/prompt/URL; phần này thuộc
  specification và ADR riêng của F-004.
- **[C1]** Tách yêu cầu Java authorization/Redis recovery chưa implement khỏi database evidence.
- **[I4]** Giữ baseline SQL test chạy độc lập; test v2 không được làm baseline suite phụ
  thuộc sự tồn tại của bảng Job.
- `supabase db push --dry-run`, lint và SQL verification phải PASS trước khi xin phép
  apply migration lên shared development.

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

**Phụ thuộc**: F-002; phần User Strategy persistence phụ thuộc Database gate; có thể làm
song song F-003.

**Phạm vi**:

- Strategy, StrategyContext, StrategyDecision và descriptor contract.
- Strategy Registry theo ADR-0005.
- Một Strategy mẫu deterministic và Composite Strategy contract.
- Strategy Library riêng theo user, version bất biến và ownership boundary.
- Rule DSL có version cho Strategy tạo từ form/prompt/URL; phải có specification và ADR
  riêng trước implementation, không cho user upload/thực thi code tùy ý.
- Strategy snapshot persistence port.
- Unit/architecture tests xác nhận Strategy không gọi Spring, database hoặc network.

**Phụ trách chính**: **Văn Minh**.

### F-005 — Experiment Persistence and Ownership

**Phụ thuộc**: F-002 và Database gate; contract tích hợp cuối cần F-003 và F-004.

**Phạm vi**:

- Experiment, Manifest, Candidate, Job, Execution Attempt và lifecycle application service.
- Durable Job là trạng thái tổng thể; Execution Attempt là lịch sử từng lần Worker thử.
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

### F-010 — Search Coordinator

**Phụ thuộc**: F-004, F-005, F-006 và F-007; mở khóa các command Start/Stop/Reproduce
còn bị readiness gate trong F-009.

**Phạm vi**:

- Search Generator contract/registry và Random Search deterministic theo seed.
- Validate search space, generator version, stop conditions và giới hạn Candidate đang chạy.
- Điều phối Search Run từ frozen Dataset, Strategy version và Experiment Manifest.
- Tạo Candidate/Backtest Job theo durable state; chống trùng bằng idempotency và fingerprint.
- Dừng, phục hồi và tiếp tục Search an toàn sau restart hoặc duplicate/out-of-order delivery.
- Điều phối Reproduce Experiment bất đồng bộ và lưu kết quả `MATCHED`, `MISMATCHED` hoặc `FAILED`.
- Phát progress/lifecycle contract để F-009 cung cấp REST/WebSocket cho Frontend.
- Chứng minh có thể thay Search Generator mà không sửa Backtest, Evaluation, Leaderboard hoặc
  public workflow.

**Ngoài phạm vi**: Không triển khai UI, public transport mới, giao dịch tiền thật, Composite
Search, Bayesian/adaptive search hoặc distributed multi-leader Coordinator.

**Cách chia**: **Văn Minh** phụ trách Generator contract và tính deterministic; **Tiến** phụ trách
durable Search state, transaction/idempotency và reproduction coordination; **Luật** tích hợp
readiness gate của F-009; **Nghi Văn** cung cấp Dataset fixture cho kiểm thử E2E.

### F-011 — Web Foundation and Authentication

**Phụ thuộc**: Public contract và authentication boundary của F-009.

**Phạm vi**:

- Khởi tạo Next.js App Router với TypeScript và cấu trúc source/test dùng chung.
- Layout, navigation, design tokens và các component nền tảng.
- Supabase login/session ở browser.
- Protected routes và truyền access token tới Backend API.
- API/WebSocket client dùng chung; browser không truy cập business table trực tiếp.
- Component/loading pattern dùng chung cho loading, empty, error và degraded state.
- Mock adapter/fixture để F-012 và F-013 phát triển độc lập khi endpoint tương ứng chưa sẵn sàng.

**Phụ trách chính**: **Luật**. F-011 phải hoàn thành foundation tối thiểu và công bố interface
API/WebSocket client trước khi F-012 và F-013 bắt đầu phát triển song song.

### F-012 — Market, Strategy and News UI

**Phụ thuộc**: F-011 và các Market/Strategy/News API tương ứng trong F-009.

**Phạm vi**:

- Market Dashboard và biểu đồ Candle historical/realtime.
- Chọn pair, timeframe và quản lý trạng thái kết nối Market Data.
- Strategy catalog, system/private Strategy và thông tin version/parameters.
- News và Sentiment screens, bao gồm degraded state khi Sentiment Service lỗi.
- Tích hợp REST/WebSocket qua client của F-011; không gọi trực tiếp Binance, Supabase business
  table hoặc Sentiment Service nội bộ.

**Phụ trách chính**: **Nghi Văn**; **Văn Minh** review Strategy contract. Có thể làm song song
F-013 sau khi F-011 công bố foundation và client interface ổn định.

### F-013 — Experiment, Result and Leaderboard UI

**Phụ thuộc**: F-011, public Experiment/Result/Leaderboard API trong F-009 và Search Coordinator
F-010 cho Start/Stop/Reproduce cùng Search progress.

**Phạm vi**:

- Form tạo, dừng và reproduce Experiment.
- Theo dõi Experiment, Job và Search progress qua REST/WebSocket.
- Hiển thị Backtest Result, danh sách Trade và bốn Evaluation metrics.
- Hiển thị Leaderboard Top-K và xử lý realtime revision/update.
- Loading, validation, conflict, authorization, failure và retryable/degraded state phù hợp.
- Chỉ dùng public contract; không tự điều phối Search hoặc tính lại Backtest/Ranking ở Frontend.

**Phụ trách chính**: **Tiến**; **Văn Minh** review Backtest/Evaluation contract và **Luật** review
API/realtime integration. Có thể làm song song F-012 sau khi F-011 hoàn thành foundation tối thiểu.

### F-014 — End-to-End Demo and Hardening

**Phụ thuộc**: F-003 đến F-013; đặc biệt yêu cầu F-012 và F-013 đã tích hợp trên cùng foundation
F-011.

**Phạm vi**:

- Luồng ingest → dataset → experiment → worker → result → leaderboard.
- News/Sentiment failure isolation.
- Reproduction và queue/cache recovery evidence.
- Security/secret scan, performance smoke test và demo fallback.
- Đồng bộ README, Architecture Evidence và demo documents.

**Phụ trách**: Cả bốn thành viên; Luật điều phối luồng tích hợp, mỗi người chịu trách
nhiệm bằng chứng cho capability mình sở hữu.

**Trạng thái ngày 2026-09-04**: implementation/hardening, demo package và clean candidate verification
đến T062 đã hoàn tất, ngoại trừ hai task evidence LIVE T029/T037.
Quality gate hiện `PARTIAL` vì full Gradle command công bố hai dependency-backed Redis skip; hai
scenario đó đã pass riêng với Redis thật. Full LIVE journey và video/Drive evidence vẫn `BLOCKED`
theo dependency gate, không bị thay bằng fixture.

**Điểm vào review**:

- [F014 specification](../specs/014-end-to-end-demo-hardening/spec.md)
- [Implementation tasks](../specs/014-end-to-end-demo-hardening/tasks.md)
- [Demo runbook](demo/f014/runbook.md)
- [Release checklist](demo/f014/release-checklist.md)
- [Rubric/evidence matrix](evidence/f014/rubric-matrix.md)
- [Quality gates](evidence/f014/quality-gates.md)
- [Performance evidence](evidence/f014/performance.md)
- [Security evidence](evidence/f014/security.md)
- [Accessibility/responsive evidence](evidence/f014/accessibility-responsive.md)

## Kế hoạch làm song song

```text
F-002 Java Foundation
        │
        ├── F-003 Market/Data ─────────────────────────────┐
        ├── DB-v2 review/apply gate ──┬── F-004 Strategy ─┤
        │                             └── F-005 Experiment ┼── F-006 Backtest/Evaluation
        └── F-008 News/Sentiment ─────────────────────────┘             │
                                                                        ├── F-007 Worker ──┐
                                                                        ├── F-009 API/Realtime ─┤
                                                                        └── F-010 Search ───────┤
                                                                                                  └── F-011 Web Foundation/Auth
                                                                                                              │
                                                                                      ┌───────────────────────┴───────────────────────┐
                                                                                      │                                               │
                                                                              F-012 Market/Strategy/News                 F-013 Experiment/Result/Leaderboard
                                                                                      │                                               │
                                                                                      └───────────────────────┬───────────────────────┘
                                                                                                              │
                                                                                                        F-014 E2E
```

## Việc cần làm ngay

1. Database owner reconcile/apply migrations theo quy trình để F006/F010 schema preflight cùng pass;
   không sửa shared table ad-hoc.
2. Demo operator inject browser-safe Supabase configuration, chuẩn bị development session và bật
   external Sentiment runtime với model bundle/token ngoài repository.
3. Chạy LIVE runbook, chụp evidence theo `docs/demo/f014/demo-checklist.md`, thêm video/Drive link và
   timestamp vào đúng rubric row.
4. Cross-owner review public contract, module ownership, ADR và UI reference trước khi merge F-014.
5. Khi trình bày performance, dùng standalone median 2.527× và công bố cả host-contended median 1.836×;
   không diễn giải in-process benchmark thành production/multi-host SLA.

## Quy tắc Git cho nhóm

- Mỗi Spec Kit feature có một feature branch và một owner chính.
- Không tạo bốn nhánh cùng sửa root Gradle/build structure trước khi F-002 merge.
- Capability owner định nghĩa port; persistence adapter implement port nhưng không làm
  capability import ngược adapter.
- Rebase/merge `main` trước integration review; không sửa migration đã apply.
- Pull request phải ghi ADR, module owner, contract, migration và test evidence bị ảnh hưởng.

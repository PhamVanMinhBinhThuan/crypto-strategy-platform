# Tasks: Public API và Realtime Delivery (F-009)

**Input**: Design documents từ `/specs/009-public-api-realtime/`

**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/`, `quickstart.md`

**Organization**: Tasks được nhóm theo user story để mỗi slice có thể kiểm thử và review độc lập.

## Phase 1: Setup (Shared Infrastructure)

**Mục đích**: Chuẩn bị contract, cấu trúc package và dependency tối thiểu cho public boundary.

- [X] T001 Cập nhật endpoint inventory, schema và status mapping trong `docs/api/openapi.yaml` theo `specs/009-public-api-realtime/contracts/rest-api-contract.md`.
- [X] T002 Cập nhật `docs/api/error-catalog.md` và `docs/api/websocket-events.md` cho F-009 operation, ticket, snapshot marker và compatibility rules.
- [X] T003 [P] Thêm dependency/configuration cần cho native WebSocket và JSON transport trong `apps/api/build.gradle.kts` và `apps/api/src/main/resources/application.yml`.
- [X] T004 [P] Tạo package skeleton transport tại `apps/api/src/main/java/com/cryptostrategy/platform/api/market/`, `strategy/`, `experiment/`, `backtest/`, `leaderboard/` và `realtime/`.
- [X] T005 [P] Tạo test fixture/support cho authenticated users, opaque IDs, correlation IDs và fake published application ports tại `apps/api/src/test/java/com/cryptostrategy/platform/api/support/`.

## Phase 2: Foundational (Blocking Prerequisites)

**Mục đích**: Hoàn thiện lớp chung bắt buộc trước mọi user story.

- [X] T006 [P] Viết contract test kiểm tra public error envelope, UTC timestamp, correlation header và không có sensitive fields tại `apps/api/src/test/java/com/cryptostrategy/platform/api/error/PublicErrorContractTest.java`.
- [X] T007 [P] Viết architecture test cấm API import persistence/internal/provider implementation tại `architecture-tests/src/test/java/com/cryptostrategy/platform/architecture/PublicApiBoundaryTest.java`.
- [X] T008 [P] Implement typed `AuthenticatedUserContext` và mapping UUID từ JWT subject tại `apps/api/src/main/java/com/cryptostrategy/platform/api/auth/AuthenticatedUserContext.java`.
- [X] T009 Implement one-time WebSocket ticket model/store/issuer với expiry, origin binding và single-use enforcement tại `apps/api/src/main/java/com/cryptostrategy/platform/api/auth/WebSocketTicketService.java`.
- [X] T010 Implement correlation context/filter/response propagation cho REST và realtime tại `apps/api/src/main/java/com/cryptostrategy/platform/api/observability/CorrelationIdFilter.java` và package `observability/`.
- [X] T011 Implement public `ErrorEnvelope`, safe details và exception-to-status mapper theo error contract tại `apps/api/src/main/java/com/cryptostrategy/platform/api/error/`.
- [X] T012 Implement owner authorization facade nhận authenticated UUID và parent-chain lookup qua published ports tại `apps/api/src/main/java/com/cryptostrategy/platform/api/auth/OwnerAuthorizationService.java`.
- [X] T013 Implement canonical request hashing và idempotency adapter qua F-005 port tại `apps/api/src/main/java/com/cryptostrategy/platform/api/idempotency/IdempotencyService.java`.
- [X] T014 Implement cursor/limit validation và common page mapping tại `apps/api/src/main/java/com/cryptostrategy/platform/api/transport/PageRequestMapper.java` và `PageResponseMapper.java`.
- [X] T015 [P] Viết foundational tests cho ticket single-use/expiry/origin, owner isolation, canonical hash và cursor validation tại `apps/api/src/test/java/com/cryptostrategy/platform/api/foundation/FoundationalBoundaryTest.java`.

**Checkpoint**: Auth, error, correlation, ownership, idempotency và common transport boundary đã testable; chưa có story nào được expose nếu checkpoint fail.

## Phase 3: User Story 1 - Access Platform Capabilities Safely (Priority: P1) 🎯 MVP Foundation

**Mục tiêu**: Mọi request business và realtime đều xác thực, owner-safe và trả lỗi nhất quán.

**Independent Test**: Chạy hai user identities qua shared/private REST và WebSocket handshake; xác nhận 401, inaccessible response, safe error và correlation behavior.

### Tests for User Story 1

- [X] T016 [P] [US1] Viết authentication integration tests cho missing/expired/malformed/wrong issuer-audience JWT tại `apps/api/src/test/java/com/cryptostrategy/platform/api/auth/AuthenticationIntegrationTest.java`.
- [X] T017 [P] [US1] Viết two-user ownership tests cho resource ID trực tiếp và parent-chain ID tại `apps/api/src/test/java/com/cryptostrategy/platform/api/security/OwnershipIsolationIntegrationTest.java`.
- [X] T018 [P] [US1] Viết error redaction tests chứng minh token, secret, SQL, path, stack trace và provider payload không xuất hiện tại `apps/api/src/test/java/com/cryptostrategy/platform/api/security/PublicRedactionIntegrationTest.java`.
- [ ] T019 [P] [US1] Viết WebSocket ticket handshake tests cho single-use, expiry, origin và authentication expiry tại `apps/api/src/test/java/com/cryptostrategy/platform/api/realtime/WebSocketTicketIntegrationTest.java`.

### Implementation for User Story 1

- [X] T020 [US1] Tích hợp `SecurityFilterChain` với authenticated user context và public endpoint policy tại `apps/api/src/main/java/com/cryptostrategy/platform/api/config/SecurityConfiguration.java`.
- [X] T021 [US1] Tạo endpoint cấp one-time WebSocket ticket, không trả access token hoặc đặt token dài hạn trong URL tại `apps/api/src/main/java/com/cryptostrategy/platform/api/auth/WebSocketTicketController.java`.
- [X] T022 [US1] Tích hợp `ApiExceptionHandler` với error catalog, ownership-safe 404 và retry headers tại `apps/api/src/main/java/com/cryptostrategy/platform/api/error/ApiExceptionHandler.java`.
- [X] T023 [US1] Tích hợp correlation ID vào mọi REST response và log context tại `apps/api/src/main/java/com/cryptostrategy/platform/api/observability/CorrelationIdFilter.java`.
- [X] T024 [US1] Tạo security configuration tests cho origin allowlist, internal service-token boundary và browser-to-internal denial tại `apps/api/src/test/java/com/cryptostrategy/platform/api/security/SecurityBoundaryIntegrationTest.java`.

**Checkpoint**: User Story 1 pass; mọi story sau phải reuse cùng auth/error/owner boundary.

## Phase 4: User Story 2 - Discover and Configure Reusable Inputs (Priority: P1)

**Mục tiêu**: Người dùng đọc Candle/Dataset/Strategy và quản lý private Strategy qua public contract.

**Independent Test**: Dùng User A tạo/retrieve/publish private Strategy và Dataset, page catalog/history, rồi xác nhận User B không thấy hoặc sửa được dữ liệu.

### Tests for User Story 2

- [ ] T025 [P] [US2] Viết REST contract tests cho Candle range, Dataset snapshot, exact decimal, UTC, cursor và provider-safe errors tại `apps/api/src/test/java/com/cryptostrategy/platform/api/market/MarketApiContractTest.java`.
- [ ] T026 [P] [US2] Viết Strategy catalog/private-library ownership tests tại `apps/api/src/test/java/com/cryptostrategy/platform/api/strategy/StrategyApiIntegrationTest.java`.
- [ ] T027 [P] [US2] Viết concurrency/conflict tests cho private Strategy publish/next-version tại `apps/api/src/test/java/com/cryptostrategy/platform/api/strategy/StrategyVersionConflictTest.java`.

### Implementation for User Story 2

- [ ] T028 [P] [US2] Tạo Candle và Dataset request/response DTO mapping tại `apps/api/src/main/java/com/cryptostrategy/platform/api/market/MarketDtos.java`.
- [ ] T029 [US2] Implement Candle/Dataset controllers gọi F-003 published ports tại `apps/api/src/main/java/com/cryptostrategy/platform/api/market/MarketController.java` và `DatasetController.java`.
- [ ] T030 [P] [US2] Tạo system/private Strategy DTO mapping giữ exact version/parameters tại `apps/api/src/main/java/com/cryptostrategy/platform/api/strategy/StrategyDtos.java`.
- [ ] T031 [US2] Implement Strategy catalog và private Strategy controllers gọi F-004 application ports tại `apps/api/src/main/java/com/cryptostrategy/platform/api/strategy/StrategyController.java` và `UserStrategyController.java`.
- [ ] T032 [US2] Thêm validation cho cursor, page size, timeframe, Dataset range và Strategy version conflict tại `apps/api/src/main/java/com/cryptostrategy/platform/api/market/` và `strategy/`.
- [ ] T033 [US2] Thêm contract fixtures và OpenAPI examples cho Market/Dataset/Strategy tại `apps/api/src/test/resources/contracts/f009/`.

**Checkpoint**: User Story 2 pass với shared catalog, owner-scoped library và immutable Dataset/Strategy references.

## Phase 5: User Story 3 - Start and Control Durable Work (Priority: P1)

**Mục tiêu**: Start/stop/cancel/reproduce Backtest và Experiment an toàn, idempotent và owner-scoped.

**Independent Test**: Replay cùng command 100 lần, đổi payload cùng key, stop/cancel/reproduce và kiểm tra Job/Experiment state bằng User A/B.

### Tests for User Story 3

- [ ] T034 [P] [US3] Viết idempotency replay/conflict integration tests cho Backtest và Experiment tại `apps/api/src/test/java/com/cryptostrategy/platform/api/experiment/IdempotencyCommandIntegrationTest.java`.
- [ ] T035 [P] [US3] Viết command acceptance tests kiểm tra 202, Location, immutable input freeze và owner validation tại `apps/api/src/test/java/com/cryptostrategy/platform/api/experiment/AsyncCommandApiTest.java`.
- [ ] T036 [P] [US3] Viết stop/cancel/reproduce state-conflict tests tại `apps/api/src/test/java/com/cryptostrategy/platform/api/experiment/ExperimentCommandStateTest.java`.

### Implementation for User Story 3

- [ ] T037 [P] [US3] Tạo Experiment/Backtest/Job command và accepted response DTO tại `apps/api/src/main/java/com/cryptostrategy/platform/api/experiment/CommandDtos.java`.
- [ ] T038 [US3] Implement start Backtest controller gọi F-005/F-006 published use cases tại `apps/api/src/main/java/com/cryptostrategy/platform/api/backtest/BacktestController.java`.
- [ ] T039 [US3] Implement start/stop/reproduce Experiment controller gọi F-005 application ports tại `apps/api/src/main/java/com/cryptostrategy/platform/api/experiment/ExperimentController.java`.
- [ ] T040 [US3] Implement Job read/cancel mapping và terminal failure representation tại `apps/api/src/main/java/com/cryptostrategy/platform/api/experiment/JobController.java`.
- [ ] T041 [US3] Tích hợp idempotency receipt với response replay, conflict mapping và Location header tại `apps/api/src/main/java/com/cryptostrategy/platform/api/idempotency/IdempotencyCommandExecutor.java`.
- [ ] T042 [US3] Tạo integration fixtures cho queued/running/retry/cancelled/failed/completed state tại `apps/api/src/test/resources/fixtures/f009/jobs/`.

**Checkpoint**: User Story 3 pass; mỗi command tạo tối đa một logical outcome và không bypass owner/application boundary.

## Phase 6: User Story 4 - Inspect Results and Current Progress (Priority: P2)

**Mục tiêu**: Cung cấp authoritative snapshots cho Experiment, Candidate, Job, Result và Leaderboard.

**Independent Test**: Đọc toàn bộ resource bằng REST sau khi notification bị tắt; page không lặp/bỏ và không lộ resource foreign owner.

### Tests for User Story 4

- [ ] T043 [P] [US4] Viết Experiment/Candidate/Job read contract và ownership tests tại `apps/api/src/test/java/com/cryptostrategy/platform/api/experiment/ExperimentReadApiTest.java`.
- [ ] T044 [P] [US4] Viết Backtest Result immutable exact-decimal/provenance tests tại `apps/api/src/test/java/com/cryptostrategy/platform/api/backtest/BacktestResultApiTest.java`.
- [ ] T045 [P] [US4] Viết Leaderboard revision/order/cursor tests tại `apps/api/src/test/java/com/cryptostrategy/platform/api/leaderboard/LeaderboardApiTest.java`.

### Implementation for User Story 4

- [ ] T046 [P] [US4] Tạo immutable snapshot DTO cho Experiment/Candidate/Job tại `apps/api/src/main/java/com/cryptostrategy/platform/api/experiment/ReadDtos.java`.
- [ ] T047 [US4] Implement Experiment/Candidate/Job read controllers và cursor mapping tại `apps/api/src/main/java/com/cryptostrategy/platform/api/experiment/`.
- [ ] T048 [P] [US4] Tạo Backtest Result/Trade DTO giữ decimal string, UTC và provenance tại `apps/api/src/main/java/com/cryptostrategy/platform/api/backtest/ResultDtos.java`.
- [ ] T049 [US4] Implement Backtest Result controller gọi F-006 result reader tại `apps/api/src/main/java/com/cryptostrategy/platform/api/backtest/BacktestResultController.java`.
- [ ] T050 [P] [US4] Tạo Leaderboard DTO với ranking policy, revision và deterministic entries tại `apps/api/src/main/java/com/cryptostrategy/platform/api/leaderboard/LeaderboardDtos.java`.
- [ ] T051 [US4] Implement Leaderboard controller gọi F-006/F-006 persistence boundary tại `apps/api/src/main/java/com/cryptostrategy/platform/api/leaderboard/LeaderboardController.java`.

**Checkpoint**: User Story 4 pass khi không cần WebSocket để xác định durable state mới nhất.

## Phase 7: User Story 5 - Follow Realtime Updates and Recover Gaps (Priority: P2)

**Mục tiêu**: Một authenticated connection multiplex subscriptions, ordering, backpressure và recovery an toàn.

**Independent Test**: Duy trì bốn Candle + workload subscriptions, inject duplicate/stale/lost event, reconnect và reconcile bằng REST snapshot.

### Tests for User Story 5

- [ ] T052 [P] [US5] Viết envelope/command/event schema tests cho tất cả event version 1 tại `apps/api/src/test/java/com/cryptostrategy/platform/api/realtime/WebSocketContractTest.java`.
- [ ] T053 [P] [US5] Viết subscription limit, duplicate ID, unsubscribe và owner isolation tests tại `apps/api/src/test/java/com/cryptostrategy/platform/api/realtime/SubscriptionLifecycleTest.java`.
- [ ] T054 [P] [US5] Viết snapshot-marker ordering/reconnect/deduplication tests tại `apps/api/src/test/java/com/cryptostrategy/platform/api/realtime/SnapshotRecoveryTest.java`.
- [ ] T055 [P] [US5] Viết bounded buffer/coalescing/terminal-event retention tests tại `apps/api/src/test/java/com/cryptostrategy/platform/api/realtime/BackpressureTest.java`.

### Implementation for User Story 5

- [ ] T056 [US5] Implement WebSocket endpoint, authenticated handshake và connection lifecycle tại `apps/api/src/main/java/com/cryptostrategy/platform/api/realtime/WebSocketConfiguration.java` và `RealtimeConnection.java`.
- [ ] T057 [US5] Implement typed envelope, command parser, version validation và isolated subscription errors tại `apps/api/src/main/java/com/cryptostrategy/platform/api/realtime/RealtimeMessageMapper.java`.
- [ ] T058 [US5] Implement subscription registry, four-Candle/workload limits, origin/rate/message checks tại `apps/api/src/main/java/com/cryptostrategy/platform/api/realtime/SubscriptionRegistry.java`.
- [ ] T059 [US5] Implement marker-based snapshot coordinator và authorized REST refresh hints tại `apps/api/src/main/java/com/cryptostrategy/platform/api/realtime/SnapshotCoordinator.java`.
- [ ] T060 [US5] Implement F-003 realtime adapter bridge cho Candle events và close-event ordering tại `apps/api/src/main/java/com/cryptostrategy/platform/api/realtime/MarketEventBridge.java`.
- [ ] T061 [US5] Implement F-007 progress/lifecycle consumer bridge cho Experiment, completion và Leaderboard events tại `apps/api/src/main/java/com/cryptostrategy/platform/api/realtime/WorkEventBridge.java`.
- [ ] T062 [US5] Implement bounded outbound queue, coalescing, heartbeat, disconnect và reconnect recovery signals tại `apps/api/src/main/java/com/cryptostrategy/platform/api/realtime/RealtimeDeliveryService.java`.

**Checkpoint**: User Story 5 pass với duplicate-safe delivery, không leak private event và recoverable terminal state.

## Phase 8: User Story 6 - Read News Without Sentiment Coupling (Priority: P3)

**Mục tiêu**: News public read hoạt động độc lập với sentiment availability và internal audit được bảo vệ.

**Independent Test**: List/filter/page News khi sentiment pending/unavailable; xác nhận Market/Strategy/Backtest vẫn usable và browser không gọi audit.

### Tests for User Story 6

- [ ] T063 [P] [US6] Viết News list/filter/pagination/public sentiment summary tests tại `apps/api/src/test/java/com/cryptostrategy/platform/api/news/NewsPublicApiTest.java`.
- [ ] T064 [P] [US6] Viết degraded sentiment isolation tests cho News, Market, Strategy và Backtest tại `apps/api/src/test/java/com/cryptostrategy/platform/api/news/NewsDegradedIsolationTest.java`.
- [ ] T065 [P] [US6] Viết browser-to-internal-audit denial và redaction tests tại `apps/api/src/test/java/com/cryptostrategy/platform/api/news/NewsAuditSecurityTest.java`.

### Implementation for User Story 6

- [ ] T066 [US6] Hoàn thiện public News DTO/filter/cursor mapping theo F-008 contract tại `apps/api/src/main/java/com/cryptostrategy/platform/api/news/NewsResponse.java` và `NewsQueryMapper.java`.
- [ ] T067 [US6] Implement public News controller gọi News query port, không gọi Python service trực tiếp tại `apps/api/src/main/java/com/cryptostrategy/platform/api/news/NewsController.java`.
- [ ] T068 [US6] Tách protected audit mapping khỏi browser route và kiểm tra dedicated credential tại `apps/api/src/main/java/com/cryptostrategy/platform/api/news/NewsAuditController.java`.
- [ ] T069 [US6] Map sentiment unavailable/timeout/invalid response thành degraded News state và stable error khi audit được gọi tại `apps/api/src/main/java/com/cryptostrategy/platform/api/news/NewsExceptionMapper.java`.

**Checkpoint**: User Story 6 pass; News/Sentiment failure không lan sang Market, Strategy hoặc technical Backtest.

## Phase 9: Polish & Cross-Cutting Concerns

**Mục đích**: Đồng bộ evidence, hiệu năng, bảo mật và release readiness.

- [ ] T070 [P] Cập nhật `docs/api/openapi.yaml`, `docs/api/websocket-events.md`, `docs/api/error-catalog.md` và examples để parity 100% với transport DTO.
- [ ] T071 [P] Thêm contract drift test cho REST/error/WebSocket docs tại `apps/api/src/test/java/com/cryptostrategy/platform/api/contract/DocumentationParityTest.java`.
- [ ] T072 [P] Thêm security scan tests bảo đảm không log token, body nhạy cảm, provider payload hoặc internal exception tại `apps/api/src/test/java/com/cryptostrategy/platform/api/security/LoggingRedactionTest.java`.
- [ ] T073 [P] Thêm performance smoke test bounded reads, async acceptance và realtime delivery theo SC-003/SC-004 tại `apps/api/src/test/java/com/cryptostrategy/platform/api/performance/PublicApiPerformanceTest.java`.
- [ ] T074 Chạy database integration tests với Supabase/PostgreSQL và Redis recovery theo `specs/009-public-api-realtime/quickstart.md`, ghi commit/môi trường/evidence.
- [ ] T075 Chạy full `JAVA_HOME=<JDK21> ./gradlew test` và Python contract suite; sửa warning/failure liên quan F-009.
- [ ] T076 Review dependency gates F-003/F-008/Search Coordinator, đánh dấu operation readiness đúng evidence và cập nhật `specs/009-public-api-realtime/quickstart.md`.
- [ ] T077 Cập nhật `docs/architecture/architecture-evidence.md` từ Planned sang Verified chỉ cho quality scenarios có evidence thật.
- [ ] T078 Review toàn bộ scope/security/ADR/contract/migration checklist trước PR tại `specs/009-public-api-realtime/checklists/`.

## Dependencies & Execution Order

### Phase Dependencies

- Phase 1 không phụ thuộc phase khác.
- Phase 2 phụ thuộc Phase 1 và block tất cả user stories.
- US1 phụ thuộc Phase 2; US2 và US3 có thể bắt đầu song song sau US1 foundational tests.
- US4 phụ thuộc các read/application ports của US2 và command/resource identity của US3.
- US5 phụ thuộc US1, US2 và US4 để có auth, subscription resource và authoritative snapshot.
- US6 có thể bắt đầu sau Phase 2; endpoint News hiện có thể phát triển song song US2–US4.
- Phase 9 phụ thuộc các story được chọn để release; integration/evidence không được bỏ qua khi claim MVP.

### User Story Dependencies

- **US1 (P1)**: chỉ phụ thuộc Foundational; là security foundation, không phải business demo hoàn chỉnh.
- **US2 (P1)**: phụ thuộc US1; cung cấp input discovery/configuration.
- **US3 (P1)**: phụ thuộc US1 và application ports F-005/F-006; có thể song song phần lớn với US2 sau auth.
- **US4 (P2)**: phụ thuộc US2/US3 và F-006 persistence readers.
- **US5 (P2)**: phụ thuộc US1/US4, F-003 realtime boundary và F-007 event boundary.
- **US6 (P3)**: phụ thuộc US1 và F-008 News query boundary; độc lập với Search/Backtest execution.

### Parallel Execution Examples

```text
Sau Phase 2:
- T025 Market contract tests + T026 Strategy tests + T027 Strategy conflict tests
- T034 idempotency tests + T035 async command tests + T036 state tests
- T063 News tests + T052 WebSocket contract tests (nếu các file khác nhau)

Trong US4:
- T043 Experiment reads + T044 Result reads + T045 Leaderboard reads

Trong US5:
- T052 protocol + T053 lifecycle + T054 recovery + T055 backpressure
```

## Implementation Strategy

### MVP First

1. Hoàn tất Phase 1–2 và US1 để khóa auth/error/ownership.
2. Hoàn tất US2 read path (Strategy + Dataset/Candle khi F-003 đã ready).
3. Hoàn tất US3 start/get Job tối thiểu với idempotency.
4. Dừng tại checkpoint, chạy quickstart và database evidence trước khi mở rộng.

### Incremental Delivery

1. Thêm US4 để dashboard đọc authoritative Experiment/Result/Leaderboard.
2. Thêm US5 để realtime progress/Candle/Leaderboard và recovery.
3. Thêm US6 để News degraded-safe.
4. Chỉ expose endpoint tương ứng khi dependency owner có published boundary và evidence.

### Ghi chú

- `[P]` chỉ dùng khi task có thể chạy song song và không sửa cùng file với task đang phụ thuộc.
- Mọi task story đều có `[USn]`, ID tuần tự và file path cụ thể.
- Test phải được viết trước implementation trong từng story và chứng minh acceptance scenario.
- Không tạo migration hoặc import `..internal..` từ `apps/api` để “lách” dependency.

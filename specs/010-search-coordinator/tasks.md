# Tasks: Search Coordinator (F-010)

**Input**: Design documents từ `/specs/010-search-coordinator/`

**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/`, `quickstart.md`

**Organization**: Task được nhóm theo user story; test/evidence đi trước implementation của từng slice.

## Phase 1: Setup

**Mục đích**: Chuẩn bị module dependencies, versioned contracts và configuration surface.

- [ ] T001 Cập nhật dependency `modules/search` trên Domain và Strategy API trong `modules/search/build.gradle.kts`
- [ ] T002 Cập nhật Worker dependency trên Search module trong `apps/worker/build.gradle.kts`
- [ ] T003 [P] Bổ sung Search Coordinator stream/group/concurrency/recovery properties trong `apps/worker/src/main/java/com/cryptostrategy/platform/worker/config/WorkerProperties.java` và `apps/worker/src/main/resources/application.yml`
- [ ] T004 [P] Nâng reserved Search Request contract thành active backward-compatible v1 trong `specs/007-worker-reliable-job-processing/contracts/search-requests-reservation.md` và `modules/contracts/src/main/java/com/cryptostrategy/platform/contracts/api/SearchRequestPayload.java`
- [ ] T005 [P] Cập nhật event topology documentation theo `specs/010-search-coordinator/contracts/search-events.md` trong `docs/architecture/data-flows.md`
- [ ] T006 Thêm Search module/persistence dependency edges được phép vào `architecture-tests/src/test/java/com/cryptostrategy/platform/architecture/ModuleBoundaryTest.java` chỉ khi khớp ADR-0016

## Phase 2: Foundational

**Mục đích**: Tạo typed model, ports, schema và transaction boundaries dùng chung cho mọi story.

- [ ] T007 [P] Viết architecture tests cấm framework/Experiment/Backtest/Evaluation/Leaderboard dependency trong Search và cấm Worker direct SQL tại `architecture-tests/src/test/java/com/cryptostrategy/platform/architecture/SearchCoordinatorBoundaryTest.java`
- [ ] T008 [P] Viết static migration contract test bảo vệ checksum cũ và F-010 ordering tại `modules/persistence/src/test/java/com/cryptostrategy/platform/persistence/internal/search/SearchMigrationContractTest.java`
- [ ] T009 [P] Tạo typed Generator/Search Run/Decision identities và enums trong `modules/search/src/main/java/com/cryptostrategy/platform/search/api/model/`
- [ ] T010 [P] Tạo canonical Search Space, parameter domain và stop-condition models trong `modules/search/src/main/java/com/cryptostrategy/platform/search/api/model/`
- [ ] T011 [P] Tạo Generator descriptor/request/outcome/versioned-state models theo generator contract trong `modules/search/src/main/java/com/cryptostrategy/platform/search/api/model/`
- [ ] T012 [P] Tạo Search Run và Coordination Decision models với invariant/state transitions trong `modules/search/src/main/java/com/cryptostrategy/platform/search/api/model/`
- [ ] T013 Tạo published generator registry/generation input ports trong `modules/search/src/main/java/com/cryptostrategy/platform/search/api/port/in/`
- [ ] T014 Tạo Search Run state/claim/fence/recovery output ports trong `modules/search/src/main/java/com/cryptostrategy/platform/search/api/port/out/`
- [ ] T015 Tạo forward-only Search Run/Decision schema, unique constraints và recovery indexes trong `supabase/migrations/20260903000100_f010_search_coordinator.sql`
- [ ] T016 Tạo JDBC row/SQL mapping cho Search-owned state trong `modules/persistence/src/main/java/com/cryptostrategy/platform/persistence/internal/search/`
- [ ] T017 Expose Search persistence adapter qua `modules/persistence/src/main/java/com/cryptostrategy/platform/persistence/api/SearchPersistenceFactory.java`
- [ ] T018 Tạo Search module factory cho registry/generator/state application components trong `modules/search/src/main/java/com/cryptostrategy/platform/search/api/SearchModuleFactory.java`
- [ ] T019 Viết foundational model/state/port contract tests tại `modules/search/src/test/java/com/cryptostrategy/platform/search/api/SearchModelContractTest.java`

**Checkpoint**: Typed/canonical Search boundary, ADR-compatible dependencies và durable schema sẵn sàng.

## Phase 3: User Story 1 — Khởi chạy tìm kiếm chiến lược (P1)

**Goal**: Start Experiment atomic/idempotent và chạy Random Search deterministic tới terminal result.

**Independent Test**: Start finite Experiment bằng seed cố định, thấy ordered Candidates/Jobs,
progress và Leaderboard terminal; replay 100 lần không tạo logical outcome trùng.

### Tests

- [ ] T020 [P] [US1] Viết Random generator determinism/canonical ordering/different-seed tests tại `modules/search/src/test/java/com/cryptostrategy/platform/search/internal/RandomStrategyGeneratorTest.java`
- [ ] T021 [P] [US1] Viết generator registry duplicate/unsupported-version tests tại `modules/search/src/test/java/com/cryptostrategy/platform/search/internal/StrategyGeneratorRegistryTest.java`
- [ ] T022 [P] [US1] Viết invalid/out-of-space/duplicate/no-progress generator tests tại `modules/search/src/test/java/com/cryptostrategy/platform/search/internal/SearchGenerationGuardTest.java`
- [ ] T023 [P] [US1] Viết atomic Start Experiment replay/conflict/rollback tests tại `modules/persistence/src/experimentIntegrationTest/java/com/cryptostrategy/platform/persistence/internal/experiment/SearchStartTransactionIntegrationTest.java`
- [ ] T024 [P] [US1] Viết concurrent Candidate allocation/fencing/unique-index tests tại `modules/persistence/src/experimentIntegrationTest/java/com/cryptostrategy/platform/persistence/internal/search/SearchAllocationConcurrencyIntegrationTest.java`
- [ ] T025 [P] [US1] Viết Search Request envelope/group/ACK contract tests tại `apps/worker/src/test/java/com/cryptostrategy/platform/worker/search/SearchRequestConsumerTest.java`
- [ ] T026 [P] [US1] Viết bounded fill-window và authoritative progress tests tại `apps/worker/src/test/java/com/cryptostrategy/platform/worker/search/SearchCoordinatorTest.java`
- [ ] T027 [P] [US1] Viết public Start 202/Location/idempotency/ownership tests tại `apps/api/src/test/java/com/cryptostrategy/platform/api/experiment/StartExperimentIntegrationTest.java`

### Implementation

- [ ] T028 [P] [US1] Implement canonical search-space validator/fingerprint trong `modules/search/src/main/java/com/cryptostrategy/platform/search/internal/CanonicalSearchSpace.java`
- [ ] T029 [P] [US1] Implement versioned deterministic PRNG state và Random generator trong `modules/search/src/main/java/com/cryptostrategy/platform/search/internal/RandomStrategyGenerator.java`
- [ ] T030 [US1] Implement generator registry exact lookup/no fallback trong `modules/search/src/main/java/com/cryptostrategy/platform/search/internal/StrategyGeneratorRegistry.java`
- [ ] T031 [US1] Implement bounded duplicate/no-progress/output validation guard trong `modules/search/src/main/java/com/cryptostrategy/platform/search/internal/SearchGenerationService.java`
- [ ] T032 [US1] Thêm atomic Start Experiment command/acceptance published port trong `modules/experiment/src/main/java/com/cryptostrategy/platform/experiment/api/port/in/StartSearchExperimentUseCase.java`
- [ ] T033 [US1] Implement frozen manifest + Experiment + SEARCH Job + Search Run + Outbox + idempotency transaction trong `modules/experiment/src/main/java/com/cryptostrategy/platform/experiment/internal/SearchExperimentApplicationService.java`
- [ ] T034 [US1] Thêm atomic allocation published port và ownership/lineage result trong `modules/experiment/src/main/java/com/cryptostrategy/platform/experiment/api/port/in/AllocateSearchCandidateUseCase.java`
- [ ] T035 [US1] Implement Candidate + Backtest Job + generator state/decision + Outbox transaction trong `modules/persistence/src/main/java/com/cryptostrategy/platform/persistence/internal/experiment/JdbcSearchExperimentStore.java`
- [ ] T036 [US1] Extend Outbox publisher mapping SEARCH Job thành `SEARCH_REQUEST` trên `search.requests.v1` trong `apps/worker/src/main/java/com/cryptostrategy/platform/worker/engine/OutboxPublisherEngine.java`
- [ ] T037 [US1] Implement separate-group Search Request consumer/reclaim/ACK behavior trong `apps/worker/src/main/java/com/cryptostrategy/platform/worker/search/consumer/SearchRequestConsumer.java`
- [ ] T038 [US1] Implement bounded Search coordination/fill decisions qua published ports trong `apps/worker/src/main/java/com/cryptostrategy/platform/worker/search/coordination/SearchCoordinator.java`
- [ ] T039 [US1] Wire Search factory, persistence, Coordinator và schedules trong `apps/worker/src/main/java/com/cryptostrategy/platform/worker/config/SearchWorkerConfiguration.java`
- [ ] T040 [US1] Wire Start Search application port trong `apps/api/src/main/java/com/cryptostrategy/platform/api/config/ExperimentApiConfiguration.java`
- [ ] T041 [US1] Thay Start Experiment readiness gate bằng validated/idempotent published command trong `apps/api/src/main/java/com/cryptostrategy/platform/api/experiment/ExperimentController.java`
- [ ] T042 [US1] Hoàn thiện Start request mapping frozen Dataset/Strategy/generator/search config trong `apps/api/src/main/java/com/cryptostrategy/platform/api/experiment/ExperimentRequestMapper.java`
- [ ] T043 [US1] Chạy finite Experiment integration và ghi evidence theo `specs/010-search-coordinator/quickstart.md`

**Checkpoint**: US1 tự chạy Start → Candidates → Jobs → Evaluations/Leaderboard → terminal.

## Phase 4: User Story 2 — Dừng và phục hồi an toàn (P1)

**Goal**: Stop chặn allocation mới; duplicate/restart/queue-loss được reconcile từ durable truth.

**Independent Test**: Stop giữa run, inject duplicate/stale completion, kill/restart và xóa stream;
không Candidate mới sau stop, progress đúng và run hữu hạn tới terminal.

### Tests

- [ ] T044 [P] [US2] Viết duplicate/stale/out-of-order completion tests tại `apps/worker/src/test/java/com/cryptostrategy/platform/worker/search/SearchCompletionConsumerTest.java`
- [ ] T045 [P] [US2] Viết stop-vs-allocation race integration tests tại `modules/persistence/src/experimentIntegrationTest/java/com/cryptostrategy/platform/persistence/internal/search/SearchStopRaceIntegrationTest.java`
- [ ] T046 [P] [US2] Viết kill-point restart/reclaim tests tại `apps/worker/src/test/java/com/cryptostrategy/platform/worker/search/SearchCrashRecoveryTest.java`
- [ ] T047 [P] [US2] Viết queue-loss/outbox repair/reconciliation tests tại `apps/worker/src/test/java/com/cryptostrategy/platform/worker/search/SearchReconciliationTest.java`
- [ ] T048 [P] [US2] Viết bounded retry/dead-letter/redaction tests tại `apps/worker/src/test/java/com/cryptostrategy/platform/worker/search/SearchFailurePolicyTest.java`

### Implementation

- [ ] T049 [US2] Thêm trusted authoritative completion/reconciliation ports trong `modules/experiment/src/main/java/com/cryptostrategy/platform/experiment/api/port/in/TrustedSearchCoordinationUseCase.java`
- [ ] T050 [US2] Implement idempotent Search Job progress và terminal decision rules trong `modules/experiment/src/main/java/com/cryptostrategy/platform/experiment/internal/TrustedSearchCoordinationService.java`
- [ ] T051 [US2] Implement separate-group Candidate Evaluated handler cho Search trong `apps/worker/src/main/java/com/cryptostrategy/platform/worker/search/consumer/SearchCompletionConsumer.java`
- [ ] T052 [US2] Implement durable status/version reload trước fill/stop/complete trong `apps/worker/src/main/java/com/cryptostrategy/platform/worker/search/coordination/SearchCoordinator.java`
- [ ] T053 [US2] Implement bounded non-terminal recovery query/adapter trong `modules/persistence/src/main/java/com/cryptostrategy/platform/persistence/internal/search/JdbcSearchRunStore.java`
- [ ] T054 [US2] Implement scheduled Search reconciliation và missing-intent repair trong `apps/worker/src/main/java/com/cryptostrategy/platform/worker/search/reconciliation/SearchReconciler.java`
- [ ] T055 [US2] Tích hợp finite retry/dead-letter/lifecycle failure publication trong `apps/worker/src/main/java/com/cryptostrategy/platform/worker/search/coordination/SearchFailureHandler.java`
- [ ] T056 [US2] Tích hợp stop gate với existing cancel/stop-completion ports trong `apps/worker/src/main/java/com/cryptostrategy/platform/worker/search/coordination/SearchCoordinator.java`
- [ ] T057 [US2] Thêm metrics/log correlation redaction cho Coordinator trong `apps/worker/src/main/java/com/cryptostrategy/platform/worker/search/coordination/SearchObservability.java`
- [ ] T058 [US2] Chạy Redis restart/queue-loss/stop acceptance và ghi evidence theo `specs/010-search-coordinator/quickstart.md`

**Checkpoint**: US2 chứng minh recovery và stop correctness không phụ thuộc Redis lock/cache.

## Phase 5: User Story 3 — Tái tạo Experiment (P2)

**Goal**: Reproduce tạo linked run, copy exact Candidate sequence, execute lại và verify evidence.

**Independent Test**: Reproduce terminal source; source bất biến, ordered Candidate fingerprints
giống nhau và verification trả match hoặc differences chính xác.

### Tests

- [ ] T059 [P] [US3] Viết source owner/terminal/evidence validation tests tại `modules/experiment/src/test/java/com/cryptostrategy/platform/experiment/internal/SearchReproductionValidationTest.java`
- [ ] T060 [P] [US3] Viết atomic reproduction graph/candidate-copy/rollback integration tests tại `modules/persistence/src/experimentIntegrationTest/java/com/cryptostrategy/platform/persistence/internal/experiment/SearchReproductionIntegrationTest.java`
- [ ] T061 [P] [US3] Viết reproduce replay/conflict/202/Location public tests tại `apps/api/src/test/java/com/cryptostrategy/platform/api/experiment/ReproduceExperimentIntegrationTest.java`
- [ ] T062 [P] [US3] Viết semantic Trade/metrics/fingerprint match/mismatch tests tại `modules/experiment-execution/src/test/java/com/cryptostrategy/platform/execution/internal/SearchReproductionExecutionTest.java`

### Implementation

- [ ] T063 [US3] Tạo atomic Reproduce Search command published port trong `modules/experiment/src/main/java/com/cryptostrategy/platform/experiment/api/port/in/StartSearchReproductionUseCase.java`
- [ ] T064 [US3] Implement owner/terminal/provenance validation và immutable source Candidate copy trong `modules/experiment/src/main/java/com/cryptostrategy/platform/experiment/internal/SearchReproductionApplicationService.java`
- [ ] T065 [US3] Implement atomic reproduction Experiment/Manifest/Candidates/Search Job/Run/Outbox/receipt trong `modules/persistence/src/main/java/com/cryptostrategy/platform/persistence/internal/experiment/JdbcSearchExperimentStore.java`
- [ ] T066 [US3] Implement reproduction-mode dispatch theo frozen sequence trong `apps/worker/src/main/java/com/cryptostrategy/platform/worker/search/coordination/SearchCoordinator.java`
- [ ] T067 [US3] Wire existing reproduction evidence verification/storage qua published execution port trong `modules/experiment-execution/src/main/java/com/cryptostrategy/platform/execution/api/ExperimentExecutionModuleFactory.java`
- [ ] T068 [US3] Thay Reproduce readiness gate bằng idempotent published command trong `apps/api/src/main/java/com/cryptostrategy/platform/api/experiment/ExperimentController.java`
- [ ] T069 [US3] Chạy end-to-end reproduction và ghi evidence theo `specs/010-search-coordinator/quickstart.md`

**Checkpoint**: US3 mở public Reproduce mà không sửa source hoặc phụ thuộc generator artifact cũ.

## Phase 6: User Story 4 — Thay generator không đổi pipeline (P3)

**Goal**: Chứng minh generator conforming có thể thay Random Search mà downstream không đổi.

**Independent Test**: Đăng ký fixture generator, chạy finite Experiment qua cùng Coordinator và
không thay Backtest/Evaluation/Leaderboard/public contracts.

### Tests

- [ ] T070 [P] [US4] Tạo deterministic fixture generator trong `modules/search/src/test/java/com/cryptostrategy/platform/search/fixtures/FixtureStrategyGenerator.java`
- [ ] T071 [P] [US4] Viết replaceability/change-scope proof tại `modules/search/src/test/java/com/cryptostrategy/platform/search/internal/GeneratorReplaceabilityTest.java`
- [ ] T072 [P] [US4] Viết downstream contract regression test tại `architecture-tests/src/test/java/com/cryptostrategy/platform/architecture/SearchGeneratorReplaceabilityTest.java`

### Implementation

- [ ] T073 [US4] Hoàn thiện public descriptor/registry factory extension point trong `modules/search/src/main/java/com/cryptostrategy/platform/search/api/SearchModuleFactory.java`
- [ ] T074 [US4] Bảo đảm Coordinator lookup generator hoàn toàn qua registry contract trong `apps/worker/src/main/java/com/cryptostrategy/platform/worker/search/coordination/SearchCoordinator.java`
- [ ] T075 [US4] Ghi replaceability diff/evidence vào `docs/architecture/architecture-evidence.md`

**Checkpoint**: US4 pass mà không sửa downstream business code hoặc message/public schema.

## Phase 7: Polish & Cross-Cutting

- [ ] T076 [P] Đồng bộ OpenAPI/error/examples từ gated sang ready trong `docs/api/openapi.yaml`, `docs/api/error-catalog.md` và `docs/api/examples.md`
- [ ] T077 [P] Cập nhật Worker/Search event operations trong `docs/api/websocket-events.md` và `docs/architecture/data-flows.md`
- [ ] T078 [P] Thêm F-010 contract/document parity tests tại `apps/api/src/test/java/com/cryptostrategy/platform/api/contract/SearchDocumentationParityTest.java`
- [ ] T079 [P] Thêm performance smoke cho Start acceptance và bounded fill trong `apps/api/src/test/java/com/cryptostrategy/platform/api/performance/SearchCoordinatorPerformanceTest.java`
- [ ] T080 Review ADR-0016, migration, owner boundary, error redaction và release gates trong `specs/010-search-coordinator/checklists/implementation-readiness.md`
- [ ] T081 Chạy full `JAVA_HOME=<JDK21> ./gradlew test` và sửa mọi F-010 failure/warning
- [ ] T082 Chạy PostgreSQL/Supabase + Redis integration suite và ghi commit/environment/config thật trong `specs/010-search-coordinator/quickstart.md`
- [ ] T083 Cập nhật F-009 tasks/readiness evidence, đóng T034/T036/T039/T074 khi có bằng chứng thật trong `specs/009-public-api-realtime/tasks.md` và `specs/009-public-api-realtime/quickstart.md`
- [ ] T084 Chuyển quality scenarios liên quan từ Planned sang Verified chỉ theo evidence thật trong `docs/architecture/architecture-evidence.md`

## Dependencies & Execution Order

### Phase dependencies

- Phase 1 → Phase 2: dependency/contract trước typed state/persistence foundation.
- Phase 2 block mọi user story.
- US1 là MVP và block US2/US3 vì cần allocation runtime.
- US2 phải pass trước gỡ public gate vì stop/recovery là release condition.
- US3 phụ thuộc US1 atomic graph; có thể phát triển test/evidence song song phần US2 sau foundation.
- US4 phụ thuộc registry/generator US1 nhưng không phụ thuộc reproduction.
- Polish/gate removal evidence phụ thuộc US1–US4 tương ứng.

### Story graph

```text
Setup -> Foundation -> US1 Start/Search
                           ├──> US2 Stop/Recovery ──┐
                           ├──> US3 Reproduction ──┼──> Polish/Public Ready
                           └──> US4 Replaceability ┘
```

## Parallel Opportunities

- Phase 1: T003–T005.
- Foundation: T007–T012 trước T013–T018.
- US1 tests: T020–T027; implementation T028/T029 song song trước registry/service.
- US2 tests: T044–T048; persistence recovery T053 song song consumer T051 sau ports.
- US3 tests: T059–T062; verification wiring T067 song song persistence implementation.
- US4 tests T070–T072 và polish docs T076–T079 ở các file tách biệt.

## Implementation Strategy

### MVP first

1. Setup + Foundation.
2. US1 finite Random Search end-to-end nhưng giữ public gate nếu US2 recovery chưa pass.
3. US2 stop/recovery; sau đó Start endpoint đủ điều kiện ready.

### Incremental delivery

1. US3 mở Reproduce sau evidence riêng.
2. US4 chứng minh replaceability.
3. Chỉ đóng F-009 gate/integration tasks và architecture evidence khi runtime tests thật pass.

## Format Validation

- Mọi executable task dùng `- [ ] Tnnn`.
- Task trong story có `[USn]`; setup/foundation/polish không có story label.
- `[P]` chỉ dùng cho file/boundary có thể làm độc lập.
- Mọi task chỉ rõ file hoặc thư mục đích cụ thể.

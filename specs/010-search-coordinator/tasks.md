# Tasks: Search Coordinator (F-010)

**Input**: Design documents tá»« `/specs/010-search-coordinator/`

**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/`, `quickstart.md`

**Organization**: Task Ä‘Æ°á»£c nhÃ³m theo user story; test/evidence Ä‘i trÆ°á»›c implementation cá»§a tá»«ng slice.

## Phase 1: Setup

**Má»¥c Ä‘Ã­ch**: Chuáº©n bá»‹ module dependencies, versioned contracts vÃ  configuration surface.

- [x] T001 Cáº­p nháº­t dependency edges: Search chá»‰ dÃ¹ng Domain/Strategy, cÃ²n Experiment Execution vÃ  Persistence dÃ¹ng public Search API trong `modules/search/build.gradle.kts`, `modules/experiment-execution/build.gradle.kts` vÃ  `modules/persistence/build.gradle.kts`
- [x] T002 Cáº­p nháº­t Worker dependency trÃªn Search module trong `apps/worker/build.gradle.kts`
- [x] T003 [P] Bá»• sung Search Coordinator stream/group/concurrency/recovery properties trong `apps/worker/src/main/java/com/cryptostrategy/platform/worker/config/WorkerProperties.java` vÃ  `apps/worker/src/main/resources/application.yml`
- [x] T004 [P] NÃ¢ng reserved Search Request contract thÃ nh active backward-compatible v1 trong `specs/007-worker-reliable-job-processing/contracts/search-requests-reservation.md` vÃ  `modules/contracts/src/main/java/com/cryptostrategy/platform/contracts/api/SearchRequestPayload.java`
- [x] T005 [P] Cáº­p nháº­t event topology documentation theo `specs/010-search-coordinator/contracts/search-events.md` trong `docs/architecture/data-flows.md`
- [x] T006 KhÃ³a dependency matrix chá»‰ cho phÃ©p `experiment-execution -> search`, `persistence -> search`, `worker -> search` vÃ  cáº¥m má»i chiá»u `search -> persistence|experiment|experiment-execution|worker` trong `architecture-tests/src/test/java/com/cryptostrategy/platform/architecture/ModuleBoundaryTest.java` theo ADR-0016

## Phase 2: Foundational

**Má»¥c Ä‘Ã­ch**: Táº¡o typed model, ports, schema vÃ  transaction boundaries dÃ¹ng chung cho má»i story.

- [x] T007 [P] Viáº¿t architecture tests cáº¥m framework/Experiment/Backtest/Evaluation/Leaderboard dependency trong Search vÃ  cáº¥m Worker direct SQL táº¡i `architecture-tests/src/test/java/com/cryptostrategy/platform/architecture/SearchCoordinatorBoundaryTest.java`
- [x] T008 [P] Viáº¿t static migration contract test báº£o vá»‡ checksum cÅ© vÃ  F-010 ordering táº¡i `modules/persistence/src/test/java/com/cryptostrategy/platform/persistence/internal/search/SearchMigrationContractTest.java`
- [x] T009 Viáº¿t trÆ°á»›c foundational model/state/port contract tests cho identities, canonical values, invariants vÃ  published ports táº¡i `modules/search/src/test/java/com/cryptostrategy/platform/search/api/SearchModelContractTest.java`
- [x] T010 [P] Táº¡o typed Generator/Search Run/Decision identities vÃ  enums trong `modules/search/src/main/java/com/cryptostrategy/platform/search/api/model/`
- [x] T011 [P] Táº¡o canonical Search Space, parameter domain vÃ  stop-condition models trong `modules/search/src/main/java/com/cryptostrategy/platform/search/api/model/`
- [x] T012 [P] Táº¡o Generator descriptor/request/outcome/versioned-state models theo generator contract trong `modules/search/src/main/java/com/cryptostrategy/platform/search/api/model/`
- [x] T013 [P] Táº¡o Search Run vÃ  Coordination Decision models vá»›i invariant/state transitions trong `modules/search/src/main/java/com/cryptostrategy/platform/search/api/model/`
- [x] T014 Táº¡o published generator registry/generation input ports trong `modules/search/src/main/java/com/cryptostrategy/platform/search/api/port/in/`
- [x] T015 Táº¡o Search Run state/claim/fence/recovery output ports trong `modules/search/src/main/java/com/cryptostrategy/platform/search/api/port/out/`
- [x] T016 Táº¡o forward-only Search Run/Decision/Reproduction Verification schema, unique constraints vÃ  recovery indexes trong `supabase/migrations/20260903000100_f010_search_coordinator.sql`
- [x] T017 Táº¡o JDBC row/SQL mapping cho Search-owned state trong `modules/persistence/src/main/java/com/cryptostrategy/platform/persistence/internal/search/`
- [x] T018 Expose Search persistence adapter qua `modules/persistence/src/main/java/com/cryptostrategy/platform/persistence/api/SearchPersistenceFactory.java`
- [x] T019 Táº¡o Search module factory cho registry/generator/state application components trong `modules/search/src/main/java/com/cryptostrategy/platform/search/api/SearchModuleFactory.java`

**Checkpoint**: Typed/canonical Search boundary, ADR-compatible dependencies vÃ  durable schema sáºµn sÃ ng.

## Phase 3: User Story 1 â€” Khá»Ÿi cháº¡y tÃ¬m kiáº¿m chiáº¿n lÆ°á»£c (P1)

**Goal**: Start Experiment atomic/idempotent vÃ  cháº¡y Random Search deterministic tá»›i terminal result.

**Independent Test**: Start finite Experiment báº±ng seed cá»‘ Ä‘á»‹nh, tháº¥y ordered Candidates/Jobs,
progress vÃ  Leaderboard terminal; replay 100 láº§n khÃ´ng táº¡o logical outcome trÃ¹ng.

### Tests

- [x] T020 [P] [US1] Viáº¿t Random generator determinism/canonical ordering/different-seed tests táº¡i `modules/search/src/test/java/com/cryptostrategy/platform/search/internal/RandomStrategyGeneratorTest.java`
- [x] T021 [P] [US1] Viáº¿t generator registry duplicate/unsupported-version tests táº¡i `modules/search/src/test/java/com/cryptostrategy/platform/search/internal/StrategyGeneratorRegistryTest.java`
- [x] T022 [P] [US1] Viáº¿t invalid/out-of-space/duplicate/no-progress generator tests táº¡i `modules/search/src/test/java/com/cryptostrategy/platform/search/internal/SearchGenerationGuardTest.java`
- [x] T023 [P] [US1] Viáº¿t atomic Start Experiment replay/conflict/rollback tests táº¡i `modules/persistence/src/experimentIntegrationTest/java/com/cryptostrategy/platform/persistence/internal/experiment/SearchStartTransactionIntegrationTest.java`
- [x] T024 [P] [US1] Viáº¿t concurrent Candidate allocation/fencing/unique-index tests táº¡i `modules/persistence/src/experimentIntegrationTest/java/com/cryptostrategy/platform/persistence/internal/search/SearchAllocationConcurrencyIntegrationTest.java`
- [x] T025 [P] [US1] Viáº¿t Search Request envelope/group/ACK contract tests táº¡i `apps/worker/src/test/java/com/cryptostrategy/platform/worker/search/SearchRequestConsumerTest.java`
- [x] T026 [P] [US1] Viáº¿t bounded fill-window vÃ  authoritative progress tests táº¡i `apps/worker/src/test/java/com/cryptostrategy/platform/worker/search/SearchCoordinatorTest.java`
- [x] T027 [P] [US1] Viáº¿t Start orchestration acceptance/idempotency/ownership tests vÃ  xÃ¡c nháº­n public gate váº«n tráº£ readiness 503 trÆ°á»›c US2 táº¡i `apps/api/src/test/java/com/cryptostrategy/platform/api/experiment/StartExperimentReadinessIntegrationTest.java`

### Implementation

- [x] T028 [P] [US1] Implement canonical search-space validator/fingerprint trong `modules/search/src/main/java/com/cryptostrategy/platform/search/internal/CanonicalSearchSpace.java`
- [x] T029 [P] [US1] Implement versioned deterministic PRNG state vÃ  Random generator trong `modules/search/src/main/java/com/cryptostrategy/platform/search/internal/RandomStrategyGenerator.java`
- [x] T030 [US1] Implement generator registry exact lookup/no fallback trong `modules/search/src/main/java/com/cryptostrategy/platform/search/internal/StrategyGeneratorRegistry.java`
- [x] T031 [US1] Implement bounded duplicate/no-progress/output validation guard trong `modules/search/src/main/java/com/cryptostrategy/platform/search/internal/SearchGenerationService.java`
- [x] T032 [US1] ThÃªm public Start orchestration port trong `modules/experiment-execution/src/main/java/com/cryptostrategy/platform/execution/api/port/in/StartSearchExperimentUseCase.java`
- [x] T033 [US1] Implement Start orchestration qua public owner policies vÃ  composite gateway trong `modules/experiment-execution/src/main/java/com/cryptostrategy/platform/execution/internal/SearchExperimentOrchestrationService.java`
- [x] T034 [US1] ThÃªm public allocation orchestration port vÃ  composite transaction gateway trong `modules/experiment-execution/src/main/java/com/cryptostrategy/platform/execution/api/port/`
- [x] T035 [US1] Implement composite Start/allocation transaction adapter cho Experiment-owned graph + Search-owned state trong `modules/persistence/src/main/java/com/cryptostrategy/platform/persistence/internal/execution/JdbcSearchExperimentTransaction.java`
- [x] T036 [US1] Extend Outbox publisher mapping SEARCH Job thÃ nh `SEARCH_REQUEST` trÃªn `search.requests.v1` trong `apps/worker/src/main/java/com/cryptostrategy/platform/worker/engine/OutboxPublisherEngine.java`
- [x] T037 [US1] Implement separate-group Search Request consumer/reclaim/ACK behavior trong `apps/worker/src/main/java/com/cryptostrategy/platform/worker/search/consumer/SearchRequestConsumer.java`
- [x] T038 [US1] Implement bounded Search coordination/fill decisions qua published ports trong `apps/worker/src/main/java/com/cryptostrategy/platform/worker/search/coordination/SearchCoordinator.java`
- [x] T039 [US1] Wire Search factory, persistence, Coordinator vÃ  schedules trong `apps/worker/src/main/java/com/cryptostrategy/platform/worker/config/SearchWorkerConfiguration.java`
- [x] T040 [US1] Wire dormant Start Search execution port sau readiness switch trong `apps/api/src/main/java/com/cryptostrategy/platform/api/config/ExperimentApiConfiguration.java`
- [x] T041 [US1] Giá»¯ Start Experiment readiness gate Ä‘Ã³ng vÃ  route chá»‰ qua switch Ä‘Æ°á»£c kiá»ƒm soÃ¡t trong `apps/api/src/main/java/com/cryptostrategy/platform/api/experiment/ExperimentController.java`
- [x] T042 [US1] HoÃ n thiá»‡n Start request mapping frozen Dataset/single-Strategy/generator/search config vÃ  stable Composite Search rejection trong `apps/api/src/main/java/com/cryptostrategy/platform/api/experiment/ExperimentRequestMapper.java`
- [x] T043 [US1] Cháº¡y finite Experiment integration vÃ  ghi evidence theo `specs/010-search-coordinator/quickstart.md`

**Checkpoint**: US1 tá»± cháº¡y Start â†’ Candidates â†’ Jobs â†’ Evaluations/Leaderboard â†’ terminal.

## Phase 4: User Story 2 â€” Dá»«ng vÃ  phá»¥c há»“i an toÃ n (P1)

**Goal**: Stop cháº·n allocation má»›i; duplicate/restart/queue-loss Ä‘Æ°á»£c reconcile tá»« durable truth.

**Independent Test**: Stop giá»¯a run, inject duplicate/stale completion, kill/restart vÃ  xÃ³a stream;
khÃ´ng Candidate má»›i sau stop, progress Ä‘Ãºng vÃ  run há»¯u háº¡n tá»›i terminal.

### Tests

- [x] T044 [P] [US2] Viáº¿t duplicate/stale/out-of-order completion tests táº¡i `apps/worker/src/test/java/com/cryptostrategy/platform/worker/search/SearchCompletionConsumerTest.java`
- [x] T045 [P] [US2] Viáº¿t stop-vs-allocation race integration tests táº¡i `modules/persistence/src/experimentIntegrationTest/java/com/cryptostrategy/platform/persistence/internal/search/SearchStopRaceIntegrationTest.java`
- [x] T046 [P] [US2] Viáº¿t kill-point restart/reclaim tests táº¡i `apps/worker/src/test/java/com/cryptostrategy/platform/worker/search/SearchCrashRecoveryTest.java`
- [x] T047 [P] [US2] Viáº¿t queue-loss/outbox repair/reconciliation tests táº¡i `apps/worker/src/test/java/com/cryptostrategy/platform/worker/search/SearchReconciliationTest.java`
- [x] T048 [P] [US2] Viáº¿t bounded retry/dead-letter/redaction tests táº¡i `apps/worker/src/test/java/com/cryptostrategy/platform/worker/search/SearchFailurePolicyTest.java`
- [x] T049 [P] [US2] Viáº¿t injected UTC clock, frozen deadline qua restart vÃ  completion/deadline race tests, gá»“m completion tháº¯ng khi authoritative `completedAt <= deadlineAt` vÃ  deadline tháº¯ng khi completion muá»™n hÆ¡n, táº¡i `modules/persistence/src/experimentIntegrationTest/java/com/cryptostrategy/platform/persistence/internal/search/SearchDeadlineIntegrationTest.java`

### Implementation

- [x] T050 [US2] ThÃªm trusted authoritative completion/reconciliation orchestration ports trong `modules/experiment-execution/src/main/java/com/cryptostrategy/platform/execution/api/port/in/TrustedSearchCoordinationUseCase.java`
- [x] T051 [US2] Implement idempotent Search Job progress, frozen deadline vÃ  deterministic terminal decision: reconcile completion cÃ³ authoritative `completedAt <= deadlineAt` trÆ°á»›c deadline, giá»¯ completion muá»™n nhÆ°ng Ä‘á»ƒ deadline cháº·n allocation/Ä‘Æ°a run sang stopping, trong `modules/experiment-execution/src/main/java/com/cryptostrategy/platform/execution/internal/TrustedSearchCoordinationService.java`
- [x] T052 [US2] Implement separate-group Candidate Evaluated handler cho Search trong `apps/worker/src/main/java/com/cryptostrategy/platform/worker/search/consumer/SearchCompletionConsumer.java`
- [x] T053 [US2] Implement durable status/version reload trÆ°á»›c fill/stop/complete trong `apps/worker/src/main/java/com/cryptostrategy/platform/worker/search/coordination/SearchCoordinator.java`
- [x] T054 [US2] Implement bounded non-terminal recovery query/adapter trong `modules/persistence/src/main/java/com/cryptostrategy/platform/persistence/internal/search/JdbcSearchRunStore.java`
- [x] T055 [US2] Implement scheduled Search reconciliation vÃ  missing-intent repair trong `apps/worker/src/main/java/com/cryptostrategy/platform/worker/search/reconciliation/SearchReconciler.java`
- [x] T056 [US2] TÃ­ch há»£p finite retry/dead-letter/lifecycle failure publication trong `apps/worker/src/main/java/com/cryptostrategy/platform/worker/search/coordination/SearchFailureHandler.java`
- [x] T057 [US2] TÃ­ch há»£p stop gate vá»›i existing cancel/stop-completion ports trong `apps/worker/src/main/java/com/cryptostrategy/platform/worker/search/coordination/SearchCoordinator.java`
- [x] T058 [US2] ThÃªm metrics/log correlation redaction cho Coordinator trong `apps/worker/src/main/java/com/cryptostrategy/platform/worker/search/coordination/SearchObservability.java`
- [x] T059 [US2] Cháº¡y Redis restart/queue-loss/stop acceptance vÃ  ghi evidence theo `specs/010-search-coordinator/quickstart.md`
- [x] T060 [US2] Viáº¿t public Start 202/Location/idempotency/ownership tests qua dormant readiness switch táº¡i `apps/api/src/test/java/com/cryptostrategy/platform/api/experiment/StartExperimentIntegrationTest.java`
- [x] T061 [US2] Ghi Start gate evidence Ä‘á»™c láº­p vÃ  quyáº¿t Ä‘á»‹nh giá»¯ Reproduce gate Ä‘Ã³ng trong `specs/010-search-coordinator/checklists/implementation-readiness.md`
- [x] T062 [US2] Gá»¡ riÃªng Start readiness gate báº±ng published execution command vÃ  cháº¡y post-activation smoke trong `apps/api/src/main/java/com/cryptostrategy/platform/api/experiment/ExperimentController.java`

**Checkpoint**: US2 chá»©ng minh recovery vÃ  stop correctness khÃ´ng phá»¥ thuá»™c Redis lock/cache.

## Phase 5: User Story 3 â€” TÃ¡i táº¡o Experiment (P2)

**Goal**: Reproduce táº¡o linked run, copy exact Candidate sequence, execute láº¡i vÃ  verify evidence.

**Independent Test**: Reproduce terminal source; source báº¥t biáº¿n, ordered Candidate fingerprints
giá»‘ng nhau vÃ  verification tráº£ match hoáº·c differences chÃ­nh xÃ¡c.

### Tests

- [x] T063 [P] [US3] Viáº¿t source owner/terminal/evidence validation tests táº¡i `modules/experiment-execution/src/test/java/com/cryptostrategy/platform/execution/internal/SearchReproductionValidationTest.java`
- [x] T064 [P] [US3] Viáº¿t atomic reproduction graph/candidate-copy/rollback integration tests táº¡i `modules/persistence/src/experimentIntegrationTest/java/com/cryptostrategy/platform/persistence/internal/experiment/SearchReproductionIntegrationTest.java`
- [x] T065 [P] [US3] Viáº¿t reproduce replay/conflict/202/Location public tests táº¡i `apps/api/src/test/java/com/cryptostrategy/platform/api/experiment/ReproduceExperimentIntegrationTest.java`
- [x] T066 [P] [US3] Viáº¿t terminal trigger, duplicate/restart vÃ  semantic verification tests: ordered Trade sequence, exact canonical Total Return/Win Rate/Maximum Drawdown/Number of Trades theo frozen metric version, fingerprints vÃ  bounded mismatch report táº¡i `modules/experiment-execution/src/test/java/com/cryptostrategy/platform/execution/internal/SearchReproductionVerificationTest.java`

### Implementation

- [x] T067 [US3] Táº¡o async Reproduce Search orchestration port trong `modules/experiment-execution/src/main/java/com/cryptostrategy/platform/execution/api/port/in/StartSearchReproductionUseCase.java`
- [x] T068 [US3] Implement owner/terminal/provenance validation vÃ  immutable source Candidate copy orchestration trong `modules/experiment-execution/src/main/java/com/cryptostrategy/platform/execution/internal/SearchReproductionApplicationService.java`
- [x] T069 [US3] Implement atomic reproduction graph + `PENDING` verification composite adapter trong `modules/persistence/src/main/java/com/cryptostrategy/platform/persistence/internal/execution/JdbcSearchExperimentTransaction.java`
- [x] T070 [US3] Implement reproduction-mode dispatch theo frozen sequence trong `apps/worker/src/main/java/com/cryptostrategy/platform/worker/search/coordination/SearchCoordinator.java`
- [x] T071 [US3] Implement durable terminal-trigger/reconciler verification lifecycle qua published comparator/store ports trong `modules/experiment-execution/src/main/java/com/cryptostrategy/platform/execution/internal/SearchReproductionVerificationCoordinator.java`
- [x] T072 [US3] Cháº¡y end-to-end async reproduction vá»›i gate cÃ²n Ä‘Ã³ng vÃ  ghi immutable-source/verification evidence theo `specs/010-search-coordinator/quickstart.md`
- [x] T073 [US3] Gá»¡ riÃªng Reproduce readiness gate báº±ng published execution command vÃ  cháº¡y post-activation smoke trong `apps/api/src/main/java/com/cryptostrategy/platform/api/experiment/ExperimentController.java`

**Checkpoint**: US3 má»Ÿ public Reproduce mÃ  khÃ´ng sá»­a source hoáº·c phá»¥ thuá»™c generator artifact cÅ©.

## Phase 6: User Story 4 â€” Thay generator khÃ´ng Ä‘á»•i pipeline (P3)

**Goal**: Chá»©ng minh generator conforming cÃ³ thá»ƒ thay Random Search mÃ  downstream khÃ´ng Ä‘á»•i.

**Independent Test**: ÄÄƒng kÃ½ fixture generator, cháº¡y finite Experiment qua cÃ¹ng Coordinator vÃ 
khÃ´ng thay Backtest/Evaluation/Leaderboard/public contracts.

### Tests

- [x] T074 [P] [US4] Táº¡o deterministic fixture generator trong `modules/search/src/test/java/com/cryptostrategy/platform/search/fixtures/FixtureStrategyGenerator.java`
- [x] T075 [P] [US4] Viáº¿t replaceability/change-scope proof táº¡i `modules/search/src/test/java/com/cryptostrategy/platform/search/internal/GeneratorReplaceabilityTest.java`
- [x] T076 [P] [US4] Viáº¿t downstream contract regression test táº¡i `architecture-tests/src/test/java/com/cryptostrategy/platform/architecture/SearchGeneratorReplaceabilityTest.java`

### Implementation

- [x] T077 [US4] HoÃ n thiá»‡n public descriptor/registry factory extension point trong `modules/search/src/main/java/com/cryptostrategy/platform/search/api/SearchModuleFactory.java`
- [x] T078 [US4] Báº£o Ä‘áº£m Coordinator lookup generator hoÃ n toÃ n qua registry contract trong `apps/worker/src/main/java/com/cryptostrategy/platform/worker/search/coordination/SearchCoordinator.java`
- [x] T079 [US4] Ghi replaceability diff/evidence vÃ o `docs/architecture/architecture-evidence.md`

**Checkpoint**: US4 pass mÃ  khÃ´ng sá»­a downstream business code hoáº·c message/public schema.

## Phase 7: Polish & Cross-Cutting

- [x] T080 [P] Äá»“ng bá»™ OpenAPI/error/examples tá»« gated sang ready trong `docs/api/openapi.yaml`, `docs/api/error-catalog.md` vÃ  `docs/api/examples.md`
- [x] T081 [P] Cáº­p nháº­t Worker/Search event operations trong `docs/api/websocket-events.md` vÃ  `docs/architecture/data-flows.md`
- [x] T082 [P] ThÃªm F-010 contract/document parity test vÃ  parameterized public error/progress/lifecycle failure redaction matrix, cáº¥m secret/provider payload/SQL/path/stack/internal exception detail cho má»i F-010 mapping, táº¡i `apps/api/src/test/java/com/cryptostrategy/platform/api/contract/SearchDocumentationParityTest.java` vÃ  `apps/api/src/test/java/com/cryptostrategy/platform/api/contract/SearchPublicFailureRedactionTest.java`
- [x] T083 [P] ThÃªm Start acceptance benchmark Ã­t nháº¥t 100 request há»£p lá»‡ sau warm-up, Ä‘o API receipt Ä‘áº¿n atomic commit/response durable identity, loáº¡i container startup/migration, assert p95 dÆ°á»›i 2 giÃ¢y vÃ  bounded fill trong `apps/api/src/test/java/com/cryptostrategy/platform/api/performance/SearchCoordinatorPerformanceTest.java`
- [x] T084 ThÃªm `SearchScopeBoundaryTest` chá»©ng minh khÃ´ng cÃ³ live-trading/wallet/financial-advice endpoint/dependency vÃ  review ADR-0016, migration, owner boundary, redaction, release gates trong `architecture-tests/src/test/java/com/cryptostrategy/platform/architecture/SearchScopeBoundaryTest.java` cÃ¹ng `specs/010-search-coordinator/checklists/implementation-readiness.md`
- [x] T085 Cháº¡y full `JAVA_HOME=<JDK21> ./gradlew test` vÃ  sá»­a má»i F-010 failure/warning
- [x] T086 Cháº¡y PostgreSQL/Supabase + Redis integration suite vÃ  ghi commit/environment/config tháº­t trong `specs/010-search-coordinator/quickstart.md`
- [x] T087 Cáº­p nháº­t F-009 tasks/readiness evidence, Ä‘Ã³ng T034/T036/T039/T074 khi cÃ³ báº±ng chá»©ng tháº­t trong `specs/009-public-api-realtime/tasks.md` vÃ  `specs/009-public-api-realtime/quickstart.md`
- [x] T088 Chuyá»ƒn quality scenarios liÃªn quan tá»« Planned sang Verified chá»‰ theo evidence tháº­t trong `docs/architecture/architecture-evidence.md`

## Phase 8: Pre-merge Hardening

- [x] T089 [P] Bổ sung PostgreSQL regression tests cho reproduction source thiếu Manifest, Search Run hoặc Candidate và xác nhận rollback toàn graph tại `modules/persistence/src/experimentIntegrationTest/java/com/cryptostrategy/platform/persistence/internal/experiment/SearchReproductionIntegrationTest.java`
- [x] T090 Kiểm tra affected-row count của mọi immutable-source copy trong composite reproduction transaction tại `modules/persistence/src/main/java/com/cryptostrategy/platform/persistence/internal/execution/JdbcSearchExperimentTransaction.java`
- [x] T091 [P] Bổ sung regression tests cho correlation ID server-generated, bounded, deterministic theo injected clock và không rò rỉ `Idempotency-Key` tại API/Worker boundaries
- [x] T092 Thay correlation derivation từ client input bằng server-generated typed-safe value và serialize event bằng contract serializer trong `apps/api`, `modules/experiment-execution` và `modules/persistence`
- [x] T093 Xóa non-durable `SearchCoordinator` runtime path, inject UTC `Clock` vào API/Worker composition roots và sửa tests dùng fixed clock/dependencies đầy đủ
- [x] T094 Xử lý artifact ngoài ownership F-010: giữ pointer feature của main, xác minh checksum F-008 và xóa isolation init script nếu canonical build không dùng
- [x] T095 Chạy canonical Java 21 unit/architecture gates, dọn `git diff --check` và tạo commit cố định cho evidence
- [x] T096 Chạy PostgreSQL/Supabase + Redis recovery suite trên chính commit cố định và cập nhật `quickstart.md` cùng architecture evidence bằng kết quả thật

## Dependencies & Execution Order

### Phase dependencies

- Phase 1 â†’ Phase 2: dependency/contract trÆ°á»›c typed state/persistence foundation.
- Phase 2 block má»i user story.
- US1 lÃ  MVP vÃ  block US2/US3 vÃ¬ cáº§n allocation runtime.
- US2 pháº£i pass trÆ°á»›c gá»¡ public gate vÃ¬ stop/recovery lÃ  release condition.
- US3 phá»¥ thuá»™c US1 atomic graph; cÃ³ thá»ƒ phÃ¡t triá»ƒn test/evidence song song pháº§n US2 sau foundation.
- US4 phá»¥ thuá»™c registry/generator US1 nhÆ°ng khÃ´ng phá»¥ thuá»™c reproduction.
- Polish/gate removal evidence phá»¥ thuá»™c US1â€“US4 tÆ°Æ¡ng á»©ng.

### Story graph

```text
Setup -> Foundation -> US1 Start/Search
                           â”œâ”€â”€> US2 Stop/Recovery â”€â”€â”
                           â”œâ”€â”€> US3 Reproduction â”€â”€â”¼â”€â”€> Polish/Public Ready
                           â””â”€â”€> US4 Replaceability â”˜
```

## Parallel Opportunities

- Phase 1: T003â€“T005.
- Foundation: T007â€“T009 viáº¿t test trÆ°á»›c; T010â€“T013 cÃ³ thá»ƒ triá»ƒn khai song song trÆ°á»›c T014â€“T019.
- US1 tests: T020â€“T027; implementation T028/T029 song song trÆ°á»›c registry/service.
- US2 tests: T044â€“T049; persistence recovery T054 song song consumer T052 sau ports.
- US3 tests: T063â€“T066; verification wiring T071 song song persistence implementation.
- US4 tests T074â€“T076 vÃ  polish docs T080â€“T083 á»Ÿ cÃ¡c file tÃ¡ch biá»‡t.

## Implementation Strategy

### MVP first

1. Setup + Foundation.
2. US1 finite Random Search end-to-end nhÆ°ng giá»¯ public gate náº¿u US2 recovery chÆ°a pass.
3. US2 stop/recovery; sau Ä‘Ã³ Start endpoint Ä‘á»§ Ä‘iá»u kiá»‡n ready.

### Incremental delivery

1. US3 má»Ÿ Reproduce sau evidence riÃªng.
2. US4 chá»©ng minh replaceability.
3. Chá»‰ Ä‘Ã³ng F-009 gate/integration tasks vÃ  architecture evidence khi runtime tests tháº­t pass.

## Format Validation

- Má»i executable task dÃ¹ng `- [ ] Tnnn`.
- Task trong story cÃ³ `[USn]`; setup/foundation/polish khÃ´ng cÃ³ story label.
- `[P]` chá»‰ dÃ¹ng cho file/boundary cÃ³ thá»ƒ lÃ m Ä‘á»™c láº­p.
- Má»i task chá»‰ rÃµ file hoáº·c thÆ° má»¥c Ä‘Ã­ch cá»¥ thá»ƒ.


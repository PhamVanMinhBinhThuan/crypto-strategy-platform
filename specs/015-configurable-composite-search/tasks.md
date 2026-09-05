# Tasks: Configurable Composite Search and Scalable Backtesting (F-015)

**Input**: Design documents from `/specs/015-configurable-composite-search/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

**Tests**: Required by FR-031 and scale evidence required by FR-032.

**Organization**: Tasks are grouped by user story. Contract/regression tests precede the implementation they govern.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel with other marked tasks that touch different files.
- **[Story]**: Maps to the matching prioritized user story in `spec.md`.

## Phase 1: Setup and Architecture Baseline

**Purpose**: Lock the cross-module decision and prepare compatibility/evidence boundaries.

- [X] T001 Register accepted ADR-0017 and its architecture index entry in `docs/adr/0017-composite-search-space-and-refill.md` and `docs/adr/README.md`
- [X] T002 [P] Add F-015 shared UI reference mapping in `docs/ui/features/F-015.md`
- [X] T003 [P] Add architecture dependency/contract guards for composite Search ownership and Worker no-direct-SQL in `architecture-tests/src/test/java/com/cryptostrategy/platform/architecture/CompositeSearchBoundaryTest.java`
- [X] T004 [P] Add forward-only F-015 migration contract test in `modules/persistence/src/test/java/com/cryptostrategy/platform/persistence/internal/search/F015SearchMigrationContractTest.java`

---

## Phase 2: Foundational Composite Search Contracts

**Purpose**: Create versioned models and compatibility mapping that block all user stories.

**⚠️ CRITICAL**: No v2 Start, execution, or UI work begins until these contracts are green.

- [X] T005 Add failing model tests for pool identity, typed domains, component bounds, Majority Vote, canonical ordering, checked cardinality and candidate fingerprinting in `modules/search/src/test/java/com/cryptostrategy/platform/search/api/CompositeSearchModelContractTest.java`
- [X] T006 [P] Add failing deterministic traversal tests for uniqueness, exhaustion, same-seed replay, changed-seed divergence and bounded memory in `modules/search/src/test/java/com/cryptostrategy/platform/search/internal/CompositeRandomStrategyGeneratorTest.java`
- [X] T007 [P] Add failing legacy v1 decode/new v2 encode compatibility tests in `modules/persistence/src/test/java/com/cryptostrategy/platform/persistence/internal/search/SearchDefinitionJsonMapperTest.java`
- [X] T008 Implement versioned strategy-pool, component constraint, combination-policy and composite Search-space models under `modules/search/src/main/java/com/cryptostrategy/platform/search/api/model/`
- [X] T009 Implement immutable composite candidate/component definitions and semantic fingerprint model under `modules/search/src/main/java/com/cryptostrategy/platform/search/api/model/`
- [X] T010 Implement canonical normalization, validation, checked cardinality and indexed candidate resolution in `modules/search/src/main/java/com/cryptostrategy/platform/search/internal/CanonicalCompositeSearchSpace.java`
- [X] T011 Extend generation request/outcome and Random Search for deterministic v2 traversal while retaining v1 behavior in `modules/search/src/main/java/com/cryptostrategy/platform/search/api/model/GenerationRequest.java`, `modules/search/src/main/java/com/cryptostrategy/platform/search/internal/RandomStrategyGenerator.java`, and `modules/search/src/main/java/com/cryptostrategy/platform/search/internal/SearchGenerationService.java`
- [X] T012 Add versioned Search-space/candidate JSON mapping and wire it into Experiment/Search persistence in `modules/persistence/src/main/java/com/cryptostrategy/platform/persistence/internal/search/SearchDefinitionJsonMapper.java`, `modules/persistence/src/main/java/com/cryptostrategy/platform/persistence/internal/search/SearchRows.java`, and `modules/persistence/src/main/java/com/cryptostrategy/platform/persistence/internal/experiment/ExperimentJsonMapper.java`
- [X] T013 Add the forward migration for v2 definition schema/version checks, candidate uniqueness and progress/recovery indexes in `supabase/migrations/20260905000100_f015_composite_search.sql`

**Checkpoint**: Canonical v2 definitions are deterministic, finite, persistable, and backward readable.

---

## Phase 3: User Story 1 - Prepare an Authoritative Frozen Dataset (Priority: P1) 🎯 MVP

**Goal**: Select pair/timeframe/start/end and create or select a frozen provider-backed dataset without raw ID entry.

**Independent Test**: Create `BTC/USDT` `1h` `[start,end)` through the public boundary, list/select it by metadata, and prove Backtest resolution reads the stored checksum/candles without provider access.

### Tests for User Story 1

- [X] T014 [P] [US1] Add dataset list/create range, ownership, idempotency and provider-failure API tests in `apps/api/src/test/java/com/cryptostrategy/platform/api/market/FrozenDatasetApiTest.java`
- [X] T015 [P] [US1] Add persistence tests for owner-scoped deterministic dataset listing in `modules/persistence/src/marketDataIntegrationTest/java/com/cryptostrategy/platform/persistence/marketdata/DatasetListingIntegrationTest.java`
- [X] T016 [P] [US1] Add execution test proving multiple candidates reuse DatasetCandleReader and never invoke MarketDataProvider in `apps/worker/src/test/java/com/cryptostrategy/platform/worker/engine/FrozenDatasetBacktestIsolationTest.java`

### Implementation for User Story 1

- [X] T017 [US1] Publish owner-scoped frozen dataset query model/port from `modules/market-data/src/main/java/com/cryptostrategy/platform/marketdata/api/` and implement it in `modules/persistence/src/main/java/com/cryptostrategy/platform/persistence/internal/marketdata/JdbcDatasetStoreAdapter.java`
- [X] T018 [US1] Add paginated `GET /api/v1/datasets` and complete provenance fields for create/list responses in `apps/api/src/main/java/com/cryptostrategy/platform/api/market/DatasetController.java` and `apps/api/src/main/java/com/cryptostrategy/platform/api/market/MarketDtos.java`
- [X] T019 [US1] Enforce visible half-open UTC range validation and safe provider errors in `apps/api/src/main/java/com/cryptostrategy/platform/api/market/MarketRequestMapper.java` and `apps/api/src/main/java/com/cryptostrategy/platform/api/error/PublicErrorMapper.java`
- [X] T020 [US1] Add dataset discovery/create types and services through the existing F-011 client in `apps/web/src/features/experiments/types/experiment-configuration.ts` and `apps/web/src/features/experiments/service/experiment-command-service.ts`
- [X] T021 [US1] Replace raw Dataset ID/default 30-day creation with pair, timeframe, UTC start/end, create/select controls and provenance card in `apps/web/src/features/experiments/components/ExperimentConfigurationForm.tsx`
- [X] T022 [US1] Add contract-equivalent dataset fixture routes with explicit fixture labeling in `apps/web/src/foundation/composition/development-clients.ts` and `apps/web/src/features/experiments/fixtures/experiment-configuration-fixtures.ts`

**Checkpoint**: A user can create/select an auditable dataset; candidate execution is snapshot-only.

---

## Phase 4: User Story 2 - Configure and Start Composite Search (Priority: P1)

**Goal**: Configure pool, per-strategy domains, component rules, registered Random generator, stop conditions and Top-K and freeze a v2 experiment.

**Independent Test**: Submit a two-strategy one-to-two-component Majority Vote space with multi-value domains and verify distinct deterministic v2 candidates and complete immutable manifest provenance.

### Tests for User Story 2

- [X] T023 [P] [US2] Add v2 request validation/mapping, impossible-space and legacy compatibility API tests in `apps/api/src/test/java/com/cryptostrategy/platform/api/experiment/CompositeSearchRequestMapperTest.java`
- [ ] T024 [P] [US2] Add atomic v2 manifest/run/candidate rollback and replay tests in `modules/persistence/src/experimentIntegrationTest/java/com/cryptostrategy/platform/persistence/internal/experiment/CompositeSearchStartIntegrationTest.java`
- [X] T025 [P] [US2] Add browser form tests for strategy pool, per-strategy domains, policy, generator registry, stop conditions and error preservation in `apps/web/tests/experiments/ExperimentConfigurationForm.test.tsx`

### Implementation for User Story 2

- [X] T026 [US2] Extend Start Search command and graph contracts with v2 Search space, generator selection, requested concurrency and compatibility discriminator in `modules/experiment-execution/src/main/java/com/cryptostrategy/platform/execution/api/`
- [X] T027 [US2] Resolve/freeze exact dataset, pool artifacts, schemas, domains, policy and generator in `modules/experiment-execution/src/main/java/com/cryptostrategy/platform/execution/internal/SearchStartCommandFactoryService.java` and `modules/experiment-execution/src/main/java/com/cryptostrategy/platform/execution/internal/SearchExperimentOrchestrationService.java`
- [X] T028 [US2] Persist the v2 immutable manifest/run graph and canonical configuration fingerprint in `modules/persistence/src/main/java/com/cryptostrategy/platform/persistence/internal/execution/JdbcSearchExperimentTransaction.java`
- [X] T029 [US2] Add registered generator discovery and v2 Start request/response DTO validation in `apps/api/src/main/java/com/cryptostrategy/platform/api/experiment/CommandDtos.java`, `apps/api/src/main/java/com/cryptostrategy/platform/api/experiment/ExperimentRequestMapper.java`, and `apps/api/src/main/java/com/cryptostrategy/platform/api/experiment/ExperimentController.java`
- [X] T030 [US2] Extend configuration state, Zod/public-contract mapping and command payloads for pool/domains/rules/generator/stops/Top-K in `apps/web/src/features/experiments/types/experiment-configuration.ts`, `apps/web/src/features/experiments/hooks/useExperimentConfiguration.ts`, and `apps/web/src/features/experiments/service/experiment-command-service.ts`
- [X] T031 [US2] Implement grouped strategy selection, typed domain editors, component bounds, Majority Vote, registry-backed generator/seed, stop conditions, cardinality preview and review summary in `apps/web/src/features/experiments/components/ExperimentConfigurationForm.tsx`
- [X] T032 [US2] Navigate an accepted command to `/search/{experimentId}` and map server field violations back into the preserved draft in `apps/web/src/features/experiments/hooks/useExperimentCommands.ts` and `apps/web/src/features/experiments/components/SearchView.tsx`

**Checkpoint**: Valid new Search requests create immutable v2 composite experiments; legacy v1 remains readable.

---

## Phase 5: User Story 3 - Execute and Observe Bounded Multi-Candidate Search (Priority: P1)

**Goal**: Refill a bounded execution window through 100–10,000 candidate budgets and expose authoritative progress.

**Independent Test**: With Top-K ten and concurrency four, a 100-candidate run reaches exactly 100 unique terminal outcomes, never exceeds four active candidates, refills after every terminal slot, and survives duplicate delivery/restart.

### Tests for User Story 3

- [ ] T033 [P] [US3] Add unit regression tests proving Top-K does not cap active window and completion/reconciliation decisions trigger refill in `modules/experiment-execution/src/test/java/com/cryptostrategy/platform/execution/internal/SearchCandidateAllocationServiceTest.java` and `apps/worker/src/test/java/com/cryptostrategy/platform/worker/search/SearchCoordinatorRefillTest.java`
- [ ] T034 [P] [US3] Add stop/deadline/exhaustion/duplicate/restart race tests with refill in `apps/worker/src/test/java/com/cryptostrategy/platform/worker/search/SearchRefillRecoveryTest.java`
- [X] T035 [P] [US3] Expand durable finite integration to 100 candidates with active-window and uniqueness assertions in `modules/persistence/src/experimentIntegrationTest/java/com/cryptostrategy/platform/persistence/internal/search/FiniteSearchExperimentIntegrationTest.java`
- [X] T036 [P] [US3] Add 1,000/10,000 opt-in concurrency and bounded-backpressure performance tests in `apps/worker/src/performanceTest/java/com/cryptostrategy/platform/worker/search/F015SearchScalePerformanceTest.java`
- [X] T037 [P] [US3] Add progress DTO/realtime snapshot contract tests for allocated/active/completed/failed/remaining/terminal reason in `apps/api/src/test/java/com/cryptostrategy/platform/api/experiment/SearchProgressApiTest.java`

### Implementation for User Story 3

- [X] T038 [US3] Remove Top-K from target-window calculation and enforce requested/per-experiment/global capacity through durable allocation in `modules/experiment-execution/src/main/java/com/cryptostrategy/platform/execution/internal/SearchCandidateAllocationService.java`
- [X] T039 [US3] Execute bounded refill after trusted completion decisions and keep duplicate/stale completions idempotent in `apps/worker/src/main/java/com/cryptostrategy/platform/worker/search/coordination/SearchCoordinator.java` and `apps/worker/src/main/java/com/cryptostrategy/platform/worker/search/consumer/SearchCompletionConsumer.java`
- [X] T040 [US3] Execute the same bounded refill/repair path from scheduled reconciliation without reopening stopped runs in `apps/worker/src/main/java/com/cryptostrategy/platform/worker/search/reconciliation/SearchReconciler.java`
- [X] T041 [US3] Persist/expose failure-inclusive progress and terminal reason with optimistic version/fence checks in `modules/experiment-execution/src/main/java/com/cryptostrategy/platform/execution/internal/TrustedSearchCoordinationService.java` and `modules/persistence/src/main/java/com/cryptostrategy/platform/persistence/internal/search/JdbcTrustedSearchCoordinationGateway.java`
- [X] T042 [US3] Add authoritative Search progress fields to REST/realtime DTO mapping in `apps/api/src/main/java/com/cryptostrategy/platform/api/experiment/ReadDtos.java`, `apps/api/src/main/java/com/cryptostrategy/platform/api/experiment/ExperimentController.java`, and `apps/api/src/main/java/com/cryptostrategy/platform/api/realtime/RealtimeMessageMapper.java`
- [X] T043 [US3] Replace single-current-candidate/fake-worker presentation with aggregate pipeline and progress counters plus reconnect recovery in `apps/web/src/features/experiments/components/CandidateDiscoveryTimeline.tsx`, `apps/web/src/features/experiments/components/ExperimentStatus.tsx`, and `apps/web/src/features/experiments/hooks/useExperimentMonitor.ts`

**Checkpoint**: Multi-candidate work remains bounded, continuously refilled, recoverable and observable.

---

## Phase 6: User Story 4 - Inspect and Reproduce Composite Leaderboard Results (Priority: P2)

**Goal**: Explain every Top-K composite result and reproduce its frozen evidence.

**Independent Test**: Open a ranked composite row and trace all four metrics, Backtest/candidate identities, exact components/parameters/policy and dataset checksum, then reproduce with matching candidate fingerprints.

### Tests for User Story 4

- [X] T044 [P] [US4] Add enriched leaderboard/candidate-detail REST ownership and compatibility tests in `apps/api/src/test/java/com/cryptostrategy/platform/api/leaderboard/CompositeLeaderboardApiTest.java`
- [X] T045 [P] [US4] Add v2 reproduction candidate-copy/fingerprint evidence tests in `modules/experiment-execution/src/test/java/com/cryptostrategy/platform/execution/internal/CompositeSearchReproductionTest.java`
- [X] T046 [P] [US4] Add UI tests for composite summary, four authoritative metrics, detail/Backtest actions and omitted Sharpe in `apps/web/tests/leaderboard/CompositeLeaderboard.test.tsx`

### Implementation for User Story 4

- [X] T047 [US4] Add candidate/evaluation/result projection query needed for authoritative enriched leaderboard entries in `modules/leaderboard/src/main/java/com/cryptostrategy/platform/leaderboard/api/` and `modules/persistence/src/main/java/com/cryptostrategy/platform/persistence/internal/leaderboard/JdbcLeaderboardStore.java`
- [X] T048 [US4] Publish candidate-detail and enriched leaderboard DTOs without browser-derived metrics in `apps/api/src/main/java/com/cryptostrategy/platform/api/leaderboard/LeaderboardDtos.java`, `apps/api/src/main/java/com/cryptostrategy/platform/api/leaderboard/LeaderboardController.java`, and `apps/api/src/main/java/com/cryptostrategy/platform/api/experiment/ExperimentController.java`
- [X] T049 [US4] Reproduce v2 experiments by copying exact candidate definitions and comparing composite fingerprints while retaining v1 logic in `modules/experiment-execution/src/main/java/com/cryptostrategy/platform/execution/internal/SearchReproductionApplicationService.java` and `modules/experiment-execution/src/main/java/com/cryptostrategy/platform/execution/internal/SearchReproductionVerificationCoordinator.java`
- [X] T050 [US4] Render enriched Top-K rows and authoritative candidate detail/Backtest navigation in `apps/web/src/features/leaderboard/components/LeaderboardTable.tsx`, `apps/web/src/features/leaderboard/types/leaderboard.ts`, and `apps/web/src/features/leaderboard/mappers/leaderboard-mapper.ts`

**Checkpoint**: Ranked v2 candidates are explainable and reproducible without unsupported UI calculations.

---

## Phase 7: Polish, Evidence and Cross-Cutting Validation

**Purpose**: Close contract documentation, responsive states, scale evidence, and repository gates.

- [X] T051 [P] Update released REST/OpenAPI/realtime and architecture data-flow documentation for F-015 in `docs/api/openapi.yaml`, `docs/api/websocket-events.md`, and `docs/architecture/data-flows.md`
- [X] T052 [P] Add full configuration/monitor/leaderboard fixture coverage and loading/empty/error/degraded/stopped/exhausted states in `apps/web/src/foundation/composition/development-clients.ts` and `apps/web/src/features/experiments/fixtures/scenarios.ts`
- [X] T053 Add responsive 360–1440 px keyboard/accessibility Playwright coverage for the F-015 flow in `apps/web/e2e/search-composite.spec.ts`
- [X] T054 Run the 100-candidate durable integration and record real command/configuration/results in `docs/evidence/f015/100-candidate-integration.md`
- [X] T055 Run the 1,000-candidate comparative worker profile and record real environment/throughput/results in `docs/evidence/f015/1000-candidate-performance.md`
- [X] T056 Run the controlled 10,000-candidate bounded-backpressure profile and record real peak active/pending/memory/results in `docs/evidence/f015/10000-candidate-backpressure.md`
- [ ] T057 Run `./gradlew check` and `npm run check` from `apps/web`, fix only F-015 regressions, and record final commands/results in `specs/015-configurable-composite-search/quickstart.md`
- [X] T058 [P] Add smoke contract tests for configurable simulated capital, transaction fee, and slippage in Search API mapping, manifest freezing, and the Search form payload
- [X] T059 Add optional `backtestConfiguration` through the F-015 UI, Start Search API, and immutable manifest while retaining defaults and requiring no database migration

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1** has no dependency and locks governance.
- **Phase 2** depends on Phase 1 and blocks every user story.
- **US1 (Phase 3)** and the backend discovery portion of **US2 (Phase 4)** can proceed after Phase 2; final US2 UI depends on US1's dataset selector.
- **US3 (Phase 5)** depends on Phase 2 for v2 candidates and US2 for accepted runs.
- **US4 (Phase 6)** depends on completed v2 execution/evaluation from US3.
- **Phase 7** depends on every selected user story.

### User Story Dependencies

- **US1**: Independently delivers authoritative dataset selection.
- **US2**: Uses US1's selected dataset and foundational v2 models.
- **US3**: Uses US2's durable Search run/candidates; refill mechanics can be regression-tested independently.
- **US4**: Uses US3 outcomes but candidate/read contracts can be developed in parallel after Phase 2.

### Parallel Opportunities

- T002–T004 touch separate documentation/test files.
- T006–T007 can run while T005 establishes the central model contract.
- Test tasks within each user story are parallel where their source sets do not overlap.
- Dataset backend and fixture work can proceed in parallel with composite-domain work after Phase 2.
- API leaderboard tests, reproduction tests, and UI leaderboard tests can be written in parallel.
- Documentation and scenario-fixture work can proceed in parallel before final measured evidence.

## Parallel Example: User Story 3

```text
Task T033: unit refill and Top-K separation regressions
Task T034: stop/restart/refill race regressions
Task T035: durable 100-candidate integration expansion
Task T036: opt-in 1,000/10,000 scale harness
Task T037: public progress contract tests
```

## Implementation Strategy

### MVP First

1. Complete setup and foundational v2 model phases.
2. Complete US1 so users select a real frozen range without raw IDs.
3. Complete US2 so new experiments freeze a real composite Search configuration.
4. Validate an initial multi-candidate flow before UI polish.

### Incremental Delivery

1. Domain/persistence compatibility baseline.
2. Dataset and configuration workflow.
3. Refill/scaling and authoritative progress.
4. Enriched leaderboard/reproduction.
5. Measured evidence and full gates.

## Notes

- Historical specs/migrations and immutable v1 experiment evidence are never edited.
- `[P]` marks file-independent work, not permission to violate test-first ordering.
- Evidence files must contain actual executed results; never pre-fill success claims.
- Do not stage or modify unrelated existing workspace changes.

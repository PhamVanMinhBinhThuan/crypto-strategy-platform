# Tasks: Sentiment Strategy (F-016)

**Input**: Design documents in `/specs/016-sentiment-strategy/`

**Tests**: Required by the specification for deterministic behavior, anti-look-ahead, provenance,
failure isolation, accessibility and responsive behavior.

## Phase 1: Setup and architecture

- [x] T001 Finalize F-016 plan, research, data model and contracts in `specs/016-sentiment-strategy/`
- [x] T002 Accept the cross-module snapshot/input decision in `docs/adr/0018-sentiment-strategy-snapshot-input.md`
- [x] T003 Update ADR index and module dependency documentation in `docs/adr/README.md` and `docs/adr/0002-module-boundaries.md`

---

## Phase 2: Foundational immutable input contract

**Purpose**: Provide pure frozen supplemental inputs before implementing any Sentiment behavior.

- [x] T004 [P] Add failing validation/canonicalization tests for supplemental inputs in `modules/strategy-core/src/test/java/com/cryptostrategy/platform/strategy/api/model/StrategySupplementalInputTest.java`
- [x] T005 Implement immutable supplemental input/observation models in `modules/strategy-core/src/main/java/com/cryptostrategy/platform/strategy/api/model/`
- [x] T006 Extend `StrategyContext` with backward-compatible supplemental inputs in `modules/strategy-core/src/main/java/com/cryptostrategy/platform/strategy/api/model/StrategyContext.java`
- [x] T007 Update module-boundary tests for the generic input contract in `architecture-tests/src/test/java/`

**Checkpoint**: Technical strategies still run unchanged; contexts can carry verified frozen input.

---

## Phase 3: User Story 1 — Backtest Sentiment Strategy (Priority: P1) 🎯 MVP

**Goal**: Register and execute Sentiment as a deterministic fifth Strategy with explained signals.

**Independent Test**: A fixed Candle/input snapshot yields known BUY/HOLD/SELL decisions, excludes future
News and exposes stable evidence.

### Tests

- [x] T008 [P] [US1] Add plugin descriptor/parameter validation tests in `modules/strategies/src/test/java/com/cryptostrategy/platform/strategies/internal/sentiment/SentimentPolarityPluginTest.java`
- [x] T009 [P] [US1] Add threshold, insufficient-data and zero-weight tests in `modules/strategies/src/test/java/com/cryptostrategy/platform/strategies/internal/sentiment/SentimentPolarityStrategyTest.java`
- [x] T010 [P] [US1] Add future-News, deduplication and deterministic-fingerprint tests in `modules/strategies/src/test/java/com/cryptostrategy/platform/strategies/internal/sentiment/SentimentPolarityStrategyTest.java`

### Implementation

- [x] T011 [US1] Implement Sentiment plugin descriptor and parameter schema in `modules/strategies/src/main/java/com/cryptostrategy/platform/strategies/internal/sentiment/SentimentPolarityPlugin.java`
- [x] T012 [US1] Implement confidence-weighted evaluator and evidence in `modules/strategies/src/main/java/com/cryptostrategy/platform/strategies/internal/sentiment/SentimentPolarityStrategy.java`
- [x] T013 [US1] Register the fifth trusted plugin in `modules/strategies/src/main/java/com/cryptostrategy/platform/strategies/api/StrategyPlugins.java`
- [x] T014 [US1] Add failing rolling-context injection tests in `modules/backtesting/src/test/java/com/cryptostrategy/platform/backtesting/internal/StrategyExecutionSessionTest.java`
- [x] T015 [US1] Add frozen supplemental-input resolver seam and inject it in `modules/backtesting/src/main/java/com/cryptostrategy/platform/backtesting/`
- [x] T016 [US1] Verify the focused Strategy Core, Strategies and Backtesting suites

**Checkpoint**: The Strategy engine can evaluate frozen Sentiment input independently of persistence/UI.

---

## Phase 4: Frozen News snapshot foundation (supports US1/US3)

- [x] T017 [P] [US1] Add snapshot model and canonical-fingerprint tests in `modules/news/src/test/java/com/cryptostrategy/platform/news/`
- [x] T018 [US1] Add immutable snapshot command/model/use-case contracts in `modules/news/src/main/java/com/cryptostrategy/platform/news/api/`
- [x] T019 [US1] Implement asset/release/cutoff/dedup snapshot construction in `modules/news/src/main/java/com/cryptostrategy/platform/news/internal/application/SentimentSnapshotService.java`
- [x] T020 [P] [US1] Add forward-only snapshot schema migration in `supabase/migrations/`
- [x] T021 [US1] Implement News-owned snapshot store/query adapters in `modules/persistence/src/main/java/com/cryptostrategy/platform/persistence/internal/news/`
- [x] T022 [US1] Add PostgreSQL snapshot immutability/ownership integration tests in `modules/persistence/src/newsIntegrationTest/java/`

---

## Phase 5: User Story 2 — Search and Composite (Priority: P2)

**Goal**: Freeze one snapshot before execution and reuse it for Sentiment Single/Composite Candidates.

**Independent Test**: A finite Search combines Sentiment with one technical Strategy and every Candidate
and Result points to the same snapshot/model release.

### Tests

- [x] T023 [P] [US2] Add Composite Sentiment conflict-policy tests in `modules/combination/src/test/java/com/cryptostrategy/platform/combination/internal/`
- [x] T024 [P] [US2] Add Search parameter-domain/catalog tests in `modules/search/src/test/java/com/cryptostrategy/platform/search/`
- [x] T025 [US2] Add experiment preflight/same-snapshot Candidate tests in `modules/experiment-execution/src/test/java/`

### Implementation

- [x] T026 [US2] Add versioned Sentiment snapshot provenance to `modules/experiment/src/main/java/com/cryptostrategy/platform/experiment/api/`
- [x] T027 [US2] Freeze/resolve Sentiment snapshots during start and reproduction in `modules/experiment-execution/src/main/java/`
- [x] T028 [US2] Map frozen inputs in API/Worker composition under `apps/api/src/main/java/` and `apps/worker/src/main/java/`
- [x] T029 [US2] Extend safe error mapping for snapshot preflight in `apps/api/src/main/java/com/cryptostrategy/platform/api/error/PublicErrorMapper.java`
- [x] T030 [US2] Verify finite Search and Composite execution integration suites

**Checkpoint**: Sentiment works in Single/Composite Search without live model calls during execution.

---

## Phase 6: User Story 3 — Explain and reproduce (Priority: P2)

**Goal**: Expose and compare the complete frozen Sentiment evidence chain.

**Independent Test**: Reproduce an accepted Result after active model change and receive MATCHED; mutate
one evidence layer and receive MISMATCHED without altering the original.

- [x] T031 [P] [US3] Add Result/reproduction provenance comparison tests in `modules/experiment-execution/src/test/java/`
- [x] T032 [US3] Persist/map snapshot and decision provenance through Result reads in `modules/persistence/` and `apps/api/src/main/java/com/cryptostrategy/platform/api/backtest/`
- [x] T033 [US3] Extend OpenAPI schemas and parity tests in `docs/api/openapi.yaml` and `apps/api/src/test/java/`
- [x] T034 [US3] Add Result evidence presentation/tests in `apps/web/src/features/backtests/` and `apps/web/tests/`

---

## Phase 7: User Story 4 — Failure isolation (Priority: P3)

**Goal**: Sentiment degradation is explicit but never blocks technical execution or frozen replay.

**Independent Test**: Stop Python Sentiment; technical Backtest and frozen replay pass, while a new run
without a valid snapshot fails safely before execution.

- [x] T035 [P] [US4] Add technical-only and frozen-replay outage tests in `apps/worker/src/test/java/`
- [x] T036 [P] [US4] Add snapshot-preflight failure/no-partial-result tests in `apps/api/src/test/java/`
- [x] T037 [US4] Complete the existing Sentiment availability status API/banner WIP under `apps/api/src/main/java/com/cryptostrategy/platform/api/news/` and `apps/web/src/features/news/`
- [x] T038 [US4] Verify persisted News remains readable and technical flows ignore Sentiment availability

---

## Phase 8: Web workflow and demo integration

- [x] T039 [P] [US1] Add fifth-plugin Composer rendering/parameter tests in `apps/web/tests/strategies/`
- [x] T040 [US1] Present Sentiment overview/parameters/disclaimer in `apps/web/src/features/strategies/`
- [x] T041 [US2] Enable Sentiment Single/Composite Search domains in `apps/web/src/features/experiments/`
- [x] T042 [US1] Add authoritative “Use as Strategy” navigation in `apps/web/src/features/news/`
- [x] T043 [P] Add keyboard/responsive browser coverage at 360/768/1024/1440 in `apps/web/tests/e2e/`

---

## Phase 9: Polish and evidence

- [x] T044 [P] Update architecture/API/demo documentation in `docs/architecture/`, `docs/api/` and `docs/demo/`
- [x] T045 Create F016 evidence index and rubric capture guide in `docs/evidence/f016/README.md`
- [x] T046 Run Java full checks and architecture gates; record exact commit/environment
- [x] T047 Run web lint/typecheck/unit/build/browser gates; record exact commit/environment
- [ ] T048 Run deterministic, anti-look-ahead, failure-isolation and live full-flow evidence from `specs/016-sentiment-strategy/quickstart.md`
  - Automated deterministic, anti-look-ahead and failure-isolation coverage is verified in
    `docs/evidence/f016/verification.md`. Both F016 migrations are now applied and verified on the
    team's PostgreSQL database. The LIVE UI/Search/Result/reproduction capture remains open because
    it requires an authenticated user session and a completed run; fixture evidence is not a substitute.
- [x] T049 Remove or reconcile duplicate restored worktree files only after confirming their canonical equivalents

---

## Dependencies and execution order

- Phase 2 blocks all runtime Sentiment Strategy work.
- Phase 3 is the MVP and blocks snapshot-backed end-to-end execution.
- Phase 4 blocks Search/Composite integration and provenance.
- Phase 5 blocks full reproduction evidence and authoritative web execution.
- Phase 7 can proceed after Phase 4 and must preserve technical-only paths.
- Phase 8 consumes released API contracts; the browser never computes Strategy outcomes.
- Phase 9 follows all selected stories.

## Implementation strategy

1. Deliver the pure fifth plugin and focused tests first.
2. Add immutable News snapshot persistence and orchestration.
3. Reuse that frozen input across standalone Backtest, Search, Composite and reproduction.
4. Expose only authoritative configuration/evidence in the existing web screens.
5. Finish with failure-isolation and reproducibility evidence.

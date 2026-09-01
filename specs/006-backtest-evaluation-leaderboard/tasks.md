# Tasks: Backtest, Evaluation and Leaderboard

**Input**: Design documents from `specs/006-backtest-evaluation-leaderboard/`

**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/`, `quickstart.md`

**Tests**: F-006 requires TDD. Write each listed test first, run it and confirm it fails for the expected missing behavior before implementing the paired production task.

**Organization**: Tasks are grouped by user story. ADR-0013 is `Accepted`; synchronization of ADR-0002, Module View, Gradle declarations and architecture tests remains the governance merge gate.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel because it edits different files and has no dependency on an incomplete task in the same phase
- **[Story]**: Maps to US1–US4 from `spec.md`; Setup, Foundational and Polish tasks have no story label
- Every task includes an exact repository-relative path

## Phase 1: Setup and Architecture Gate

**Purpose**: Approve the proposed architecture change and prepare the existing modules without implementing business behavior.

- [x] T001 Record the explicit project-owner approval, scoped supersession and `Accepted` status in `docs/adr/0013-backtest-execution-integration.md`
- [x] T002 Update the accepted dependency table and scoped supersession reference after T001 in `docs/adr/0002-module-boundaries.md`
- [x] T003 [P] Synchronize the F-006 dependency arrows, allowed-dependency table and next-open execution note after T001 in `docs/architecture/module-view.md`
- [x] T004 Update the production and Gradle dependency matrices after T001 in `architecture-tests/src/test/java/com/cryptostrategy/platform/architecture/ModuleBoundaryTest.java`
- [x] T005 Declare only ADR-0013-approved project dependencies in `modules/backtesting/build.gradle.kts`, `modules/evaluation/build.gradle.kts`, and `modules/leaderboard/build.gradle.kts`
- [x] T006 Configure the isolated `backtestEvaluationLeaderboardIntegrationTest` source set and F-006 capability dependencies in `modules/persistence/build.gradle.kts`
- [x] T007 [P] Add F-006 package ownership descriptions in `modules/backtesting/src/main/java/com/cryptostrategy/platform/backtesting/api/package-info.java`, `modules/evaluation/src/main/java/com/cryptostrategy/platform/evaluation/api/package-info.java`, and `modules/leaderboard/src/main/java/com/cryptostrategy/platform/leaderboard/api/package-info.java`

**Checkpoint**: ADR-0013 is Accepted and the documented/tested module matrix permits only the public edges required by F-006.

---

## Phase 2: Foundational Contracts (Blocking)

**Purpose**: Establish typed IDs, decimal/config contracts, error taxonomy and F-004 materialization extensions shared by all stories.

**⚠️ CRITICAL**: No user-story implementation starts until this phase passes.

### Tests first

- [x] T008 [P] Add failing typed-ID generation/validation/equality tests in `modules/backtesting/src/test/java/com/cryptostrategy/platform/backtesting/api/BacktestingIdentifiersTest.java`
- [x] T009 [P] Add failing Evaluation and Leaderboard typed-ID/version tests in `modules/evaluation/src/test/java/com/cryptostrategy/platform/evaluation/api/EvaluationContractsTest.java` and `modules/leaderboard/src/test/java/com/cryptostrategy/platform/leaderboard/api/LeaderboardContractsTest.java`
- [x] T010 [P] Add failing exact-decimal scale, nonnegative Money/Quantity and `HALF_EVEN` tests in `modules/backtesting/src/test/java/com/cryptostrategy/platform/backtesting/api/TradingValuesTest.java`
- [x] T011 [P] Add failing versioned BacktestAssumptions validation/canonicalization tests in `modules/backtesting/src/test/java/com/cryptostrategy/platform/backtesting/api/BacktestAssumptionsTest.java`
- [x] T012 [P] Add failing parameter-aware MA lookback and frozen-parameter equality tests in `modules/strategy-core/src/test/java/com/cryptostrategy/platform/strategy/api/StrategyLookbackContractTest.java`
- [x] T013 [P] Add failing public Single/Composite materialization tests with ordered components in `modules/combination/src/test/java/com/cryptostrategy/platform/combination/api/CompositeStrategyMaterializerTest.java`
- [x] T014 [P] Add failing compatibility test proving F-006 imports all five existing F-005 `FailureClassification` values and declares no replacement enum in `modules/backtesting/src/test/java/com/cryptostrategy/platform/backtesting/api/FailureClassificationCompatibilityTest.java`

### Contract implementation

- [x] T015 [P] Implement typed `BacktestResultId` and `TradeId` using shared `UlidIdentifier`/`Ulids` in `modules/backtesting/src/main/java/com/cryptostrategy/platform/backtesting/api/BacktestResultId.java` and `modules/backtesting/src/main/java/com/cryptostrategy/platform/backtesting/api/TradeId.java`
- [x] T016 [P] Implement typed `EvaluationResultId`, `MetricVersion`, and `RankingVersion` in `modules/evaluation/src/main/java/com/cryptostrategy/platform/evaluation/api/EvaluationResultId.java`, `modules/evaluation/src/main/java/com/cryptostrategy/platform/evaluation/api/MetricVersion.java`, and `modules/evaluation/src/main/java/com/cryptostrategy/platform/evaluation/api/RankingVersion.java`
- [x] T017 [P] Implement typed `LeaderboardRevisionId` in `modules/leaderboard/src/main/java/com/cryptostrategy/platform/leaderboard/api/LeaderboardRevisionId.java`
- [x] T018 Implement exact-decimal and execution values in `modules/backtesting/src/main/java/com/cryptostrategy/platform/backtesting/api/model/Money.java`, `modules/backtesting/src/main/java/com/cryptostrategy/platform/backtesting/api/model/Quantity.java`, `modules/backtesting/src/main/java/com/cryptostrategy/platform/backtesting/api/model/Rate.java`, `modules/backtesting/src/main/java/com/cryptostrategy/platform/backtesting/api/model/PositionSide.java`, `modules/backtesting/src/main/java/com/cryptostrategy/platform/backtesting/api/model/PositionMode.java`, `modules/backtesting/src/main/java/com/cryptostrategy/platform/backtesting/api/model/ExecutionRule.java`, `modules/backtesting/src/main/java/com/cryptostrategy/platform/backtesting/api/model/InvalidSignalPolicy.java`, and `modules/backtesting/src/main/java/com/cryptostrategy/platform/backtesting/api/model/EndOfDatasetPolicy.java`
- [x] T019 Implement immutable `BacktestAssumptions` contract `backtest-assumptions-v1` with scale/range validation in `modules/backtesting/src/main/java/com/cryptostrategy/platform/backtesting/api/model/BacktestAssumptions.java`
- [x] T020 [P] Implement stable F-006 domain error codes and exception types in `modules/backtesting/src/main/java/com/cryptostrategy/platform/backtesting/api/error/BacktestErrorCode.java` and `modules/backtesting/src/main/java/com/cryptostrategy/platform/backtesting/api/error/BacktestException.java`
- [x] T021 Extend the public Strategy plugin/registry contract with parameter-aware lookback resolution in `modules/strategy-core/src/main/java/com/cryptostrategy/platform/strategy/api/StrategyPlugin.java` and `modules/strategy-core/src/main/java/com/cryptostrategy/platform/strategy/api/port/in/StrategyRegistry.java`
- [x] T022 Implement parameter-aware lookback in the registry and MA plugin without exposing internals in `modules/strategy-core/src/main/java/com/cryptostrategy/platform/strategy/internal/registry/DefaultStrategyRegistry.java` and `modules/strategies/src/main/java/com/cryptostrategy/platform/strategies/internal/ma/MovingAverageCrossoverPlugin.java`
- [x] T023 Implement the public Composite materialization boundary that returns `Strategy` and preserves ordered frozen components in `modules/combination/src/main/java/com/cryptostrategy/platform/combination/api/CompositeStrategyMaterializer.java` and `modules/combination/src/main/java/com/cryptostrategy/platform/combination/internal/DefaultCompositeStrategyMaterializer.java`
- [x] T024 Add a typed Manifest-to-F-006 config parser that rejects hidden/default values in `modules/backtesting/src/main/java/com/cryptostrategy/platform/backtesting/api/BacktestConfigurationParser.java` and `modules/backtesting/src/test/java/com/cryptostrategy/platform/backtesting/api/BacktestConfigurationParserTest.java`

**Checkpoint**: Shared contracts compile, their tests pass, and no F-006 type duplicates Candle, Dataset, Strategy, Experiment identity or FailureClassification.

---

## Phase 3: User Story 1 — Deterministic Historical Backtest (Priority: P1) 🎯 MVP

**Goal**: Stream a frozen Dataset, execute a frozen Single/Composite Strategy without look-ahead bias, and atomically accept one immutable Result/Trade sequence for the correct successful Attempt.

**Independent Test**: Run a multi-batch fixture with a deterministic Strategy and valid successful Attempt; verify exact Trades, capital and `backtest-v1`, then repeat with a different batch size and obtain identical output.

### Tests for User Story 1 — write and confirm failure first

- [x] T025 [P] [US1] Add failing immutability and invariant tests for Position, Trade and BacktestResult in `modules/backtesting/src/test/java/com/cryptostrategy/platform/backtesting/api/model/BacktestDomainModelTest.java`
- [x] T026 [P] [US1] Add failing BUY/SELL/HOLD, one-LONG, ignored-invalid-signal and forced-final-close tests in `modules/backtesting/src/test/java/com/cryptostrategy/platform/backtesting/internal/ExecutionStateMachineTest.java`
- [x] T027 [P] [US1] Add failing next-open, adverse slippage, exact `cash / (fill × (1 + feeRate))` quantity, non-negative cash, two-sided fee, forced-close SELL slippage/fee and no-look-ahead tests in `modules/backtesting/src/test/java/com/cryptostrategy/platform/backtesting/internal/TradeExecutionPolicyTest.java`
- [x] T028 [P] [US1] Add failing CandleBatch interoperability tests for sequence zero, contiguous progress, requested dataset identity, `hasMore`, missing/duplicate/out-of-order Candle, bounded batch size and instrumented maximum retained Candle count over a large multi-batch fixture in `modules/backtesting/src/test/java/com/cryptostrategy/platform/backtesting/internal/DatasetBatchCursorContractTest.java`
- [x] T029 [P] [US1] Add failing StrategyContext/StrategyDecision tests for closed rolling windows, evaluation time, frozen reference equality and dynamic lookback in `modules/backtesting/src/test/java/com/cryptostrategy/platform/backtesting/internal/StrategyExecutionInteropTest.java`
- [x] T030 [P] [US1] Add failing DatasetSnapshot/provenance/count/checksum validation and fail-fast/no-partial-output tests in `modules/backtesting/src/test/java/com/cryptostrategy/platform/backtesting/internal/BacktestInputValidatorTest.java`
- [x] T031 [P] [US1] Add failing `equity-curve-v1` incremental digest and `backtest-v1` canonical serialization tests for ordered Trades, EquityCurveSummary/digest, scale normalization, excluded runtime fields and changed-input sensitivity in `modules/backtesting/src/test/java/com/cryptostrategy/platform/backtesting/internal/BacktestFingerprintV1Test.java`
- [x] T032 [P] [US1] Add failing application tests for BACKTEST Job, matching Experiment/Candidate/Attempt, `SUCCEEDED` requirement, ownership and duplicate Candidate outcome in `modules/backtesting/src/test/java/com/cryptostrategy/platform/backtesting/internal/RunBacktestServiceTest.java`

### Implementation for User Story 1

- [x] T033 [P] [US1] Implement immutable Position and execution-state transitions in `modules/backtesting/src/main/java/com/cryptostrategy/platform/backtesting/internal/Position.java` and `modules/backtesting/src/main/java/com/cryptostrategy/platform/backtesting/internal/ExecutionStateMachine.java`
- [x] T034 [P] [US1] Implement immutable Trade with zero-based sequence, entry/exit fees and canonical decimals in `modules/backtesting/src/main/java/com/cryptostrategy/platform/backtesting/api/model/Trade.java`
- [x] T035 [P] [US1] Implement immutable BacktestResult with complete typed lineage, assumptions/provenance, ordered Trades and streaming `EquityCurveSummary` with `equity-curve-v1` digest in `modules/backtesting/src/main/java/com/cryptostrategy/platform/backtesting/api/model/BacktestResult.java` and `modules/backtesting/src/main/java/com/cryptostrategy/platform/backtesting/api/model/EquityCurveSummary.java`
- [x] T036 [US1] Implement next-open execution, pending-decision handling, adverse slippage, fee and forced close in `modules/backtesting/src/main/java/com/cryptostrategy/platform/backtesting/internal/TradeExecutionPolicy.java`
- [x] T037 [US1] Implement streaming `DatasetCandleReader` cursor and O(batch + lookback) structural guards in `modules/backtesting/src/main/java/com/cryptostrategy/platform/backtesting/internal/DatasetBatchCursor.java`
- [x] T038 [US1] Implement frozen Dataset/Manifest/Strategy/lineage validation with stable error codes in `modules/backtesting/src/main/java/com/cryptostrategy/platform/backtesting/internal/BacktestInputValidator.java`
- [x] T039 [US1] Implement Single/Composite Strategy resolution, exact parameter comparison and rolling `StrategyContext` construction in `modules/backtesting/src/main/java/com/cryptostrategy/platform/backtesting/internal/StrategyExecutionSession.java`
- [x] T040 [US1] Implement deterministic Backtest engine orchestration with online EquityCurveSummary/digest calculation and without materializing the Dataset or full equity curve in `modules/backtesting/src/main/java/com/cryptostrategy/platform/backtesting/internal/DeterministicBacktestEngine.java`
- [x] T041 [US1] Implement canonical `backtest-v1` encoder and SHA-256 calculator in `modules/backtesting/src/main/java/com/cryptostrategy/platform/backtesting/internal/fingerprint/BacktestFingerprintV1.java`
- [x] T042 [US1] Define `RunBacktestUseCase`, `BacktestResultStore`, and Result lookup ports in `modules/backtesting/src/main/java/com/cryptostrategy/platform/backtesting/api/port/in/RunBacktestUseCase.java`, `modules/backtesting/src/main/java/com/cryptostrategy/platform/backtesting/api/port/out/BacktestResultStore.java`, and `modules/backtesting/src/main/java/com/cryptostrategy/platform/backtesting/api/port/out/BacktestResultReader.java`
- [x] T043 [US1] Implement the application service that verifies F-005 lineage, runs the engine and performs idempotent atomic acceptance in `modules/backtesting/src/main/java/com/cryptostrategy/platform/backtesting/internal/RunBacktestService.java`
- [x] T044 [US1] Add failing SQL assertions for successful Attempt, cross-lineage rejection, Result/Trade uniqueness, explicit entry/exit/total fee constraints, migration rejection when an unsplittable legacy Trade row exists, EquityCurveSummary/digest, assumptions/fingerprint fields and immutability in `supabase/tests/database/004_backtest_evaluation_leaderboard_test.sql`
- [x] T045 [US1] Create the forward-only F-006 schema migration after `20260831000100`; abort with an explicit diagnostic if any legacy Trade row exists because total `fee` cannot be split safely, otherwise add non-null `entry_fee`/`exit_fee`, preserve constrained total `fee = entry_fee + exit_fee`, and store EquityCurveSummary/digest in `supabase/migrations/20260901000100_f006_backtest_evaluation_leaderboard.sql`
- [x] T046 [US1] Add failing JDBC integration tests for atomic Result/Trade insert, read-back, rollback and Candidate idempotency in `modules/persistence/src/backtestEvaluationLeaderboardIntegrationTest/java/com/cryptostrategy/platform/persistence/backtesting/JdbcBacktestResultStoreIntegrationTest.java`
- [x] T047 [US1] Implement JDBC Result/Trade adapter and transaction boundary without business policy in `modules/persistence/src/main/java/com/cryptostrategy/platform/persistence/internal/backtesting/JdbcBacktestResultStore.java` and `modules/persistence/src/main/java/com/cryptostrategy/platform/persistence/api/BacktestingPersistenceFactory.java`

**Checkpoint**: US1 independently produces and persists one deterministic Result for a valid Candidate/Attempt and rejects every invalid/partial path.

---

## Phase 4: User Story 2 — Versioned Evaluation (Priority: P2)

**Goal**: Calculate and persist the four required metrics, normalized score, eligibility and `evaluation-v1` deterministically.

**Independent Test**: Evaluate known Trade/EquityCurveSummary fixtures, including zero Trades and invalid zero capital; compare all formulas, scale, score, eligibility and fingerprint with expected values.

### Tests for User Story 2 — write and confirm failure first

- [x] T048 [P] [US2] Add failing formula tests for Total Return, net-P&L-positive Win Rate with break-even excluded, EquityCurveSummary-based Maximum Drawdown and Number of Trades in `modules/evaluation/src/test/java/com/cryptostrategy/platform/evaluation/internal/MetricCalculatorTest.java`
- [x] T049 [P] [US2] Add failing zero-Trade, zero-win, zero-initial-capital, negative/overflow and rounding edge tests in `modules/evaluation/src/test/java/com/cryptostrategy/platform/evaluation/internal/EvaluationEdgeCasesTest.java`
- [x] T050 [P] [US2] Add failing clamp normalization, 45/30/25 score, five-Trade eligibility and version tests in `modules/evaluation/src/test/java/com/cryptostrategy/platform/evaluation/internal/DeterministicEvaluatorTest.java`
- [x] T051 [P] [US2] Add failing `evaluation-v1` canonical serialization and changed Result/metric/ranking version tests in `modules/evaluation/src/test/java/com/cryptostrategy/platform/evaluation/internal/EvaluationFingerprintV1Test.java`
- [x] T052 [P] [US2] Add failing JDBC integration tests for same-Experiment linkage, `(result, metric, ranking)` idempotency, read-back and immutability in `modules/persistence/src/backtestEvaluationLeaderboardIntegrationTest/java/com/cryptostrategy/platform/persistence/evaluation/JdbcEvaluationResultStoreIntegrationTest.java`

### Implementation for User Story 2

- [x] T053 [P] [US2] Implement immutable EvaluationResult and normalized metric value objects in `modules/evaluation/src/main/java/com/cryptostrategy/platform/evaluation/api/model/EvaluationResult.java` and `modules/evaluation/src/main/java/com/cryptostrategy/platform/evaluation/api/model/NormalizedMetrics.java`
- [x] T054 [US2] Implement the four deterministic metric formulas, deriving Maximum Drawdown from immutable EquityCurveSummary evidence without rereading Dataset or retaining an equity curve in `modules/evaluation/src/main/java/com/cryptostrategy/platform/evaluation/internal/MetricCalculator.java`
- [x] T055 [US2] Implement fixed normalization, overall score and five-Trade eligibility in `modules/evaluation/src/main/java/com/cryptostrategy/platform/evaluation/internal/DeterministicEvaluator.java`
- [x] T056 [US2] Implement canonical `evaluation-v1` encoder/fingerprint in `modules/evaluation/src/main/java/com/cryptostrategy/platform/evaluation/internal/fingerprint/EvaluationFingerprintV1.java`
- [x] T057 [US2] Define evaluation input/output ports and service contract in `modules/evaluation/src/main/java/com/cryptostrategy/platform/evaluation/api/port/in/EvaluateBacktestUseCase.java`, `modules/evaluation/src/main/java/com/cryptostrategy/platform/evaluation/api/port/out/EvaluationResultStore.java`, and `modules/evaluation/src/main/java/com/cryptostrategy/platform/evaluation/internal/EvaluateBacktestService.java`
- [x] T058 [US2] Implement JDBC Evaluation adapter and factory composition in `modules/persistence/src/main/java/com/cryptostrategy/platform/persistence/internal/evaluation/JdbcEvaluationResultStore.java` and `modules/persistence/src/main/java/com/cryptostrategy/platform/persistence/api/EvaluationPersistenceFactory.java`
- [x] T059 [US2] Extend F-006 SQL verification for Evaluation versions, same-Experiment FK, score ranges and immutable updates/deletes in `supabase/tests/database/004_backtest_evaluation_leaderboard_test.sql`

**Checkpoint**: US2 independently evaluates an accepted Result with exact deterministic metrics and persists each metric/ranking version safely.

---

## Phase 5: User Story 3 — Immutable Top-10 Leaderboard (Priority: P3)

**Goal**: Create immutable, same-Experiment Top-10 revisions with stable ordering and no duplicate entries/revisions.

**Independent Test**: Project a fixed Evaluation set multiple times in different input orders and confirm identical members, ranks, tie-break evidence and `leaderboard-v1`; then change one eligible Evaluation and confirm one new revision.

### Tests for User Story 3 — write and confirm failure first

- [x] T060 [P] [US3] Add failing immutable Revision/Entry, contiguous rank, Top-10 and same-Experiment invariant tests in `modules/leaderboard/src/test/java/com/cryptostrategy/platform/leaderboard/api/model/LeaderboardModelTest.java`
- [x] T061 [P] [US3] Add failing score-desc/drawdown-asc/fingerprint-asc ordering tests with shuffled input in `modules/leaderboard/src/test/java/com/cryptostrategy/platform/leaderboard/internal/TopKProjectorTest.java`
- [x] T062 [P] [US3] Add failing ineligible/duplicate/cross-Experiment rejection and unchanged-content revision reuse tests in `modules/leaderboard/src/test/java/com/cryptostrategy/platform/leaderboard/internal/LeaderboardServiceTest.java`
- [x] T063 [P] [US3] Add failing `leaderboard-v1` canonical ordered-entry and changed-ranking-version tests in `modules/leaderboard/src/test/java/com/cryptostrategy/platform/leaderboard/internal/LeaderboardFingerprintV1Test.java`
- [x] T064 [P] [US3] Add failing JDBC concurrency/read-back tests for revision numbering, atomic entries, duplicate content and immutability in `modules/persistence/src/backtestEvaluationLeaderboardIntegrationTest/java/com/cryptostrategy/platform/persistence/leaderboard/JdbcLeaderboardStoreIntegrationTest.java`

### Implementation for User Story 3

- [x] T065 [P] [US3] Implement immutable LeaderboardRevision and LeaderboardEntry models in `modules/leaderboard/src/main/java/com/cryptostrategy/platform/leaderboard/api/model/LeaderboardRevision.java` and `modules/leaderboard/src/main/java/com/cryptostrategy/platform/leaderboard/api/model/LeaderboardEntry.java`
- [x] T066 [US3] Implement deterministic Top-10 filtering, sorting, tie-break and contiguous rank assignment in `modules/leaderboard/src/main/java/com/cryptostrategy/platform/leaderboard/internal/TopKProjector.java`
- [x] T067 [US3] Implement canonical `leaderboard-v1` encoder/fingerprint in `modules/leaderboard/src/main/java/com/cryptostrategy/platform/leaderboard/internal/fingerprint/LeaderboardFingerprintV1.java`
- [x] T068 [US3] Define projection/query/store ports and revision service in `modules/leaderboard/src/main/java/com/cryptostrategy/platform/leaderboard/api/port/in/ProjectLeaderboardUseCase.java`, `modules/leaderboard/src/main/java/com/cryptostrategy/platform/leaderboard/api/port/out/LeaderboardStore.java`, and `modules/leaderboard/src/main/java/com/cryptostrategy/platform/leaderboard/internal/LeaderboardService.java`
- [x] T069 [US3] Implement JDBC atomic Revision/Entry adapter with concurrency-safe next revision and unchanged-content reuse in `modules/persistence/src/main/java/com/cryptostrategy/platform/persistence/internal/leaderboard/JdbcLeaderboardStore.java` and `modules/persistence/src/main/java/com/cryptostrategy/platform/persistence/api/LeaderboardPersistenceFactory.java`
- [x] T070 [US3] Extend F-006 SQL verification for Top K, rank, same-Experiment Evaluation, duplicate entry/revision and immutable updates/deletes in `supabase/tests/database/004_backtest_evaluation_leaderboard_test.sql`

**Checkpoint**: US3 independently creates stable Top-10 revisions and never mutates or duplicates an existing revision.

---

## Phase 6: User Story 4 — Reproduce and Verify Results (Priority: P4)

**Goal**: Reload frozen evidence, create a linked reproduction run, and report exact match/mismatch without overwriting the original.

**Independent Test**: Persist and reload the original graph, rerun it with different batch sizes, verify identical Trades/metrics/fingerprints, then change one input/version and verify a structured mismatch.

### Tests for User Story 4 — write and confirm failure first

- [x] T071 [P] [US4] Add failing cross-batch-size reproduction tests for identical Trade sequence and `backtest-v1` in `modules/backtesting/src/test/java/com/cryptostrategy/platform/backtesting/internal/BacktestReproductionTest.java`
- [x] T072 [P] [US4] Add failing end-to-end metric/fingerprint reproduction and single-input/version mutation tests in `modules/evaluation/src/test/java/com/cryptostrategy/platform/evaluation/internal/EvaluationReproductionTest.java`
- [x] T073 [P] [US4] Add failing Leaderboard ordered-content reproduction test independent of input/DB order in `modules/leaderboard/src/test/java/com/cryptostrategy/platform/leaderboard/internal/LeaderboardReproductionTest.java`
- [x] T074 [P] [US4] Add failing canonical JSON serialize/store/read-back tests for assumptions and hierarchical fingerprints in `modules/persistence/src/test/java/com/cryptostrategy/platform/persistence/internal/backtesting/BacktestJsonMapperTest.java`
- [x] T075 [P] [US4] Add failing persistence integration test proving the Experiment-owned linked Reproduction Run does not overwrite original evidence, capability reports have no competing owner, and mismatch rolls back only the reproduction success claim in `modules/persistence/src/backtestEvaluationLeaderboardIntegrationTest/java/com/cryptostrategy/platform/persistence/reproduction/ReproductionPersistenceIntegrationTest.java`

### Implementation for User Story 4

- [x] T076 [P] [US4] Implement immutable non-durable BacktestVerificationReport with exact Trade/equity-summary/fingerprint differences in `modules/backtesting/src/main/java/com/cryptostrategy/platform/backtesting/api/model/BacktestVerificationReport.java`
- [x] T077 [US4] Implement Backtest reproduction comparison and link validation in `modules/backtesting/src/main/java/com/cryptostrategy/platform/backtesting/internal/BacktestReproductionVerifier.java`
- [x] T078 [US4] Implement Evaluation/Leaderboard hierarchical comparison without runtime-field influence in `modules/evaluation/src/main/java/com/cryptostrategy/platform/evaluation/internal/EvaluationReproductionVerifier.java` and `modules/leaderboard/src/main/java/com/cryptostrategy/platform/leaderboard/internal/LeaderboardReproductionVerifier.java`
- [x] T079 [US4] Implement canonical assumptions/result JSON mapping in `modules/persistence/src/main/java/com/cryptostrategy/platform/persistence/internal/backtesting/BacktestJsonMapper.java`
- [x] T080 [US4] Integrate the Experiment-owned F-005 `ReproduceExperimentUseCase`/Reproduction Run with F-006 immutable verification reports through public boundaries, without creating a second durable run owner, in `modules/backtesting/src/main/java/com/cryptostrategy/platform/backtesting/api/port/in/ReproduceBacktestUseCase.java` and `modules/backtesting/src/main/java/com/cryptostrategy/platform/backtesting/internal/ReproduceBacktestService.java`
- [x] T081 [US4] Extend JDBC read ports/adapters to load the immutable original Result/Trades/Evaluation/Revision graph in `modules/persistence/src/main/java/com/cryptostrategy/platform/persistence/internal/backtesting/JdbcBacktestEvidenceReader.java`

**Checkpoint**: US4 independently proves exact reproduction or reports a structured mismatch while preserving original evidence.

---

## Phase 7: Architecture, Database Documentation and Verification

**Purpose**: Enforce cross-cutting boundaries and collect only real evidence.

- [x] T082 [P] Add F-006 ArchUnit purity tests forbidding Spring, JDBC, PostgreSQL, Redis, Binance, WebSocket, Controller and UI dependencies in `architecture-tests/src/test/java/com/cryptostrategy/platform/architecture/BacktestEvaluationLeaderboardArchitectureTest.java`
- [x] T083 [P] Extend internal-package, persistence-port, reverse-dependency and cycle coverage for F-006 in `architecture-tests/src/test/java/com/cryptostrategy/platform/architecture/PurityAndCycleTest.java`
- [x] T084 [P] Add an apps/api boundary test preventing imports from F-006 `internal` packages in `architecture-tests/src/test/java/com/cryptostrategy/platform/architecture/ApplicationBoundaryTest.java`
- [x] T085 Update F-006 table columns, versions, fingerprints, ownership and immutability rules in `docs/database/data-dictionary.md`
- [x] T086 [P] Update Result/Evaluation/Leaderboard relationships and new constraints in `docs/database/erd.md`
- [x] T087 [P] Update the architecture evidence entries for F-006 as `Planned` until commands actually pass in `docs/architecture/architecture-evidence.md`
- [x] T088 Run focused unit/contract tests and record real command/result/environment evidence in `specs/006-backtest-evaluation-leaderboard/verification.md`
- [x] T089 Run SQL verification and JDBC integration tests only against an isolated local/test database and record real evidence or the environment blocker in `specs/006-backtest-evaluation-leaderboard/verification.md`
- [x] T090 Run `:architecture-tests:test` and Gradle Wrapper `clean check --no-daemon --max-workers=1 --no-configuration-cache`, recording real evidence in `specs/006-backtest-evaluation-leaderboard/verification.md`
- [x] T091 Run `git diff --check` and scope scan for Worker, Redis Streams, Search, REST, WebSocket and UI implementation; record results in `specs/006-backtest-evaluation-leaderboard/verification.md`
- [x] T092 Run `npx supabase db push --dry-run` only to inspect the linked target/migration list, record output without remote apply, and stop on any unexpected/shared target in `specs/006-backtest-evaluation-leaderboard/verification.md`
- [x] T093 Review `git status --short` and exclude unrelated `docs/thesis.pdf` and `docs/thesis_text.txt` from the F-006 change set in `specs/006-backtest-evaluation-leaderboard/verification.md`

---

## Dependencies & Execution Order

### Phase dependencies

- **Phase 1 Setup**: T001 records the completed ADR-0013 acceptance; T002–T005 synchronize the accepted decision and remain merge prerequisites. T006–T007 may proceed, but dependent code must not merge before T002–T005 pass.
- **Phase 2 Foundation**: Depends on approved dependency/build structure from Phase 1 and blocks all stories.
- **US1 (Phase 3)**: Depends on Foundation; delivers the MVP Backtest outcome.
- **US2 (Phase 4)**: Depends on immutable Backtest Result contract from US1, but its formula/model tests can begin after Foundation using fixtures.
- **US3 (Phase 5)**: Depends on Evaluation Result contract from US2, but projection/model tests can begin with fixtures after Foundation.
- **US4 (Phase 6)**: Depends on US1–US3 outputs and persistence readers.
- **Phase 7**: Architecture tasks may start after module APIs stabilize; full verification depends on all selected stories.

### User story graph

```text
Setup/ADR → Foundation → US1 Backtest → US2 Evaluation → US3 Leaderboard
                              └──────────────┬──────────────┘
                                             ▼
                                      US4 Reproduction
```

### Within each story

1. Add tests and confirm the expected failure.
2. Implement immutable models/value objects.
3. Implement deterministic policy/service.
4. Implement public ports.
5. Add persistence tests before JDBC adapters/schema completion.
6. Run the story checkpoint independently.

### Parallel opportunities

- Foundation ID, decimal, lookback, Composite and failure-compatibility test tasks T008–T014 are parallel.
- US1 tests T025–T032 are parallel; models T033–T035 are parallel after their tests.
- US2 tests T048–T052 are parallel; model/fingerprint work can proceed separately from formulas.
- US3 tests T060–T064 are parallel; model and fingerprint implementation can proceed separately.
- US4 tests T071–T075 are parallel after prerequisite story contracts exist.
- Architecture/documentation tasks T082–T087 are parallel after APIs/schema stabilize.

---

## Parallel Examples

### User Story 1

```text
T026 Execution state-machine tests
T027 Execution price/fee/slippage tests
T028 Dataset batch contract tests
T029 Strategy interoperability tests
T030 Input integrity tests
T031 Fingerprint tests
T032 Application lineage tests
```

### User Story 2

```text
T048 Metric formula tests
T049 Edge-case tests
T050 Normalization/score tests
T051 Evaluation fingerprint tests
T052 Evaluation persistence tests
```

### User Story 3

```text
T060 Leaderboard model tests
T061 Top-K/tie-break tests
T062 revision/idempotency tests
T063 Leaderboard fingerprint tests
T064 Leaderboard persistence tests
```

### User Story 4

```text
T071 Backtest cross-batch reproduction
T072 Evaluation reproduction
T073 Leaderboard reproduction
T074 Serialization/read-back
T075 Reproduction persistence

## Phase 9: Review remediation F1–F10

- [x] T094 [US1] Replace caller-supplied executable Backtest inputs with an identity-only command and frozen execution query in `modules/backtesting/src/main/java/com/cryptostrategy/platform/backtesting/api/model/BacktestRunCommand.java` and `modules/experiment/src/main/java/com/cryptostrategy/platform/experiment/api/execution/FrozenBacktestExecution.java`
- [x] T095 [US1] Resolve exact Strategy version, parameters, fingerprint and lookback behind `FrozenStrategyResolver` in `modules/experiment-execution/src/main/java/com/cryptostrategy/platform/execution/internal/RegistryFrozenStrategyResolver.java`
- [x] T096 [US4] Replace caller comparison with Experiment-owned reproduction orchestration in `modules/experiment-execution/src/main/java/com/cryptostrategy/platform/execution/internal/ReproduceExperimentExecutionService.java`
- [x] T097 [US4] Add immutable reproduction audit migration in `supabase/migrations/20260901000200_f006_review_remediation.sql`
- [x] T098 [US1] Return canonical persisted Backtest and Evaluation aggregates during idempotent/concurrent retries in `modules/persistence/src/main/java/com/cryptostrategy/platform/persistence/internal/`
- [x] T099 [US1] Enforce the complete MVP assumptions contract at the domain constructor in `modules/backtesting/src/main/java/com/cryptostrategy/platform/backtesting/api/model/BacktestAssumptions.java`
- [x] T100 [US3] Add database negative-behavior tests for lineage, Top-K snapshots and evidence immutability in `supabase/tests/database/005_f006_review_remediation_test.sql`
- [x] T101 [US4] Add and pass exact Backtest, Evaluation, Leaderboard and durable reproduction tests from T071–T075
- [x] T102 Run focused module and architecture tests for review remediation
- [x] T103 Run Java 21 Gradle clean check
- [x] T104 Run migration, SQL and JDBC verification against Supabase test project `qdcefzikpakdmunyenem`
- [x] T105 Synchronize ADR/spec/evidence/roadmap and pass `git diff --check main...HEAD` — PASSED (exit 0)
```

---

## Implementation Strategy

### MVP first

1. Complete Setup and verify the accepted ADR-0013 is synchronized by T002–T005.
2. Complete Foundational contracts.
3. Complete US1 only.
4. Stop and validate streaming, next-open execution, Result lineage/idempotency and `backtest-v1`.
5. Do not claim Evaluation/Leaderboard/Reproduction complete until their phases pass.

### Incremental delivery

1. US1: deterministic Backtest and durable Result.
2. US2: versioned Evaluation from an accepted Result.
3. US3: immutable Top-10 projection.
4. US4: end-to-end reproduction evidence.
5. Final architecture/database/regression verification.

### Team strategy

- One owner controls shared API/schema changes and migration ordering.
- After Foundation, Backtesting and Evaluation tests may proceed in parallel using immutable fixtures.
- Leaderboard begins against the stable Evaluation contract.
- Persistence adapters are integrated only after owner ports/tests exist.
- No task authorizes remote Supabase apply.

## Notes

- `[P]` means different files and no incomplete dependency, not permission to merge incompatible contracts.
- Never import another module's `internal` package or duplicate F-003/F-004/F-005 models.
- Do not mark verification tasks complete when environment/tooling blocks them; record the real blocker.
- Do not edit an applied migration; use only the new forward migration.
- Do not add Worker, Redis Streams, retry/dead-letter, Search, REST, WebSocket or UI implementation.

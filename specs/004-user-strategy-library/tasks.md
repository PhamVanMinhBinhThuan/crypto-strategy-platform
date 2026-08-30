# Tasks: Strategy Registry and User Strategy Library

**Input**: Design documents from `specs/004-user-strategy-library/`

**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/`, `quickstart.md`

**Tests**: F-004 explicitly requires TDD, unit tests, contract tests, authorization tests, PostgreSQL integration tests, and architecture tests. Test tasks must be written and observed failing for the intended reason before their paired implementation task.

**Organization**: Tasks are grouped by user story. Setup and Foundational phases contain only work shared by every story; story tasks use `[US1]`–`[US4]` labels and exact repository paths.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel because it targets different files and has no dependency on another incomplete task in the same phase.
- **[Story]**: Maps the task to a user story in `spec.md`.
- No task adds REST/UI, Job, Execution Attempt, Search Generator, Backtest engine, or remote Supabase apply behavior.

## Phase 1: Setup and Governance

**Purpose**: Confirm the feature pointer, merge gates, module skeleton, and test infrastructure before domain work.

- [X] T001 Verify the active feature pointer, branch, clean baseline, architecture/contracts/data model, and ADR statuses using `specs/004-user-strategy-library/quickstart.md`, `.specify/feature.json`, `docs/adr/0001-modular-monolith.md`, `docs/adr/0002-module-boundaries.md`, `docs/adr/0005-strategy-plugin-registry.md`, `docs/adr/0009-reproducible-experiments.md`, `docs/adr/0011-supabase-auth-user-ownership.md`, and `docs/adr/0012-user-strategy-job-ownership.md`; record reviewer confirmation before implementation and treat its absence as a blocking gate for T002 onward; do not change a `Proposed` status without team approval
- [X] T002 Update Java project dependencies for the planned direction (`strategy-core -> domain`, `strategies/combination -> strategy-core + domain`, `persistence -> strategy-core`, `apps/api -> strategy modules`) in `modules/strategy-core/build.gradle.kts`, `modules/strategies/build.gradle.kts`, `modules/combination/build.gradle.kts`, `modules/persistence/build.gradle.kts`, and `apps/api/build.gradle.kts`
- [X] T003 [P] Add the environment-gated `strategyIntegrationTest` source set and task without attaching it to default offline `check` in `modules/persistence/build.gradle.kts`
- [X] T004 [P] Align public/internal package documentation with F-004 ownership in `modules/strategy-core/src/main/java/com/cryptostrategy/platform/strategy/api/package-info.java`, `modules/strategy-core/src/main/java/com/cryptostrategy/platform/strategy/internal/package-info.java`, `modules/strategies/src/main/java/com/cryptostrategy/platform/strategies/api/package-info.java`, and `modules/combination/src/main/java/com/cryptostrategy/platform/combination/api/package-info.java`

**Checkpoint**: Gradle recognizes the source sets and intended project dependencies; planning may continue while the ADR gate remains explicitly blocked for merge.

---

## Phase 2: Foundational Strategy Values and Boundaries

**Purpose**: Implement typed identities, canonical parameters, fingerprint primitives, stable errors, and architecture enforcement required by every story.

**⚠️ CRITICAL**: No user story implementation starts until this phase is green.

- [X] T005 [P] Add failing typed-identity tests for `StrategyVersionId`, `UserStrategyId`, and `UserStrategyVersionId`, including shared `UlidIdentifier` acceptance, canonical first-character validation, equality, and UUID owner separation, in `modules/strategy-core/src/test/java/com/cryptostrategy/platform/strategy/api/model/StrategyIdentityTest.java`
- [X] T006 Implement `StrategyVersionId`, `UserStrategyId`, and `UserStrategyVersionId` with existing `UlidIdentifier`/`Ulids` reuse in `modules/strategy-core/src/main/java/com/cryptostrategy/platform/strategy/api/model/StrategyVersionId.java`, `modules/strategy-core/src/main/java/com/cryptostrategy/platform/strategy/api/model/UserStrategyId.java`, and `modules/strategy-core/src/main/java/com/cryptostrategy/platform/strategy/api/model/UserStrategyVersionId.java`
- [X] T007 [P] Add failing value-object tests for plugin/policy slugs, semantic versions, Strategy kind/status, and BUY/SELL/HOLD signals in `modules/strategy-core/src/test/java/com/cryptostrategy/platform/strategy/api/model/StrategyValueObjectTest.java`
- [X] T008 Implement core value objects in `modules/strategy-core/src/main/java/com/cryptostrategy/platform/strategy/api/model/{StrategyPluginId,CombinationPolicyId,SemanticVersion,StrategyKind,UserStrategyStatus,UserStrategyVersionStatus,StrategySignal}.java`
- [X] T009 [P] Add failing parameter-contract tests covering typed integer/decimal/boolean/text/enum values, unknown fields, missing fields, defaults, exact decimals, ranges, allowed values, stable issue paths, and `fastPeriod < slowPeriod` in `modules/strategy-core/src/test/java/com/cryptostrategy/platform/strategy/internal/parameter/StrategyParameterValidatorTest.java`
- [X] T010 Implement immutable parameter values in `modules/strategy-core/src/main/java/com/cryptostrategy/platform/strategy/api/model/parameter/{ParameterType,StrategyParameterValue,ParameterDefinition,CrossParameterConstraint,StrategyParameterSchema,StrategyParameterSet}.java`, plus shared validation/default resolution in `modules/strategy-core/src/main/java/com/cryptostrategy/platform/strategy/internal/parameter/StrategyParameterValidator.java`
- [X] T011 [P] Add failing golden-vector tests for `strategy-v1:sha256` scalar type tags, length-prefixed strings, exact-decimal normalization, sorted parameters, single snapshots, and order-independent majority components in `modules/strategy-core/src/test/java/com/cryptostrategy/platform/strategy/internal/fingerprint/StrategyFingerprintV1Test.java`
- [X] T012 Implement canonical encoding and `strategy-v1:sha256:<hex>` generation in `modules/strategy-core/src/main/java/com/cryptostrategy/platform/strategy/internal/fingerprint/CanonicalStrategyEncoder.java` and `modules/strategy-core/src/main/java/com/cryptostrategy/platform/strategy/internal/fingerprint/StrategyFingerprintV1.java`
- [X] T013 [P] Add failing contract tests for immutable `StrategyReference`, `StrategyContext`, `StrategyDecision`, typed evidence, stable errors, ordered/closed/same-pair/same-timeframe Candle validation, and `INSUFFICIENT_DATA` distinct from HOLD in `modules/strategy-core/src/test/java/com/cryptostrategy/platform/strategy/api/StrategyContractTest.java`
- [X] T014 Implement runtime contracts in `modules/strategy-core/src/main/java/com/cryptostrategy/platform/strategy/api/{Strategy,StrategyPlugin}.java`, `modules/strategy-core/src/main/java/com/cryptostrategy/platform/strategy/api/model/{StrategyReference,StrategyDescriptor,StrategyContext,StrategyDecision,StrategyEvidenceValue}.java`, and `modules/strategy-core/src/main/java/com/cryptostrategy/platform/strategy/api/error/{StrategyErrorCode,StrategyException}.java`
- [X] T015 [P] Add failing architecture tests for Strategy purity, forbidden clock/network/database/provider/framework dependencies, typed ULID/UUID fields, no cross-module internal imports, and allowed public `persistence -> strategy` direction in `architecture-tests/src/test/java/com/cryptostrategy/platform/architecture/StrategyArchitectureTest.java` and `architecture-tests/src/test/java/com/cryptostrategy/platform/architecture/fixtures/strategy/{AllowedStrategyDependency,ForbiddenStrategyTechnologyDependency,ForbiddenStrategyInternalDependency}.java`
- [X] T016 Update the dependency matrix to allow only public `persistence -> strategy`, preserve all other F-002/F-003 boundaries, and make the new fixtures pass in `architecture-tests/src/test/java/com/cryptostrategy/platform/architecture/ModuleBoundaryTest.java` and `architecture-tests/src/test/java/com/cryptostrategy/platform/architecture/PurityAndCycleTest.java`
- [X] T017 [P] Add public input/output port compile-contract tests covering one combined usable catalog, its independent default-20/max-100 cursor pages, owner-scoped library, publication, archive, and snapshot resolution without raw unscoped private lookup in `modules/strategy-core/src/test/java/com/cryptostrategy/platform/strategy/api/port/StrategyPortContractTest.java`
- [X] T018 Define `StrategyRegistry` and catalog/library use cases in `modules/strategy-core/src/main/java/com/cryptostrategy/platform/strategy/api/port/in/{ListUsableStrategiesUseCase,CreateUserStrategyUseCase,CreateUserStrategyVersionUseCase,PublishUserStrategyVersionUseCase,GetUserStrategyUseCase,ResolveStrategySnapshotUseCase,ArchiveUserStrategyUseCase}.java`, plus `modules/strategy-core/src/main/java/com/cryptostrategy/platform/strategy/api/port/out/{StrategyCatalogStore,UserStrategyStore}.java`

**Checkpoint**: Shared domain values compile, foundational tests pass, and Strategy has no production dependency on Market Data storage or infrastructure.

---

## Phase 3: User Story 1 — Discover and Execute Trusted Strategies (Priority: P1) 🎯 MVP

**Goal**: Discover, validate, construct, and deterministically execute trusted Strategy versions through one registry contract.

**Independent Test**: Register a deterministic MA plugin, list/resolve its descriptor, resolve defaults, evaluate 100 identical fixed contexts, reject duplicates/invalid input, and confirm short lookback returns `INSUFFICIENT_DATA` without external access.

### Tests for User Story 1

- [X] T019 [P] [US1] Add failing registry contract tests for deterministic listing, exact key/version lookup, immutable descriptors, duplicate rejection, unsupported contract version, and order-independent assembly in `modules/strategy-core/src/test/java/com/cryptostrategy/platform/strategy/internal/registry/DefaultStrategyRegistryTest.java`
- [X] T020 [P] [US1] Add failing MA crossover golden-fixture tests for descriptor/defaults, required lookback, BUY/SELL/HOLD, 100-run determinism, input immutability, and insufficient data in `modules/strategies/src/test/java/com/cryptostrategy/platform/strategies/internal/ma/MovingAverageCrossoverStrategyTest.java`
- [X] T021 [P] [US1] Add a failing test-only F-003 interoperability harness that consumes `CandleBatch` membership into a descriptor-sized rolling window without putting Dataset/reader state in `StrategyContext` in `architecture-tests/src/test/java/com/cryptostrategy/platform/architecture/StrategyCandleBatchInteropTest.java`
- [X] T022 [P] [US1] Add failing catalog synchronization service tests for insert, identical reuse, semantic-key/ID/fingerprint mismatch, and concurrent register-or-verify behavior using a fake store in `modules/strategy-core/src/test/java/com/cryptostrategy/platform/strategy/internal/application/StrategyCatalogSynchronizerTest.java`
- [X] T023 [P] [US1] Add failing SQL/row-mapping contract tests for deterministic descriptor JSONB, exact IDs, plugin/version lookup, fingerprint comparison, and safe catalog error translation in `modules/persistence/src/test/java/com/cryptostrategy/platform/persistence/internal/strategy/JdbcStrategyCatalogStoreTest.java`

### Implementation for User Story 1

- [X] T024 [US1] Implement immutable fail-fast `DefaultStrategyRegistry` with shared parameter resolution and exact version lookup in `modules/strategy-core/src/main/java/com/cryptostrategy/platform/strategy/internal/registry/DefaultStrategyRegistry.java`
- [X] T025 [US1] Implement `MovingAverageCrossoverStrategy` and `MovingAverageCrossoverPlugin` as immutable trusted code in `modules/strategies/src/main/java/com/cryptostrategy/platform/strategies/internal/ma/MovingAverageCrossoverStrategy.java` and `modules/strategies/src/main/java/com/cryptostrategy/platform/strategies/internal/ma/MovingAverageCrossoverPlugin.java`
- [X] T026 [US1] Expose the trusted plugin contribution without Spring in Strategy logic through `modules/strategies/src/main/java/com/cryptostrategy/platform/strategies/api/StrategyPlugins.java`
- [X] T027 [US1] Implement `StrategyCatalogSynchronizer` so trusted descriptors are registered/verified before private use in `modules/strategy-core/src/main/java/com/cryptostrategy/platform/strategy/internal/application/StrategyCatalogSynchronizer.java`
- [X] T028 [US1] Implement deterministic descriptor/parameter JSONB mapping, owner-neutral catalog SQL, and stable catalog error translation in `modules/persistence/src/main/java/com/cryptostrategy/platform/persistence/internal/strategy/StrategyJsonMapper.java`, `modules/persistence/src/main/java/com/cryptostrategy/platform/persistence/internal/strategy/StrategySql.java`, `modules/persistence/src/main/java/com/cryptostrategy/platform/persistence/internal/strategy/StrategyRows.java`, and `modules/persistence/src/main/java/com/cryptostrategy/platform/persistence/internal/strategy/StrategyPersistenceExceptionTranslator.java`
- [X] T029 [US1] Implement `JdbcStrategyCatalogStore` with stable source-declared Strategy ULIDs and register-or-verify race recovery in `modules/persistence/src/main/java/com/cryptostrategy/platform/persistence/internal/strategy/JdbcStrategyCatalogStore.java`
- [X] T030 [US1] Expose the catalog adapter through `modules/persistence/src/main/java/com/cryptostrategy/platform/persistence/api/StrategyPersistenceFactory.java`
- [X] T031 [US1] Make the F-003 batch interoperability harness pass using only test dependencies and canonical `PersistedCandle.candle()` values in `architecture-tests/src/test/java/com/cryptostrategy/platform/architecture/StrategyCandleBatchInteropTest.java`

**Checkpoint**: US1 works without User Strategy persistence, Composite, REST, UI, Search, or Backtest code.

---

## Phase 4: User Story 2 — Manage a Private Strategy Library (Priority: P1)

**Goal**: Create, list, version, publish, resolve, and archive owner-scoped private single Strategies with immutable provenance.

**Independent Test**: With two authenticated UUIDs, create equivalent names independently, deny every cross-owner operation, publish a complete canonical snapshot, create a new version rather than mutate, archive the root, and prove exactly one concurrent operation wins.

### Tests for User Story 2

- [X] T032 [P] [US2] Add failing aggregate tests for owner UUID, normalized active-name rules, `ACTIVE -> ARCHIVED`, complete single drafts, positive monotonic versions, and immutable published snapshots in `modules/strategy-core/src/test/java/com/cryptostrategy/platform/strategy/api/model/UserStrategyModelTest.java`
- [X] T033 [P] [US2] Add failing application tests with an owner-aware fake store for one combined usable listing with independent cursor pages, list/create/get/create-next/publish/resolve/archive, cross-owner non-disclosure, archived-root rejection, full default materialization, canonical snapshot equality, and fingerprint stability in `modules/strategy-core/src/test/java/com/cryptostrategy/platform/strategy/internal/application/UserStrategyServiceTest.java`
- [X] T034 [P] [US2] Add failing concurrency tests proving one create-next/publish/archive request wins and stale expected state maps to `STRATEGY_CONFLICT` without automatic renumbering in `modules/strategy-core/src/test/java/com/cryptostrategy/platform/strategy/internal/application/UserStrategyConcurrencyTest.java`
- [X] T035 [P] [US2] Add failing JDBC SQL contract tests requiring owner predicates on every private query/update, bounded owner listing, active-name conflict translation, immutable read mapping, and absence of raw private `findById` in `modules/persistence/src/test/java/com/cryptostrategy/platform/persistence/internal/strategy/JdbcUserStrategyStoreTest.java`
- [X] T036 [P] [US2] Add environment-gated local PostgreSQL integration tests for atomic first draft, two-owner isolation, published immutability, archive preservation, rollback, and concurrent next-version/publication in `modules/persistence/src/strategyIntegrationTest/java/com/cryptostrategy/platform/persistence/strategy/UserStrategyPersistenceIntegrationTest.java`

### Implementation for User Story 2

- [X] T037 [P] [US2] Implement owner-scoped values in `modules/strategy-core/src/main/java/com/cryptostrategy/platform/strategy/api/model/user/{UserStrategy,UserStrategySummary,UserStrategyVersion,SingleStrategyDraftSource,StrategySnapshot}.java`
- [X] T038 [P] [US2] Implement owner-scoped commands in `modules/strategy-core/src/main/java/com/cryptostrategy/platform/strategy/api/model/user/command/{CreateUserStrategyCommand,CreateNextStrategyVersionCommand,PublishStrategyVersionCommand,ArchiveUserStrategyCommand}.java` and query/result values in `modules/strategy-core/src/main/java/com/cryptostrategy/platform/strategy/api/model/user/query/{UsableStrategyPageRequest,UsableStrategyCatalog,StrategyCatalogPage,UserStrategyPage,GetUserStrategyQuery,ResolveStrategySnapshotQuery}.java`
- [X] T039 [US2] Implement `UserStrategyService` with one combined usable listing, independent cursor pagination, authenticated UUID propagation, registry validation, complete defaults, `strategy-v1` fingerprinting, non-disclosing lookup, archive rules, and expected-state conflict handling in `modules/strategy-core/src/main/java/com/cryptostrategy/platform/strategy/internal/application/UserStrategyService.java`
- [X] T040 [US2] Implement owner-scoped root/version SQL and single-version row mapping in `modules/persistence/src/main/java/com/cryptostrategy/platform/persistence/internal/strategy/StrategySql.java` and `modules/persistence/src/main/java/com/cryptostrategy/platform/persistence/internal/strategy/StrategyRows.java`
- [X] T041 [US2] Implement `JdbcUserStrategyStore` transactions for root + complete first draft, bounded listing, next version, conditional publication, snapshot resolution, and archive in `modules/persistence/src/main/java/com/cryptostrategy/platform/persistence/internal/strategy/JdbcUserStrategyStore.java`
- [X] T042 [US2] Extend safe uniqueness, stale-state, immutability, integrity, and availability translation for private operations in `modules/persistence/src/main/java/com/cryptostrategy/platform/persistence/internal/strategy/StrategyPersistenceExceptionTranslator.java`
- [X] T043 [US2] Expose the owner-scoped store through `modules/persistence/src/main/java/com/cryptostrategy/platform/persistence/api/StrategyPersistenceFactory.java`
- [X] T044 [US2] Add application-boundary authorization tests proving `AuthenticatedUserContext.userId()` is the only owner source and request data cannot override it in `apps/api/src/test/java/com/cryptostrategy/platform/api/config/StrategyAuthorizationConfigurationTest.java`

**Checkpoint**: US2 independently provides a private single-Strategy library; PostgreSQL integration remains optional for default offline `check` but must pass in local evidence before merge.

---

## Phase 5: User Story 3 — Build a Deterministic Composite Strategy (Priority: P2)

**Goal**: Publish and evaluate flat majority-vote Composites made only from exact system Strategy versions.

**Independent Test**: Evaluate at least two deterministic component Strategies across every registration-order permutation, prove majority/tie behavior and identical fingerprint, reject nested/duplicate/invalid components, and persist/resolve the exact immutable snapshot.

### Tests for User Story 3

- [X] T045 [P] [US3] Add failing majority-vote policy tests for unique BUY/SELL/HOLD maximum, all tie shapes, deterministic evidence, and stable `majority-vote@1.0.0` identity in `modules/combination/src/test/java/com/cryptostrategy/platform/combination/internal/MajorityVotePolicyTest.java`
- [X] T046 [P] [US3] Add failing Composite Strategy tests for shared context, at least two distinct system components, no User Strategy/nested Composite, error propagation, all component permutations, and order-independent decision/fingerprint in `modules/combination/src/test/java/com/cryptostrategy/platform/combination/internal/CompositeStrategyTest.java`
- [X] T047 [P] [US3] Add failing User Strategy service tests for complete Composite drafts, canonical component parameters, majority policy validation, duplicate/nested rejection, publication snapshot, and archive/version behavior in `modules/strategy-core/src/test/java/com/cryptostrategy/platform/strategy/internal/application/CompositeUserStrategyServiceTest.java`
- [X] T048 [P] [US3] Extend local PostgreSQL integration tests with atomic component rollback, minimum two components, system-version foreign keys, published component immutability, owner resolution, and canonical snapshot mapping in `modules/persistence/src/strategyIntegrationTest/java/com/cryptostrategy/platform/persistence/strategy/CompositeStrategyPersistenceIntegrationTest.java`

### Implementation for User Story 3

- [X] T049 [P] [US3] Define `CombinationPolicy` and policy reference values in `modules/combination/src/main/java/com/cryptostrategy/platform/combination/api/CombinationPolicy.java` and `modules/combination/src/main/java/com/cryptostrategy/platform/combination/api/CombinationPolicyReference.java`
- [X] T050 [US3] Implement deterministic `MajorityVotePolicy` and `CompositeStrategy` without nested components in `modules/combination/src/main/java/com/cryptostrategy/platform/combination/internal/MajorityVotePolicy.java` and `modules/combination/src/main/java/com/cryptostrategy/platform/combination/internal/CompositeStrategy.java`
- [X] T051 [US3] Expose the supported policy through `modules/combination/src/main/java/com/cryptostrategy/platform/combination/api/CombinationPolicies.java`
- [X] T052 [P] [US3] Implement Composite persistence values in `modules/strategy-core/src/main/java/com/cryptostrategy/platform/strategy/api/model/user/{CompositeStrategyDraftSource,UserStrategyComponent,CompositeStrategySnapshot}.java`
- [X] T053 [US3] Extend `UserStrategyService` validation/publication/resolution for flat majority Composites in `modules/strategy-core/src/main/java/com/cryptostrategy/platform/strategy/internal/application/UserStrategyService.java`
- [X] T054 [US3] Extend `JdbcUserStrategyStore`, `StrategySql`, and `StrategyRows` for atomic component insertion, conditional publication, and canonical Composite reads in `modules/persistence/src/main/java/com/cryptostrategy/platform/persistence/internal/strategy/JdbcUserStrategyStore.java`, `modules/persistence/src/main/java/com/cryptostrategy/platform/persistence/internal/strategy/StrategySql.java`, and `modules/persistence/src/main/java/com/cryptostrategy/platform/persistence/internal/strategy/StrategyRows.java`

**Checkpoint**: A single and Composite Strategy both satisfy the same runtime contract; nested Composite and weighted voting remain absent.

---

## Phase 6: User Story 4 — Extend the Strategy Catalog Safely (Priority: P2)

**Goal**: Prove QA-01 by adding a test-only MACD plugin through existing boundaries with no downstream production changes.

**Independent Test**: Register, describe, validate, create, and evaluate a deterministic test MACD plugin, then verify architecture/source dependencies require no Backtester, Evaluator, Leaderboard, Search, UI, or F-003 contract modification.

### Tests and proof for User Story 4

- [X] T055 [P] [US4] Add a deterministic test-only `MacdStrategyPluginFixture` with descriptor/schema/decisions and no production registration change in `architecture-tests/src/test/java/com/cryptostrategy/platform/architecture/fixtures/strategyextension/MacdStrategyPluginFixture.java`
- [X] T056 [US4] Add the failing/passing QA-01 extension contract test for list/validate/create/evaluate through `StrategyRegistry` in `architecture-tests/src/test/java/com/cryptostrategy/platform/architecture/StrategyExtensionTest.java`
- [X] T057 [P] [US4] Add architecture assertions that the extension fixture depends only on public Domain/Strategy contracts and production Backtester, Evaluator, Leaderboard, Search, UI, and F-003 contracts remain unmodified consumers in `architecture-tests/src/test/java/com/cryptostrategy/platform/architecture/StrategyArchitectureTest.java`
- [X] T058 [US4] Record the reproducible QA-01 change-surface verification procedure and expected allowed paths in `specs/004-user-strategy-library/quickstart.md` and `specs/004-user-strategy-library/contracts/verification-matrix.md`

**Checkpoint**: QA-01 is executable evidence, not a claim based only on diagrams or prose.

---

## Phase 7: Composition and Cross-Cutting Verification

**Purpose**: Wire the completed internal capability, run all evidence, and preserve explicit environment/governance state.

- [X] T059 [P] Add failing composition tests for trusted MA registration, majority policy availability, catalog synchronization, public ports, and absence of controllers/WebSocket/Job/Search/Backtest beans in `apps/api/src/test/java/com/cryptostrategy/platform/api/config/StrategyConfigurationTest.java`
- [X] T060 Implement Spring-only wiring for registry, trusted plugins, majority policy, Strategy persistence adapters, catalog synchronization, and `UserStrategyService` in `apps/api/src/main/java/com/cryptostrategy/platform/api/config/StrategyConfiguration.java`
- [X] T061 [P] Add structured lifecycle/error logging tests that preserve owner privacy and redact parameters, SQL, credentials, stack traces, and other users' metadata in `modules/strategy-core/src/test/java/com/cryptostrategy/platform/strategy/internal/application/StrategyObservabilityTest.java` and `modules/persistence/src/test/java/com/cryptostrategy/platform/persistence/internal/strategy/StrategyPersistenceObservabilityTest.java`
- [X] T062 Implement safe Strategy lifecycle/catalog log events without putting logging dependencies in Strategy evaluation objects in `modules/strategy-core/src/main/java/com/cryptostrategy/platform/strategy/internal/application/StrategyEventLogger.java` and `modules/persistence/src/main/java/com/cryptostrategy/platform/persistence/internal/strategy/StrategyPersistenceEventLogger.java`
- [X] T063 Run the focused offline suites and fix failures using the commands and expected evidence in `specs/004-user-strategy-library/quickstart.md`
- [X] T064 If local Supabase is available, run `strategyIntegrationTest` and record the actual environment/result; otherwise record the exact blocker without fabricating evidence in `specs/004-user-strategy-library/contracts/verification-matrix.md`
- [X] T065 Verify the two existing migration files are unchanged and perform no remote apply using `supabase/migrations/20260827000100_create_database_baseline.sql`, `supabase/migrations/20260828000100_add_user_strategies_and_jobs.sql`, and `specs/004-user-strategy-library/quickstart.md`
- [X] T066 Run `./gradlew clean check`, `git diff --check`, local-link/fence checks, scope scans, `git diff --stat`, and `git status`; record only real final evidence in `specs/004-user-strategy-library/contracts/verification-matrix.md`
- [X] T067 Re-check Constitution/ADR gates before merge and document unresolved approval blockers without self-accepting ADRs in `specs/004-user-strategy-library/plan.md` and `specs/004-user-strategy-library/contracts/verification-matrix.md`

---

## Dependencies and Execution Order

### Phase dependencies

- **Phase 1 — Setup**: Starts with T001 as a blocking reviewer-confirmation gate. T002–T004 and every implementation task remain blocked until that confirmation is recorded; after approval, T002 precedes compilation and T003/T004 can run in parallel.
- **Phase 2 — Foundation**: Depends on Phase 1 and blocks all user stories. Each failing test task precedes its paired implementation task: T005→T006, T007→T008, T009→T010, T011→T012, T013→T014, T015→T016, T017→T018.
- **Phase 3 — US1**: Depends on Phase 2. Registry/MA/interoperability/catalog tests T019–T023 may be written in parallel; implementations follow their corresponding tests.
- **Phase 4 — US2**: Depends on US1 registry, parameter validation, catalog synchronization, and fingerprinting. Tests T032–T036 precede model/service/JDBC implementation T037–T044.
- **Phase 5 — US3**: Depends on US1 runtime and US2 aggregate/store lifecycle. Combination tests T045–T048 precede T049–T054.
- **Phase 6 — US4**: Depends on the US1 registry extension contract; it may run in parallel with late US2/US3 persistence work because it changes only architecture-test files and documentation.
- **Phase 7 — Verification**: Depends on all selected story phases. T059 precedes T060; T061 precedes T062; T063–T067 run after composition.

### User story dependencies

```text
Setup -> Foundation -> US1 Trusted Registry/MA
                           ├──> US2 Private Single Strategy -> US3 Flat Composite
                           └──> US4 QA-01 extension proof

US1 + US2 + US3 + US4 -> Composition and verification
```

- **US1 (P1)** is the minimum independently executable runtime capability.
- **US2 (P1)** requires US1 validation/registry but can be demonstrated without Composite.
- **US3 (P2)** extends US2 storage/lifecycle and uses the US1 runtime contract.
- **US4 (P2)** validates US1's change point independently of US2/US3 behavior.

### Within each user story

- Write the named tests first and confirm the intended failure.
- Implement values before services, and services before adapters/composition.
- Do not weaken an assertion to make an implementation pass.
- Default `check` stays offline; environment-dependent PostgreSQL verification is explicit.
- Story completion requires its Independent Test to pass without implementing excluded scope.

## Parallel Opportunities

### Foundation

After setup, identity tests (T005), value tests (T007), parameter tests (T009), fingerprint tests (T011), runtime contract tests (T013), architecture tests (T015), and port tests (T017) target separate files and can be authored in parallel. Their implementation partners remain sequential.

### User Story 1

```text
T019 Registry tests
T020 MA golden tests
T021 F-003 batch harness
T022 Catalog synchronizer tests
T023 JDBC catalog tests
```

After their dependencies exist, MA implementation (T025–T026) and catalog persistence work (T027–T030) can progress in parallel before T031 integration.

### User Story 2

Model/application/concurrency tests (T032–T034), JDBC tests (T035), and environment-gated integration test design (T036) use separate source sets. Model/command implementation T037–T038 can be parallel before service T039; JDBC work T040–T043 follows the port/model contract.

### User Story 3 and 4

Majority policy tests (T045), Composite runtime tests (T046), User Strategy Composite tests (T047), and PostgreSQL test design (T048) can be authored in parallel. US4 fixture/tests T055–T057 can proceed once US1 is stable without waiting for Composite persistence.

## Implementation Strategy

### MVP first

1. Complete Setup and Foundation.
2. Complete US1 trusted registry and MA Strategy.
3. Run the US1 independent test and full offline regression.
4. Stop and review the runtime contract before adding persistence.

### Incremental delivery

1. **US1**: trusted Strategy runtime, registry, MA, catalog verification, bounded F-003 interoperability.
2. **US2**: private owner-scoped single Strategy and immutable publication.
3. **US3**: flat majority-vote Composite using the same runtime/snapshot boundary.
4. **US4**: test-only MACD proof that the change point is preserved.
5. **Final**: composition, optional local PostgreSQL evidence, full check, and ADR merge-gate review.

## Notes

- `[P]` means different files and no dependency on another incomplete task in that phase.
- Every story task has `[US1]`, `[US2]`, `[US3]`, or `[US4]` traceability.
- Existing migrations are inputs, not edit targets.
- No task authorizes remote Supabase mutation or fabricated evidence.
- No task adds REST/UI, Job, Execution Attempt, Search Generator, or Backtest engine behavior.

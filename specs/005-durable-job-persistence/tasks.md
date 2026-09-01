# Tasks: Experiment Persistence and Ownership

**Input**: Design documents from `specs/005-durable-job-persistence/`  
**Prerequisites**: `spec.md`, `plan.md`, `research.md`, `data-model.md`, `contracts/`, `quickstart.md`, `checklists/requirements.md`  

**Tests**: F-005 mandates rigorous unit tests, domain state-machine tests, ArchUnit architecture rules, isolated PostgreSQL integration tests, ownership isolation tests, concurrency tests, and database contract tests. Test tasks are written and must fail before paired implementation.

**Organization**: Tasks are grouped by user story. Setup and Foundational phases establish shared dependencies, typed identifiers, domain interfaces, and persistence ports; story tasks use `[US1]`–`[US7]` labels with exact repository file paths.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependency on incomplete tasks in the same phase).
- **[Story]**: Maps the task to a user story in `spec.md` (`[US1]` through `[US7]`).
- Tasks do NOT implement Backtest engine, Evaluation, Leaderboard, Redis Streams worker orchestration, Outbox publisher, REST API, realtime delivery, or frontend UI.

---

## Phase 1: Setup and Build Foundations

**Purpose**: Establish Gradle project dependencies, source sets, migration skeletons, and package documentation for F-005.

- [X] T001 Update Java project dependencies for F-005 (`modules/experiment` depends on `modules/domain`; `modules/persistence` depends on `modules/domain` and `modules/experiment`) in `modules/experiment/build.gradle.kts` and `modules/persistence/build.gradle.kts`
- [X] T002 [P] Configure the environment-gated `experimentIntegrationTest` source set and task in `modules/persistence/build.gradle.kts` following the pattern established in `marketDataIntegrationTest` and `strategyIntegrationTest`
- [X] T003 [P] Document package ownership and public/internal boundaries in `modules/experiment/src/main/java/com/cryptostrategy/platform/experiment/api/package-info.java`, `modules/experiment/src/main/java/com/cryptostrategy/platform/experiment/internal/package-info.java`, and `modules/persistence/src/main/java/com/cryptostrategy/platform/persistence/internal/experiment/package-info.java`
- [X] T004 Create new forward-only F-005 schema-alignment migration(s) under `supabase/migrations/` after inspecting the current migration directory; do NOT edit applied migrations. Cover: (a) tighten `experiment.execution_attempt.status` to remove legacy `RETRY_SCHEDULED`, (b) add nullable `reproduces_experiment_id` self-reference if absent, (c) allow `experiment.experiment_manifest.fingerprint` to be `NULL` while Experiment is `CREATED` and rely on the atomic `CREATED → QUEUED` transaction to persist a non-empty fingerprint before commit, and (d) align `platform.idempotency_record` for durable atomic-claim lifecycle with `state IN ('IN_PROGRESS','COMPLETED')`, nullable completion fields while `IN_PROGRESS`, application-level outcome metadata, and preserved uniqueness on `(user_id, scope, idempotency_key)` in `supabase/migrations/20260830000100_f005_schema_alignment.sql`

**Checkpoint**: Gradle recognizes `:modules:experiment` and `:modules:persistence:experimentIntegrationTest`; forward migration script is ready for local validation.

---

## Phase 2: Foundational Domain Types & Persistence Ports

**Purpose**: Implement typed ULID identifiers, status enums, error types, provenance snapshot placeholders, output ports, and ArchUnit architecture tests required across all user stories.

**⚠️ CRITICAL**: No user story implementation starts until this foundational phase is complete.

- [X] T005 [P] Add failing tests for typed Crockford ULID identifiers (`ExperimentId`, `CandidateId`, `JobId`, `AttemptId`, `WorkerId`) verifying canonical Crockford alphabet, equality, and separation from UUID `ownerUserId` in `modules/experiment/src/test/java/com/cryptostrategy/platform/experiment/api/ExperimentIdentityTest.java`
- [X] T006 Implement strongly-typed business identifiers (`ExperimentId`, `CandidateId`, `JobId`, `AttemptId`, `WorkerId`) extending `UlidIdentifier` and reusing `Ulids` in `modules/experiment/src/main/java/com/cryptostrategy/platform/experiment/api/{ExperimentId,CandidateId}.java` and `modules/experiment/src/main/java/com/cryptostrategy/platform/experiment/api/job/{JobId,AttemptId,WorkerId}.java`
- [X] T007 [P] Add failing tests for canonical Experiment statuses (`CREATED`, `QUEUED`, `RUNNING`, `COMPLETED`, `FAILED`, `STOP_REQUESTED`, `STOPPED`), Job statuses, terminal Attempt statuses (`QUEUED`, `RUNNING`, `SUCCEEDED`, `FAILED`, `CANCELLED`), and `FailureClassification` enums in `modules/experiment/src/test/java/com/cryptostrategy/platform/experiment/api/ExperimentEnumsTest.java`
- [X] T008 Implement canonical status and classification enums in `modules/experiment/src/main/java/com/cryptostrategy/platform/experiment/api/ExperimentStatus.java`, `modules/experiment/src/main/java/com/cryptostrategy/platform/experiment/api/job/{JobType,JobStatus,AttemptStatus,FailureClassification}.java`
- [X] T009 [P] Add failing tests for domain error types (`ExperimentValidationException`, `ResourceInaccessibleException`, `IdempotencyConflictException`, `InvalidStateTransitionException`) in `modules/experiment/src/test/java/com/cryptostrategy/platform/experiment/api/error/ExperimentExceptionTest.java`
- [X] T010 Implement domain error types in `modules/experiment/src/main/java/com/cryptostrategy/platform/experiment/api/error/{ExperimentException,ExperimentValidationException,ResourceInaccessibleException,IdempotencyConflictException,InvalidStateTransitionException}.java`
- [X] T011 [P] Implement provenance snapshot value objects in `modules/experiment/src/main/java/com/cryptostrategy/platform/experiment/api/provenance/{DatasetProvenanceSnapshot,StrategyComponentSnapshot,StrategyProvenanceSnapshot}.java`; bind Dataset provenance directly to the published F-003 contract using `DatasetVersionId` as the stable Dataset identity, `version = candle-v1` contract semantics, and F-003 provider/Trading Pair/Timeframe provenance types. Do NOT create a separate `DatasetId` or duplicate F-003 Dataset/checksum/ingestion logic
- [X] T012 Define public persistence output port interfaces (`ExperimentStore`, `JobStore`, `ExecutionAttemptStore`, `IdempotencyStore`, `OutboxStore`) in `modules/experiment/src/main/java/com/cryptostrategy/platform/experiment/api/port/out/{ExperimentStore,JobStore,ExecutionAttemptStore,IdempotencyStore,OutboxStore}.java`
- [X] T013 [P] Add ArchUnit architecture tests verifying `modules/experiment` depends only on `modules/domain`, contains zero JDBC/SQL/Spring imports, and that `modules/persistence` accesses only public `api` packages of `modules/experiment` in `architecture-tests/src/test/java/com/cryptostrategy/platform/architecture/ExperimentArchitectureTest.java`
- [X] T014 Update the dependency matrix in `architecture-tests/src/test/java/com/cryptostrategy/platform/architecture/ModuleBoundaryTest.java` to enforce allowed dependencies for `experiment` and `persistence`

**Checkpoint**: Foundational types, ports, and ArchUnit boundary tests pass; domain layer is clean and decoupled from persistence.

---

## Phase 3: User Story 1 — Create Experiment & Frozen Manifest (Priority: P1) 🎯 MVP

**Goal**: Create an Experiment in `CREATED` status with a draft Manifest, validate completeness and freeze the Manifest at `CREATED → QUEUED`, compute deterministic SHA-256 `experimentFingerprint`, reject post-freeze updates, and query frozen content.

**Independent Test**: Create an Experiment (CREATED), submit a Queue/Freeze command, verify status transitions to `QUEUED`, verify `fingerprint` is non-empty, verify all subsequent manifest update attempts are rejected with `ExperimentValidationException`, and verify querying returns the frozen Manifest.

### Tests for User Story 1

- [X] T015 [P] [US1] Add failing unit tests for `ExperimentAggregate` and `ExperimentManifest` verifying `CREATED` mutability, completeness validation, freeze transition to `QUEUED`, deterministic SHA-256 fingerprinting across JSON key orderings and UTC formats, and post-freeze immutability in `modules/experiment/src/test/java/com/cryptostrategy/platform/experiment/internal/ExperimentAggregateTest.java`
- [X] T016 [P] [US1] Add failing unit tests for `CanonicalFingerprintCalculator` verifying SHA-256 stability, field exclusion of runtime metadata, decimal canonical string formatting, and detection of single-field mutations in `modules/experiment/src/test/java/com/cryptostrategy/platform/experiment/internal/CanonicalFingerprintCalculatorTest.java`
- [X] T017 [P] [US1] Add failing SQL mapping and row mapper tests for `JdbcExperimentStore` verifying draft insertion, nullable fingerprint during `CREATED`, and freeze update in `modules/persistence/src/test/java/com/cryptostrategy/platform/persistence/internal/experiment/JdbcExperimentStoreTest.java`
- [X] T018 [P] [US1] Add environment-gated PostgreSQL integration tests verifying atomic Experiment creation, freeze transition, Manifest immutability, fingerprint persistence, and query consistency in `modules/persistence/src/experimentIntegrationTest/java/com/cryptostrategy/platform/persistence/experiment/ExperimentPersistenceIntegrationTest.java`

### Implementation for User Story 1

- [X] T019 [US1] Implement `CanonicalFingerprintCalculator` performing canonical JSON serialization and SHA-256 hashing in `modules/experiment/src/main/java/com/cryptostrategy/platform/experiment/internal/CanonicalFingerprintCalculator.java`
- [X] T020 [US1] Implement `ExperimentManifest` record and `Experiment` aggregate root in `modules/experiment/src/main/java/com/cryptostrategy/platform/experiment/api/ExperimentManifest.java` and `modules/experiment/src/main/java/com/cryptostrategy/platform/experiment/internal/ExperimentAggregate.java`
- [X] T021 [US1] Implement Experiment use-case ports and service (`CreateExperimentUseCase`, `FreezeExperimentUseCase`, `GetExperimentUseCase`) in `modules/experiment/src/main/java/com/cryptostrategy/platform/experiment/api/port/in/{CreateExperimentUseCase,FreezeExperimentUseCase,GetExperimentUseCase}.java` and `modules/experiment/src/main/java/com/cryptostrategy/platform/experiment/internal/ExperimentApplicationService.java`
- [X] T022 [US1] Implement `ExperimentSql`, `ExperimentRows`, `ExperimentJsonMapper`, and `JdbcExperimentStore` in `modules/persistence/src/main/java/com/cryptostrategy/platform/persistence/internal/experiment/{ExperimentSql,ExperimentRows,ExperimentJsonMapper,JdbcExperimentStore}.java`
- [X] T023 [US1] Expose `ExperimentStore` through public factory `ExperimentPersistenceFactory` in `modules/persistence/src/main/java/com/cryptostrategy/platform/persistence/api/ExperimentPersistenceFactory.java`

**Checkpoint**: User Story 1 MVP is fully functional and testable; Experiments can be created, frozen, and queried with deterministic fingerprints.

---

## Phase 4: User Story 2 — User Ownership & Cross-User Authorization (Priority: P1)

**Goal**: Enforce Supabase UUID (`owner_user_id`) ownership authorization across all application queries and commands, traversing the authoritative parent hierarchy (`attempt → job → experiment → owner`, `candidate → experiment → owner`), and returning ownership-safe inaccessible outcomes without data leakage.

**Independent Test**: Create User A and User B fixtures. User A creates an Experiment, Candidates, and Jobs. User B queries or commands those resources by ID. Verify 100% of User B requests fail with `ResourceInaccessibleException` (empty optional/404) with zero data or existence leakage.

### Tests for User Story 2

- [X] T024 [P] [US2] Add failing application service tests with an owner-aware mock store verifying that supplying another user's `owner_user_id` on any read, freeze, stop, or query operation throws `ResourceInaccessibleException` or returns empty in `modules/experiment/src/test/java/com/cryptostrategy/platform/experiment/internal/ExperimentOwnershipAuthorizationTest.java`
- [X] T025 [P] [US2] Add failing JDBC store contract tests verifying that SQL queries for Experiment, Candidate, Job, and Attempt contain mandatory `owner_user_id` / parent joins in `modules/persistence/src/test/java/com/cryptostrategy/platform/persistence/internal/experiment/OwnershipSqlPredicateTest.java`
- [X] T026 [P] [US2] Add environment-gated PostgreSQL integration tests verifying complete ownership isolation between User A and User B across all persistence tables (`experiment`, `candidate_definition`, `job`, `execution_attempt`) in `modules/persistence/src/experimentIntegrationTest/java/com/cryptostrategy/platform/persistence/experiment/OwnershipIsolationIntegrationTest.java`

### Implementation for User Story 2

- [X] T027 [US2] Enforce mandatory `UUID ownerUserId` parameters and ownership validation predicates across all use-case methods in `modules/experiment/src/main/java/com/cryptostrategy/platform/experiment/internal/ExperimentApplicationService.java` and `modules/experiment/src/main/java/com/cryptostrategy/platform/experiment/internal/JobApplicationService.java`
- [X] T028 [US2] Implement owner-scoped SQL queries and row joins (`job → experiment → owner_user_id`, `execution_attempt → job → experiment → owner_user_id`) in `modules/persistence/src/main/java/com/cryptostrategy/platform/persistence/internal/experiment/{ExperimentSql,JdbcExperimentStore,JdbcJobStore,JdbcExecutionAttemptStore}.java`

**Checkpoint**: User Stories 1 and 2 work seamlessly; cross-user resources are strictly isolated.

---

## Phase 5: User Story 3 — Durable Job Identity & Retry History (Priority: P1)

**Goal**: Manage durable Job entities for Search and Backtest, enforce one Backtest Job per Candidate (including terminal states), maintain append-only Execution Attempt history with terminal-only statuses (`QUEUED`, `RUNNING`, `SUCCEEDED`, `FAILED`, `CANCELLED`), handle retry failure by marking Attempt `FAILED` + Job `RETRY_SCHEDULED`, and serialize monotonic attempt number allocation via parent Job row locking.

**Independent Test**: Create a Backtest Job for a Candidate. Simulate 3 Worker tries with transient failures and retries. Verify exactly 1 Job exists, exactly 3 Execution Attempts exist with sequential attempt numbers (1, 2, 3), Attempt statuses are `FAILED, FAILED, SUCCEEDED`, the Job status is `SUCCEEDED`, and attempting to create a second Backtest Job for the Candidate returns the existing Job or is rejected.

### Tests for User Story 3

- [X] T029 [P] [US3] Add failing unit tests for `JobAggregate` and `ExecutionAttemptEntity` verifying Job state machine (`QUEUED → RUNNING → SUCCEEDED | FAILED | RETRY_SCHEDULED`), retry calculation (`handleFailure()` exponential backoff), terminal-only Attempt statuses, rejection of `RETRY_SCHEDULED` on Attempt, and monotonic attempt number validation in `modules/experiment/src/test/java/com/cryptostrategy/platform/experiment/internal/JobAggregateTest.java`
- [X] T030 [P] [US3] Add failing unit tests for Search Job vs Backtest Job structural constraints (Search Job has `candidate_id = NULL` and zero Attempts; Backtest Job requires Candidate) in `modules/experiment/src/test/java/com/cryptostrategy/platform/experiment/internal/JobTypeConstraintTest.java`
- [X] T031 [P] [US3] Add failing JDBC contract tests for `JdbcJobStore` and `JdbcExecutionAttemptStore` verifying parent Job row locking (`SELECT ... FOR UPDATE`), `attempt_no = MAX + 1` allocation, and single Backtest Job per Candidate uniqueness in `modules/persistence/src/test/java/com/cryptostrategy/platform/persistence/internal/experiment/JdbcJobStoreTest.java`
- [X] T032 [P] [US3] Add environment-gated PostgreSQL integration tests verifying one Job per Candidate across all states (including CANCELLED), 3-retry history recording, and concurrent Attempt allocation with zero collisions in `modules/persistence/src/experimentIntegrationTest/java/com/cryptostrategy/platform/persistence/experiment/JobPersistenceIntegrationTest.java`
- [X] T033 [P] [US3] Add multi-threaded concurrency integration tests proving that concurrent attempt-start requests on the same Job row serialize cleanly and assign sequential numbers without duplicate key errors in `modules/persistence/src/experimentIntegrationTest/java/com/cryptostrategy/platform/persistence/experiment/AttemptConcurrencyIntegrationTest.java`

### Implementation for User Story 3

- [X] T034 [US3] Implement `Job` aggregate root and `ExecutionAttempt` entity in `modules/experiment/src/main/java/com/cryptostrategy/platform/experiment/internal/{JobAggregate,ExecutionAttemptEntity}.java` adhering to `contracts/job-execution-attempt-contract.md`
- [X] T035 [US3] Implement Job use cases (`CreateBacktestJobUseCase`, `CreateSearchJobUseCase`, `StartNextAttemptUseCase`, `FinalizeAttemptUseCase`, `ScheduleRetryUseCase`, `RequeueRetryUseCase`) in `modules/experiment/src/main/java/com/cryptostrategy/platform/experiment/api/port/in/` and `modules/experiment/src/main/java/com/cryptostrategy/platform/experiment/internal/JobApplicationService.java`
- [X] T036 [US3] Implement `JdbcJobStore` and `JdbcExecutionAttemptStore` with parent Job row locking for monotonic attempt number allocation and atomic coupled Job/Attempt state updates in `modules/persistence/src/main/java/com/cryptostrategy/platform/persistence/internal/experiment/{JdbcJobStore,JdbcExecutionAttemptStore}.java`
- [X] T037 [US3] Expose `JobStore` and `ExecutionAttemptStore` in `modules/persistence/src/main/java/com/cryptostrategy/platform/persistence/api/ExperimentPersistenceFactory.java`

**Checkpoint**: User Stories 1, 2, and 3 are complete; durable Jobs and append-only retry Attempt histories are persistent, thread-safe, and isolated.

---

## Phase 6: User Story 4 — Experiment Stop & Job Cancellation Lifecycle (Priority: P2)

**Goal**: Support Experiment stop lifecycle (`RUNNING → STOP_REQUESTED → STOPPED`), Job cancellation (`RUNNING → CANCEL_REQUESTED → CANCELLED`, `QUEUED → CANCELLED`, `RETRY_SCHEDULED → CANCELLED`), non-blocking cancel polling (`isCancelRequested()`), cancel vs requeue race serialization, and result preservation.

**Independent Test**: Advance an Experiment to `QUEUED`, create Jobs in `QUEUED`, `RUNNING`, and `RETRY_SCHEDULED`. Issue Experiment Stop command. Verify Experiment transitions to `STOP_REQUESTED`, `QUEUED` and `RETRY_SCHEDULED` Jobs transition to `CANCELLED`, `RUNNING` Job transitions to `CANCEL_REQUESTED`, worker confirms cancellation moving it to `CANCELLED`, Experiment reaches `STOPPED`, and previously completed results remain accessible.

### Tests for User Story 4

- [X] T038 [P] [US4] Add failing unit tests for Experiment stop and Job cancellation state machines, verifying transitions from `QUEUED`, `RUNNING`, `RETRY_SCHEDULED`, and `CANCEL_REQUESTED` to `CANCELLED` in `modules/experiment/src/test/java/com/cryptostrategy/platform/experiment/internal/CancellationStateMachineTest.java`
- [X] T039 [P] [US4] Add an environment-gated PostgreSQL concurrency integration test for cancel racing with retry requeuing (`RETRY_SCHEDULED → QUEUED`), verifying with real database transactions that parent Job row locking (`SELECT ... FOR UPDATE`) serializes the operations, produces one unambiguous durable final state, and prevents a cancelled Job from being redispatched in `modules/persistence/src/experimentIntegrationTest/java/com/cryptostrategy/platform/persistence/experiment/CancelVsRequeueConcurrencyIntegrationTest.java`
- [X] T040 [P] [US4] Add environment-gated PostgreSQL integration tests verifying Experiment stop transition, cascading cancellation of queued Jobs, and cancel polling visibility in `modules/persistence/src/experimentIntegrationTest/java/com/cryptostrategy/platform/persistence/experiment/CancellationIntegrationTest.java`

### Implementation for User Story 4

- [X] T041 [US4] Implement `StopExperimentUseCase` and `CancelJobUseCase` in `modules/experiment/src/main/java/com/cryptostrategy/platform/experiment/api/port/in/{StopExperimentUseCase,CancelJobUseCase}.java` and `modules/experiment/src/main/java/com/cryptostrategy/platform/experiment/internal/ExperimentApplicationService.java`
- [X] T042 [US4] Implement cancel polling and cancel confirmation methods in `modules/experiment/src/main/java/com/cryptostrategy/platform/experiment/internal/JobAggregate.java` and `modules/experiment/src/main/java/com/cryptostrategy/platform/experiment/internal/JobApplicationService.java`
- [X] T043 [US4] Implement atomic cancel and stop transitions with row locking in `modules/persistence/src/main/java/com/cryptostrategy/platform/persistence/internal/experiment/{JdbcExperimentStore,JdbcJobStore}.java`

**Checkpoint**: Stop and cancellation lifecycles are fully functional, atomic, and concurrency-safe.

---

## Phase 7: User Story 5 — Reproducibility, Lineage & Provenance Snapshots (Priority: P2)

**Goal**: Persist immutable Candidate Definitions (`candidate_definition`) with deterministic generation indices, record reproduction lineage (`derived_from_experiment_id`, `reproduces_experiment_id`), support Reproduction Runs that create linked new Experiments without mutating original data, and provide full provenance traversal.

**Independent Test**: Create an Experiment with frozen Manifest and 10 Candidates. Initiate a Reproduction Run. Verify a new Experiment entity is created with `reproduces_experiment_id` pointing to the original, original Manifest/Candidates/Jobs/Results remain unmodified, and the new Experiment reuses the exact frozen Candidate Definition list.

### Tests for User Story 5

- [X] T044 [P] [US5] Add failing unit tests for Candidate definition immutability, generation index ordering, duplicate candidate rejection (`UNIQUE(experiment_id, fingerprint)`), and lineage link validation in `modules/experiment/src/test/java/com/cryptostrategy/platform/experiment/internal/CandidateDefinitionTest.java`
- [X] T045 [P] [US5] Add failing unit tests for Reproduction Run initialization verifying original entities remain unmodified and `reproduces_experiment_id` is set in `modules/experiment/src/test/java/com/cryptostrategy/platform/experiment/internal/ReproductionLineageTest.java`
- [X] T046 [P] [US5] Add environment-gated PostgreSQL integration tests verifying Candidate insertion under frozen Experiment, generation index ordering, and reproduction lineage persistence in `modules/persistence/src/experimentIntegrationTest/java/com/cryptostrategy/platform/persistence/experiment/CandidatePersistenceIntegrationTest.java`

### Implementation for User Story 5

- [X] T047 [US5] Implement `CandidateDefinition` record and Candidate use-case ports (`CreateCandidateUseCase`, `ListCandidatesUseCase`) in `modules/experiment/src/main/java/com/cryptostrategy/platform/experiment/api/CandidateDefinition.java` and `modules/experiment/src/main/java/com/cryptostrategy/platform/experiment/api/port/in/{CreateCandidateUseCase,ListCandidatesUseCase}.java`
- [X] T048 [US5] Implement Candidate persistence methods in `modules/persistence/src/main/java/com/cryptostrategy/platform/persistence/internal/experiment/JdbcExperimentStore.java` and `modules/persistence/src/main/java/com/cryptostrategy/platform/persistence/internal/experiment/ExperimentSql.java`
- [X] T049 [US5] Implement `ReproduceExperimentUseCase` in `modules/experiment/src/main/java/com/cryptostrategy/platform/experiment/api/port/in/ReproduceExperimentUseCase.java` and `modules/experiment/src/main/java/com/cryptostrategy/platform/experiment/internal/ExperimentApplicationService.java`

**Checkpoint**: Provenance, Candidate definitions, and reproduction lineage are persistent, immutable, and traceable.

---

## Phase 8: User Story 6 — Durable Idempotency & Transactional Outbox (Priority: P2)

**Goal**: Implement durable idempotency with an active `request_hash` conflict gate and atomic `claim()`, and transactional Outbox writes co-committed strictly for 6 cross-boundary dispatch/cancellation triggers (`ExperimentQueued`, `ExperimentStopRequested`, `JobQueued` on create, `JobQueued` on retry requeue, `JobCancelRequested`, `JobCancelled`) while excluding internal progress mutations.

**Independent Test**:
1. Submit same key + same hash twice -> second request returns cached outcome without re-executing.
2. Submit same key + different hash -> rejected with `IdempotencyConflictException`.
3. Create Job and force transaction rollback -> neither Job nor Outbox event is committed.
4. Increment progress counters -> verify zero Outbox rows are inserted.

### Tests for User Story 6

- [X] T050 [P] [US6] Add failing unit tests for `IdempotentCommandExecutor` verifying active `request_hash` gate, replay of in-progress and completed outcomes, rejection of payload conflicts, and exception handling in `modules/experiment/src/test/java/com/cryptostrategy/platform/experiment/internal/IdempotentCommandExecutorTest.java`
- [X] T051 [P] [US6] Add failing unit tests for Outbox event serialization verifying payload schemas for `ExperimentQueued`, `ExperimentStopRequested`, `JobQueued`, `JobCancelRequested`, and `JobCancelled` in `modules/experiment/src/test/java/com/cryptostrategy/platform/experiment/internal/OutboxEventSerializationTest.java`
- [X] T052 [P] [US6] Add failing JDBC contract tests for `JdbcIdempotencyStore` (atomic `claim()`, `complete()`, `getOutcome()`) and `JdbcOutboxStore` in `modules/persistence/src/test/java/com/cryptostrategy/platform/persistence/internal/experiment/JdbcIdempotencyAndOutboxStoreTest.java`
- [X] T053 [P] [US6] Add environment-gated PostgreSQL integration tests verifying atomic Outbox co-commit, rollback isolation, and non-publishing progress counter updates in `modules/persistence/src/experimentIntegrationTest/java/com/cryptostrategy/platform/persistence/experiment/OutboxPersistenceIntegrationTest.java`
- [X] T054 [P] [US6] Add environment-gated PostgreSQL integration tests verifying atomic idempotency claim concurrency (first caller acquires `IN_PROGRESS`, second receives replay or conflict) in `modules/persistence/src/experimentIntegrationTest/java/com/cryptostrategy/platform/persistence/experiment/IdempotencyPersistenceIntegrationTest.java`

### Implementation for User Story 6

- [X] T055 [US6] Implement `IdempotentCommandExecutor` coordinating atomic claim, command execution, conflict detection, and outcome completion in `modules/experiment/src/main/java/com/cryptostrategy/platform/experiment/internal/IdempotentCommandExecutor.java`
- [X] T056 [US6] Implement Outbox event builder and records adhering to `contracts/idempotency-outbox-contract.md` in `modules/experiment/src/main/java/com/cryptostrategy/platform/experiment/internal/OutboxEvents.java`
- [X] T057 [US6] Implement `JdbcIdempotencyStore` with atomic `INSERT ... ON CONFLICT DO NOTHING` claim semantics in `modules/persistence/src/main/java/com/cryptostrategy/platform/persistence/internal/experiment/JdbcIdempotencyStore.java`
- [X] T058 [US6] Implement `JdbcOutboxStore` inserting into `platform.outbox_event` within active transactions in `modules/persistence/src/main/java/com/cryptostrategy/platform/persistence/internal/experiment/JdbcOutboxStore.java`
- [X] T059 [US6] Wire atomic Outbox insertion only for the approved F-005 cross-boundary triggers across `ExperimentApplicationService` and `JobApplicationService`: `freezeAndQueue()` (`ExperimentQueued`), `stopExperiment()` (`ExperimentStopRequested`), `createBacktestJob()` (`JobQueued`), `requeueRetry()` (`JobQueued`), and `cancelJob()` only when `RUNNING → CANCEL_REQUESTED` (`JobCancelRequested`) or `QUEUED → CANCELLED` (`JobCancelled`). Explicitly emit NO Outbox event for `RETRY_SCHEDULED → CANCELLED` and NO Outbox event for `CANCEL_REQUESTED → CANCELLED` / `confirmCancelled()`

**Checkpoint**: Durable idempotency with active conflict detection and transactional Outbox writes are fully functional and atomic.

---

## Phase 9: User Story 7 — Resilience, Recovery & Forward Migration Backfill (Priority: P3)

**Goal**: Ensure Job/Attempt state survives complete Redis and cache wipe, deliver forward migration for FR-028 legacy Attempt → Job backfill (aborting if ambiguous, asserting zero orphans), and provide query methods for recovery scanning.

**Independent Test**:
1. Run legacy Attempt fixture with unambiguous mapping -> migration succeeds and backfills `job_id`.
2. Run legacy Attempt fixture with ambiguous mapping -> migration aborts without corrupting data.
3. Query unfinished Jobs from durable storage after simulated crash -> verify all Job records and Attempt histories are retrievable.

### Tests for User Story 7

- [X] T060 [P] [US7] Add SQL migration tests for FR-028 legacy Attempt → Job backfill verifying unambiguous backfill, abort on ambiguity, and zero orphan Attempt assertion in `supabase/tests/database/003_f005_legacy_attempt_backfill_test.sql`
- [X] T061 [P] [US7] Add failing JDBC tests for unfinished Job recovery scanning (`listUnfinishedJobs()`) in `modules/persistence/src/test/java/com/cryptostrategy/platform/persistence/internal/experiment/JobRecoveryQueryTest.java`
- [X] T062 [P] [US7] Add environment-gated PostgreSQL integration tests verifying durable recovery of Experiment and Job state after simulated process/cache wipe in `modules/persistence/src/experimentIntegrationTest/java/com/cryptostrategy/platform/persistence/experiment/RecoveryPersistenceIntegrationTest.java`

### Implementation for User Story 7

- [X] T063 [US7] Implement a new forward-only migration under `supabase/migrations/` (choose the timestamp/name only after inspecting the current migration directory) for FR-028 legacy Execution Attempt → Job backfill: derive a parent Job only when mapping is deterministic and unique, abort on zero/multiple candidate mappings, backfill valid `job_id` values, assert zero orphan Attempts, and only then tighten final FK/NOT NULL constraints; never edit an already-applied migration in `supabase/migrations/20260830000200_f005_legacy_attempt_backfill.sql`
- [X] T064 [US7] Implement recovery query methods (`listUnfinishedJobs()`) in `modules/persistence/src/main/java/com/cryptostrategy/platform/persistence/internal/experiment/{ExperimentSql,JdbcJobStore}.java`

**Checkpoint**: Recovery queries and legacy migration backfills are verified and safe.

---

## Phase 10: External Contract Bindings

**Purpose**: Finalize integration with published F-003 and F-004 contracts. F-003 Dataset integration is resolved and actionable on this branch.

- [X] T065 Bind `DatasetProvenanceSnapshot` to the published F-003 Dataset Version contract in `modules/experiment/src/main/java/com/cryptostrategy/platform/experiment/api/provenance/DatasetProvenanceSnapshot.java`: use canonical `DatasetVersionId` as the sole Dataset identity, preserve `version` (`candle-v1`), checksum, provider, Trading Pair, Timeframe, `normalizationVersion`, range, and candle count, and do not introduce a separate Dataset root/model
- [X] T066 Add integration verification tests against the published F-003 contract in `modules/experiment/src/test/java/com/cryptostrategy/platform/experiment/internal/DatasetProvenanceBindingTest.java`, proving the frozen Manifest uses canonical `DatasetVersionId`, preserves F-003 Dataset Version provenance/checksum semantics, and does not duplicate Dataset membership or checksum calculation
- [X] T067 [P] Verify `StrategyProvenanceSnapshot` alignment with F-004 `strategy.user_strategy_version` and registry contracts in `modules/experiment/src/test/java/com/cryptostrategy/platform/experiment/internal/StrategyProvenanceBindingTest.java`

**Checkpoint**: F-003 Dataset binding is verified against the published canonical contract; external integration boundaries remain decoupled from F-005-owned persistence/lifecycle behavior.

---

## Phase 11: Polish, Quality Gates & Verification

**Purpose**: Execute the full verification suite across all non-blocked F-005 layers and record external dependency blockers truthfully; do not claim 100% checklist completion while F-003 binding remains unresolved.

- [X] T068 [P] Run full unit test suite across `:modules:experiment:test` and `:modules:persistence:test`
- [X] T069 [P] Run ArchUnit architecture tests across `:architecture-tests:test`
- [X] T070 Run full isolated database integration test suite across `:modules:persistence:experimentIntegrationTest`
- [X] T071 Run Supabase database test suite across `supabase test db`
- [X] T072 [P] Verify all 112 items in `specs/005-durable-job-persistence/checklists/requirements.md` against the final artifacts, including resolved F-003 Dataset binding, and update checklist status accurately before implementation handoff

---

## Dependencies & Execution Order

```mermaid
flowchart TD
    Phase1[Phase 1: Setup & Migrations] --> Phase2[Phase 2: Foundational Types & Ports]
    Phase2 --> US1[Phase 3: US1 - Experiment & Manifest Freeze (P1)]
    Phase2 --> US2[Phase 4: US2 - Ownership & Authorization (P1)]
    US1 --> US3[Phase 5: US3 - Job Identity & Retry History (P1)]
    US2 --> US3
    US3 --> US4[Phase 6: US4 - Stop & Cancel Lifecycle (P2)]
    US1 --> US5[Phase 7: US5 - Reproducibility & Lineage (P2)]
    US3 --> US6[Phase 8: US6 - Idempotency & Outbox (P2)]
    US3 --> US7[Phase 9: US7 - Resilience & Backfill (P3)]
    US5 --> Blocked[Phase 10: External Contract Bindings]
    US4 --> Polish[Phase 11: Polish & Quality Gates]
    US6 --> Polish
    US7 --> Polish
```

---

## Parallel Opportunities

### Parallel Within Phases

- **Phase 1 (Setup)**: `T002`, `T003`, `T004` can run in parallel after `T001`.
- **Phase 2 (Foundational)**: Tests `T005`, `T007`, `T009`, `T011`, `T013` can run in parallel.
- **Phase 3 (US1)**: Test tasks `T015`, `T016`, `T017`, `T018` can run in parallel before implementation.
- **Phase 4 (US2)**: Test tasks `T024`, `T025`, `T026` can run in parallel.
- **Phase 5 (US3)**: Test tasks `T029`, `T030`, `T031`, `T032`, `T033` can run in parallel.
- **Phase 6 (US4)**: Test tasks `T038`, `T039`, `T040` can run in parallel.
- **Phase 7 (US5)**: Test tasks `T044`, `T045`, `T046` can run in parallel.
- **Phase 8 (US6)**: Test tasks `T050`, `T051`, `T052`, `T053`, `T054` can run in parallel.
- **Phase 9 (US7)**: Tasks `T060`, `T061`, `T062` can run in parallel.
- **Phase 11 (Polish)**: `T068`, `T069`, `T072` can run in parallel.

---

## Tasks Summary & Implementation Status

- **Total Tasks**: 72
- **Completed Tasks**: 72
- **Blocked Tasks**: 0
- **Critical Path**: Completed.
- **Status**: **100% IMPLEMENTED**

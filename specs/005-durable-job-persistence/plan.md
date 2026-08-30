# Implementation Plan: F-005 Experiment Persistence and Ownership

**Branch**: `feature/005-experiment-persistence` | **Date**: 2026-08-30 | **Spec**: [spec.md](spec.md)  
**Input**: Feature specification from `specs/005-durable-job-persistence/spec.md`  

---

## Summary

F-005 delivers the durable persistence, lifecycle state machines, and ownership authorization layer for the Experiment graph. It establishes:
1. The **Experiment** aggregate root and immutable **Experiment Manifest** (frozen at the `CREATED → QUEUED` boundary with deterministic SHA-256 fingerprinting).
2. Immutable **Candidate Definitions** owned by a single Experiment.
3. Durable **Job** work identity (for Search and Backtest operations) and append-only **Execution Attempt** history (terminal statuses `QUEUED`, `RUNNING`, `SUCCEEDED`, `FAILED`, `CANCELLED`; monotonic attempt number allocation).
4. **User Ownership** authorization rooted in Supabase Auth UUID (`owner_user_id`) traversing the authoritative hierarchy (`attempt → job → experiment → owner`, `candidate → experiment → owner`).
5. **Durable Idempotency** with active `request_hash` conflict detection.
6. **Transactional Outbox** writes co-committed strictly for 6 cross-boundary dispatch and cancellation triggers (`ExperimentQueued`, `ExperimentStopRequested`, `JobQueued`, `JobCancelRequested`, `JobCancelled`).
7. Decoupled **Persistence Output Ports** in `modules/experiment` implemented by JDBC adapters in `modules/persistence`.
8. Forward-only migration(s) aligning Manifest fingerprint lifecycle, reproduction lineage, durable idempotency claim state, Execution Attempt status, and the FR-028 legacy Attempt→Job backfill.

---

## Technical Context

**Language/Version**: Java 21 (LTS), SQL (PostgreSQL 15+ compatible with Supabase)  
**Primary Dependencies**: Spring Boot 3.3.x (`spring-jdbc`), Jackson (`jackson-databind`), SLF4J, ArchUnit, JUnit 5, AssertJ  
**Storage**: PostgreSQL (Supabase schemas `experiment`, `strategy`, `platform`, `auth`)  
**Testing**: JUnit 5, AssertJ, ArchUnit architecture rules, isolated Spring JDBC integration tests (`experimentIntegrationTest`), pgTAP / Supabase SQL assertions  
**Target Platform**: JVM (Linux server / container, Windows/macOS developer environments)  
**Project Type**: Modular Monolith Domain & Persistence Layer (Java Library / Internal Capability)  
**Performance Goals**: Sub-10ms primary key / indexed query latency, deterministic SHA-256 fingerprint generation in < 1ms, zero race-condition duplicates for concurrent Job creation and Attempt numbering  
**Constraints**:
- Strict dependency direction: `apps/* → contracts → domain ← capabilities (experiment) ← persistence` (ADR-0002).
- Zero direct browser/frontend access to business tables; authorization enforced at Java application boundary (ADR-0011).
- Applied migrations must never be edited; forward migrations only (Constitution).
- F-005 owns Outbox write-side only; F-007 owns polling, Redis Streams, and worker orchestration.
- Zero duplication of F-003 Dataset or F-004 Strategy canonical contracts.  
**Scale/Scope**: Up to 10,000 Candidates per Experiment, append-only Attempt history surviving complete Redis wipe.

---

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-checked after Phase 1 design.*

| Principle / Rule | Compliance Status | Analysis & Evidence |
|---|---|---|
| **I. Specification first, ADR for architecture decisions** | **PASS** | Driven directly by `spec.md` and finalized clarifications. Reconciled with accepted ADR-0002, ADR-0006, ADR-0007, ADR-0009, ADR-0011, ADR-0012. |
| **II. Explicit module & data ownership** | **PASS** | `modules/experiment` owns Experiment, Manifest, Candidate, Job, and Attempt domain entities and declares output ports. `modules/persistence` implements output ports. No cross-module internal package leakage. |
| **III. Reproducibility & immutable evidence** | **PASS** | Manifest permanently frozen at `CREATED → QUEUED` with deterministic SHA-256 fingerprint. Candidates and Results immutable. Retries create Attempts under same Job; Reproduction runs create linked new Experiments without overwriting originals. |
| **IV. Versioned contracts & provider isolation** | **PASS** | F-003 Dataset provenance and F-004 Strategy provenance integrated via typed snapshot value objects without duplicating domain logic. |
| **V. Security, reliability, observability & verifiability** | **PASS** | Authenticated `owner_user_id` enforced on all operations. Outbox written atomically with business state changes. Idempotency records prevent double execution and reject payload conflicts. Distributed tracing with `correlationId`. |
| **Database Gate: Forward-only migrations** | **PASS (plan)** | No applied migration will be edited. F-005 requires new forward migration(s) for schema alignment/backfill; exact filenames/timestamps are chosen during implementation after inspecting the current migration directory. |

---

## Project Structure

### Documentation (this feature)

```text
specs/005-durable-job-persistence/
├── spec.md                          # Finalized feature specification
├── checklists/
│   └── requirements.md              # Quality requirements checklist (16/16 pass)
├── research.md                      # Phase 0: Research & technical design decisions
├── data-model.md                    # Phase 1: Database schemas, constraints, ER diagram
├── quickstart.md                    # Phase 1: Test execution & validation guide
├── contracts/                       # Phase 1: Canonical interface & event contracts
│   ├── job-execution-attempt-contract.md
│   ├── experiment-manifest-contract.md
│   ├── persistence-ports-contract.md
│   └── idempotency-outbox-contract.md
└── tasks.md                         # Phase 2: Actionable task list (generated by /speckit-tasks)
```

### Source Code Layout

```text
modules/domain/
└── src/main/java/com/cryptostrategy/platform/domain/api/identity/
    ├── UlidIdentifier.java          # Marker interface for ULID identifiers
    └── Ulids.java                   # Canonical Crockford ULID generator/validator

modules/experiment/
├── build.gradle.kts                 # Declares dependency on :modules:domain
└── src/
    ├── main/java/com/cryptostrategy/platform/experiment/
    │   ├── api/
    │   │   ├── Experiment.java                     # Experiment aggregate root interface
    │   │   ├── ExperimentId.java                   # Strongly typed ULID identifier
    │   │   ├── CandidateId.java                    # Strongly typed ULID identifier
    │   │   ├── ExperimentStatus.java               # CREATED, QUEUED, RUNNING, COMPLETED, FAILED, STOP_REQUESTED, STOPPED
    │   │   ├── ExperimentManifest.java             # Immutable manifest record & fingerprint calculator
    │   │   ├── CandidateDefinition.java            # Immutable candidate definition record
    │   │   ├── job/
    │   │   │   ├── Job.java                        # Job aggregate root interface
    │   │   │   ├── JobId.java                      # Strongly typed ULID identifier
    │   │   │   ├── JobType.java                    # SEARCH, BACKTEST
    │   │   │   ├── JobStatus.java                  # QUEUED, RUNNING, RETRY_SCHEDULED, SUCCEEDED, FAILED, CANCEL_REQUESTED, CANCELLED
    │   │   │   ├── ExecutionAttempt.java           # Execution attempt entity interface
    │   │   │   ├── AttemptId.java                  # Strongly typed ULID identifier
    │   │   │   ├── AttemptStatus.java              # QUEUED, RUNNING, SUCCEEDED, FAILED, CANCELLED
    │   │   │   ├── WorkerId.java                   # Strongly typed worker identifier
    │   │   │   └── FailureClassification.java      # Error category enum
    │   │   ├── provenance/
    │   │   │   ├── DatasetProvenanceSnapshot.java  # F-003 dataset integration snapshot
    │   │   │   └── StrategyProvenanceSnapshot.java # F-004 strategy integration snapshot
    │   │   └── port/out/
    │   │       ├── ExperimentStore.java            # Output port for Experiment & Candidate persistence
    │   │       ├── JobStore.java                   # Output port for Durable Job persistence
    │   │       ├── ExecutionAttemptStore.java      # Output port for Execution Attempt persistence
    │   │       ├── IdempotencyStore.java           # Output port for Idempotency record persistence
    │   │       └── OutboxStore.java                # Output port for Outbox event persistence
    │   └── internal/
    │       ├── ExperimentAggregate.java            # Concrete Experiment domain implementation
    │       ├── JobAggregate.java                   # Concrete Job domain implementation
    │       ├── ExecutionAttemptEntity.java         # Concrete Attempt domain implementation
    │       ├── CanonicalFingerprintCalculator.java # Deterministic SHA-256 JSON serializer/hasher
    │       ├── ExperimentApplicationService.java   # Use-case service for Experiment commands & queries
    │       ├── JobApplicationService.java          # Use-case service for Job & Attempt lifecycle
    │       └── IdempotentCommandExecutor.java      # Active request_hash idempotency coordinator
    └── test/java/com/cryptostrategy/platform/experiment/
        ├── ExperimentDomainTest.java               # State machine & freeze invariant unit tests
        ├── JobDomainTest.java                      # Job lifecycle & retry calculation unit tests
        ├── ExecutionAttemptDomainTest.java         # Attempt status & terminal transition unit tests
        └── CanonicalFingerprintTest.java           # SHA-256 fingerprint determinism unit tests

modules/persistence/
├── build.gradle.kts                 # Dependencies on :modules:domain, :modules:experiment, spring-jdbc
└── src/
    ├── main/java/com/cryptostrategy/platform/persistence/
    │   ├── api/
    │   │   └── ExperimentPersistenceFactory.java   # Public factory exposing store port implementations
    │   └── internal/experiment/
    │       ├── JdbcExperimentStore.java            # JDBC adapter for Experiment & Candidates
    │       ├── JdbcJobStore.java                   # JDBC adapter for Durable Jobs
    │       ├── JdbcExecutionAttemptStore.java      # JDBC adapter for Execution Attempts (serialized attempt_no)
    │       ├── JdbcIdempotencyStore.java           # JDBC adapter for platform.idempotency_record
    │       ├── JdbcOutboxStore.java                # JDBC adapter for platform.outbox_event
    │       ├── ExperimentSql.java                  # Centralized SQL query constants
    │       ├── ExperimentRows.java                 # RowMapper implementations
    │       ├── ExperimentJsonMapper.java           # Jackson JSONB serializer/deserializer
    │       └── ExperimentExceptionTranslator.java  # Maps SQLExceptions to domain exceptions
    ├── test/java/com/cryptostrategy/platform/persistence/internal/experiment/
    │   ├── ExperimentSqlContractTest.java          # SQL syntax & column mapping contract tests
    │   └── ExperimentJsonMapperTest.java           # JSONB serialization unit tests
    └── experimentIntegrationTest/java/com/cryptostrategy/platform/persistence/experiment/
        ├── ExperimentPersistenceIntegrationTest.java   # Experiment freeze, ownership, query integration tests
        ├── JobPersistenceIntegrationTest.java          # Job lifecycle, unique candidate, retry integration tests
        ├── AttemptPersistenceIntegrationTest.java      # Monotonic attempt allocation concurrency tests
        ├── IdempotencyPersistenceIntegrationTest.java  # Active hash replay & conflict integration tests
        └── OutboxPersistenceIntegrationTest.java       # Transactional co-commit & rollback integration tests

supabase/migrations/
├── <next>_f005_schema_alignment.sql            # fingerprint lifecycle, reproduction lineage, idempotency state, Attempt status
└── <next>_f005_execution_attempt_backfill.sql  # deterministic FR-028 legacy Attempt -> Job backfill; aborts if ambiguous
```

---

## 4. Transaction Boundaries & Concurrency Strategy

Every state-changing operation executes within an explicit database transaction boundary. Coupled Job/Attempt state must never be left half-committed.

| Operation | Transaction Boundary & Actions | Concurrency / Locking Strategy | Outbox Event Emitted |
|---|---|---|---|
| **Create Experiment** | Insert `experiment.experiment` (`CREATED`) + draft `experiment_manifest` with `fingerprint = NULL` | Single transaction; unique `experiment_id` | None |
| **Update CREATED Experiment** | Update name/Manifest only while status is `CREATED` | Lock/expected-status predicate; reject once non-CREATED | None |
| **Freeze Experiment** (`CREATED → QUEUED`) | Lock Experiment -> validate Manifest -> compute/store SHA-256 fingerprint -> transition to `QUEUED` -> insert Outbox | `SELECT ... FOR UPDATE` on Experiment; verify `CREATED` | `ExperimentQueued` |
| **Create Search Job** | Insert one SEARCH Job for Experiment, `candidate_id = NULL` | Unique partial index per Experiment; verify owned/frozen Experiment | None; dispatch is driven by `ExperimentQueued` |
| **Create Backtest Job** | Verify Candidate + same Experiment -> atomically create durable Job (`QUEUED`) -> insert Outbox; wrap in idempotent command claim when applicable | Unique Backtest Job per Candidate + composite FK | `JobQueued` |
| **Start Backtest Attempt** | Lock parent Job -> verify `QUEUED`/dispatch-ready -> read `MAX(attempt_no)+1` -> transition Job `QUEUED → RUNNING` -> insert/start Attempt with allocated number | **Lock parent Job row first**; `UNIQUE(job_id, attempt_no)` final collision guard | None |
| **Backtest succeeds** | Lock Job/Attempt -> finalize Attempt `SUCCEEDED` -> transition Job `RUNNING → SUCCEEDED` | Same transaction prevents Attempt/Job divergence | None |
| **Schedule Job Retry** | Lock Job/Attempt -> finalize current Attempt `FAILED` -> transition Job `RUNNING → RETRY_SCHEDULED` + set `next_retry_at` | Row lock on Job; same transaction | None |
| **Requeue Retry** (`RETRY_SCHEDULED → QUEUED`) | Lock Job -> verify still `RETRY_SCHEDULED` and retry eligible -> transition to `QUEUED` -> insert Outbox | Row lock serializes requeue against cancellation | `JobQueued` |
| **Terminal Job Failure** | Finalize current Attempt `FAILED` + transition Job `RUNNING → FAILED` | Same transaction | None |
| **Cancel Job request** | `RUNNING → CANCEL_REQUESTED` + Outbox; `QUEUED → CANCELLED` + Outbox; `RETRY_SCHEDULED → CANCELLED` durable local transition only | Lock Job row; serializes cancel vs requeue | `JobCancelRequested` or `JobCancelled` only for specified publishing transitions |
| **Worker confirms cancellation** | Finalize active Attempt `CANCELLED` + Job `CANCEL_REQUESTED → CANCELLED` | Same transaction | None (request event was already persisted) |
| **Stop Experiment** (`RUNNING → STOP_REQUESTED`) | Transition Experiment + insert Outbox | Lock Experiment row | `ExperimentStopRequested` |
| **Idempotent command claim** | Atomically insert `IN_PROGRESS` record. Exactly one caller acquires execution; losers resolve replay/conflict by persisted hash/state. Completion updates the same record. | PK/unique key `(user_id, scope, idempotency_key)` + atomic insert-on-conflict | Determined by underlying business command |

### 4.1. Monotonic Attempt Allocation

Do **not** use `SELECT MAX(attempt_no) ... FOR UPDATE` as the lock primitive. The parent Job row is the serialization point:

```sql
BEGIN;

SELECT job_id, status
FROM experiment.job
WHERE job_id = ?
FOR UPDATE;

SELECT COALESCE(MAX(attempt_no), 0) + 1
FROM experiment.execution_attempt
WHERE job_id = ?;

-- validate Job state, then insert Attempt with the allocated number
-- and transition Job to RUNNING in this same transaction.

COMMIT;
```

This guarantees one allocator at a time per Job; `UNIQUE(job_id, attempt_no)` remains the database collision guard.

### 4.2. Durable Idempotency Claim

The persistence API must expose an atomic `claim`, not a non-atomic `check()` followed by a later insert:

```text
no row
  -> atomically create IN_PROGRESS
  -> ACQUIRED (only one caller executes)

same key + same hash + IN_PROGRESS
  -> IN_PROGRESS_REPLAY
  -> do not execute

same key + same hash + COMPLETED
  -> COMPLETED_REPLAY
  -> return original outcome

same key + different hash
  -> CONFLICT
  -> do not execute
```

The record therefore stores explicit state (`IN_PROGRESS` / `COMPLETED`), and completion fields may be null until the command completes.

### 4.3. FR-028 Legacy Attempt Backfill

The migration plan must include the legacy Execution Attempt → Job backfill required by FR-028, not just the Attempt status check change:

1. detect orphan/legacy Attempt rows;
2. map only when exactly one valid Job can be derived;
3. abort on ambiguous or missing mappings;
4. backfill `job_id`;
5. assert zero orphans remain;
6. then apply/tighten final constraints.


---

## 5. Authorization & Ownership Architecture

1. **Authentication Principle**: Supabase Auth owns user identities. The application receives the authenticated `UUID ownerUserId` from the security context (Bearer JWT validation in F-009/API layer).
2. **Authoritative Ownership Chains**:
   - `Experiment` → `experiment.owner_user_id == caller_user_id`
   - `Candidate` → `candidate.experiment_id → experiment.owner_user_id == caller_user_id`
   - `Job` → `job.experiment_id → experiment.owner_user_id == caller_user_id`
   - `ExecutionAttempt` → `attempt.job_id → job.experiment_id → experiment.owner_user_id == caller_user_id`
3. **Inaccessible Outcomes**: If a requested resource ID does not exist OR belongs to another owner, the service returns `Optional.empty()` or throws `ResourceInaccessibleException`. The response does NOT reveal whether the resource exists under a different owner.
4. **Database Privilege Isolation**: Database roles `anon` and `authenticated` have `REVOKE ALL` on all business tables. Java backend uses a dedicated backend database user.

---

## 6. Verification & Test Plan

### 6.1. Automated Unit & State Machine Tests
- `ExperimentDomainTest`: Verifies `CREATED → QUEUED → RUNNING → COMPLETED | FAILED | STOP_REQUESTED → STOPPED`. Rejects invalid transitions (e.g. `CREATED → RUNNING`, editing manifest after `QUEUED`).
- `CanonicalFingerprintTest`: Verifies SHA-256 fingerprint determinism across field ordering, UTC timestamps, decimal formatting, and detects single-field mutations.
- `JobDomainTest`: Verifies `QUEUED → RUNNING → SUCCEEDED`, `RUNNING → RETRY_SCHEDULED → QUEUED`, `RUNNING → FAILED`, and `RETRY_SCHEDULED → CANCELLED`. Verifies exponential backoff calculation.
- `ExecutionAttemptDomainTest`: Verifies that Attempts only take terminal statuses (`SUCCEEDED`, `FAILED`, `CANCELLED`) and reject `RETRY_SCHEDULED`.
- `IdempotentCommandExecutorTest`: Verifies same-key + same-hash returns cached outcome; same-key + different-hash throws `IdempotencyConflictException`.

### 6.2. Architecture Tests (`:architecture-tests`)
- `ModuleBoundaryTest`: Verifies production packages respect the dependency matrix (`modules/persistence` depends on `modules/experiment`, `modules/experiment` depends only on `modules/domain`).
- `PurityAndCycleTest`: Verifies zero circular dependencies and that `modules/experiment` contains zero JDBC or SQL imports.

### 6.3. Database Integration Tests (`:modules:persistence:experimentIntegrationTest`)
- **Ownership Isolation**: Two users (User A & User B). User A creates an Experiment and Job. User B attempts read/update/stop using User A's IDs; verifies access is denied with zero data leakage.
- **Freeze Atomicity & Manifest Immutability**: Verifies a CREATED Manifest can persist with no fingerprint; freezing atomically stores the fingerprint, updates status, and writes `ExperimentQueued`. Direct modification after QUEUED fails.
- **Job Uniqueness per Candidate**: Concurrent attempts to create a Backtest Job for the same Candidate result in exactly one Job.
- **Monotonic Attempt Allocation Concurrency**: concurrent starters lock the parent Job row; verifies sequential attempt numbers with zero collisions and no half-committed Job/Attempt state.
- **Cancel vs Requeue Race**: verifies one unambiguous durable result. `RETRY_SCHEDULED → CANCELLED` emits no Outbox; if requeue wins first, `QUEUED → CANCELLED` emits `JobCancelled`.
- **Outbox Atomicity & Rollback**: Verifies that throwing an exception during a business command rolls back both the business state and the Outbox event.
- **Idempotency Durability & Claim Concurrency**: verifies exactly one concurrent first caller acquires execution; same-hash losers replay, different-hash reuse conflicts, and state survives repository restart.
- **Legacy Attempt Backfill**: unambiguous fixture succeeds; ambiguous fixture aborts; post-migration assertion reports zero orphan Attempts.
- **Coupled Job/Attempt Atomicity**: failure injection between Attempt and Job updates rolls back both, covering start, success, retry scheduling, terminal failure, and cancellation acknowledgement.
- **Reproduction Lineage**: verifies `reproduces_experiment_id` persists independently from `derived_from_experiment_id`.

---

## 7. Traceability Matrix: Staging `specs/002-user-strategy-jobs` Migration

| Staging Requirement (from `specs/002`) | F-005 Specification Requirement | Plan Artifact / Implementation Target |
|---|---|---|
| Two-phase Experiment lifecycle (Draft & Confirm) | `FR-002`, `FR-007` | `ExperimentStatus.CREATED` & `QUEUED`, `ExperimentStore.freezeAndQueue()` |
| Manifest provenance snapshot & fingerprint | `FR-001`, `FR-003` | `ExperimentManifest`, `CanonicalFingerprintCalculator` |
| Immutable Candidate definitions | `FR-008`, `FR-009`, `FR-010` | `CandidateDefinition`, `ExperimentStore.insertCandidate()` |
| Durable Job identity for Search & Backtest | `FR-012`, `FR-013` | `JobAggregate`, `JobStore.insert()` |
| Single Backtest Job per Candidate | `FR-014` | Database constraint `job_backtest_candidate_unique`, `JobStore` |
| Job state machine & cancel polling | `FR-015`, `FR-016` | `JobStatus`, `Job.isCancelRequested()`, `Job.requestCancel()` |
| Execution Attempt history under Job | `FR-017`, `FR-018`, `FR-019`, `FR-020` | `ExecutionAttemptEntity`, `ExecutionAttemptStore.startNewAttempt()` |
| Authenticated owner isolation | `FR-021`, `FR-022`, `FR-023`, `FR-024` | `owner_user_id` predicates on all store ports & application services |
| Decoupled Persistence Ports & Adapters | `FR-025`, `FR-026` | `com.cryptostrategy.platform.experiment.api.port.out.*`, `Jdbc*Store` |
| Forward-only migrations & legacy backfill | `FR-027`, `FR-028` | new F-005 schema-alignment migration(s) + deterministic Attempt→Job backfill migration/tests |
| Durable Idempotency with active hash gate | `FR-029`, `FR-030` | `JdbcIdempotencyStore`, `IdempotentCommandExecutor` |
| Transactional Outbox write side | `FR-031`, `FR-032`, `FR-033` | `JdbcOutboxStore`, atomic multi-store transactions |
| Transaction-enforced invariants | `FR-034`, `FR-035` | Database constraints + domain aggregate validation rules |

---

## 8. Known Blockers & Integration Notes

- **F-003 Market Data Dataset Integration — RESOLVED**: F-003 is available on this branch. `DatasetVersionId` is the canonical immutable Dataset identity; there is no separate Dataset root. `DatasetProvenanceSnapshot` binds to F-003's published Dataset Version metadata and `candle-v1` checksum contract (`version`, checksum, provider, Trading Pair, Timeframe, `normalizationVersion`, range, candle count). F-005 does not duplicate Dataset membership, checksum calculation, provider normalization, or Market Data ingestion logic.
- **F-004 Strategy Library Integration**: The Strategy snapshot contract (`StrategyProvenanceSnapshot`) and optional `source_user_strategy_version_id` link integrate with `strategy.user_strategy_version` without re-implementing user strategy versioning. If the final F-004 public contract is unavailable, that integration alone remains a typed-placeholder blocker.

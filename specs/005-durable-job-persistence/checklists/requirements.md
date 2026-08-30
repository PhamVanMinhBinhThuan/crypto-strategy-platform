# Requirements Quality & Consistency Checklist: F-005 Experiment Persistence and Ownership

**Purpose**: Validate requirements quality, completeness, consistency, traceability, and architectural boundary clarity across specification, planning, data model, and contract artifacts prior to task generation.  
**Created**: 2026-08-30  
**Feature**: [spec.md](../spec.md) | [plan.md](../plan.md) | [data-model.md](../data-model.md)  
**Contracts**: [experiment-manifest-contract.md](../contracts/experiment-manifest-contract.md) | [job-execution-attempt-contract.md](../contracts/job-execution-attempt-contract.md) | [persistence-ports-contract.md](../contracts/persistence-ports-contract.md) | [idempotency-outbox-contract.md](../contracts/idempotency-outbox-contract.md)  

---

## 1. Feature Scope and Boundaries

- [x] CHK001 Are the explicit ownership boundaries of F-005 clearly restricted to Experiment/Manifest/Candidate persistence, Job/Attempt lifecycle, Supabase user authorization, durable idempotency, Outbox write side, and forward migrations? [Completeness, Spec §Overview, Plan §Summary]
- [x] CHK002 Are downstream capabilities (Backtest engine in F-006, Redis Streams & Worker orchestration in F-007, REST/realtime APIs in F-009, UI in F-010) explicitly declared out of scope for F-005? [Boundary Clarity, Spec §Overview, Plan §Summary]
- [x] CHK003 Is Outbox responsibility strictly partitioned between F-005 (transactional persistence of Outbox event rows) and F-007 (polling, Redis Streams publishing, deduplication, and worker delivery)? [Boundary Clarity, Spec §FR-033, Plan §Summary]
- [x] CHK004 Does the specification avoid declaring HTTP status mappings or REST error codes, leaving transport mapping to F-009? [Boundary Clarity, Spec §US-2 Scenario 1, §FR-029]

---

## 2. Experiment Lifecycle

- [x] CHK005 Are the canonical Experiment lifecycle statuses consistently defined as `CREATED`, `QUEUED`, `RUNNING`, `COMPLETED`, `FAILED`, `STOP_REQUESTED`, `STOPPED` across all artifacts? [Consistency, Spec §FR-007, Plan §Summary, Data Model §2.1]
- [x] CHK006 Are obsolete statuses (`DRAFT`, `CONFIRMED`) completely retired from persisted Experiment state machine definitions? [Consistency, Spec §Clarifications Q2, §FR-007, Data Model §2.1]
- [x] CHK007 Is `CREATED → QUEUED` consistently defined as the atomic Manifest validation, fingerprint calculation, freeze, and `ExperimentQueued` Outbox publication boundary? [Clarity, Spec §FR-002, Plan §4, Contracts §1.1]
- [x] CHK008 Is Manifest mutability strictly restricted to the `CREATED` status, with 100% of post-freeze mutations rejected? [Completeness, Spec §FR-002, §SC-006, Plan §4]
- [x] CHK009 Does the specification define unambiguous transitions for Experiment stop (`RUNNING → STOP_REQUESTED → STOPPED`) and terminal outcomes (`RUNNING → COMPLETED | FAILED`)? [Completeness, Spec §FR-007, Contracts §1.1]

---

## 3. Manifest and Fingerprint

- [x] CHK010 Is `experiment_manifest.fingerprint` permitted to be null while an Experiment is in `CREATED` status and strictly required from `QUEUED` onward? [Consistency, Plan §4, Data Model §2.2, Research §1.1]
- [x] CHK011 Are the canonicalization rules for SHA-256 `experimentFingerprint` computation explicitly defined (sorted JSON keys, UTC timestamps, decimal string formatting, exclusion of runtime metadata)? [Clarity, Spec §FR-003, Plan §6.1, Contracts §2.2]
- [x] CHK012 Does the specification require fingerprint re-computation to be deterministic and verifiable against the stored manifest fields? [Measurability, Spec §SC-005, Contracts §2.2]
- [x] CHK013 Is a missing mandatory provenance field at the `CREATED → QUEUED` boundary specified to reject the freeze transaction without modifying state? [Edge Cases, Spec §Edge Cases, Plan §4]

---

## 4. Reproduction Lineage

- [x] CHK014 Are both lineage relationships (`derived_from_experiment_id` for variations and `reproduces_experiment_id` for exact reproduction runs) explicitly modeled? [Completeness, Spec §FR-004, Data Model §2.1, Plan §7]
- [x] CHK015 Does the specification state that reproduction runs MUST create a new Experiment entity and MUST NOT overwrite original Manifests, Candidates, Jobs, Attempts, or Results? [Consistency, Spec §US-5 Scenario 3, §Edge Cases, Research §1.1]
- [x] CHK016 Are reproduction runs specified to replay from the frozen Candidate Definition list rather than re-running non-deterministic time-based generation? [Clarity, Spec §US-5 Scenario 3, §FR-011]

---

## 5. F-003 / F-004 Provenance Integration

- [x] CHK017 Does F-005 avoid creating duplicate or parallel canonical models for Dataset (F-003) and Strategy (F-004)? [Boundary Clarity, Spec §Integration Dependency Notice, Plan §8]
- [x] CHK018 Does the F-003 Dataset provenance snapshot bind to the canonical published Dataset Version contract without inventing a parallel Dataset model? Verified mapping: `DatasetVersionId` is the stable Dataset identity (no separate Dataset root), `version` is the String Dataset/checksum contract ID (`candle-v1` for F-003), and the snapshot preserves checksum, provider, Trading Pair, Timeframe, `normalizationVersion`, range, and candle count. [Completeness, Resolved Integration, Spec §FR-005, F-003 Data Model §Dataset Version, Contracts §1.2]
- [x] CHK019 Are F-004 Strategy provenance attributes (plugin ID, version, parameters, composite policy, ordered components, weights, optional `source_user_strategy_version_id`) encapsulated in a typed snapshot value object? [Completeness, Spec §FR-006, Contracts §1.2]
- [x] CHK020 Is F-003 explicitly documented as a resolved integration using its published Dataset Version contract, while any still-unavailable F-004 contract is isolated as a separate dependency rather than conflated with F-003? [Dependencies, Spec §Integration Dependency Notice, Plan §8]

---

## 6. Candidate Invariants

- [x] CHK021 Is the invariant that a Candidate belongs to exactly one Experiment enforced at both the application boundary and database composite foreign key level? [Consistency, Spec §FR-009, §FR-035, Data Model §2.3]
- [x] CHK022 Are Candidate definitions specified to be permanently immutable once inserted under a frozen Experiment? [Clarity, Spec §FR-010, Data Model §2.3]
- [x] CHK023 Is Candidate creation restricted to Experiments that have reached `QUEUED` status? [Consistency, Spec §FR-008, Plan §4]
- [x] CHK024 Are Candidate generation indices unique per Experiment (`UNIQUE(experiment_id, generation_index)`), and are duplicate Candidate definitions separately guarded by per-Experiment fingerprint uniqueness (`UNIQUE(experiment_id, fingerprint)`)? [Completeness, Spec §FR-011, Data Model §2.3]

---

## 7. Job Semantics

- [x] CHK025 Are Search Job semantics (belongs to Experiment, `candidate_id = NULL`, no Execution Attempts in MVP) clearly distinguished from Backtest Job semantics? [Clarity, Spec §FR-013, §FR-017, ADR-0012]
- [x] CHK026 Is the invariant of exactly one logical Backtest Job per Candidate enforced across all lifecycle states (including `FAILED`, `SUCCEEDED`, `CANCELLED`) with no replacement Job allowed upon cancellation? [Completeness, Spec §US-3 Scenario 4, §FR-014, Data Model §2.4]
- [x] CHK027 Is the canonical Job lifecycle state machine (`QUEUED → RUNNING → SUCCEEDED | FAILED`, `RUNNING → RETRY_SCHEDULED → QUEUED`, `RUNNING → CANCEL_REQUESTED → CANCELLED`, `QUEUED → CANCELLED`, `RETRY_SCHEDULED → CANCELLED`) consistently documented across all artifacts? [Consistency, Spec §FR-015, Plan §4, Contracts §3.1]
- [x] CHK028 Are invalid Job state transitions specified to be rejected by application services? [Completeness, Spec §FR-015, Contracts §4.2]
- [x] CHK029 Is cancel polling specified as a non-blocking capability for Worker checkpoints rather than forcibly interrupting threads? [Clarity, Spec §FR-016, Contracts §2]

---

## 8. Execution Attempt Semantics

- [x] CHK030 Does an Execution Attempt represent exactly one concrete Worker try with allowed statuses strictly limited to `QUEUED`, `RUNNING`, `SUCCEEDED`, `FAILED`, `CANCELLED`? [Consistency, Spec §Clarifications Q1, §FR-019, Contracts §3.2]
- [x] CHK031 Is `RETRY_SCHEDULED` explicitly excluded from Execution Attempt statuses and confirmed as a Job-only status? [Consistency, Spec §Clarifications Q1, §FR-019, Data Model §2.5]
- [x] CHK032 On retryable failure, does the specification mandate that the current Attempt is finalized as `FAILED`, the parent Job transitions to `RETRY_SCHEDULED`, and the Job ID remains unchanged? [Clarity, Spec §US-3 Scenario 2, §FR-019, Contracts §4.2]
- [x] CHK033 Is the creation of a new Execution Attempt deferred until a Worker actually begins the next retry try? [Clarity, Spec §US-3 Scenario 2, §FR-020, Plan §4]

---

## 9. Attempt Number Concurrency

- [x] CHK034 Is attempt number allocation specified as serialized per Job, assigning `previous_max_attempt_number + 1`? [Clarity, Spec §FR-018, Plan §4.1]
- [x] CHK035 Does the concurrency plan mandate locking the parent Job row (`SELECT ... FOR UPDATE`) before calculating the next attempt number, rather than using an unsafe attempt-table lock? [Completeness, Plan §4.1, Contracts §1.3]
- [x] CHK036 Is database uniqueness (`UNIQUE(job_id, attempt_no)`) defined as the final collision guard rather than the primary allocation mechanism? [Clarity, Spec §FR-018, Plan §4.1, Data Model §2.5]
- [x] CHK037 Are Attempt and Job state updates (e.g. Job `QUEUED → RUNNING` and Attempt creation) specified to commit within the same atomic transaction to prevent half-committed states? [Consistency, Plan §4, §6.3]

---

## 10. Idempotency

- [x] CHK038 Is durable idempotency scope consistently defined as `owner_user_id` + `operation_scope` + `idempotency_key`? [Consistency, Spec §FR-029, Plan §2, Data Model §2.6]
- [x] CHK039 Is `request_hash` specified as an active conflict detection gate rather than passive audit metadata? [Clarity, Spec §Clarifications Q3, §FR-029, Research §2.1]
- [x] CHK040 Is same key + same hash specified to resolve to the original outcome without re-executing, and same key + different hash specified to reject as an application conflict? [Completeness, Spec §US-6 Scenarios 3-4, §FR-029]
- [x] CHK041 Does the persistence port expose an atomic `claim()` operation with explicit states (`ACQUIRED`, `IN_PROGRESS_REPLAY`, `COMPLETED_REPLAY`, `CONFLICT`) to eliminate non-atomic `check() → save()` race conditions? [Clarity, Plan §4.2, Contracts §4, Data Model §2.6]
- [x] CHK042 Are idempotency records specified to persist across application restarts and remain decoupled from HTTP status representations? [Durability, Spec §US-6, Data Model §2.6, Plan §2]

---

## 11. Transactional Outbox

- [x] CHK043 Are Outbox events strictly limited to approved cross-boundary triggers (`ExperimentQueued`, `ExperimentStopRequested`, `JobQueued` on create, `JobQueued` on retry requeue, `JobCancelRequested`, `JobCancelled`)? [Completeness, Spec §Clarifications Q4, §FR-031, Contracts §1]
- [x] CHK044 Is `RETRY_SCHEDULED → CANCELLED` explicitly documented as a durable local cancellation that does NOT emit an Outbox event? [Clarity, Spec §FR-031, Plan §4, Research §3.1]
- [x] CHK045 Are internal mutations (progress counters, `best_score`, Execution Attempt insertions/status changes, timestamps) explicitly prohibited from emitting Outbox events? [Consistency, Spec §FR-032, Plan §4, Contracts §1]
- [x] CHK046 Does each Outbox row contain a stable `event_id`, `message_id` for consumer deduplication, event type/version, and sufficient routing payload for F-007 dispatch? [Completeness, Spec §FR-033, Data Model §2.7, Contracts §1]
- [x] CHK047 Is Outbox co-commit atomicity specified such that rollback of a business command guarantees rollback of the Outbox row? [Measurability, Spec §US-6 Scenario 1, §FR-031, Plan §6.3]

---

## 12. Atomic Transaction Boundaries

- [x] CHK048 Is the transaction boundary for Experiment creation defined to commit Experiment and draft Manifest atomically? [Completeness, Plan §4]
- [x] CHK049 Is the transaction boundary for Manifest freeze defined to validate, calculate fingerprint, update Experiment status to `QUEUED`, and insert `ExperimentQueued` Outbox row atomically? [Completeness, Spec §FR-002, Plan §4]
- [x] CHK050 Is Backtest Job creation defined to validate Candidate/Experiment consistency, enforce one-Job-per-Candidate, set `QUEUED` status, and insert `JobQueued` Outbox row atomically? [Completeness, Spec §US-3 Scenario 1, Plan §4]
- [x] CHK051 Are Attempt finalization and Job outcome updates (success, retry scheduling, terminal failure, cancellation acknowledgement) defined to commit within single transactions? [Consistency, Plan §4, Contracts §3]
- [x] CHK052 Is the cancel vs retry requeue race serialized via parent Job row locking to guarantee an unambiguous durable final state? [Edge Cases, Spec §FR-015, Plan §4, Research §5.1]

---

## 13. Ownership and Authorization

- [x] CHK053 Are authoritative ownership chains explicitly rooted in `owner_user_id` (`auth.users.id`) for Experiment, Candidate, Job, and Execution Attempt? [Completeness, Spec §US-2, §FR-021, ADR-0011, ADR-0012]
- [x] CHK054 Are child resources prohibited from defining independent ownership fields that could contradict the parent Experiment owner? [Consistency, Spec §FR-022, ADR-0011]
- [x] CHK055 Do all user-facing application services and persistence port methods require authenticated `owner_user_id` predicates? [Completeness, Spec §FR-023, Contracts §1-4]
- [x] CHK056 Does cross-user access produce an ownership-safe inaccessible outcome without leaking existence, content, or owner identity? [Security, Spec §US-2 Scenario 1, §SC-001, Plan §5]
- [x] CHK057 Is direct database access prohibited for browser roles (`anon`, `authenticated`), with authorization enforced authoritatively in Java application services? [Security, Spec §FR-024, ADR-0007, ADR-0011]

---

## 14. Database Invariants

- [x] CHK058 Are database check constraints defined for canonical Experiment statuses (`CREATED`, `QUEUED`, `RUNNING`, `COMPLETED`, `FAILED`, `STOP_REQUESTED`, `STOPPED`)? [Completeness, Spec §FR-035, Data Model §2.1]
- [x] CHK059 Are database check constraints defined for canonical Job statuses (`QUEUED`, `RUNNING`, `RETRY_SCHEDULED`, `SUCCEEDED`, `FAILED`, `CANCEL_REQUESTED`, `CANCELLED`)? [Completeness, Spec §FR-035, Data Model §2.4]
- [x] CHK060 Are database check constraints defined for tightened Execution Attempt statuses (`QUEUED`, `RUNNING`, `SUCCEEDED`, `FAILED`, `CANCELLED`)? [Completeness, Spec §FR-035, Data Model §2.5]
- [x] CHK061 Are composite foreign keys defined ensuring Backtest Job Candidate belongs to the same Experiment (`FOREIGN KEY (candidate_id, experiment_id) REFERENCES experiment.candidate_definition`)? [Consistency, Spec §FR-035, Data Model §2.4]
- [x] CHK062 Are unique constraints defined for one Backtest Job per Candidate, Attempt numbering within Job, and idempotency scoping? [Completeness, Spec §FR-035, Data Model §2.4-2.6]
- [x] CHK063 Are database constraints clearly distinguished from application transaction invariants? [Clarity, Spec §FR-034 vs §FR-035, Plan §4]

---

## 15. Forward Migrations

- [x] CHK064 Does the plan strictly mandate forward-only migrations without editing already-applied migrations (`20260827000100_create_database_baseline.sql`, `20260828000100_add_user_strategies_and_jobs.sql`)? [Compliance, Spec §FR-027, Plan §Technical Context, Data Model §3]
- [x] CHK065 Is a forward migration specified to tighten `experiment.execution_attempt.status` check constraint and remove legacy `RETRY_SCHEDULED`? [Completeness, Spec §FR-035, Data Model §3.2, Research §1.2]
- [x] CHK066 Is a deterministic legacy Attempt → Job backfill migration specified per FR-028, aborting if ambiguous mappings are detected and asserting zero orphan Attempts remain? [Completeness, Spec §FR-028, Data Model §3.3, Plan §4.3]
- [x] CHK067 Does the forward migration plan specify schema alignment for nullable CREATED fingerprint, reproduction lineage, and atomic idempotency claim state? [Completeness, Data Model §3.1, §3.4, Plan §Technical Context]

---

## 16. Persistence Ports

- [x] CHK068 Are persistence output ports (`ExperimentStore`, `JobStore`, `ExecutionAttemptStore`, `IdempotencyStore`, `OutboxStore`) declared in `modules/experiment` and decoupled from concrete JDBC implementations? [Architecture, Spec §FR-025, Plan §3, Contracts §1-4]
- [x] CHK069 Do JDBC adapters in `modules/persistence` implement domain ports without leaking SQL, Spring, or row types into `modules/experiment`? [Purity, Spec §FR-025, Plan §3, Contracts §1-4]
- [x] CHK070 Is a public factory (`ExperimentPersistenceFactory`) provided in `com.cryptostrategy.platform.persistence.api` following established repository patterns? [Consistency, Plan §3, Research §6]
- [x] CHK071 Do persistence ports exclude Redis, message queue, and publisher polling logic? [Boundary Clarity, Spec §FR-025, Plan §Summary]

---

## 17. Test Coverage Requirements

- [x] CHK072 Are unit tests planned for Experiment lifecycle, Manifest freeze immutability, Job state transitions, Attempt statuses, and exponential retry backoff? [Completeness, Spec §User Scenarios, Plan §6.1]
- [x] CHK073 Are ArchUnit architecture tests planned to enforce module boundaries, package purity, and zero circular dependencies? [Verification, Plan §6.2, Quickstart §2.2]
- [x] CHK074 Are two-user ownership isolation integration tests planned to verify zero cross-user data leakage? [Security, Spec §SC-001, Plan §6.3, Quickstart §2.4]
- [x] CHK075 Are concurrency tests planned for simultaneous Backtest Job creation, attempt number allocation, cancel vs requeue race, and atomic idempotency claims? [Concurrency, Spec §SC-008, Plan §6.3, Quickstart §2.4]
- [x] CHK076 Are Outbox atomicity and rollback tests planned to verify co-commit and failure isolation? [Durability, Spec §US-6 Scenario 1, Plan §6.3]
- [x] CHK077 Are SQL contract tests planned for schema constraints, forward migrations, and deterministic legacy backfill verification? [Verification, Plan §6.3, Quickstart §2.3]

---

## 18. Functional Requirements Traceability Matrix

- [x] CHK078 Is **FR-001** (Experiment creation & mandatory Manifest provenance groups) mapped to design, schema, and tests? [Traceability, Spec §FR-001, Data Model §2.2, Plan §7]
- [x] CHK079 Is **FR-002** (Two-phase CREATED → QUEUED freeze lifecycle) mapped to design, schema, and tests? [Traceability, Spec §FR-002, Contracts §1.1, Plan §7]
- [x] CHK080 Is **FR-003** (Deterministic SHA-256 fingerprint calculation and verification) mapped to design, schema, and tests? [Traceability, Spec §FR-003, Contracts §2.2, Plan §7]
- [x] CHK081 Is **FR-004** (Lineage tracking: `derivedFromExperimentId` and `reproducesExperimentId`) mapped to design, schema, and tests? [Traceability, Spec §FR-004, Data Model §2.1, Plan §7]
- [x] CHK082 Is **FR-005** (F-003 Dataset provenance integration without model duplication) mapped to design and contracts? [Traceability, Spec §FR-005, Contracts §1.2, Plan §7]
- [x] CHK083 Is **FR-006** (F-004 Strategy provenance integration and `source_user_strategy_version_id`) mapped to design and contracts? [Traceability, Spec §FR-006, Contracts §1.2, Plan §7]
- [x] CHK084 Is **FR-007** (Canonical Experiment state machine transitions) mapped to design, schema, and tests? [Traceability, Spec §FR-007, Data Model §2.1, Plan §7]
- [x] CHK085 Is **FR-008** (Candidate creation under frozen QUEUED Experiment) mapped to design and schema? [Traceability, Spec §FR-008, Data Model §2.3, Plan §7]
- [x] CHK086 Is **FR-009** (Single Experiment ownership of Candidate) mapped to design and schema? [Traceability, Spec §FR-009, Data Model §2.3, Plan §7]
- [x] CHK087 Is **FR-010** (Immutable Candidate definitions) mapped to design and schema? [Traceability, Spec §FR-010, Data Model §2.3, Plan §7]
- [x] CHK088 Is **FR-011** (Durable generation index and definitions for Search reproducibility) mapped to design and schema? [Traceability, Spec §FR-011, Data Model §2.3, Plan §7]
- [x] CHK089 Is **FR-012** (Durable Job entity with ULID, type, status, correlationId, progress) mapped to design and schema? [Traceability, Spec §FR-012, Data Model §2.4, Plan §7]
- [x] CHK090 Is **FR-013** (Search Job without candidate vs Backtest Job with candidate) mapped to design and schema? [Traceability, Spec §FR-013, Data Model §2.4, Plan §7]
- [x] CHK091 Is **FR-014** (One Backtest Job per Candidate across all lifecycle states) mapped to design, schema, and tests? [Traceability, Spec §FR-014, Data Model §2.4, Plan §7]
- [x] CHK092 Is **FR-015** (Canonical Job lifecycle state machine and rejection of invalid transitions) mapped to design, schema, and tests? [Traceability, Spec §FR-015, Contracts §3.1, Plan §7]
- [x] CHK093 Is **FR-016** (Non-blocking cancel polling support for Workers) mapped to design and contracts? [Traceability, Spec §FR-016, Contracts §4.2, Plan §7]
- [x] CHK094 Is **FR-017** (Append-only Execution Attempt history under Backtest Job) mapped to design and schema? [Traceability, Spec §FR-017, Data Model §2.5, Plan §7]
- [x] CHK095 Is **FR-018** (Monotonic attempt numbering serialized per Job) mapped to design, schema, and tests? [Traceability, Spec §FR-018, Plan §4.1, Plan §7]
- [x] CHK096 Is **FR-019** (Execution Attempt record fields and terminal status set) mapped to design and schema? [Traceability, Spec §FR-019, Data Model §2.5, Plan §7]
- [x] CHK097 Is **FR-020** (Retry creates Attempt under same Job without replacement Job) mapped to design and tests? [Traceability, Spec §FR-020, Plan §4, Plan §7]
- [x] CHK098 Is **FR-021** (Authoritative Supabase owner identity on the Experiment root plus hierarchical ownership derivation for Candidate, Job, and Execution Attempt) mapped to design, schema, and ownership chains? [Traceability, Spec §FR-021, Data Model §2.1, Plan §5, Plan §7]
- [x] CHK099 Is **FR-022** (Authenticated owner predicates required on application-service operations; resource ID alone never grants access) mapped to design, persistence ports, and services? [Traceability, Spec §FR-022, Contracts §1-4, Plan §5, Plan §7]
- [x] CHK100 Is **FR-023** (Cross-user access returns an ownership-safe inaccessible outcome without revealing another user's resource existence or content; HTTP mapping remains F-009) mapped to design and tests? [Traceability, Spec §FR-023, Plan §5, Plan §7]
- [x] CHK101 Is **FR-024** (Zero direct browser access to business tables) mapped to database privileges and architecture? [Traceability, Spec §FR-024, Plan §5, Plan §7]
- [x] CHK102 Is **FR-025** (Decoupled Persistence Ports declared in capability and implemented in persistence) mapped to design and contracts? [Traceability, Spec §FR-025, Contracts §1-4, Plan §7]
- [x] CHK103 Is **FR-026** (Persistence adapter capabilities for Experiment, Manifest, Candidate, Job, Attempt) mapped to design and classes? [Traceability, Spec §FR-026, Plan §3, Plan §7]
- [x] CHK104 Is **FR-027** (Forward-only migrations with no editing of applied migrations) mapped to design and schema? [Traceability, Spec §FR-027, Data Model §3, Plan §7]
- [x] CHK105 Is **FR-028** (Deterministic legacy Attempt → Job backfill aborting on ambiguity) mapped to migration design and tests? [Traceability, Spec §FR-028, Data Model §3.3, Plan §7]
- [x] CHK106 Is **FR-029** (Durable idempotency with active `request_hash` conflict detection) mapped to design, schema, and tests? [Traceability, Spec §FR-029, Data Model §2.6, Plan §7]
- [x] CHK107 Is **FR-030** (Downstream duplicate delivery detection query boundary) mapped to design and ports? [Traceability, Spec §FR-030, Contracts §4, Plan §7]
- [x] CHK108 Is **FR-031** (Transactional Outbox write-side co-commit for 6 explicit dispatch triggers) mapped to design, schema, and tests? [Traceability, Spec §FR-031, Contracts §1, Plan §7]
- [x] CHK109 Is **FR-032** (Outbox event routing context and exclusion of internal mutations) mapped to design and contracts? [Traceability, Spec §FR-032, Contracts §1, Plan §7]
- [x] CHK110 Is **FR-033** (Outbox schema definition and F-005 write / F-007 publish boundary) mapped to design and data model? [Traceability, Spec §FR-033, Data Model §2.7, Plan §7]
- [x] CHK111 Is **FR-034** (Application transaction invariants enforcement) mapped to application services and tests? [Traceability, Spec §FR-034, Plan §4, Plan §7]
- [x] CHK112 Is **FR-035** (Database-level schema constraints, composite FKs, and unique indices) mapped to schema and SQL tests? [Traceability, Spec §FR-035, Data Model §2, Plan §7]

---

## Checklist Summary

- **Total Checklist Items**: 112
- **Categories Covered**: 18 requirement quality dimensions (Scope, Experiment Lifecycle, Manifest/Fingerprint, Lineage, Provenance Integration, Candidates, Jobs, Execution Attempts, Concurrency, Idempotency, Outbox, Transactions, Ownership, Database Constraints, Migrations, Ports, Test Plan, Traceability FR-001..FR-035)
- **Known Requirement Ambiguities / External Blockers**: 0 for F-003. F-003 Dataset integration is resolved: `DatasetVersionId` is the canonical stable Dataset identity, there is no separate Dataset root, and `version = candle-v1` identifies the Dataset/checksum canonicalization contract.
- **Blocking Architectural Conflicts**: None identified for the F-003/F-005 Dataset integration. F-005 consumes F-003's public immutable Dataset Version contract without duplicating Market Data ownership.
- **Traceability Status**: 100% for FR-001 through FR-035, including FR-005's resolved mapping to F-003 `DatasetVersionId` and immutable Dataset Version provenance.
- **Feature Readiness Status**: **READY FOR /speckit-analyze**. F-003 no longer blocks task generation or F-005 implementation analysis.
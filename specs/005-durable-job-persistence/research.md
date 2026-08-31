# Research & Design Decisions: F-005 Experiment Persistence and Ownership

**Feature**: F-005 — Experiment Persistence and Ownership  
**Branch**: `feature/005-experiment-persistence`  
**Directory**: `specs/005-durable-job-persistence`  
**Status**: Completed  
**Date**: 2026-08-30  

---

## 1. Domain & Lifecycle State Machines

### Decision 1.1: Experiment Lifecycle and Confirmation Boundary
- **Chosen Design**: The canonical Experiment statuses are `CREATED`, `QUEUED`, `RUNNING`, `COMPLETED`, `FAILED`, `STOP_REQUESTED`, `STOPPED` (matching the existing database baseline in `supabase/migrations/20260827000100_create_database_baseline.sql`).
- **Semantic Boundary**:
  - `CREATED`: Mutable preparation/input phase. Manifest fields, parameters, assumptions, and search configs can be edited.
  - `CREATED → QUEUED`: The immutable freeze and submission boundary. Executed in a single atomic database transaction:
    1. Validate Manifest completeness (all mandatory provenance groups present).
    2. Compute canonical SHA-256 `experimentFingerprint` (field-sorted, UTC instant, decimal as string).
    3. Freeze the Manifest (no subsequent updates allowed).
    4. Transition Experiment status to `QUEUED`.
    5. Write `ExperimentQueued` Outbox event to `platform.outbox_event`.
  - While `CREATED`, the stored fingerprint may be absent because the canonical freeze hash has not yet been computed.
  - The `CREATED → QUEUED` transaction computes and stores the fingerprint before committing the queued state.
  - Once `QUEUED`, the Manifest is permanently immutable and its fingerprint must be present. Subsequent transitions (`RUNNING`, `COMPLETED`, `FAILED`, `STOP_REQUESTED`, `STOPPED`) update runtime status and timestamps only.
  - Experiment lineage stores both `derived_from_experiment_id` and the distinct optional `reproduces_experiment_id` required for explicit reproduction runs.
- **Rationale**: Reconciles the spec with the database baseline without requiring status renames or an unneeded `manifest_status`/`manifest_frozen` column in MVP.
- **Alternatives Considered**:
  - *Adding `DRAFT` and `CONFIRMED` statuses via forward migration*: Rejected to preserve established DB baseline status names from ADR-0007 and avoid enum churn.
  - *Adding a separate boolean `manifest_frozen` column*: Rejected because `status != 'CREATED'` is a clean, natural freeze invariant in MVP.

### Decision 1.2: Job and Execution Attempt Durability & Retry Semantics
- **Chosen Design**:
  - **Job** is the durable, logical long-running work identity (`experiment.job`). Status set: `QUEUED`, `RUNNING`, `RETRY_SCHEDULED`, `SUCCEEDED`, `FAILED`, `CANCEL_REQUESTED`, `CANCELLED`.
  - **Execution Attempt** is the append-only record of a single concrete Worker try (`experiment.execution_attempt`). Status set: `QUEUED`, `RUNNING`, `SUCCEEDED`, `FAILED`, `CANCELLED`.
  - `RETRY_SCHEDULED` is strictly a Job-level status and NOT an Execution Attempt status.
  - When a Worker try fails with a retryable error:
    1. The current Execution Attempt is finalized as `FAILED` with `failure_code`, `failure_message`, `retryable = true`, and `finished_at = now()`.
    2. The parent Job transitions to `RETRY_SCHEDULED` with calculated `next_retry_at = now + exponential_backoff(attempt_no)`.
    3. Job ID remains identical; no new Job is created.
  - When `next_retry_at` is reached:
    - Job transitions `RETRY_SCHEDULED → QUEUED` and an `JobQueued` Outbox event is emitted.
  - When a Worker claims and starts the next try:
    - A new Execution Attempt is appended under the same `job_id`.
    - Monotonically increasing `attempt_no` is assigned (`previous_max + 1`).
- **Migration Impact**: F-005 requires new forward-only migration(s), with exact filenames chosen at implementation time after inspecting the migration directory. They must: (a) remove legacy `RETRY_SCHEDULED` from `execution_attempt.status`; (b) align Manifest fingerprint lifecycle and reproduction lineage where needed; (c) support durable idempotency `IN_PROGRESS → COMPLETED` claim state; and (d) perform the FR-028 legacy Attempt→Job backfill, aborting on ambiguous mappings. Already-applied migrations are never edited.
- **Rationale**: Clean separation between logical progress (Job) and transient execution history (Attempt). Prevents double-counting retries as separate logical jobs and prevents duplicate business outcomes.

---

## 2. Idempotency & Conflict Enforcement

### Decision 2.1: Active `request_hash` Enforcement Gate
- **Chosen Design**:
  - Idempotency records are scoped to `(owner_user_id, operation_scope, idempotency_key)` in `platform.idempotency_record`.
  - `request_hash` (SHA-256 of canonical request payload) acts as an active gate:
    1. **Same Key + Same Hash**: Idempotent replay. Return original outcome (in-progress acknowledgment or completed result) without re-executing command.
    2. **Same Key + Different Hash**: Application-layer idempotency conflict. Immediately reject command without mutating state; return distinct domain error `IdempotencyConflictException`.
  - Concurrency: F-005 uses an **atomic claim**, not `check()` followed by later insert. The first caller inserts an `IN_PROGRESS` record (e.g. `INSERT ... ON CONFLICT DO NOTHING`); exactly one caller acquires execution. Callers that lose the claim load the existing record, compare `request_hash`, and return replay/conflict without executing.
  - The idempotency row therefore needs explicit persisted lifecycle state (`IN_PROGRESS`, `COMPLETED`); completion payload/status fields are nullable until completion.
- **Rationale**: Prevents accidental or malicious key reuse from executing conflicting payload mutations or returning mismatched data.
- **Alternatives Considered**:
  - *Passive `request_hash` (audit-only)*: Rejected because returning a stale outcome for a different payload is unsafe.
  - *Keying by `(user_id, scope, key, hash)`*: Rejected because it allows multiple executions under the same client key, violating the feature's idempotency-key semantics.

---

## 3. Transactional Outbox Boundaries

### Decision 3.1: Explicit Cross-Boundary Event Set
- **Chosen Design**:
  - Outbox rows (`platform.outbox_event`) are written atomically within the business transaction strictly for cross-boundary dispatch and cancellation triggers:
    1. `ExperimentQueued` (Experiment `CREATED → QUEUED`)
    2. `ExperimentStopRequested` (Experiment `RUNNING → STOP_REQUESTED`)
    3. `JobQueued` (Backtest Job creation in `QUEUED`)
    4. `JobQueued` (Job `RETRY_SCHEDULED → QUEUED` when retry is dispatch-ready)
    5. `JobCancelRequested` (Job `RUNNING → CANCEL_REQUESTED`)
    6. `JobCancelled` (Job `QUEUED → CANCELLED`)
  - Internal state mutations do NOT generate Outbox events:
    - Counter increments (`completed_work`, `failed_work`)
    - Score updates (`best_score`)
    - Execution Attempt creation and Attempt status changes
    - Local heartbeat and timestamps
- **Outbox Payload Structure**:
  - `outbox_event_id`: ULID
  - `message_id`: ULID (stable deduplication key for consumers)
  - `aggregate_type`: `'EXPERIMENT'` or `'JOB'`
  - `aggregate_id`: ULID
  - `event_type`: e.g. `'EXPERIMENT_QUEUED'`, `'JOB_QUEUED'`
  - `event_version`: `'1.0'`
  - `payload`: JSONB containing routing context (`experimentId`, `jobId`, `candidateId`, `attemptNo`, `correlationId`, `ownerUserId`)
  - `headers`: JSONB trace headers (`correlationId`)
- **Rationale**: Avoids database write amplification and redundant queue traffic while guaranteeing at-least-once dispatch for critical lifecycle transitions. F-005 owns the Outbox write side; F-007 owns polling/publishing to Redis Streams.

---

## 4. Authorization & Ownership Enforcement

### Decision 4.1: Root Owner Predicate via Authoritative Parent Chain
- **Chosen Design**:
  - Supabase Auth UUID (`auth.users.id`) is the root owner identifier.
  - Every application query and command must supply an authenticated `owner_user_id` parameter.
  - Authorization resolution traverses the authoritative hierarchy:
    - `Experiment` → `experiment.owner_user_id == caller_user_id`
    - `Candidate` → `candidate.experiment_id → experiment.owner_user_id == caller_user_id`
    - `Job` → `job.experiment_id → experiment.owner_user_id == caller_user_id`
    - `ExecutionAttempt` → `attempt.job_id → job.experiment_id → experiment.owner_user_id == caller_user_id`
  - Downstream child entities never store an independent owner that could contradict the parent.
  - If a resource does not exist OR belongs to another owner, the service returns an ownership-safe `ResourceInaccessibleException` (mapped to HTTP 404/403 in F-009).
- **Rationale**: Strict compliance with ADR-0011 and ADR-0012; prevents ID probing and cross-user data leakage.

---

## 5. Concurrency & Race Condition Controls

### Decision 5.1: Database Constraints and Locking
- **One Job per Candidate**:
  - Database unique index: `create unique index job_backtest_candidate_unique on experiment.job (candidate_id) where job_type = 'BACKTEST';` (already in migration baseline).
  - Enforced across the entire Job lifecycle (including `CANCELLED`). Second creation attempts return existing Job or fail cleanly.
- **Candidate Belongs to Same Experiment as Job**:
  - Composite foreign key: `foreign key (candidate_id, experiment_id) references experiment.candidate_definition(candidate_id, experiment_id)` (already in migration baseline).
- **Monotonic Attempt Number Allocation**:
  - Lock the **parent Job row** first: `SELECT ... FROM experiment.job WHERE job_id = ? FOR UPDATE`.
  - While that Job lock is held, query `COALESCE(MAX(attempt_no), 0) + 1` from `experiment.execution_attempt` and insert the new Attempt.
  - This serializes all attempt allocation for one Job. Database `UNIQUE (job_id, attempt_no)` remains the final collision guard.
- **Cancel vs Requeue Race**:
  - Cancellation command executes `SELECT * FROM experiment.job WHERE job_id = ? FOR UPDATE`.
  - If `RETRY_SCHEDULED`, directly transitions to `CANCELLED` **without Outbox publication**; the retry has not yet become dispatch-ready.
  - If `QUEUED`, directly transitions to `CANCELLED` and emits `JobCancelled`.
  - If `RUNNING`, transitions to `CANCEL_REQUESTED` and emits `JobCancelRequested`.
  - The row lock serializes cancellation against the `RETRY_SCHEDULED → QUEUED` transition, ensuring a single unambiguous final state.
  - F-005 provides the durable transition contract only; the actual retry scheduler/Worker orchestration that attempts requeue belongs to F-007 and must validate current durable Job state.

---

## 6. Integration Contract Strategy (F-003 & F-004)

### Decision 6.1: Typed Placeholders for External Dependencies
- **Chosen Design**:
  - **F-003 Dataset Provenance — resolved**: `DatasetVersionId` is the canonical immutable Dataset identity; F-003 defines no separate Dataset root. `DatasetProvenanceSnapshot` binds to the published Dataset Version provenance: `datasetVersionId`, `version` (`candle-v1`), `checksum`, provider, Trading Pair, Timeframe, `normalizationVersion`, `rangeStart`, `rangeEnd`, and `candleCount`. F-005 does NOT duplicate Dataset membership, checksum calculation, provider normalization, or Market Data ingestion logic.
  - **F-004 Strategy Provenance**: Represented with F-004 public types (`StrategyKind`, `StrategyReference`, `StrategyParameterSet`, typed policy ID/version, typed components, optional `UserStrategyVersionId`, and required `strategy-v1` fingerprint). Does NOT duplicate strategy execution or plugin catalog logic.
  - If final contracts are not yet merged, these value objects live in `modules/experiment/src/main/java/com/cryptostrategy/platform/experiment/api/provenance/` as typed contracts.
- **Rationale**: Prevents inventing parallel models while allowing F-005 persistence and validation logic to be fully specified and tested.

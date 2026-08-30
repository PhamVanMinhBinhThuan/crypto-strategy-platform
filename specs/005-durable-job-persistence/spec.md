# Feature Specification: Experiment Persistence and Ownership

**Feature Branch**: `feature/005-experiment-persistence`

**Feature Directory**: `specs/005-durable-job-persistence`

**Created**: 2026-08-30

**Status**: Draft

**Feature ID**: F-005

**Migrated from**: `specs/002-user-strategy-jobs` (staging - F-005-relevant requirements migrated here)

**Dependencies**:
- F-002 Java Backend Foundation (required)
- Database gate - User Strategy and Durable Job schema increment (required)
- F-003 Market Data and Dataset (integration contract for Dataset identity/version/checksum - dependency/blocker)
- F-004 Strategy Registry and User Strategy Library (integration contract for Strategy/User Strategy snapshots - dependency/blocker)

---

## Overview

F-005 establishes the durable persistence and ownership layer for the Experiment graph. It
defines the Experiment aggregate, an immutable Experiment Manifest, Candidate definitions,
durable Jobs, append-only Execution Attempt history, ownership-enforced authorization,
persistence ports/adapters, transaction-enforced invariants, idempotency records and
transactional Outbox writes.

Actors who interact with this layer are authenticated users (via Supabase JWT identity) and
internal system components such as the Search Coordinator and Backtest Worker. All durable
state lives in PostgreSQL-compatible storage; Redis and the queue layer are ephemeral and
must be fully recoverable from durable state.

**Scope boundary**: This feature does NOT implement the Backtest engine, Evaluation metrics,
Leaderboard ranking, Redis Streams worker orchestration, Outbox publisher, public REST API,
WebSocket delivery or frontend UI. F-006 owns Backtest/Evaluation/Leaderboard behaviour;
F-007 owns Worker/Redis reliable processing and Outbox publication; F-009 owns public API
and realtime delivery; F-010 owns frontend UI.

---

## Clarifications

### Session 2026-08-30

- Q: Should a finalized-but-retryable Execution Attempt carry the status `RETRY_SCHEDULED`, or should it always be `FAILED` — with only the parent Job (not the Attempt) carrying `RETRY_SCHEDULED`? → A: Option A — Execution Attempts are always terminal after resolution. The allowed status set is `QUEUED → RUNNING → SUCCEEDED | FAILED | CANCELLED`. `RETRY_SCHEDULED` is exclusively a Job-level status. A retrying Attempt is finalized as `FAILED`, and the Job itself transitions to `RETRY_SCHEDULED`. The `RETRY_SCHEDULED` value currently permitted on `execution_attempt.status` in the DB baseline is a legacy artefact; a forward migration in F-005 must remove it from the enum constraint.
- Q: Which Experiment lifecycle status names are canonical for F-005 — spec's `DRAFT/CONFIRMED` or the DB baseline's `CREATED/QUEUED`? → A: Option B — The DB baseline's `CREATED/QUEUED/RUNNING/COMPLETED/FAILED/STOP_REQUESTED/STOPPED` are canonical. `CREATED` is the mutable input/preparation phase (formerly called `DRAFT`). The `CREATED → QUEUED` transition is the freeze/submission boundary: the system validates Manifest completeness, computes and verifies the experimentFingerprint, freezes the Manifest, persists `QUEUED` Experiment status, and writes any required Outbox event atomically in a single transaction. Once `QUEUED`, the Manifest is immutable. `QUEUED` means frozen and submitted for downstream execution. `DRAFT` and `CONFIRMED` are retired from all F-005 status references. No separate `manifest_status` or `manifest_frozen` column is added in MVP.
- Q: When an idempotency key is reused with a different request body, how does the system behave? → A: Option A — The `request_hash` is an active enforcement gate, not passive audit metadata. Within the same `owner_user_id` and `operation_scope`: (1) Same key + same `request_hash` is an idempotent replay; the command MUST NOT re-execute, and the system resolves to the original in-progress or completed outcome. (2) Same key + different `request_hash` is an idempotency conflict; the command MUST be rejected immediately without execution, producing a distinct application-layer conflict error (HTTP mapping owned by F-009). No second idempotency record is created.
- Q: Which specific Experiment and Job lifecycle transitions in F-005 MUST write a transactional Outbox event row for downstream publication? → A: Option A — Outbox events are strictly limited to cross-boundary dispatch/cancellation triggers: (1) `Experiment CREATED → QUEUED` (`ExperimentQueued`), (2) `Experiment RUNNING → STOP_REQUESTED` (`ExperimentStopRequested`), (3) `Backtest Job created in QUEUED` (`JobQueued`), (4) `Job RETRY_SCHEDULED → QUEUED` (`JobQueued` when retry becomes dispatch-ready; `RETRY_SCHEDULED` itself does not publish), (5) `Job RUNNING → CANCEL_REQUESTED` (`JobCancelRequested`), (6) `Job QUEUED → CANCELLED` (`JobCancelled`). Internal mutations (progress counters, `best_score`, Execution Attempt creation/status transitions, timestamps) do NOT write Outbox records. F-005 owns atomic Outbox persistence; F-007 owns polling, publication, and queue delivery.

---

## Integration Dependency Notice

### F-003 Dataset Provenance (Resolved Integration)

F-003 is available to F-005 and defines the canonical Dataset contract. F-003 has no separate
Dataset root identity: `DatasetVersionId` is the stable immutable Dataset identity used by
downstream capabilities.

The Experiment Manifest MUST bind to the published F-003 Dataset Version contract and record
the immutable provenance required for reproducibility:

- `datasetVersionId` — canonical F-003 `DatasetVersionId`;
- `version` — the Dataset/checksum canonicalization contract ID (`candle-v1` for F-003);
- `checksum` — F-003 checksum in `sha256:<64 lowercase hex>` form;
- provider;
- canonical Trading Pair identity/value;
- Timeframe;
- `normalizationVersion`;
- `rangeStart` / `rangeEnd`;
- `candleCount`.

F-005 MUST NOT introduce a second `DatasetId`, Dataset root entity, checksum algorithm, Dataset
membership model, or Market Data ingestion logic. Dataset evidence remains owned and immutable
under F-003. F-005 references/materializes the published F-003 provenance for Manifest
fingerprinting and reproducibility only.

### F-004 Strategy / User Strategy Provenance (Dependency/Blocker)

The Manifest must snapshot exact Strategy/User Strategy provenance: plugin ID, version,
exact parameters and, for Composite strategies, the combination policy, ordered components
and weights. The optional source_user_strategy_version_id links the initiating saved
Strategy version for provenance without replacing the frozen snapshot.

If F-004 final contracts are not merged on this branch, F-005 must treat the Strategy
provenance slot as a typed placeholder. F-005 MUST NOT create a second Strategy version or
User Strategy representation that duplicates F-004 ownership.

---

## User Scenarios and Testing

### User Story 1 - Create an Experiment with a Frozen Manifest (Priority: P1)

As an authenticated user, I want to create an Experiment that captures my exact dataset,
strategy configuration and backtest assumptions in an immutable record so that future
observers, including myself, can understand and reproduce every result that this Experiment
produces.

**Why this priority**: The Experiment Manifest is the reproducibility anchor for every
downstream result, Leaderboard entry and audit. Without a frozen, verified manifest,
correctness of F-006/F-007/F-009 results cannot be established.

**Independent Test**: Create an Experiment (status: CREATED), submit a Queue/Freeze command,
verify the Experiment transitions to QUEUED, attempt to change a Manifest field after
QUEUED, and verify rejection. Verify that querying the Experiment returns exactly the
frozen Manifest content.

**Acceptance Scenarios**:

1. **Given** an authenticated user provides valid Experiment configuration (Dataset
   reference, Strategy provenance, Backtest assumptions, Search configuration),
   **When** the user submits a Create Experiment command,
   **Then** the system persists an Experiment with a unique experimentId, CREATED status,
   a Manifest containing all mandatory provenance fields, and returns the experimentId.

2. **Given** an Experiment in CREATED status,
   **When** the user submits a Queue/Freeze command,
   **Then** the system validates Manifest completeness, computes and stores the
   experimentFingerprint, freezes the Manifest, transitions the Experiment to QUEUED, and
   writes any required Outbox event — all atomically in a single transaction. All subsequent
   attempts to modify any Manifest field are rejected.

3. **Given** an Experiment in QUEUED or later status,
   **When** a system or user attempts to update any Manifest field (dataset, strategy
   version, assumptions, seed),
   **Then** the operation is rejected and the Manifest remains unchanged.

4. **Given** a user wants to re-run with different settings,
   **When** the user creates a new Experiment with adjusted configuration,
   **Then** the system creates a new Experiment (with optional derivedFromExperimentId
   provenance) and leaves the original Experiment and Manifest untouched.

5. **Given** a frozen Manifest (Experiment in QUEUED or later status),
   **When** the system computes or verifies the experimentFingerprint,
   **Then** the fingerprint is a deterministic SHA-256 over the canonical manifest
   (field-sorted, UTC timestamps, decimal as canonical string) and matches on
   re-computation from the same stored manifest.

---

### User Story 2 - Ownership Isolation Between Two Users (Priority: P1)

As an authenticated user, I want my Experiments, Candidates and Jobs to be visible and
controllable only by me so that another user cannot read, stop or reproduce my work by
knowing or guessing an identifier.

**Why this priority**: Ownership isolation is a non-negotiable security invariant. All
downstream features (API, UI, Worker) inherit this boundary.

**Independent Test**: Create two user accounts (User A and User B). User A creates an
Experiment with Candidates and a Job. User B supplies those IDs in read/stop/cancel
commands. All such operations must be rejected without leaking data.

**Acceptance Scenarios**:

1. **Given** User A and User B are both authenticated,
   **When** User B supplies User A's experimentId in a read, stop or reproduce command,
   **Then** the application boundary rejects access using an ownership-safe inaccessible
   outcome and does not reveal the Experiment's existence or content. HTTP 403/404 mapping
   is owned by F-009.

2. **Given** User B supplies User A's candidateId or jobId in any command,
   **When** the system resolves authorization,
   **Then** the system derives ownership through the resource's authoritative parent chain:
   Candidate -> Experiment -> owner_user_id, or Job -> Experiment -> owner_user_id, and
   rejects the request without granting access.

3. **Given** User A stops an Experiment,
   **When** User B queries the same Experiment status,
   **Then** User B's query is rejected; User A sees the updated status.

4. **Given** possession of experimentId, candidateId or jobId alone,
   **When** an unauthenticated or wrong-owner request is made,
   **Then** the identifier alone never grants access; only a valid, authenticated, matching
   owner identity authorizes the operation.

---

### User Story 3 - Durable Job Identity and Retry History (Priority: P1)

As an authenticated user who has started a Backtest or Search, I want the system to maintain
a single logical Job identity across retries so that I see coherent progress and only one
accepted business outcome, even if the Worker processes the job message multiple times.

**Why this priority**: The Job/Attempt distinction is the core durability invariant. Without
it, duplicate deliveries or Worker crashes create duplicate results or lost work.

**Independent Test**: Create an Experiment, a Candidate and a Backtest Job. Simulate a
Worker failure and a retry. Verify that: only one Job exists, a new Execution Attempt is
created with an incremented attempt number, and no duplicate business outcome is recorded.

**Acceptance Scenarios**:

1. **Given** a Candidate belonging to a QUEUED (or later) Experiment,
   **When** a Backtest Job is created for that Candidate,
   **Then** exactly one Job exists for that Candidate, the Job is in QUEUED status with a
   durable jobId and correlationId, and an Outbox event is written atomically in the same
   transaction.

2. **Given** a running Backtest Job whose current Execution Attempt encounters a transient
   failure,
   **When** the system classifies the failure as retryable and retry budget is not exhausted,
   **Then** the current Execution Attempt is finalized as failed, the Job transitions to
   RETRY_SCHEDULED, the Job ID remains the same, and no replacement Job is created.
   **When** the retry becomes eligible and a Worker begins the next try,
   **Then** a new Execution Attempt is appended under the same Job with the next sequential
   attempt number.

3. **Given** downstream processing receives a duplicate delivery for a logical operation
   whose Job is already SUCCEEDED,
   **When** the downstream component checks the durable Job and idempotency state exposed by
   F-005,
   **Then** it can determine that the operation has already completed and no second accepted
   business result or replacement Job is created. Queue acknowledgement/redelivery behavior is
   owned by F-007.

4. **Given** a Candidate already has a Backtest Job in any lifecycle state, including a
   terminal state (SUCCEEDED, FAILED or CANCELLED),
   **When** a second attempt to create a Backtest Job for the same Candidate is made,
   **Then** the system rejects the creation or returns the existing Job ID idempotently.
   Cancellation does not permit a second logical Backtest Job for the same Candidate.

5. **Given** a Backtest Job with multiple Execution Attempts,
   **When** querying the Job's history,
   **Then** all Attempts are visible with unique, monotonically increasing attempt numbers
   scoped to the Job, along with Worker identity, start/finish times, status and failure
   classification.

---

### User Story 4 - Experiment Lifecycle and Stop/Cancel (Priority: P2)

As an authenticated user, I want to stop a running Experiment or cancel pending Jobs so that
I can interrupt long-running work safely and know that results captured before stopping are
preserved.

**Why this priority**: Stop/cancel is critical for user control and resource safety, but
depends on the Experiment and Job lifecycle being well-defined first.

**Independent Test**: Advance an Experiment to QUEUED, create Jobs and transition one to
RUNNING. Issue a stop command. Verify that the Experiment moves to STOP_REQUESTED then
STOPPED, QUEUED Jobs (Job status) are CANCELLED, RUNNING Jobs are flagged CANCEL_REQUESTED
and results produced before the stop are still accessible.

**Acceptance Scenarios**:

1. **Given** a running Experiment,
   **When** the authenticated owner issues a Stop command,
   **Then** the Experiment transitions to STOP_REQUESTED and the state change is persisted
   durably in the same transaction as the Outbox event.

2. **Given** an Experiment in STOP_REQUESTED state,
   **When** all Jobs are either in terminal states or flagged CANCEL_REQUESTED,
   **Then** the Experiment transitions to STOPPED once no Jobs remain active.

3. **Given** Jobs in QUEUED state when Stop is issued,
   **When** the Experiment reaches STOP_REQUESTED,
   **Then** those Jobs transition directly to CANCELLED without starting execution.

4. **Given** a Job in RETRY_SCHEDULED state when Stop or cancellation is issued,
   **When** the cancellation transaction wins before the retry is re-queued,
   **Then** the Job transitions directly to CANCELLED and no new Execution Attempt is created.
   If `RETRY_SCHEDULED -> QUEUED` commits first, cancellation follows the normal
   `QUEUED -> CANCELLED` path.

5. **Given** a Job in RUNNING state when Stop is issued,
   **When** the Worker polls the cancel flag at the next safe checkpoint,
   **Then** the Job is marked CANCEL_REQUESTED; after Worker acknowledgement, it moves to
   CANCELLED, and no partial result is recorded as a successful business outcome.

6. **Given** a stopped Experiment,
   **When** the owner queries the Candidate results,
   **Then** results recorded before the stop are still visible and have not been deleted or
   invalidated.

---

### User Story 5 - Reproducibility and Provenance Traceability (Priority: P2)

As an authenticated user or auditor, I want every Experiment result to be traceable to an
exact frozen Manifest so that I can reproduce a result by using the same dataset, strategy
version, parameters and assumptions recorded in the Manifest.

**Why this priority**: Reproducibility is a constitutional requirement. Every result without
provenance is unverifiable.

**Independent Test**: Create an Experiment (CREATED), freeze it (QUEUED). Run a Backtest
(mocked). Record the result. Query the Experiment detail. Verify that Dataset checksum,
Strategy version, exact parameters, Backtest assumptions, search seed and software version
are all retrievable from the persisted Manifest.

**Acceptance Scenarios**:

1. **Given** a frozen Experiment Manifest (Experiment in QUEUED or later status),
   **When** a Backtest result is accepted for a Candidate,
   **Then** the result is linked to the Candidate and the Candidate to the Experiment, so
   the full provenance chain (result -> candidate -> experiment -> manifest) is traversable.

2. **Given** a persisted Manifest,
   **When** the Dataset checksum is re-computed from the stored Dataset provenance fields,
   **Then** the checksum matches the value frozen in the Manifest, confirming no silent
   modification has occurred.

3. **Given** a user who wants to reproduce an Experiment,
   **When** a Reproduction Run is initiated,
   **Then** the system creates a new Experiment linked via derivedFromExperimentId and
   reproducesExperimentId, leaves the original Manifest and results untouched, and uses
   the frozen Candidate Definition list rather than regenerating from seed and time alone.

4. **Given** a Strategy or User Strategy is renamed, archived, or updated after the
   Experiment,
   **When** the Experiment Manifest is queried,
   **Then** the Manifest still contains the snapshot taken at freeze time (CREATED→QUEUED
   transition); the original provenance is not affected by post-freeze changes.

---

### User Story 6 - Durable Idempotency and Outbox Writes (Priority: P2)

As a system operator, I want all state-changing commands that require downstream publication
to write their Outbox event atomically with the business state change so that process crashes
or queue unavailability never leave the system in an inconsistent state.

**Why this priority**: The Outbox pattern is the durability contract for the entire
asynchronous pipeline. Violation leads to lost Jobs or duplicate results.

**Independent Test**: Create a Backtest Job. Simulate a Redis/queue failure immediately after
the transaction commits. Verify that: the Job and Outbox row both exist in durable storage;
the Job is not in a dispatched-but-lost phantom state; the Outbox publisher (F-007) can
recover and republish.

**Acceptance Scenarios**:

1. **Given** a Create Job command,
   **When** the application service persists the Job,
   **Then** the Job row and the Outbox event row are written in the same database transaction
   such that either both are visible or neither is, even under process crash.

2. **Given** an Outbox event not yet published to the queue,
   **When** the Outbox publisher (F-007) scans pending events,
   **Then** the event is found and can be published without re-deriving state, because the
   Outbox row contains sufficient context for dispatch.

3. **Given** the same idempotency key is submitted twice for the same command scope
   (`owner_user_id` + `operation_scope` + `idempotency_key`),
   **When** the second request provides the identical payload (`request_hash` matches),
   **Then** the durable idempotency mechanism prevents a second execution and makes the
   original in-progress or completed outcome available without running the command again.

4. **Given** the same idempotency key is submitted with a different payload (`request_hash` mismatch),
   **When** the system resolves the idempotency check,
   **Then** the command is rejected as an idempotency conflict, no second execution occurs,
   and the stored state/outcome is not modified.

5. **Given** downstream Worker/queue processing receives a duplicate delivery for a logical
   operation already accepted,
   **When** the downstream component queries the durable F-005 idempotency boundary,
   **Then** it can determine that processing has already been accepted without creating a
   second business outcome or Outbox event. Queue acknowledgement/redelivery behavior is owned
   by F-007.

---

### User Story 7 - Recovery After Process/Queue/Cache Failure (Priority: P3)

As a system operator, I want the Experiment and Job state to survive complete Redis and
Worker process loss so that the system can resume work from durable state without manual
intervention or data loss.

**Why this priority**: Resilience against queue and cache loss is a constitutional
requirement; it must be specifiable before implementation.

**Independent Test**: Wipe Redis completely while Jobs are in QUEUED and RUNNING states.
Restart the persistence/application layer. Verify that Jobs are still present in PostgreSQL
with their last known status and that durable Outbox data remains available and sufficient for
F-007 to resume publication later without creating new Job IDs.

**Acceptance Scenarios**:

1. **Given** Jobs in QUEUED state and Redis is completely lost,
   **When** the persistence/application layer restarts,
   **Then** the Jobs and their Outbox events remain in PostgreSQL under the same Job IDs. The
   durable records remain sufficient for the F-007 publisher to discover and republish later;
   actual republication and Worker pickup are outside F-005.

2. **Given** a Job in RUNNING state and the Worker process crashes,
   **When** the system recovers,
   **Then** the Job remains durably recorded in RUNNING with the incomplete Execution Attempt
   preserved; no replacement Job is created and no durable history is lost. F-005 exposes the
   persisted state required for recovery. Any recovery scan, retry classification, or transition
   to RETRY_SCHEDULED is orchestration owned by F-007.

3. **Given** an Experiment with a frozen Manifest (QUEUED or later) and associated Jobs,
   **When** Redis cache is cleared,
   **Then** Experiment configuration, Manifest, Candidate definitions and all Job/Attempt
   history remain fully readable from durable storage with no data loss.

---

### Edge Cases

- **Ownership isolation**: Two users may create Experiments, Candidates and Jobs with
  identical content; each is owned by a different user and is not accessible to the other.
- **Manifest field completeness**: A Manifest with a missing required provenance field must
  be rejected at the CREATED→QUEUED freeze boundary. The system must not silently transition
  an incomplete Manifest to QUEUED status.
- **Candidate/Experiment consistency**: A Candidate must belong to exactly one Experiment.
  Creating a Backtest Job for a Candidate that belongs to a different Experiment than the
  Job is rejected by a same-Experiment constraint.
- **One Job per Candidate**: A Candidate may have at most one logical Backtest Job across
  the entire Job lifecycle, including CANCELLED. A repeated create request must return the
  existing Job idempotently or be rejected; cancellation does not authorize a replacement Job.
- **Attempt number uniqueness**: Two Execution Attempts under the same Job must not share
  the same attempt number. Numbers must be monotonically increasing.
- **Search Job has no Execution Attempt in MVP**: Only Backtest Jobs own Execution Attempts.
  A Search Job records Search Coordinator status and progress directly on the Job.
- **Concurrency and duplicate Job creation**: Concurrent create-Job commands for the same
  Candidate must produce exactly one Job. A unique constraint enforces this; the second
  concurrent write is rejected or the caller receives the existing Job ID.
- **Idempotency key reuse and payload conflicts**: An idempotency key is bound to exactly one
  logical request. Reusing an existing key with a mismatched `request_hash` within the same
  owner and operation scope is rejected as an application conflict, regardless of whether the
  original operation is in-progress or completed.
- **Outbox event scope boundary**: Outbox events are written only for cross-boundary dispatch
  and cancellation triggers (`ExperimentQueued`, `ExperimentStopRequested`, `JobQueued`,
  `JobCancelRequested`, `JobCancelled`). Internal progress updates, `best_score` changes, and
  Execution Attempt insertions/status changes do not produce Outbox events.
- **Duplicate Outbox events**: F-005 MUST persist a stable `event_id` and sufficient
  idempotency/routing metadata on each Outbox row so F-007 consumers can detect duplicate
  publication. Consumer-side deduplication, acknowledgement and redelivery behavior are owned
  by F-007.
- **Retry exceeds budget**: Once the maximum retry count is reached, the Job transitions
  to FAILED and the failure code/message is stored.
- **Cancel during transient state**: A Job in `RETRY_SCHEDULED` may transition directly to
  `CANCELLED` because no Worker is actively executing that retry. If cancellation races with
  `RETRY_SCHEDULED -> QUEUED`, the transition MUST be serialized so only one durable state
  change wins. If `QUEUED` commits first, cancellation follows the normal `QUEUED -> CANCELLED`
  path. The Job must never be left in an ambiguous state.
- **Reproduction does not overwrite originals**: Initiating a Reproduction Run must not
  mutate the original Experiment, Manifest, Candidates, Jobs or Results.
- **Applied migrations must not be edited**: Schema changes must be expressed as new
  forward migrations, never by editing a baseline migration already applied to any shared
  environment.
- **Execution Attempt `RETRY_SCHEDULED` is a legacy DB artefact**: The DB baseline permits
  `RETRY_SCHEDULED` on `execution_attempt.status`. This value is incorrect per the F-005
  decision (Q1-2026-08-30): Attempts are always terminal. A forward migration in F-005 MUST
  tighten the check constraint to remove `RETRY_SCHEDULED` from the Attempt status enum.
- **F-003 integration is resolved**: F-005 MUST use F-003's published `DatasetVersionId`,
  Dataset Version metadata, and `candle-v1` checksum contract. It MUST NOT introduce a separate
  Dataset root identity or duplicate F-003 Dataset/Market Data ownership.
- **F-004 contract availability**: If the canonical Strategy contract is not available on this
  branch, only the Strategy provenance slot remains a typed placeholder. Implementation must
  not invent types that duplicate F-004 ownership.
---

## Requirements

### Functional Requirements

#### Experiment and Manifest

- **FR-001**: The system MUST allow an authenticated user to create an Experiment containing
  an Experiment Manifest with all mandatory provenance groups: Dataset identity/version/
  checksum, Strategy/Composite provenance, Backtest assumptions, Search configuration
  (algorithm, seed, stop conditions, Top-K) and software provenance.

- **FR-002**: The Experiment MUST support a two-phase lifecycle with respect to its Manifest:
  the CREATED phase (mutable input) and the freeze boundary at the CREATED → QUEUED
  transition (one-way, irreversible). As part of the CREATED → QUEUED transition, the system
  MUST validate Manifest completeness, compute and store the experimentFingerprint, freeze
  the Manifest, persist the QUEUED status, and write any required Outbox event atomically
  in a single transaction. Once the Experiment is QUEUED, no Manifest field may be
  updated.

- **FR-003**: At the CREATED → QUEUED freeze transition, the system MUST compute and store
  a deterministic experimentFingerprint (SHA-256 over the canonical field-sorted, UTC,
  decimal-as-string manifest). The fingerprint MUST be verifiable on re-computation from
  the stored manifest fields.

- **FR-004**: The system MUST record derivedFromExperimentId when an Experiment is created
  as a variation of an existing one, and reproducesExperimentId for Reproduction Runs.

- **FR-005**: Experiment Manifest provenance for Dataset MUST integrate, not duplicate, the
  canonical F-003 Dataset Version contract. `DatasetVersionId` is the stable Dataset identity;
  F-003 has no separate Dataset root entity. The Manifest provenance MUST preserve the published
  Dataset Version metadata needed for reproducibility, including contract `version`
  (`candle-v1` for F-003), checksum, provider, Trading Pair, Timeframe, normalization version,
  range, and candle count. F-005 MUST NOT introduce a separate `DatasetId`, Dataset membership
  model, checksum algorithm, or Market Data ingestion behavior.

- **FR-006**: Experiment Manifest provenance for Strategy MUST integrate, not duplicate, the
  canonical Strategy/User Strategy snapshot from F-004, including the optional
  source_user_strategy_version_id. If F-004 contracts are not yet merged, the slot must be
  a typed placeholder.

- **FR-007**: The system MUST support the Experiment state machine using the canonical DB
  status names. Required transitions:
  `CREATED → QUEUED → RUNNING → STOP_REQUESTED → STOPPED`;
  `RUNNING → COMPLETED`; `RUNNING → FAILED`.
  `CREATED` is the mutable input phase. `QUEUED` is the freeze/submission boundary (see
  FR-002). All state changes MUST be validated at the application boundary. The status
  values `DRAFT` and `CONFIRMED` MUST NOT be used.

#### Candidates

- **FR-008**: The system MUST allow Candidate definitions to be created under an Experiment
  that has reached QUEUED status (Manifest frozen). Each Candidate captures an immutable
  definition: generation index, strategy configuration and parameters.

- **FR-009**: A Candidate MUST belong to exactly one Experiment. The same-Experiment
  constraint MUST be enforced at both the persistence level and the application service.

- **FR-010**: Candidate definitions MUST be stored immutably once created. A Candidate's
  strategy configuration and parameters must not be altered after creation.

- **FR-011**: For Search Experiments, Candidate generation index and definition list MUST be
  stored to support reproducibility without re-running time-based generation.

#### Durable Job

- **FR-012**: The system MUST create a durable Job entity for each logical Search or Backtest
  operation. A Job MUST have: a unique jobId (ULID), job_type (SEARCH or BACKTEST), status,
  correlationId for tracing, progress fields, failure detail fields and lifecycle timestamps.

- **FR-013**: A Search Job MUST belong to an Experiment and MUST NOT be linked to a
  Candidate. A Backtest Job MUST belong to exactly one Candidate from the same Experiment.
  This structural constraint MUST be enforced at the persistence level.

- **FR-014**: At most one logical Backtest Job MUST exist per Candidate, regardless of Job
  lifecycle or terminal state. A repeated creation request for the same Candidate MUST either
  return the existing Job idempotently or be rejected as already existing. Cancellation MUST
  NOT permit creation of a second logical Backtest Job for the same Candidate.

- **FR-015**: The Job MUST support the following state machine:
  QUEUED -> RUNNING -> SUCCEEDED;
  RUNNING -> RETRY_SCHEDULED -> QUEUED;
  RUNNING -> FAILED;
  RUNNING -> CANCEL_REQUESTED -> CANCELLED;
  QUEUED -> CANCELLED;
  RETRY_SCHEDULED -> CANCELLED.
  Invalid state transitions MUST be rejected by the application service. A cancellation that races
  with `RETRY_SCHEDULED -> QUEUED` MUST be serialized so that exactly one transition wins; if the
  requeue commits first, the normal `QUEUED -> CANCELLED` rule applies.

- **FR-016**: The Job MUST support cancel polling: a capability that Workers can check at
  safe checkpoints without interrupting thread execution forcibly.

#### Execution Attempt

- **FR-017**: The system MUST record an Execution Attempt for each Worker try of a Backtest
  Job. In MVP, Search Jobs do NOT have Execution Attempts.

- **FR-018**: Each Execution Attempt MUST have a unique, monotonically increasing attempt
  number within its parent Job. Attempt-number allocation MUST be serialized per Job, and each
  newly created Attempt MUST receive `previous_max_attempt_number + 1` (starting from the
  repository-defined first attempt number). No two Attempts under the same Job may share an
  attempt number. A database unique constraint on `(job_id, attempt_number)` is the final
  collision guard, but uniqueness alone does not satisfy the monotonic allocation invariant.

- **FR-019**: An Execution Attempt MUST record: attempt identifier, Job reference, attempt
  number, Worker identity, status, start time, finish time, failure classification and error
  message. The allowed Execution Attempt status set is exactly: `QUEUED`, `RUNNING`,
  `SUCCEEDED`, `FAILED`, `CANCELLED`. `RETRY_SCHEDULED` is NOT a valid Execution Attempt
  status; it belongs exclusively to the Job state machine. A retryable failure finalizes the
  current Attempt as `FAILED` and transitions the Job to `RETRY_SCHEDULED`.

- **FR-020**: Retry MUST create a new Execution Attempt under the same Job ID. Retry MUST
  NOT create a new Job.

#### Ownership and Authorization

- **FR-021**: Every Experiment MUST have exactly one authenticated owner, recorded as the
  Supabase Auth UUID at creation time. Ownership MUST be derived through the authoritative
  parent chain for each resource: Experiment -> owner_user_id; Candidate -> Experiment ->
  owner_user_id; Job -> Experiment -> owner_user_id; Execution Attempt -> Job -> Experiment ->
  owner_user_id. Downstream resources MUST NOT define an independent ownership identity that
  can contradict the parent Experiment.

- **FR-022**: All application service operations on Experiment, Candidate and Job MUST
  include an authenticated owner predicate. Supplying only a resource identifier MUST NOT
  grant access; the system MUST resolve ownership from authenticated identity.

- **FR-023**: Cross-user access MUST be rejected at the application boundary using an
  ownership-safe inaccessible outcome that does not reveal the existence or content of another
  user's resources. Mapping that outcome to HTTP 403/404 is owned by F-009.

- **FR-024**: Browser/frontend roles MUST NOT be granted direct read or write access to the
  business schema tables introduced by this feature. Authorization is enforced at the Java
  application boundary.

#### Persistence Ports and Adapters

- **FR-025**: The experiment capability module MUST expose persistence output ports
  (repository interfaces) for Experiment, Manifest, Candidate, Job and ExecutionAttempt.
  The persistence module MUST implement these ports without the experiment module importing
  the adapter directly.

- **FR-026**: The persistence adapters MUST support: creating and querying Experiments with
  their Manifests, storing Candidates and their definitions, persisting Job lifecycle
  transitions atomically, appending Execution Attempts and querying them by Job.

- **FR-027**: Existing applied database migrations MUST NOT be edited. Any schema change for
  F-005 MUST be delivered as forward-only migration files.

- **FR-028**: Existing Execution Attempt rows created before the Job entity existed MUST be
  backfilled: each orphan Attempt must be associated with a valid Job. Migration MUST abort
  rather than guess if the legacy mapping is ambiguous.

#### Idempotency

- **FR-029**: The system MUST maintain durable idempotency records scoped by
  `owner_user_id` + `operation_scope` + `idempotency_key`. `operation_scope` identifies the
  logical application command independently of any HTTP endpoint (mapping owned by F-009).
  The `request_hash` MUST serve as an active conflict-detection gate:
  - If a command arrives with an existing key and identical `request_hash`, it MUST NOT
    re-execute; the system returns or resolves to the original in-progress or completed outcome.
  - If a command arrives with an existing key and differing `request_hash`, the system MUST
    reject the request as an application-layer idempotency conflict and MUST NOT execute or
    create a second record.

- **FR-030**: F-005 MUST expose durable idempotency operations sufficient for downstream
  processing to determine whether a logical operation has already been accepted or completed
  before executing duplicate business work. Actual queue acknowledgement, redelivery and
  consumer behavior are owned by F-007.

#### Transactional Outbox

- **FR-031**: The system MUST write an Outbox event row atomically in the same database
  transaction as the state change strictly for the following cross-boundary dispatch and
  cancellation transitions:
  - `Experiment CREATED → QUEUED`: produces `ExperimentQueued` event;
  - `Experiment RUNNING → STOP_REQUESTED`: produces `ExperimentStopRequested` event;
  - Backtest Job creation in `QUEUED`: produces `JobQueued` event;
  - `Job RETRY_SCHEDULED → QUEUED`: produces `JobQueued` event when the retry becomes
    dispatch-ready (`RETRY_SCHEDULED` itself does NOT produce an Outbox event);
  - `Job RUNNING → CANCEL_REQUESTED`: produces `JobCancelRequested` event;
  - `Job QUEUED → CANCELLED`: produces `JobCancelled` event.

- **FR-032**: Internal state mutations MUST NOT produce Outbox events in F-005. This non-publishing
  set includes: progress counter increments (`completed_work`, `failed_work`), `best_score`
  updates, Execution Attempt creation, Attempt status transitions, and local timestamps.

- **FR-033**: The Outbox row MUST contain a stable unique `event_id`, event type/version,
  aggregate identity, and sufficient idempotency/routing context for the publisher (F-007) to
  dispatch the message without additional database lookups for basic routing and for downstream
  processing to identify duplicate publication. F-005 owns atomic Outbox write-side persistence
  only; Outbox publisher polling, dispatch to Redis Streams, consumer deduplication, acknowledgement
  and delivery are owned by F-007.

#### Transaction-Enforced Invariants

- **FR-034**: The following invariants MUST be enforced within application transactions:
  - A Candidate belongs to the same Experiment as its Backtest Job.
  - Attempt-number allocation is serialized per Job and assigns the next monotonically
    increasing number (`previous max + 1`), with database uniqueness as the collision guard.
  - Job state transition validity.
  - Experiment Manifest immutability from the `CREATED → QUEUED` freeze boundary onward.
  - Outbox event co-written with the triggering state change.

- **FR-035**: The following invariants MUST be enforced at the database level:
  - Valid status enum for Experiment: `CREATED`, `QUEUED`, `RUNNING`, `COMPLETED`, `FAILED`,
    `STOP_REQUESTED`, `STOPPED`. The status values `DRAFT` and `CONFIRMED` MUST NOT appear.
  - Foreign key from Job to Experiment; composite FK ensuring Backtest Job Candidate belongs
    to the same Experiment.
  - Unique constraint on Attempt number within Job.
  - Valid status enum for Job: `QUEUED`, `RUNNING`, `RETRY_SCHEDULED`, `SUCCEEDED`, `FAILED`,
    `CANCEL_REQUESTED`, `CANCELLED`.
  - Valid status enum for Execution Attempt: `QUEUED`, `RUNNING`, `SUCCEEDED`, `FAILED`,
    `CANCELLED` only. A forward migration MUST remove `RETRY_SCHEDULED` from the
    `execution_attempt.status` check constraint inherited from the DB baseline.
  - Unique constraint preventing duplicate idempotency records.

---

### Key Entities

- **Experiment**: The top-level aggregate owned by one authenticated user. Starts in CREATED
  status (mutable input phase). The CREATED → QUEUED transition freezes the Manifest and
  submits the Experiment for execution. From QUEUED the Experiment progresses through
  RUNNING → COMPLETED | FAILED | STOP_REQUESTED → STOPPED. Tracks derivation and
  reproduction lineage.

- **Experiment Manifest**: An immutable provenance record capturing all inputs required to
  reproduce the Experiment: Dataset identity/checksum (from F-003), Strategy/Composite
  snapshot (from F-004), Backtest assumptions, Search configuration, software version and
  experimentFingerprint. Immutable from the CREATED → QUEUED freeze transition onward.

- **Candidate Definition**: An immutable record of one strategy configuration generated for
  an Experiment, with generation index and exact parameters. Belongs to exactly one
  Experiment.

- **Durable Job**: The logical long-running work identity for one Search or Backtest
  operation. Carries type, durable status, progress counters, correlation identity for
  tracing, failure classification, retry schedule and lifecycle timestamps. Survives Redis
  and Worker loss.

- **Execution Attempt**: An append-only, per-Worker-try record under a Backtest Job.
  Captures Worker identity, start/finish time, status, failure classification and error
  detail. Attempt numbers are unique and monotonically increasing within the Job. In MVP,
  only Backtest Jobs have Execution Attempts.

- **Idempotency Record**: A durable record scoped to `owner_user_id` + `operation_scope` +
  `idempotency_key`. Stores `request_hash`, `response_status`, `response_body` (or resource
  reference), and expiry. Serves as an active enforcement gate to prevent duplicate execution
  for identical requests and reject payload conflicts for mismatched requests.

- **Outbox Event**: A transactionally co-written event row (`platform.outbox_event`) recording
  a cross-boundary dispatch intent (`ExperimentQueued`, `ExperimentStopRequested`, `JobQueued`,
  `JobCancelRequested`, `JobCancelled`). Contains aggregate type/ID, event type/version, payload,
  headers, and routing context sufficient for F-007 dispatch without extra lookups.

---

## Success Criteria

### Measurable Outcomes

- **SC-001**: 100% of cross-user access attempts on Experiment, Candidate and Job are
  rejected; zero data leakage in two-user isolation tests.

- **SC-002**: A Backtest Job subjected to at least three simulated Worker failures records
  exactly three Execution Attempts with sequential attempt numbers and zero duplicate
  business outcomes.

- **SC-003**: 100% of state-changing commands that require publication have their Outbox
  event and business state row written in the same transaction; verified by injecting a
  transaction rollback and confirming both are absent.

- **SC-004**: After complete Redis wipe, 100% of Jobs in QUEUED or RUNNING state remain
  retrievable from durable storage with their last-known status, and Outbox events are
  available for republication.

- **SC-005**: The experimentFingerprint computed on a frozen Manifest is identical on two
  independent re-computations from the stored fields; any single-field mutation produces a
  different fingerprint.

- **SC-006**: A Manifest frozen at the `CREATED → QUEUED` boundary cannot be modified; 100% of direct Manifest
  field mutation commands are rejected by the application service.

- **SC-007**: Supplying only a jobId, candidateId or experimentId without a valid matching
  authenticated identity grants zero access to any resource in 100% of test cases.

- **SC-008**: Concurrent create-Job commands for the same Candidate produce exactly one Job;
  the second concurrent attempt returns the existing jobId or is rejected cleanly.

- **SC-009**: All existing Execution Attempt rows are associated with a valid Job after the
  forward migration completes; zero orphan Attempts remain.

- **SC-010**: A Reproduction Run creates a new Experiment linked to the original; the
  original Manifest, Candidates, Jobs and accepted Results remain unchanged.

---

## Assumptions

- Supabase Auth continues to own password, session and refresh token management. F-005 stores
  only the authenticated UUID identity for ownership purposes.

- The Database gate (db-setup-v2 schema increment) must be reviewed, approved and applied
  before F-005 implementation can be completed. F-005 specifications are forward-compatible
  with that schema increment.

- F-003 (Market Data/Dataset) and F-004 (Strategy Registry/User Strategy Library) define the
  canonical Dataset and Strategy/User Strategy provenance contracts. F-003 integration is
  resolved on this branch: F-005 uses `DatasetVersionId` as the canonical Dataset identity and
  the published immutable Dataset Version provenance/checksum contract. F-005 does not create a
  separate Dataset root identity. If F-004 is not yet available, only the Strategy provenance
  integration remains a typed-placeholder blocker.

- User Strategies (private saved configurations) are owned and specified by F-004.
  F-005 only references the User Strategy version ID as a provenance link in the Manifest;
  it does not re-implement User Strategy lifecycle, versioning or ownership.

- The Backtest engine, Evaluation metrics, Leaderboard, Redis Streams worker orchestration,
  Outbox publisher, public REST API, WebSocket delivery and frontend UI are explicitly out of
  scope for F-005.

- News/Sentiment data has its own lifecycle; F-005 records Sentiment provenance slots in the
  Manifest only if F-005 Experiments use Sentiment (optional in MVP). Sentiment ownership
  belongs to F-008.

- In MVP, Search Jobs record Search Coordinator progress directly on the Job; they do not
  produce Execution Attempts. This boundary may be extended in a future feature with a new
  ADR.

- The Execution Attempt data model established in the DB baseline (before the Job parent
  existed) must be migrated by the Database gate and F-005 backfill migration. The migration
  aborts if legacy Attempt rows cannot be unambiguously associated with a Job.

- At most one logical Backtest Job per Candidate is enforced across the entire lifecycle. A
  CANCELLED Job remains the Candidate's logical Backtest Job; cancellation does not permit a
  replacement Job for the same Candidate.

- The retention policy for Experiments, Manifests, Candidates, Jobs and Attempts is that no
  record referenced by a live Experiment may be deleted. Draft cleanup of unreferenced
  records is out of scope for F-005 and belongs to a dedicated retention feature.

- Application-layer authorization (not database RLS) is the primary enforcement mechanism
  for ownership. RLS may be applied as a defense-in-depth measure but is not the
  authoritative path.

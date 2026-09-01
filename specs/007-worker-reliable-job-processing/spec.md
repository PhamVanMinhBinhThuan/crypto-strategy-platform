# Feature Specification: Worker and Reliable Job Processing

**Feature Branch**: `feature/007-worker-reliable-job-processing`

**Feature Directory**: `specs/007-worker-reliable-job-processing`

**Created**: 2026-09-01

**Status**: Clarified — Ready for `/speckit-plan`

**Feature ID**: F-007

**Dependencies**:
- F-005 — Experiment Persistence and Ownership (required; owns Job/Attempt state machines and Outbox write-side)
- F-006 — Backtest, Evaluation and Leaderboard (required; owns all business execution contracts)

---

## Overview

F-007 establishes the reliable, asynchronous execution layer connecting the durable
Experiment/Job model (F-005) to the business execution capabilities (F-006). It owns:
versioned Redis Streams integration contracts; the `apps/worker` orchestration runtime;
at-least-once delivery via Redis Consumer Groups; the Transactional Outbox publisher;
bounded retry orchestration; processed-message deduplication; pending-message recovery after
Worker failure; recovery after complete Redis queue/cache loss using PostgreSQL as durable
truth; dead-letter handling after retry exhaustion; durable progress state plus transient
versioned progress notifications for later API/WebSocket delivery (F-009); graceful
cancellation polling; and end-to-end Worker observability.

**F-007 is orchestration only.** Business execution logic (Strategy application, Backtest
simulation, Evaluation metrics, Leaderboard Top-K) is invoked through the public contracts
of the capability modules and MUST NOT be reimplemented inside `apps/worker`.

**F-007 MUST NOT** implement public REST endpoints, WebSocket server, browser subscription
handling, or any frontend component. F-009 owns those.

**F-007 MUST NOT** reimplement the Job or Execution Attempt state machines. Those are owned
by F-005 and must be called through the published F-005 use-case boundary.

---

## Clarifications

### Session 2026-09-01

- Q: Which transitions MUST produce Outbox events (write-side)? → A: F-005 Outbox write-side trigger semantics are fixed. F-007 owns discovery, publication, and marking of `platform.outbox_event` rows only. The triggering transitions (`ExperimentQueued`, `ExperimentStopRequested`, `JobQueued`, `JobCancelRequested`, `JobCancelled`) are owned by F-005 and MUST NOT be re-triggered or duplicated by F-007. F-007 publication logic MUST treat those events according to their individual semantics; lifecycle/cancellation notifications are not automatically work-dispatch messages, and `JobCancelled` must never be suppressed merely because the durable Job is already `CANCELLED`. Exact physical routing of non-work lifecycle notifications is a planning concern, but they may not be silently discarded.
- Q: What is the exact scope of Search Coordinator and Random Search execution within F-007? → A: Option C — F-007 implements only the reliable Backtest Worker and Ranking Handler paths (consuming `backtest.jobs.v1`, producing `candidate.evaluated.v1`, updating Leaderboard Top-K, and handling `jobs.dead-letter.v1`). The `search.requests.v1` stream definition is documented as a reserved contract, but Search Coordinator execution, `RandomStrategyGenerator`, `StrategyGenerator` registry, and Search-specific stop conditions are deferred to a subsequent dedicated Search feature.
- Q: How should the retry lifecycle, retry budget, and failure classifications be orchestrated between Worker, F-005 use cases, and Outbox? → A: Option A — Worker maps runtime exceptions to FailureClassification and calls `FinalizeAttemptUseCase.finalizeFailure(...)` with `nextRetryAt` when retryable; F-005 finalizes the current Attempt as `FAILED` and sets parent Job status to `RETRY_SCHEDULED`. A dedicated Retry Orchestrator polling loop discovers due Jobs (`next_retry_at <= now()`), verifies durable state, and calls `RequeueRetryUseCase.requeueRetry(...)`, which transitions `RETRY_SCHEDULED → QUEUED` and writes a `JobQueued` Outbox event for dispatch by Outbox Publisher. Max attempts (e.g. 3) includes the initial try. A Worker execution timeout maps to `WORKER_CRASHED` (retryable); a transport/provider network timeout maps to `TRANSIENT_NETWORK_ERROR` (retryable). `PERMANENT_LOGIC_ERROR` and `UNKNOWN_ERROR` are non-retryable terminal failures.
- Q: How should message deduplication, atomicity, and concurrency protection be structured between platform.processed_message and domain Job state? → A: Option A — Dual-layer idempotency: `platform.processed_message` serves as a completed marker written upon completion before `XACK`, while F-005 Job/Attempt state machines (`QUEUED → RUNNING` atomic transition & unique attempt constraints) prevent concurrent duplicate execution. On message receipt, Worker checks both `processed_message` and durable Job state (skipping execution and calling `XACK` if already completed); after business effects are committed, `processed_message` is inserted and the message is acknowledged.
- Q: How should F-007 perform recovery when Redis is completely lost for already-published QUEUED jobs and stale RUNNING jobs? → A: Option A — Durable-state-driven dual reconciliation: (1) Queue Reconciler periodically scans PostgreSQL for durable QUEUED Jobs that remain dispatchable beyond a configurable reconciliation grace period, re-verifies durable state, and redispatches them using existing Job IDs and original Outbox/message context without depending on checking Redis; (2) Stale Job Reconciler detects orphaned RUNNING Attempts when `now > attempt.started_at + executionTimeout + recoveryGracePeriod`, re-verifies durable state, and calls `FinalizeAttemptUseCase.finalizeFailure(...)` with `WORKER_CRASHED` (retryable), which finalizes the Attempt as `FAILED` and moves the Job to `RETRY_SCHEDULED` (or terminal `FAILED` if max attempts exhausted). No heartbeat column or worker-lease table is introduced in MVP.
- Q: What is the reliability and persistence model for candidate.evaluated.v1, jobs.dead-letter.v1, and internal progress events? → A: Option A — Tiered reliability: (1) `candidate.evaluated.v1` is published by Worker upon Evaluation completion, with a durable Leaderboard reconciler ensuring that all completed Evaluations in PostgreSQL are safely projected into the Top-K Leaderboard even if Redis message loss or crash occurs; (2) `jobs.dead-letter.v1` is an operator diagnostic projection (PostgreSQL `experiment.job.status = FAILED` is the authoritative source of truth); (3) progress events are transient notifications backed by authoritative PostgreSQL Job progress counters (`completed_work`, `failed_work`, `best_score`).
- Q: How should trusted background Workers obtain the required owner authorization context when invoking owner-scoped F-005 use cases? → A: Option B — F-007 requires a dedicated trusted internal Worker/system use-case boundary owned by F-005. If that boundary is not already present in the current repository, F-007 integration work MUST add/extend the F-005 public API without weakening existing user-facing authorization. The trusted boundary resolves ownership internally from authoritative durable state (`Job → Experiment → owner_user_id`) and validates parent relationships (`Candidate → Experiment`, `Attempt → Job`) before executing transitions. F-007 Redis messages MUST NOT carry `ownerUserId` as authorization authority, carrying only routing/resource identities (`messageId`, `correlationId`, `experimentId`, `jobId`, `candidateId`). User-facing F-005 operations remain strictly owner-scoped and require explicit authenticated `ownerUserId`. Only trusted internal runtime composition (`apps/worker`) may bind to this Worker boundary; public API/F-009 must not use it. F-007 MUST NOT query F-005 persistence stores directly or import internal persistence packages.

---

## Integration Dependency Notice

### F-005 Contracts That F-007 Must Reuse

F-005 owns the durable Job and Execution Attempt state machines. F-007 MUST call the
F-005 public application-service boundary and MUST NOT reimplement status transitions.

| F-005 Use Case / Concept | F-007 Usage |
|---|---|
| Trusted Worker/system boundary | F-005-owned internal application boundary used only by `apps/worker`; resolves owner from durable Job/Experiment state and delegates to the canonical lifecycle logic |
| `StartNextAttemptUseCase` semantics | Invoked through the trusted Worker boundary when a `QUEUED` Backtest Job is eligible to start |
| `FinalizeAttemptUseCase` semantics | Invoked through the trusted Worker boundary when a Backtest attempt ends (success, failure, cancellation) |
| `RequeueRetryUseCase` semantics | Invoked through the trusted Worker boundary only when a durable `RETRY_SCHEDULED` Job is due and still eligible; performs `RETRY_SCHEDULED → QUEUED` and causes F-005 to write `JobQueued` |
| `CancelJobUseCase` semantics | Invoked through the trusted Worker boundary when a running Job reaches a safe cancellation checkpoint |
| `GetFrozenBacktestExecutionUseCase` semantics | Loads immutable Experiment Manifest and Candidate definition through the trusted Worker boundary |
| Job cancel-poll boundary | Worker-safe public query used at checkpoints to detect `CANCEL_REQUESTED` without direct persistence access |
| F-007 Outbox publication-side port | F-007 planning MUST introduce/use a public publication-side read/mark boundary implemented in `modules/persistence`; the existing F-005 `OutboxStore` write side remains unchanged |

### F-006 Contracts That F-007 Must Reuse

F-006 owns deterministic business execution. F-007 MUST NOT copy Backtest, Evaluation,
scoring, Strategy execution, Dataset traversal, or Top-K logic into `apps/worker`.

| F-006 Use Case | F-007 Usage |
|---|---|
| `RunBacktestUseCase` | Called by Backtest Worker with frozen provenance from F-005 |
| `EvaluateBacktestUseCase` | Called by Backtest Worker after a successful Backtest Result |
| `ProjectLeaderboardUseCase` | Called by Ranking Handler when `candidate.evaluated.v1` is consumed |

Worker maps queue message fields to the typed command expected by each use case. The
integration contract (`modules/contracts` message types) is not passed directly into
capability modules.

### F-009 Boundary

F-009 owns: public WebSocket endpoint (`/ws`), subscription IDs and browser protocol,
`SUBSCRIBE_EXPERIMENT`/`SUBSCRIBE_LEADERBOARD` commands, public REST endpoint mapping,
and HTTP status codes.

F-005 owns the durable Job progress fields. F-007 may update them only through a published
F-005 application/use-case boundary; it MUST NOT mutate F-005 persistence stores directly.
F-007 owns the transient versioned internal progress-notification boundary consumed later by
F-009 and ensures durable Job/Experiment/Result state remains authoritative — progress MUST
NOT exist only in Redis.

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 — End-to-End Job Dispatch from Outbox to Worker (Priority: P1)

As a system operator, I want any Job that has been durably committed to the PostgreSQL Outbox
to be reliably dispatched to the Redis Stream queue and processed by a Worker, so that no
committed Job is silently lost even if Redis was unavailable at commit time.

**Why this priority**: This is the fundamental reliability contract of the entire asynchronous
pipeline. Without it, nothing downstream works.

**Independent Test**: Create a Backtest Job (F-005), confirm the Outbox row is written.
Start the Outbox Publisher. Confirm the message appears on `backtest.jobs.v1` and the Outbox
row is marked published. Confirm a Backtest Worker picks up the message, begins execution,
and the Job transitions to SUCCEEDED or RETRY_SCHEDULED. No duplicate business outcomes.

**Acceptance Scenarios**:

1. **Given** a `JobQueued` Outbox event row exists with `published_at IS NULL`,
   **When** the Outbox Publisher scans for unpublished events,
   **Then** the publisher sends the versioned message to the target Redis Stream and only
   after Redis confirms receipt, marks the Outbox row's `published_at` and increments
   `publish_attempts`.

2. **Given** an unpublished Outbox event and Redis is temporarily unavailable,
   **When** the publisher attempts to send the message,
   **Then** the Outbox row remains with `published_at IS NULL`, `publish_attempts` and
   `last_error` are updated, and the event is retried on the next scan cycle without
   losing the committed Job.

3. **Given** an Outbox event is published to Redis but the publisher crashes before marking
   `published_at`,
   **When** the publisher recovers and scans again,
   **Then** the event is re-published (duplicate delivery). The downstream Worker handles
   the duplicate idempotently via `processed_message` and produces no second durable
   business outcome.

4. **Given** a valid `BacktestJobMessage` on the `backtest.jobs.v1` stream,
   **When** a Backtest Worker in the consumer group claims the message,
   **Then** the Worker checks `processed_message` and authoritative durable Job state, calls
   `StartNextAttemptUseCase` (F-005) before business execution, executes the Backtest pipeline
   through F-006 public use cases, commits/finalizes the required durable effects, inserts the
   completed `processed_message` marker, and only then acknowledges (`XACK`).

5. **Given** a Worker claims a message and completes durable effects but crashes before
   acknowledgement,
   **When** the consumer group detects the pending message has exceeded idle timeout,
   **Then** another Worker reclaims the message, detects the existing `processed_message`
   entry, skips re-execution, and acknowledges without creating a duplicate business outcome.

---

### User Story 2 — Bounded Retry After Transient Failure (Priority: P1)

As a system operator, I want transient Worker failures to trigger automatic bounded retries
without duplicating Job identities or business outcomes, so that temporary infrastructure
issues recover gracefully within a configurable retry budget.

**Why this priority**: Retry is the primary resilience mechanism. Without it, any transient
blip causes permanent Job failure.

**Independent Test**: Simulate a transient failure in the Backtest execution path. Confirm
the Worker calls `FinalizeAttemptUseCase.finalizeFailure(...)` with `nextRetryAt`, transitioning
the current Attempt to FAILED and the Job to RETRY_SCHEDULED. Confirm the Retry Orchestrator
discovers the due retry, calls `RequeueRetryUseCase.requeueRetry(...)`, transitioning Job to QUEUED
and generating a `JobQueued` Outbox event. Confirm the Publisher re-dispatches, a new Attempt is
created on the next try, and the retry count is bounded by the configured maximum.

**Acceptance Scenarios**:

1. **Given** a Backtest Worker encounters a transient failure and the retry budget is not
   exhausted (`attemptNumber < maxAttempts`),
   **When** the Worker maps the error to a retryable `FailureClassification` and calls
   `FinalizeAttemptUseCase.finalizeFailure(...)` with computed `nextRetryAt`,
   **Then** the current Execution Attempt is finalized as FAILED, the Job transitions to
   RETRY_SCHEDULED with `next_retry_at` set, the same Job ID is preserved, and no new Job is created.

2. **Given** a Job in RETRY_SCHEDULED state and `next_retry_at <= now()`,
   **When** the Retry Orchestrator polling loop discovers the due retry and verifies durable Job state,
   **Then** it calls `RequeueRetryUseCase.requeueRetry(...)`, which transitions the Job from
   `RETRY_SCHEDULED → QUEUED` and atomically writes a `JobQueued` Outbox event.
   **When** the Outbox Publisher dispatches the event, the Worker reads durable Job state before
   execution (skipping if CANCELLED) and begins the next Execution Attempt.

3. **Given** the retry budget is exhausted (`attemptNumber >= maxAttempts`),
   **When** the Worker encounters a failure and calls `FinalizeAttemptUseCase.finalizeFailure(...)`
   with `nextRetryAt = null` (or terminal classification),
   **Then** the Job transitions to FAILED in PostgreSQL (via F-005), failure classification and
   diagnostic summary are stored durably, and a notification message is routed to `jobs.dead-letter.v1`.

4. **Given** a failure is classified as permanent (e.g. `PERMANENT_LOGIC_ERROR`),
   **When** the Worker finalizes the Attempt,
   **Then** the Job transitions immediately to FAILED without scheduling any retry, and a
   notification is routed to dead-letter.

5. **Given** Worker A fails mid-execution and Worker B reclaims the pending message,
   **When** Worker B evaluates the redelivery,
   **Then** Worker B first checks `processed_message` and durable Job/Attempt state. If the
   original durable effect had completed, it skips and acknowledges. If the original Attempt
   is still `RUNNING`, Worker B MUST NOT start another Attempt; stale-execution reconciliation
   must finalize the orphaned Attempt before the normal retry path can create a later Attempt.

---

### User Story 3 — Processed-Message Deduplication (Priority: P1)

As a system operator, I want each message, even if delivered multiple times due to
at-least-once queue semantics, to produce at most one durable business effect, so that
queue redelivery never corrupts Backtest Results, Evaluations, or Leaderboard entries.

**Why this priority**: At-least-once delivery is a fundamental property of Redis Streams.
Idempotent consumption is non-negotiable.

**Independent Test**: Publish the same `BacktestJobMessage` (identical `messageId` and
`jobId`) to `backtest.jobs.v1` twice. Confirm only one Backtest Result, one Evaluation
Result, and at most one Leaderboard Revision are produced. The second delivery is detected
via `platform.processed_message` and acknowledged without re-executing business logic.

**Acceptance Scenarios**:

1. **Given** a Worker successfully processes a message and records a `processed_message`
   entry keyed by `(consumer_name, messageId)`,
   **When** the same `messageId` is delivered again,
   **Then** the Worker detects the existing entry, skips business execution, and acknowledges
   without creating a duplicate Backtest Result, Evaluation, or Leaderboard change.

2. **Given** a Worker crashes after recording durable business effects but before
   acknowledging,
   **When** the message is redelivered,
   **Then** the `processed_message` entry is present, the Worker skips re-execution, and
   acknowledges.

3. **Given** a `CandidateEvaluatedMessage` on `candidate.evaluated.v1` is delivered twice,
   **When** the Ranking Handler processes the duplicate,
   **Then** the Leaderboard Top-K is not updated twice; the duplicate is detected and
   acknowledged without creating a second Leaderboard Revision.

4. **Given** a `processed_message` entry has expired (`expires_at` has passed),
   **When** a message with the same `messageId` arrives,
   **Then** the system treats it as unprocessed and executes normally. TTL must be long
   enough to cover the maximum expected redelivery window.

---

### User Story 4 — Recovery After Redis Queue/Cache Loss (Priority: P2)

As a system operator, I want the system to recover all unfinished work from PostgreSQL
durable state after Redis is wiped or lost, so that no committed Jobs are permanently
abandoned due to transient infrastructure failure.

**Why this priority**: Constitutional guarantee that Redis loss cannot destroy business truth.

**Independent Test**: Advance multiple Jobs to QUEUED and RUNNING status with corresponding
Outbox rows. Wipe Redis entirely. Restart the Worker and Publisher. Confirm Queue Reconciler
discovers QUEUED jobs and redispatches them without altering Job IDs; confirm Stale Job
Reconciler recovers orphaned RUNNING jobs as WORKER_CRASHED; confirm no duplicate business
outcomes are produced.

**Acceptance Scenarios**:

1. **Given** Jobs in QUEUED status whose Outbox rows were already marked published (`published_at IS NOT NULL`)
   and Redis is completely lost/flushed,
   **When** the Queue Reconciler background loop runs (after configurable grace period),
   **Then** it discovers the dispatchable QUEUED Jobs from PostgreSQL, re-verifies durable state,
   and redispatches them to Redis Streams using the existing Job ID and original correlation/message
   context. It does not depend on inspecting Redis state to decide dispatchability.

2. **Given** a Job is in RUNNING status when the Worker process or Redis crashes,
   **When** `now > attempt.started_at + executionTimeout + recoveryGracePeriod`,
   **Then** the Stale Job Reconciler detects the orphaned Attempt, verifies durable state, and
   calls `FinalizeAttemptUseCase.finalizeFailure(...)` with `WORKER_CRASHED` (retryable). The Attempt
   is marked FAILED, and the Job moves to RETRY_SCHEDULED (or FAILED if max attempts exhausted).
   The normal Retry Orchestrator later requeues the Job via `RequeueRetryUseCase`.

3. **Given** a Job is in RETRY_SCHEDULED status when Redis is lost,
   **When** recovery runs and `next_retry_at <= now()`,
   **Then** the Retry Orchestrator discovers the due retry from durable state and calls
   `RequeueRetryUseCase`, transitioning `RETRY_SCHEDULED → QUEUED` and creating a new `JobQueued`
   Outbox event. The retry count and original Job ID are preserved.

4. **Given** a duplicate redispatch occurs because a previously-published message was still
   in-flight during reconciliation,
   **When** a Worker consumes the duplicate message,
   **Then** dual-layer idempotency (checking `processed_message` and durable Job state) ensures
   the Worker skips redundant execution and acknowledges (`XACK`) without corrupting state.

5. **Given** Redis is restored after an outage,
   **When** the system resumes,
   **Then** no new Experiment or Job identities are created; the system publishes only from
   existing durable entities and picks up Jobs from the same original IDs.

---

### User Story 5 — Graceful Cancellation at Safe Checkpoints (Priority: P2)

As an authenticated user who has requested to stop an Experiment, I want running Workers to
observe the cancellation signal at safe execution points and conclude cleanly without forcing
thread termination, so that partial business outcomes are never produced.

**Why this priority**: Graceful cancellation protects data integrity and is required for the
stop-Experiment user flow defined in F-005 and ADR-0006.

**Independent Test**: Start a Backtest Job. While the Worker is executing, issue a Stop
command on the Experiment. Confirm the Job transitions to CANCEL_REQUESTED. Confirm the
Worker polls the cancel flag at the next checkpoint, calls `CancelJobUseCase` (F-005), the
Job reaches CANCELLED, and no partial Backtest Result is persisted.

**Acceptance Scenarios**:

1. **Given** a Job is RUNNING and the Experiment owner issues a Stop command,
   **When** the Job is flagged as CANCEL_REQUESTED,
   **Then** the Worker polls the cancel flag at its next safe checkpoint, halts execution,
   does not persist a partial Result, calls `CancelJobUseCase` (F-005), and the Job reaches
   CANCELLED.

2. **Given** a Worker detects CANCEL_REQUESTED at a checkpoint and stops,
   **When** the Job reaches CANCELLED,
   **Then** no Backtest Result, Evaluation Result, or Leaderboard entry is produced.

3. **Given** a Job in RETRY_SCHEDULED when cancellation is issued,
   **When** the cancellation is processed before the retry is re-queued,
   **Then** the Job transitions directly to CANCELLED (via F-005) and no new Execution
   Attempt is created.

4. **Given** a QUEUED Job when cancellation arrives,
   **When** the Outbox Publisher would dispatch the Job,
   **Then** the Publisher reads the Job's current durable state, detects CANCELLED status,
   and skips dispatch without sending the message to the Redis Stream.

5. **Given** all Jobs under an Experiment are terminal after a Stop,
   **When** the F-007 stop-completion reconciliation path observes no active Jobs remain,
   **Then** it invokes the published F-005 lifecycle boundary to complete the Experiment as
   `STOPPED`. This does not require the deferred Search Coordinator.

---

### User Story 6 — Progress Events for API/WebSocket Delivery (Priority: P2)

As an authenticated user, I want to see live progress updates for my running Experiment
without polling — showing candidates completed, failures, current best score, and pipeline
step — so that I can monitor long-running searches in real time.

**Why this priority**: Progress visibility is a core user requirement. F-009 WebSocket events
(`EXPERIMENT_PROGRESS_UPDATED`, `BACKTEST_COMPLETED`, `LEADERBOARD_UPDATED`) depend on
authoritative durable F-005/F-006 state plus a versioned transient notification boundary
provided by F-007.

**Independent Test**: Start an Experiment and observe the F-007 progress event boundary
updated after each Candidate completes. Confirm progress fields in the durable Job are
updated and a progress event is emitted at a boundary consumable by F-009. Confirm progress
is readable from durable PostgreSQL state even if WebSocket is unavailable.

**Acceptance Scenarios**:

1. **Given** a Backtest Worker completes a Candidate successfully,
   **When** the Backtest/Evaluation durable effects and F-005 success finalization complete,
   **Then** the durable completed-work progress is updated through the F-005 public progress
   boundary and a candidate/backtest-completed notification may be emitted. Leaderboard
   projection remains asynchronous through `candidate.evaluated.v1` or reconciliation; a
   Leaderboard Revision reference is included only after Ranking Handler/reconciliation
   actually creates or confirms that revision.

2. **Given** a Candidate fails permanently,
   **When** the Worker finalizes the failure,
   **Then** the failed-count progress field is incremented durably in the Job and a progress
   event reflecting the updated failure count is emitted.

3. **Given** a Worker produces a new Leaderboard Revision,
   **When** the Ranking Handler completes `ProjectLeaderboardUseCase` (F-006),
   **Then** a `LEADERBOARD_UPDATED`-equivalent event is emitted with the new Revision
   reference for F-009 to push to WebSocket subscribers.

4. **Given** Redis is unavailable temporarily,
   **When** progress events cannot be published to any ephemeral Redis channel,
   **Then** the durable progress fields on the Job remain the authoritative source. Progress
   MUST NOT exist only in Redis.

---

### User Story 7 — Dead-Letter Handling and Diagnostic Preservation (Priority: P3)

As a system operator, I want permanently failed Jobs to be routed to a dead-letter stream
with safe diagnostic context, so that I can inspect the failure without losing the original
Job identity or corrupting other Candidates in the same Experiment.

**Why this priority**: Dead-letter isolation prevents poison messages from blocking the queue
and enables post-hoc diagnosis.

**Independent Test**: Exhaust the retry budget for a Backtest Job. Confirm the Job
transitions to FAILED and failure classification/diagnostic state is stored durably. With
Redis available, confirm a dead-letter diagnostic message appears on `jobs.dead-letter.v1`.
If Redis is unavailable or the DLQ is lost, confirm durable FAILED state remains sufficient
to reconstruct the diagnostic projection. Confirm other Jobs in the same Experiment continue.

**Acceptance Scenarios**:

1. **Given** a Job has exhausted all retry attempts,
   **When** the final attempt is finalized as FAILED,
   **Then** the Job remains FAILED with failure classification and safe diagnostic summary
   stored durably (without stack traces, credentials, or raw SQL error text). A dead-letter
   message is published to `jobs.dead-letter.v1` containing jobId, experimentId,
   candidateId, failure classification, and a safe diagnostic reference.

2. **Given** a dead-letter message for one Candidate exists,
   **When** other Candidates in the same Experiment are processed,
   **Then** the dead-lettered Candidate does not block, corrupt, or affect any other
   Candidate's processing.

3. **Given** an operator inspects the dead-letter stream,
   **When** the operator reads a dead-letter message,
   **Then** the message contains jobId, experimentId, failure code, and enough context to
   trace the failure in durable state without exposing internal class names, credentials,
   raw SQL errors, or stack traces.

---

### User Story 8 — Worker Observability and Correlation Propagation (Priority: P3)

As a system operator, I want Worker logs and monitoring data to carry consistent correlation
identifiers so that I can trace the complete lifecycle of any Job across Outbox Publisher,
Consumer, and capability module boundaries.

**Why this priority**: Without correlation IDs, diagnosing failures across asynchronous
boundaries is impractical.

**Independent Test**: Run an Experiment end-to-end and inspect Worker logs. Confirm every
log entry for a given Job carries `correlationId`, `experimentId`, `candidateId`, and
`jobId`. Confirm progress counters are observable.

**Acceptance Scenarios**:

1. **Given** a message is consumed from any F-007 stream,
   **When** the Worker begins processing,
   **Then** `correlationId`, `experimentId`, `candidateId` (where applicable), and `jobId`
   are propagated into the Worker's logging context for the full duration of the operation.

2. **Given** a Worker calls an F-005 or F-006 use case,
   **When** any log or error is emitted,
   **Then** the same correlation identifiers are present, enabling end-to-end trace.

3. **Given** a Worker processes a Job,
   **When** the Execution Attempt is recorded (F-005),
   **Then** the Worker instance/consumer identity is recorded in the Attempt for audit.

4. **Given** an Experiment is running,
   **When** observability data is collected,
   **Then** at minimum the following can be monitored: Experiment status, candidates
   generated/evaluated/succeeded/failed, queued and running job counts, dead-letter count,
   elapsed time, retry count, current best score, queue lag/pending count, and Backtest
   execution duration.

---

### Edge Cases

- **Duplicate Outbox publication**: Crash between Redis publish and `published_at` mark
  produces a duplicate message. The downstream Worker must detect and skip via
  `processed_message`. This is a required safety path, not optional.
- **Cancel races retry re-queue**: If `RETRY_SCHEDULED → QUEUED` and cancellation commit
  race, exactly one wins (per F-005 serialization). If re-queue wins, the normal
  `QUEUED → CANCELLED` path applies. The Worker always reads durable Job state before
  beginning execution.
- **No full dataset payload in messages**: Queue messages carry IDs and routing context
  only. Workers load all execution data from durable storage via capability module contracts.
  Candle datasets, Trade lists, and full Strategy definitions are never embedded in messages.
- **Permanent failure in first attempt**: A permanently invalid message skips retry entirely
  and goes directly to FAILED + dead-letter.
- **Worker polls cancelled Job after reclaim**: A reclaimed pending message may have a Job
  already CANCELLED. The Worker detects this before business execution, skips, and
  acknowledges.
- **Ranking after Backtest success**: `candidate.evaluated.v1` normally refers to a Backtest
  Job that is already `SUCCEEDED`. The Ranking Handler MUST still perform/verify the
  idempotent Leaderboard projection; Backtest terminal-state suppression applies to
  `backtest.jobs.v1` execution, not to ranking messages.
- **Dead-letter for a still-active Experiment**: One dead-lettered Candidate MUST NOT
  propagate failure to the entire Experiment or affect other Candidates' Jobs.
- **No exactly-once delivery**: At-least-once delivery + idempotent durable effects is
  guaranteed. Exactly-once Redis delivery is never claimed.
- **Recovery preserves identities**: Redis loss or service restart MUST NOT result in new
  Experiment IDs, Job IDs, or Candidate IDs.
- **Unroutable Outbox event type**: An event type the publisher does not recognize must not
  be silently discarded; it must be flagged as unroutable and require operator attention.
- **Queue message schema version mismatch**: Messages carry `messageVersion`. A breaking
  schema change requires a new stream version. Consumers MUST reject messages with
  unrecognized `messageVersion` values.
- **Back-pressure**: F-007 Worker consumption MUST enforce configurable bounded in-flight
  concurrency/prefetch so Redis redelivery and Worker scaling cannot create unbounded local
  work. Search-space generation and per-Experiment Candidate/Job production limits belong to
  the deferred Search capability, not F-007.

---
## Requirements *(mandatory)*

### Versioned Redis Streams Integration Contracts

- **FR-001**: F-007 MUST use the following logical Redis Streams as the integration boundary:

  | Stream | Physical Redis Producer | Logical Initiator | Consumer Role | Purpose |
  |---|---|---|---|---|
  | `search.requests.v1` | Reserved contract only in F-007 | Future Search feature | Not consumed by F-007 | Reserved start-Search contract for a later dedicated Search capability |
  | `backtest.jobs.v1` | Outbox Publisher | Durable Backtest Job creation/requeue via F-005 `JobQueued` Outbox | Backtest Worker Group | Execute Backtest + Evaluate for one Candidate |
  | `candidate.evaluated.v1` | Backtest Worker | Evaluation Completed | Ranking Handler | Update score and Top-K Leaderboard |
  | `jobs.dead-letter.v1` | Worker / Retry Orchestrator | Retry Exhaustion / Permanent Failure | Operator / Recovery | Preserve exhausted Jobs for inspection |

  Physical stream names MAY include an environment prefix. The version segment MUST be
  incremented when a breaking message schema change is introduced.

  `search.requests.v1` is reservation/documentation only in F-007: no consumer group,
  Search Coordinator runtime, Candidate generation, or Search stop-condition behavior is
  implemented by this feature.

- **FR-002**: Every operational F-007 message MUST include a stable `messageId` (ULID format),
  `messageVersion` (positive integer), `messageType` (UPPER_SNAKE_CASE), `occurredAt`
  (UTC ISO-8601), `correlationId`, and the minimum routing/resource identifiers required by that
  message type. `BacktestJobMessage` MUST include `experimentId`, `jobId`, and `candidateId`.
  Queue messages MUST NOT carry `ownerUserId` as authorization authority; F-005 resolves ownership
  internally through its dedicated trusted Worker use-case boundary (`Job → Experiment → owner_user_id`).
  Dataset/Strategy provenance MUST NOT be treated as queue truth: the Worker loads the frozen
  Dataset/Strategy provenance from F-005 `GetFrozenBacktestExecutionUseCase`. F-007 MUST NOT
  introduce a separate F-003 `DatasetId` root or duplicate frozen Manifest provenance in Redis
  merely for execution convenience.

- **FR-003**: Queue messages MUST NOT embed full Candle datasets, Trade lists, Java-serialized
  objects, internal class names, credentials, raw SQL errors, or stack traces. Workers load
  all execution data from durable capability module contracts using IDs/references in the message.

- **FR-004**: Breaking changes to a message schema MUST result in a new stream version.
  Consumers MUST reject messages with unrecognized `messageVersion` values.

### Outbox Publisher

- **FR-005**: The Outbox Publisher MUST poll `platform.outbox_event` for rows where
  `published_at IS NULL`, in order of `occurred_at`, and dispatch each event to the
  appropriate Redis Stream based on `event_type` and `aggregate_type`.

- **FR-006**: Every physical publish attempt MUST increment `publish_attempts`. On successful
  Redis receipt confirmation, the publisher MUST set `published_at` and clear `last_error`.
  A crash after Redis accepts the message but before the database success mark is safe because
  the same durable event may be re-published and consumers are duplicate-safe.

- **FR-007**: If Redis is unavailable or publication fails, the publisher MUST keep
  `published_at IS NULL`, store only a safe diagnostic in `last_error`, increment
  `publish_attempts`, and retry on a later scan according to bounded publisher backoff.

- **FR-007A**: F-007 MUST access `platform.outbox_event` through a publication-side public
  port supporting discovery/claim (if required for concurrency), success marking,
  suppression marking/audit, and failure-attempt recording. The existing F-005 write-side
  `OutboxStore.insertOutboxEvent(...)` remains unchanged. `apps/worker` MUST NOT import JDBC,
  SQL, or `modules/persistence` internal implementation classes.

- **FR-008**: Outbox publication MUST apply event-specific durable-state validation rather
  than a generic terminal-state suppression rule:
  - `JobQueued` is a work-dispatch intent and MUST be published only while the Job remains
    dispatchable as `QUEUED`; if the Job has since become `CANCELLED` or another non-dispatchable
    state, the stale dispatch intent is safely suppressed.
  - `JobCancelRequested` is a cancellation notification for work that was running. If the Job
    has already reached `CANCELLED`, the stale cancel-request notification MAY be suppressed
    because no active cancellation action remains.
  - `JobCancelled` MUST NOT be suppressed merely because the Job is `CANCELLED`; `CANCELLED`
    is the state this event announces.
  - `ExperimentQueued` and `ExperimentStopRequested` MUST likewise be validated according to
    their own event meaning. An event MUST NOT be suppressed solely because the aggregate has
    reached the state that the event represents.
  Suppression MUST be explicit/auditable and MUST NOT mutate the F-005 lifecycle semantics.

### Redis Consumer Groups and At-Least-Once Delivery

- **FR-009**: F-007 operational consumers (`backtest.jobs.v1` and
  `candidate.evaluated.v1`) MUST use Redis Consumer Groups with explicit acknowledgement
  (`XACK`) only after the required durable effects/duplicate-safe terminal checks are complete.
  F-007 does not consume reserved `search.requests.v1` in this feature.

- **FR-010**: Workers MUST support pending-message reclaim: after a configurable idle timeout,
  a pending message may be claimed by another Worker instance or the same instance after
  restart. The reclaiming Worker applies the same idempotency check before execution.

- **FR-011**: The system MUST NOT claim exactly-once delivery. The guaranteed semantic is
  at-least-once delivery combined with idempotent durable effects.

### Processed-Message Deduplication and Dual-Layer Idempotency

- **FR-012**: Every F-007 consumer MUST combine the shared
  `platform.processed_message` completed marker with the durable idempotency guard appropriate
  to that consumer:
  1. *Infrastructure layer (`platform.processed_message`)*: On receipt, the consumer MUST check
     for an unexpired row keyed by `(consumer_name, messageId)`. If present, it MUST skip the
     already-completed effect and acknowledge (`XACK`).
  2. *Backtest Worker (`backtest.jobs.v1`)*: F-005 Job/Attempt state is the domain concurrency
     guard. Only an eligible `QUEUED` Job may start a new Attempt. The F-005 atomic
     `QUEUED → RUNNING` transition plus unique `(job_id, attempt_number)` constraints prevents
     concurrent duplicate Backtest execution. A terminal Job (`SUCCEEDED`, `FAILED`,
     `CANCELLED`) is not re-executed; the completed marker may be repaired where safe before
     acknowledgement. A `RUNNING` Job is not started concurrently and is handled by the
     pending/stale-execution recovery policy.
  3. *Ranking Handler (`candidate.evaluated.v1`)*: A `SUCCEEDED` Backtest Job is expected and
     MUST NOT itself cause the Ranking Handler to skip projection. Ranking deduplication relies
     on the processed-message marker plus F-006's idempotent/durable Leaderboard projection
     semantics and the Leaderboard reconciler. Duplicate messages or reconciliation races MUST
     produce no duplicate logical Leaderboard effect.

- **FR-013**: After successfully completing the durable effect required by a consumed
  message, the responsible F-007 consumer MUST insert a `platform.processed_message` record
  (`consumer_name`, `message_id`, `processed_at`, `expires_at`) and only then acknowledge
  (`XACK`). If a process crashes after the durable effect commits but before the marker is
  inserted, redelivery MUST be made safe by that consumer's durable domain guard described in
  FR-012; the consumer repairs the completed marker where safe and acknowledges without
  creating a duplicate logical effect.

- **FR-014**: F-007 MUST reuse the baseline `platform.processed_message` schema
  (`consumer_name`, `message_id`, `processed_at`, `expires_at`). The TTL (`expires_at`) MUST be
  environment-configurable and longer than the configured Redis pending/reclaim/redelivery and
  operational recovery horizon. Expiry of the infrastructure marker MUST NOT make an otherwise
  durable terminal Backtest or already-projected Evaluation unsafe to process again.

### Retry Orchestration

- **FR-015**: Retry MUST be bounded. Maximum retry count, retry delay (including exponential
  backoff strategy), and execution timeout MUST be configurable per environment and per
  failure classification. The system MUST NOT create infinite retry loops.

- **FR-016**: Retry MUST preserve Job identity. The same `jobId` is retained across all
  retries. A new Execution Attempt is created by calling `StartNextAttemptUseCase` (F-005)
  at the start of each new try.

- **FR-017**: The F-007 Retry Orchestrator MUST discover retries from durable
  `Job.status = RETRY_SCHEDULED` plus `next_retry_at`, re-read the current Job state, and call
  `RequeueRetryUseCase` only when the Job is still `RETRY_SCHEDULED`, due, and within budget.
  Cancellation must therefore win safely before `RETRY_SCHEDULED → QUEUED`. Once F-005 performs
  that transition it writes `JobQueued`; the Outbox Publisher only publishes the resulting
  durable event and may still suppress stale `JobQueued` delivery if the Job has since become
  `CANCELLED`.

- **FR-018**: Failure classification MUST be mapped at runtime by F-007 (Worker runtime) and
  passed to F-005 via `FinalizeAttemptUseCase`. F-007 maps runtime exceptions to the five canonical
  failure classifications defined by the F-005 Job/Attempt contract: `TRANSIENT_NETWORK_ERROR`,
  `DATA_UNAVAILABLE_RETRY`, `WORKER_CRASHED`, `PERMANENT_LOGIC_ERROR`, `UNKNOWN_ERROR`. F-005
  owns the resulting durable Job/Attempt status transition; F-007 MUST NOT reimplement the
  transition logic.

- **FR-019**: Permanent failures MUST skip retry and transition the Job directly to FAILED.

### Dead-Letter Behavior

- **FR-020**: When a Job's retry budget is exhausted, the Worker MUST publish a dead-letter
  message to `jobs.dead-letter.v1` containing: `jobId`, `experimentId`, `candidateId` (if
  Backtest), `messageId` (original), failure classification, and a safe diagnostic reference.
  The message MUST NOT contain stack traces, credentials, internal class names, or raw SQL
  error text. PostgreSQL `experiment.job.status = FAILED` plus durable failure metadata is the
  authoritative source of truth; `jobs.dead-letter.v1` is a diagnostic projection. Failure to
  publish or loss of the Redis DLQ MUST NOT alter durable Job failure state, and diagnostic
  messages MAY be regenerated/reconciled from durable FAILED Jobs.

- **FR-021**: A dead-lettered Candidate MUST NOT prevent other Candidates in the same
  Experiment from being processed. Progress counters MUST reflect the failure without
  halting the Experiment.

### Cancellation

- **FR-022**: Workers MUST poll the Job cancel flag (F-005 cancel-poll boundary) at safe
  execution checkpoints during Backtest execution. Workers MUST also re-read durable Job state
  before beginning execution to handle stale in-flight queue messages.

- **FR-023**: Upon detecting a cancel signal at a checkpoint, the Worker MUST call
  `CancelJobUseCase` (F-005), discard in-memory partial results, and acknowledge without
  persisting any Backtest Result, Evaluation, or Leaderboard change.

- **FR-024**: QUEUED Jobs whose Outbox events are pending publication MUST be skipped by
  the publisher if durable status is already CANCELLED.

### apps/worker Orchestration

- **FR-025**: `apps/worker` MUST be a standalone Java/Spring Boot runtime that uses the
  public API boundaries of capability modules and MUST NOT import internal implementation
  packages or persistence adapters of any capability module.

- **FR-026**: `apps/worker` MUST support multiple concurrent instances running in the same
  Redis Consumer Group without duplicate execution. Scaling Worker instances MUST NOT
  require changes to Strategy, Backtest, Evaluation, or API contracts.

- **FR-027**: `apps/worker` MUST enforce configurable bounded consumer concurrency,
  prefetch/read batch size, and in-flight work per Worker instance so local execution cannot
  grow without bound. Search-space generation and per-Experiment Job production limits are
  outside F-007 because Search Coordinator execution is deferred.

- **FR-028**: Each Worker execution of a Backtest Job MUST have a configurable execution
  timeout. Exceeding the Worker execution timeout maps to F-005 `WORKER_CRASHED`; a distinct
  provider/transport network timeout maps to `TRANSIENT_NETWORK_ERROR`. Retry behavior follows
  the canonical F-005 retryability contract and F-007 bounded retry budget.

- **FR-029**: `apps/worker` MUST map integration message fields to the typed command objects
  expected by capability use cases before calling them. Messages from `modules/contracts`
  MUST NOT be passed directly into capability module business logic.

- **FR-029A**: Background execution MUST use an F-005-owned trusted Worker/system application
  boundary that derives owner authorization from durable `Job → Experiment → owner_user_id`
  and validates Candidate/Attempt parent relationships. If the current repository does not
  yet expose this facade, F-007 implementation MAY extend the F-005 public API to add it.
  This boundary is for trusted `apps/worker` composition only and MUST NOT be exposed through
  F-009/public API as an authorization bypass.

### Progress Events, Downstream Reliability, and F-009 Boundary

- **FR-030**: F-007 MUST enforce tiered reliability for downstream events:
  1. *Candidate evaluation and Leaderboard*: `candidate.evaluated.v1` is an asynchronous fast-path
     notification to the Ranking Handler. A durable Leaderboard reconciler MUST discover completed
     Evaluations that are eligible but not yet projected according to F-006 durable state and invoke
     idempotent `ProjectLeaderboardUseCase`, so a crash/message loss cannot permanently lose ranking.
  2. *Dead-letter queue*: `jobs.dead-letter.v1` is an operator notification and diagnostic projection.
     PostgreSQL `experiment.job.status = FAILED` plus durable failure metadata is authoritative.
  3. *Progress events*: Progress notifications emitted to the internal F-009-facing boundary are
     transient change notifications backed by authoritative PostgreSQL Job progress counters
     (`completed_work`, `failed_work`, `total_work`, `best_score`).
  4. F-007 MUST update those F-005-owned progress fields only through a published F-005
     application/use-case boundary. If the current repository lacks such a boundary, the F-007 plan
     MUST add/extend that public F-005 boundary without allowing `apps/worker` to access F-005
     persistence stores directly.

- **FR-031**: F-007 MUST emit a versioned internal progress event at a boundary consumable
  by F-009, carrying sufficient context for F-009 to push `EXPERIMENT_PROGRESS_UPDATED`,
  `BACKTEST_COMPLETED`, or `LEADERBOARD_UPDATED` WebSocket events.

- **FR-032**: Progress MUST NOT exist only in Redis. Durable Job progress fields in
  PostgreSQL MUST be the authoritative source. REST API clients (future F-009) must be able to
  read accurate progress from durable state even if WebSocket delivery is absent.

- **FR-033**: F-007 MUST NOT implement the public WebSocket endpoint, subscription protocol,
  `subscriptionId` semantics, or REST endpoint. Those belong to F-009.

### Observability and Correlation

- **FR-034**: Every log entry within `apps/worker` during message processing MUST carry
  `correlationId`, `experimentId`, `jobId`, and `candidateId` (where applicable), sourced
  from the message envelope.

- **FR-035**: Worker logs and error records MUST NOT contain database credentials, Redis
  passwords, user personal data, raw exception stack traces in external outputs, internal
  Java class names, or raw SQL error strings.

- **FR-036**: F-007 MUST provide observable metrics covering at minimum: queued job count,
  running job count, dead-letter count, Outbox unpublished event lag, Backtest execution
  duration, retry count, and per-Experiment candidate progress counters.

### Key Entities

- **Outbox Publisher**: A polling process in `apps/worker` that discovers `platform.outbox_event`
  rows with `published_at IS NULL`, checks durable aggregate state before dispatch, sends each to
  the appropriate Redis Stream, and records `published_at` only after Redis confirms receipt.

- **Queue Reconciler**: A background process in `apps/worker` that periodically scans PostgreSQL
  for dispatchable QUEUED Jobs whose Outbox rows were marked published but whose queue messages
  may have been lost during Redis wipe/restart, redispatching them with existing Job IDs.

- **Stale Job Reconciler**: A background process in `apps/worker` that detects orphaned RUNNING
  Attempts (`now > attempt.started_at + executionTimeout + recoveryGracePeriod`) and finalizes
  them as `WORKER_CRASHED` via `FinalizeAttemptUseCase`, moving the Job to `RETRY_SCHEDULED`.

- **Backtest Worker**: A consumer group member on `backtest.jobs.v1`. Deduplicates via
  `processed_message` plus authoritative Job/Attempt state, calls `StartNextAttemptUseCase`
  (F-005), loads frozen provenance, calls `RunBacktestUseCase` then
  `EvaluateBacktestUseCase` (F-006), finalizes the Attempt/Job through F-005, then emits the
  transient `candidate.evaluated.v1` fast-path notification. After required durable effects are
  complete it inserts the completed `processed_message` marker and acknowledges (`XACK`).
  Missing candidate-evaluated publication is repaired by the durable Leaderboard reconciler.

- **Ranking Handler**: A consumer of `candidate.evaluated.v1`. It does not treat a
  `SUCCEEDED` Backtest Job as a reason to skip; success is the expected prerequisite for
  ranking. It combines `processed_message` with F-006 idempotent/durable projection semantics,
  calls `ProjectLeaderboardUseCase`, records the completed marker, acknowledges, and emits a
  transient progress notification to the F-009 boundary. The Leaderboard reconciler repairs
  missed fast-path messages from durable Evaluation state.

- **Retry Orchestrator**: Integrated into the Worker runtime. Classifies runtime failures
  into the five canonical categories, calls `FinalizeAttemptUseCase.finalizeFailure(...)` to
  set `RETRY_SCHEDULED`, polls due retries (`next_retry_at <= now()`), and calls
  `RequeueRetryUseCase.requeueRetry(...)` to re-queue.

- **Versioned Message Contract**: Typed message classes in `modules/contracts` carrying a
  stable `messageId`, `messageVersion`, routing context IDs, and no embedded business
  payloads beyond references.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Publishing the same `messageId` twice to `backtest.jobs.v1` produces exactly
  one Backtest Result, one Evaluation Result, and at most one Leaderboard Revision in 100%
  of tests. Zero duplicate business outcomes.

- **SC-002**: After complete Redis wipe with Jobs in QUEUED, RUNNING, and RETRY_SCHEDULED
  states, 100% of eligible unfinished Jobs are recoverable from PostgreSQL durable state
  without creating new Job, Candidate, or Experiment IDs: unpublished Outbox rows are retried,
  already-published-but-lost QUEUED Jobs are redispatched by Queue Reconciler, stale RUNNING
  Attempts are recovered by Stale Job Reconciler, and due retries are requeued by Retry
  Orchestrator.

- **SC-003**: A transient failure triggers automatic retry within the configured retry
  budget. After budget exhaustion, the Job reaches durable `FAILED` with safe failure metadata.
  When Redis is available, a `jobs.dead-letter.v1` diagnostic notification is produced; loss
  of that transient notification does not change failure truth and it can be reconstructed
  from PostgreSQL. One failed Candidate does not stop other Candidates in the same Experiment.

- **SC-004**: Worker crash/redelivery produces zero duplicate durable business outcomes.
  If durable work had completed before the crash, `processed_message` or terminal durable
  Job/Result state causes the reclaimed delivery to skip execution and acknowledge. If the
  Worker died during a still-RUNNING Attempt, the message is not started concurrently; stale
  execution recovery finalizes the orphaned Attempt through F-005 before the normal retry path
  can create a later Attempt.

- **SC-005**: Stop Experiment + cancellation causes all QUEUED Jobs to be CANCELLED without
  business execution. RUNNING Jobs reach CANCELLED at the next safe checkpoint with no
  partial Backtest Result persisted. The Experiment reaches STOPPED when all Jobs are
  terminal.

- **SC-006**: Every Worker log entry for a given Job carries `correlationId`, `experimentId`,
  `jobId`, and `candidateId`. End-to-end trace is reconstructable from log entries alone.

- **SC-007**: Dead-letter messages contain no stack traces, credentials, internal class
  names, or raw SQL error text in 100% of dead-letter test cases.

- **SC-008**: Retry configuration (max retries, delay, backoff strategy, timeout) is fully
  environment-configurable. No retry parameters are hard-coded in business or worker logic.

- **SC-009**: Progress fields on the durable Job (PostgreSQL) reflect correct counts for
  completed, failed, and total Candidates at any point. These counts are readable from
  durable state even if WebSocket delivery was unavailable.

- **SC-010**: With at least two Worker instances in the same Consumer Group, distinct queued
  messages can be processed concurrently while duplicate/redelivered messages produce zero
  duplicate durable business effects. Scaling Worker instances requires no Strategy, Backtest,
  Evaluation, Leaderboard, or API contract change.

---

## Assumptions

- F-005's canonical lifecycle semantics and existing public use-case contracts
  (`StartNextAttemptUseCase`, `FinalizeAttemptUseCase`, `RequeueRetryUseCase`,
  `CancelJobUseCase`, `GetFrozenBacktestExecutionUseCase`, and cancel-poll semantics) are the
  behavior F-007 must reuse; F-007 MUST NOT duplicate their state machines.

- The current repository may not yet expose the dedicated trusted Worker/system facade chosen
  in Clarification 7. If absent, F-007 implementation planning MUST add/extend an F-005-owned
  public application boundary that resolves ownership internally from authoritative durable
  state (`Job → Experiment → owner_user_id`) and validates parent relationships
  (`Candidate → Experiment`, `Attempt → Job`). Existing user-facing owner-scoped APIs remain
  unchanged and `apps/worker` must not bypass them by accessing persistence internals.

- The current F-005 `OutboxStore` is write-side only. F-007 planning MUST introduce a separate
  publication-side read/claim/mark contract implemented by `modules/persistence` for
  `platform.outbox_event`, without changing F-005's six transactional trigger semantics or
  exposing JDBC/SQL to `apps/worker`.

- F-006 public use-case boundary (`RunBacktestUseCase`, `EvaluateBacktestUseCase`,
  `ProjectLeaderboardUseCase`) is available and stable before F-007 implementation begins.

- The existing `platform.outbox_event` schema (baseline migration `20260827000100`) is the
  only Outbox table. F-007 MUST NOT introduce a duplicate table.

- The existing `platform.processed_message` schema (baseline migration `20260827000100`)
  is the only deduplication table. F-007 MUST NOT introduce a parallel table unless a
  concrete schema incompatibility is identified during planning and documented as a forward
  migration.

- Redis Streams with Consumer Groups provide at-least-once delivery semantics. Exactly-once
  delivery is not guaranteed and is not assumed anywhere in F-007.

- The five canonical failure classifications (`TRANSIENT_NETWORK_ERROR`,
  `DATA_UNAVAILABLE_RETRY`, `WORKER_CRASHED`, `PERMANENT_LOGIC_ERROR`, `UNKNOWN_ERROR`)
  are stable contracts from F-005. F-007 maps runtime exceptions to these classifications but
  does not define new classification values.

- `apps/worker` is a Java 21 / Spring Boot 3 runtime sharing the Gradle multi-module build,
  running as a separate JVM process from `apps/api` but sharing capability modules.

- Maximum Attempts (including the initial try), retry delay/backoff parameters, execution
  timeout, Consumer Group names, stream names (with optional environment prefix), reconciliation
  grace periods, and Outbox scan interval are environment-configurable. No retry/scheduling
  values are hard-coded in business or orchestration logic.

- Search Coordinator execution and `RandomStrategyGenerator` are deferred to a subsequent
  dedicated Search feature per Clarification 1 (Option C). F-007 documents `search.requests.v1`
  as a reserved stream contract and focuses its implementation on the reliable Backtest Worker
  and Ranking Handler paths.

- The Outbox Publisher may be implemented as a polling loop within `apps/worker` (same JVM)
  or as a lightweight background thread. Implementation choice belongs to the planning phase.
  It is not a CDC platform or Kafka Connect equivalent, per ADR-0006.

- F-007 does NOT implement: public REST endpoints (F-009), public WebSocket server (F-009),
  browser subscription handling (F-009), frontend UI (F-010), new Backtest business rules
  (F-006), new Evaluation formulas (F-006), new Leaderboard scoring logic (F-006), Strategy
  implementations (F-004), Market Data ingestion (F-003), News/Sentiment analysis (F-008),
  real trading, wallet management, order execution, Kafka, Kubernetes, or exactly-once
  distributed processing.

- PostgreSQL-compatible storage is the source of truth for all durable business state.
  Redis is transient infrastructure. Redis loss must never result in loss of Experiment,
  Job, Candidate, Result, Outbox, or processed-message truth.

- All schema changes required by F-007 MUST be expressed as forward-only migration files
  in `supabase/migrations/`. Applied migrations MUST NOT be edited.

- Worker instances share a PostgreSQL connection pool. The pool size must be configured to
  stay within Supabase plan limits when running multiple Worker instances.

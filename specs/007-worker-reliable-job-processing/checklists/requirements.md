# Requirements & Design Quality Checklist: F-007 Worker and Reliable Job Processing

**Feature**: F-007 — Worker and Reliable Job Processing  
**Branch**: `feature/007-worker-reliable-job-processing`  
**Feature Directory**: `specs/007-worker-reliable-job-processing/`  
**Date**: 2026-09-01  
**Status**: Re-evaluated Against Revised Planning Artifacts  

---

## 1. Feature Scope & Ownership

- [x] **CHK001**: Is the functional scope of F-007 strictly limited to asynchronous orchestration and reliability without embedding business execution logic? [Completeness, Spec §Overview, Plan §1]
- [x] **CHK002**: Does the specification explicitly establish that F-005 owns the Job and Execution Attempt state machines, forbidding F-007 from reimplementing status transitions? [Consistency, Spec §Overview, Plan §Constitution Check]
- [x] **CHK003**: Are Strategy execution, Backtest simulation, Evaluation metric calculations, and Top-K Leaderboard ranking explicitly delegated to F-006 capability modules? [Consistency, Spec §Overview, Plan §Technical Context]
- [x] **CHK004**: Are public REST endpoints, WebSocket servers, and browser subscription protocols explicitly excluded from F-007 and preserved for F-009? [Scope Boundary, Spec §Overview, Plan §1]
- [x] **CHK005**: Is Search Coordinator runtime, `RandomStrategyGenerator`, Candidate generation, and search stop-condition execution deferred as reserved scope only without implementing a Search consumer group? [Scope Boundary, Spec §Clarifications Q2, Plan §Constitution Check, Contract `search-requests-reservation.md`]

---

## 2. PostgreSQL vs Redis Durability Model

- [x] **CHK006**: Is PostgreSQL unambiguously specified as the single authoritative source of truth for all durable business state (Jobs, Attempts, Results, Evaluations, Leaderboard)? [Durability, Spec §Clarifications Q5, Data Model §1]
- [x] **CHK007**: Are Redis Streams defined as transient at-least-once delivery infrastructure that can be completely wiped without destroying business truth? [Durability, Spec §Clarifications Q5, Research §5]
- [x] **CHK008**: Does the design avoid relying on Redis `SETNX` or distributed Redis locks as the authoritative domain concurrency guard? [Consistency, Research §3, Data Model §1]
- [x] **CHK009**: Is retry scheduling persisted durably in PostgreSQL (`experiment.job.next_retry_at`) rather than held only in Redis memory? [Durability, Spec §Clarifications Q3, Data Model §1, Contract `retry-recovery-contract.md`]
- [x] **CHK010**: Are progress counters (`completed_work`, `failed_work`, `best_score`) durably rooted in PostgreSQL so that progress truth is preserved across WebSocket drops? [Durability, Spec §Clarifications Q6, Data Model §1, Contract `progress-event-contract.md`]

---

## 3. Versioned Message Contracts

- [x] **CHK011**: Does every Redis Stream message conform to a standardized, versioned envelope containing `messageId`, `messageVersion`, `messageType`, `occurredAt`, `correlationId`, and `payload`? [Completeness, Spec §FR-002, Contract `redis-message-envelope.md`]
- [x] **CHK012**: Is the format of `messageId` constrained to a valid 26-character ULID pattern? [Clarity, Spec §FR-002, Contract `redis-message-envelope.md`]
- [x] **CHK013**: Does `BacktestJobPayload` carry minimal routing identifiers (`experimentId`, `jobId`, `candidateId`) rather than embedding Candle data, Strategy code, or heavy trade logs? [Clarity, Spec §FR-003, Contract `backtest-job-message.md`]
- [x] **CHK014**: Does the contract explicitly forbid treating caller-supplied `ownerUserId` in Redis messages as authorization authority? [Security, Spec §Clarifications Q7, Spec §FR-002, Contract `backtest-job-message.md`]
- [x] **CHK015**: Is dataset and strategy provenance loaded authoritatively from F-005 `GetFrozenBacktestExecutionUseCase` rather than duplicated in Redis messages? [Consistency, Spec §FR-002, Contract `backtest-job-message.md`]
- [x] **CHK016**: Is the rejection policy for unrecognized or unsupported `messageVersion` values explicitly defined for consumers? [Exception Flow, Spec §FR-004, Contract `redis-message-envelope.md`]
- [x] **CHK017**: Are JSON schema extensibility rules consistent between Jackson deserialization (`@JsonIgnoreProperties(ignoreUnknown = true)`) and envelope schemas? [Consistency, Contract `redis-message-envelope.md`]

---

## 4. Trusted F-005 Worker Boundary

- [x] **CHK018**: Does F-005 expose a dedicated `TrustedWorkerExperimentUseCase` interface that resolves `ownerUserId` internally from `Job -> Experiment -> owner_user_id`? [Security, Spec §Clarifications Q7, Spec §FR-029A, Contract `trusted-worker-boundary.md`]
- [x] **CHK019**: Does the trusted Worker boundary validate parent-child relationships (`Candidate -> Experiment`, `Attempt -> Job`) before executing state transitions? [Consistency, Spec §Clarifications Q7, Contract `trusted-worker-boundary.md`]
- [x] **CHK020**: Are existing user-facing owner-scoped use cases preserved without weakening authorization predicates? [Security, Spec §Clarifications Q7, Spec §FR-029A]
- [x] **CHK021**: Is access to `TrustedWorkerExperimentUseCase` restricted to trusted `apps/worker` composition, with public API/F-009 explicitly forbidden from invoking it? [Architecture, Spec §FR-029A, Plan §Verification Plan, Contract `trusted-worker-boundary.md`]
- [x] **CHK022**: Does the design include ArchUnit architecture tests to enforce that `apps/worker` does not access internal persistence packages or JDBC repositories? [Measurability, Plan §Verification Plan, Quickstart §3]

---

## 5. Persistence & Recovery Query Ports

- [x] **CHK023**: Does the architecture isolate all SQL/JDBC access inside `modules/persistence`, exposing only clean typed public ports to `apps/worker`? [Architecture, Plan §Project Structure, Contract `outbox-publication-contract.md`]
- [x] **CHK024**: Is there an explicit public publication-side port (`OutboxPublicationPort`) for listing unpublished rows and recording success, failure, and intentional suppression outcomes without exposing JDBC/SQL to `apps/worker`? [Completeness, Contract `outbox-publication-contract.md`]
- [x] **CHK025**: Is there an explicit public port (`ProcessedMessageStore`) for checking and recording processed message deduplication markers? [Completeness, Contract `processed-message-contract.md`]
- [x] **CHK026**: Are recovery queries for due retries, stale attempts, and orphaned queued jobs encapsulated within domain/persistence ports rather than constructed as raw SQL in `apps/worker`? [Architecture, Plan §Project Structure, Contract `retry-recovery-contract.md`]
- [x] **CHK027**: Are F-007 persistence capabilities exposed through public `modules/persistence` APIs while JDBC implementations remain internal, without requiring `apps/worker` to know implementation factories, repositories, or SQL details? [Architecture, Plan §Project Structure, Data Model §4, Contract `outbox-publication-contract.md`]

---

## 6. Outbox Publication Model

- [x] **CHK028**: Does the design strictly preserve F-005's transactional write-side event triggers while assigning F-007 ownership of discovery, dispatch, and marking only? [Consistency, Spec §Clarifications Q1, Spec §FR-005]
- [x] **CHK029**: Is `publish_attempts` incremented on every physical publication attempt? [Clarity, Spec §FR-006, Contract `outbox-publication-contract.md`]
- [x] **CHK030**: Is `published_at` set and `last_error` cleared immediately upon successful confirmation of Redis receipt? [Clarity, Spec §FR-006, Contract `outbox-publication-contract.md`]
- [x] **CHK031**: Does publication failure preserve `published_at IS NULL` and store a sanitized, safe diagnostic in `last_error` without leaking credentials or raw SQL? [Security, Spec §FR-007, Contract `outbox-publication-contract.md`]
- [x] **CHK032**: Is the system resilient to a publisher crash occurring after Redis accepts a message but before the database success mark is committed? [Resilience, Spec §FR-006, Plan §4, Quickstart §4]
- [x] **CHK033**: Is intentional suppression of stale Outbox events explicitly auditable via `last_error` rather than silently discarding rows? [Auditability, Spec §FR-008, Contract `outbox-publication-contract.md`]

---

## 7. Outbox Multi-Publisher Concurrency

- [x] **CHK034**: Does the Outbox Publisher use a duplicate-tolerant scan of `published_at IS NULL` rows rather than incorrectly treating a short `FOR UPDATE SKIP LOCKED` transaction as an exclusive claim across Redis network I/O? [Concurrency, Research §2, Plan §4, Contract `outbox-publication-contract.md`]
- [x] **CHK035**: Is multi-publisher behavior explicitly defined so concurrent publishers may physically publish the same Outbox event, while conditional publication-outcome updates and downstream idempotency prevent lost durable events and duplicate business effects? [Clarity, Research §2, Plan §4, Contract `outbox-publication-contract.md`]
- [x] **CHK036**: Does the design guarantee that no unpublished Outbox event can be permanently lost if a publisher crashes mid-batch? [Durability, Research §2, Plan §4]
- [x] **CHK037**: Is multi-publisher Outbox behavior verifiable via concurrent PostgreSQL + Redis integration tests covering duplicate physical publication, conditional success/failure recording, and no lost durable event? [Testability, Plan §Verification Plan, Quickstart §Publisher Crash / Multi-Publisher Scenarios]

---

## 8. Outbox Event Routing Completeness

- [x] **CHK038**: Does `JobQueued` publication explicitly verify that the Job remains in `QUEUED` status, suppressing stale dispatch if cancelled? [Event Validation, Spec §FR-008, Contract `outbox-publication-contract.md`]
- [x] **CHK039**: Does `JobCancelRequested` publication allow suppression only if the Job has already durably reached `CANCELLED`? [Event Validation, Spec §FR-008, Contract `outbox-publication-contract.md`]
- [x] **CHK040**: Does the design forbid suppressing `JobCancelled` merely because the target Job is already `CANCELLED` (since `CANCELLED` is the state the event announces)? [Consistency, Spec §Clarifications Q1, Spec §FR-008, Contract `outbox-publication-contract.md`]
- [x] **CHK041**: Are `ExperimentQueued` and `ExperimentStopRequested` validated according to their specific aggregate lifecycle meanings? [Event Validation, Spec §FR-008, Contract `outbox-publication-contract.md`]
- [x] **CHK042**: Does every non-suppressed Outbox event type map to an explicit destination stream or notification channel so no event is stranded? [Completeness, Spec §FR-001, Spec §FR-008]

---

## 9. Backtest Worker Idempotency & Concurrency

- [x] **CHK043**: Does the Backtest Worker flow perform a fast infrastructure check on `platform.processed_message` and immediately call `XACK` if already processed? [Idempotency, Spec §FR-012, Plan §2]
- [x] **CHK044**: Does the Worker inspect authoritative PostgreSQL Job status before executing, skipping and acknowledging if the Job is already in a terminal state (`SUCCEEDED`, `FAILED`, `CANCELLED`)? [Domain Guard, Spec §FR-012, Plan §2]
- [x] **CHK045**: Does the atomic `QUEUED -> RUNNING` transition in `StartNextAttemptUseCase` plus database unique constraint on `(job_id, attempt_number)` prevent concurrent duplicate executions? [Concurrency, Spec §FR-012, Plan §2]
- [x] **CHK046**: Does the Worker refrain from starting a new Attempt if the Job is currently `RUNNING`, leaving stranded executions to the Stale Job Reconciler? [Resilience, Spec §FR-012, Plan §2]
- [x] **CHK047**: Is `platform.processed_message` inserted only after all durable F-005 and F-006 effects have been committed? [Ordering, Spec §FR-013, Plan §2]
- [x] **CHK048**: Is Redis acknowledgement (`XACK`) issued only after the `processed_message` marker is successfully recorded? [Ordering, Spec §FR-013, Plan §2]

---

## 10. Processed_Message Semantics & TTL

- [x] **CHK049**: Is `platform.processed_message` maintained strictly as a completed marker rather than an in-progress distributed lock? [Clarity, Research §3, Data Model §4.1]
- [x] **CHK050**: Is the primary key of `platform.processed_message` defined as `(consumer_name, message_id)` to isolate consumer group deduplication? [Completeness, Data Model §4.1, Contract `processed-message-contract.md`]
- [x] **CHK051**: Is the processed-message TTL environment-configurable and required to exceed the configured Redis pending/reclaim/redelivery and operational recovery horizon, without hard-coding a production default in the planning artifacts? [Configurability, Spec §FR-014, Contract `processed-message-contract.md`, Quickstart §Environment Configuration]
- [x] **CHK052**: Does the design ensure that expiration of the infrastructure TTL does not make an already-completed terminal Job unsafe upon redelivery? [Durability, Spec §FR-014, Plan §Summary]
- [x] **CHK053**: Are concurrent completed-marker insert races handled safely by the existing `(consumer_name, message_id)` primary key and `INSERT ... ON CONFLICT DO NOTHING`, while durable domain state remains the true business concurrency guard? [Concurrency, Contract `processed-message-contract.md`]
- [x] **CHK054**: Are processed message checks and insertions supported by the existing baseline schema without requiring new tables? [Database, Spec §Assumptions, Plan §Database Changes]

---

## 11. F-006 Commit -> F-005 Finalization Crash Window

- [x] **CHK055**: Does the design define explicit recovery behavior when F-006 Backtest/Evaluation outputs are committed in PostgreSQL, but the Worker crashes before invoking F-005 `finalizeSuccess`? [Exception Flow, Spec §SC-004, Plan §2]
- [x] **CHK056**: Does the Stale Job Reconciler check for existing durable `EvaluationResult` / `BacktestResult` before automatically classifying an orphaned RUNNING attempt as `WORKER_CRASHED`? [Resilience, Plan §5, Data Model §3]
- [x] **CHK057**: If durable execution evidence already exists, does the reconciler repair/finalize success on the Job without creating a duplicate Backtest run? [Idempotency, Spec §SC-004, Plan §5]
- [x] **CHK058**: Are races between a late-completing original Worker and the Stale Job Reconciler safely arbitrated by F-005's optimistic locking and state machine transition guards? [Concurrency, Plan §5, Contract `trusted-worker-boundary.md`]

---

## 12. Retry Orchestration & Classification

- [x] **CHK059**: Does the Backtest Worker classify runtime exceptions into the 5 canonical F-005 `FailureClassification` categories (`TRANSIENT_NETWORK_ERROR`, `DATA_UNAVAILABLE_RETRY`, `WORKER_CRASHED`, `PERMANENT_LOGIC_ERROR`, `UNKNOWN_ERROR`)? [Completeness, Spec §FR-018, Contract `retry-recovery-contract.md`]
- [x] **CHK060**: Does a retryable failure calculate `nextRetryAt` with exponential backoff and jitter, finalize the Attempt as `FAILED`, and transition the Job to `RETRY_SCHEDULED`? [Flow, Spec §Clarifications Q3, Contract `retry-recovery-contract.md`]
- [x] **CHK061**: Does the design ensure `RequeueRetryUseCase` is NOT called immediately at failure time, allowing the configured backoff delay to elapse? [Timing, Spec §Clarifications Q3, Plan §6]
- [x] **CHK062**: Does the Retry Orchestrator discover due retries via PostgreSQL query (`status = 'RETRY_SCHEDULED' AND next_retry_at <= now()`) rather than in-memory sleep timers? [Durability, Spec §Clarifications Q3, Plan §6]
- [x] **CHK063**: Does `RequeueRetryUseCase` transition `RETRY_SCHEDULED -> QUEUED` and atomically generate a `JobQueued` Outbox event for dispatch by Outbox Publisher? [Consistency, Spec §Clarifications Q3, Plan §6]
- [x] **CHK064**: Is `maxAttempts` environment-configurable and explicitly defined to include the initial Attempt, without imposing an ungrounded production default in the planning artifacts? [Clarity, Spec §FR-017, Contract `retry-recovery-contract.md`, Quickstart §Environment Configuration]
- [x] **CHK065**: Are `PERMANENT_LOGIC_ERROR` and `UNKNOWN_ERROR` treated as non-retryable terminal failures that transition the Job directly to `FAILED` and emit to dead-letter? [Exception Flow, Spec §FR-018, Contract `retry-recovery-contract.md`]

---

## 13. Queue Reconciliation After Total Redis Loss

- [x] **CHK066**: Does the Queue Reconciler periodically query PostgreSQL for `experiment.job` records where `status = 'QUEUED'` and `created_at < now() - :reconciliationGracePeriod`? [Completeness, Spec §Clarifications Q4, Plan §5]
- [x] **CHK067**: Does queue reconciliation operate without requiring proof that the Job is absent from Redis Streams? [Resilience, Spec §Clarifications Q4, Research §5]
- [x] **CHK068**: Does the reconciler redispatch the exact same `JobId`, `ExperimentId`, and `CandidateId` rather than generating replacement Jobs? [Integrity, Spec §Clarifications Q4, Plan §5]
- [x] **CHK069**: Does the design re-verify current durable state immediately before redispatching to ensure the Job has not since transitioned to `RUNNING` or `CANCELLED`? [Concurrency, Spec §Clarifications Q4, Plan §5]
- [x] **CHK070**: Is duplicate redispatch guaranteed to produce zero duplicate business effects due to dual-layer idempotency? [Safety, Spec §SC-001, Plan §5]

---

## 14. Stale RUNNING Attempt Recovery

- [x] **CHK071**: Is stale attempt detection determined by the objective condition: `now() > attempt.started_at + executionTimeout + recoveryGracePeriod`? [Measurability, Spec §Clarifications Q4, Contract `retry-recovery-contract.md`]
- [x] **CHK072**: Does the design avoid introducing new Worker heartbeat columns or worker-lease tables in the MVP schema? [Simplicity, Spec §Clarifications Q4, Plan §Database Changes]
- [x] **CHK073**: Does the Stale Job Reconciler invoke `finalizeFailure(...)` with `WORKER_CRASHED` through the trusted F-005 boundary rather than mutating database tables directly? [Architecture, Spec §Clarifications Q4, Contract `trusted-worker-boundary.md`]
- [x] **CHK074**: Does finalizing a stale attempt transition the parent Job to `RETRY_SCHEDULED` (or terminal `FAILED` if max attempts are exhausted)? [State Machine, Spec §Clarifications Q4, Contract `retry-recovery-contract.md`]
- [x] **CHK075**: Are timeout and grace period parameters environment-configurable to support varied strategy computational profiles? [Configurability, Spec §FR-028, Plan §Technical Context]

---

## 15. Redis Pending Message Reclaim

- [x] **CHK076**: Is Redis pending message reclaim (`XAUTOCLAIM` / `XCLAIM`) explicitly separated from stale execution recovery? [Clarity, Spec §FR-011, Plan §1]
- [x] **CHK077**: Does reclaiming a message require evaluating `processed_message` and durable Job status before authorizing an Attempt to start? [Safety, Spec §FR-011, User Story 1 Scenario 5]
- [x] **CHK078**: If a reclaimed message corresponds to a Job that is currently `RUNNING`, does the Worker skip starting a duplicate Attempt and defer to the Stale Job Reconciler? [Concurrency, Spec §FR-012, Plan §2]
- [x] **CHK079**: Are reclaim idle timeout and batch sizes configurable via environment variables? [Configurability, Spec §FR-011, Plan §Configuration]

---

## 16. Ranking Handler Idempotency

- [x] **CHK080**: Does the Ranking Handler consume `candidate.evaluated.v1` without skipping execution merely because the parent Backtest Job is `SUCCEEDED` (since `SUCCEEDED` is expected)? [Logic, Spec §FR-012, Spec §Key Entities, Plan §3]
- [x] **CHK081**: Does the Ranking Handler combine `platform.processed_message` with F-006's deterministic, idempotent `ProjectLeaderboardUseCase`? [Idempotency, Spec §FR-012, Contract `candidate-evaluated-message.md`]
- [x] **CHK082**: Does the Ranking Handler record a `platform.processed_message` completed marker and call `XACK` upon successful Leaderboard projection? [Ordering, Spec §FR-013, Plan §3]
- [x] **CHK083**: Does the Ranking Handler treat `overallScore` in `candidate.evaluated.v1` as a notification hint while resolving authoritative score data from PostgreSQL `experiment.evaluation_result`? [Integrity, Data Model §2.3, Contract `candidate-evaluated-message.md`]
- [x] **CHK084**: Do duplicate `candidate.evaluated.v1` messages produce zero duplicate logical Leaderboard revisions? [Idempotency, Spec §FR-012, Plan §3]

---

## 17. Leaderboard Reconciliation & Cursor

- [x] **CHK085**: Does the Leaderboard Reconciler use a bounded F-006-owned durable reconciliation scan that recomputes deterministic Top-K from eligible Evaluation state, rather than selecting Evaluations merely because they lack a Leaderboard entry? [Recovery, Spec §Clarifications Q5, Spec §FR-030, Plan §5, Contract `leaderboard-reconciliation-contract.md`]
- [x] **CHK086**: Does the Leaderboard Reconciler design avoid treating "not in Top-K" as "never projected", preventing endless re-projection of legitimate low-scoring evaluations? [Logic, Spec §FR-030, Plan §5]
- [x] **CHK087**: Are Ranking Handler vs Leaderboard Reconciler races resolved through F-006-owned deterministic projection/fingerprint idempotency and its durable concurrency guard, with database uniqueness serving as an additional collision guard rather than the sole correctness mechanism? [Concurrency, Plan §5, Contract `leaderboard-reconciliation-contract.md`]
- [x] **CHK088**: Is the Leaderboard Reconciler scan interval and batch size environment-configurable? [Configurability, Spec §FR-030, Plan §Configuration]
- [x] **CHK089**: Does the Leaderboard Reconciler guarantee eventual consistency of the Top-K Leaderboard even if all Redis fast-path notifications are dropped? [Durability, Spec §SC-004, Research §6]

---

## 18. Progress Ownership & Double-Count Prevention

- [x] **CHK090**: Does the Backtest completion path exclusively own terminal candidate-work completion through the idempotent F-005 `recordTerminalProgress(...)` boundary, so successful work resolves exactly once? [Single Ownership, Contract `trusted-worker-boundary.md`, Plan §Backtest Worker / Progress Semantics]
- [x] **CHK091**: Does the Ranking Handler refrain from incrementing `completed_work` a second time when projecting Leaderboard scores? [Double-Count Prevention, Contract `candidate-evaluated-message.md`, Plan §3]
- [x] **CHK092**: Is `failed_work` incremented only when a Candidate transitions to terminal `FAILED`? [Clarity, Contract `trusted-worker-boundary.md`, Plan §2]
- [x] **CHK093**: Does terminal progress use idempotent SET semantics (`SUCCEEDED -> completed_work=1, failed_work=0`; terminal `FAILED -> completed_work=0, failed_work=1`) rather than blind increments, preventing retry/redelivery from double-counting while preserving safe best-score handling? [Concurrency, Data Model §Progress Invariant, Contract `trusted-worker-boundary.md`]
- [x] **CHK094**: Can durable progress counts be read reliably from PostgreSQL at any time even if WebSocket/transient notifications are lost? [Durability, Spec §SC-009, Spec §FR-032]

---

## 19. Cross-JVM Progress & Lifecycle Event Boundary

- [x] **CHK095**: Does the design recognize that `apps/worker` and `apps/api` run in separate JVM processes, requiring a cross-process transport mechanism for progress notifications? [Architecture, Spec §FR-033, Plan §Summary]
- [x] **CHK096**: Are progress notifications (`EXPERIMENT_PROGRESS_UPDATED`, `BACKTEST_COMPLETED`, `LEADERBOARD_UPDATED`) defined with versioned payloads in `modules/contracts`? [Contracts, Contract `progress-event-contract.md`]
- [x] **CHK097**: Does the design strictly forbid F-007 from implementing public WebSocket endpoints, client sessions, or HTTP subscription routes (preserving F-009 ownership)? [Scope Boundary, Spec §Overview, Spec §FR-033]
- [x] **CHK098**: If progress notifications are dropped in transit, can F-009 / API clients reconstruct full progress truth directly from PostgreSQL `experiment.job` counters? [Resilience, Spec §FR-032, Contract `progress-event-contract.md`]

---

## 20. Dead-Letter Diagnostic Model

- [x] **CHK099**: Is PostgreSQL `experiment.job.status = 'FAILED'` plus durable `failure_code` and `failure_message` established as the authoritative failure truth? [Durability, Spec §Clarifications Q5, Spec §FR-020]
- [x] **CHK100**: Is `jobs.dead-letter.v1` treated as a best-effort diagnostic notification stream rather than a required transactional dependency for Job failure? [Resilience, Spec §FR-020, Contract `dead-letter-message.md`]
- [x] **CHK101**: Does the dead-letter contract enforce sanitization and redaction of credentials, database URLs, raw SQL errors, stack traces, and internal class names? [Security, Spec §FR-021, Contract `dead-letter-message.md`]
- [x] **CHK102**: Does the failure of one Candidate to execute or dead-letter prevent other Candidates in the same Experiment from completing? [Isolation, Spec §SC-003, Acceptance Scenario 7.2]

---

## 21. Cancellation Orchestration

- [x] **CHK103**: Does the Backtest Worker re-read durable Job state immediately before execution to detect pre-execution cancellation? [Pre-Execution Guard, Spec §FR-023, Plan §2]
- [x] **CHK104**: Does the Worker poll cancellation state only at safe, non-corrupting checkpoints during execution via `TrustedWorkerExperimentUseCase.isCancelRequested(...)`? [Safe Checkpoints, Spec §FR-023, Contract `trusted-worker-boundary.md`]
- [x] **CHK105**: Upon detecting `CANCEL_REQUESTED`, does the Worker discard in-memory partial results and refrain from committing partial Backtest, Evaluation, or Leaderboard data? [Data Integrity, Spec §FR-024, Acceptance Scenario 6.2]
- [x] **CHK106**: Does cancellation of a `RETRY_SCHEDULED` Job prevent future requeue by the Retry Orchestrator? [State Machine, Spec §FR-025, Plan §6]
- [x] **CHK107**: Does the design handle Experiment stop-completion reconciliation without depending on the deferred Search Coordinator? [Scope, Spec §FR-026, Plan §1]

---

## 22. Module & Package Ownership

- [x] **CHK108**: Are all message DTOs and envelopes located exclusively in `modules/contracts/src/main/java/com/cryptostrategy/platform/contracts/api/`? [Modularity, Plan §Project Structure]
- [x] **CHK109**: Is `TrustedWorkerExperimentUseCase` declared in `modules/experiment/src/main/java/com/cryptostrategy/platform/experiment/api/port/in/` and implemented in `modules/experiment/internal/`? [Modularity, Plan §Project Structure, Contract `trusted-worker-boundary.md`]
- [x] **CHK110**: Are `OutboxPublicationPort` and `ProcessedMessageStore` exposed from public `modules/persistence` API packages while their JDBC implementations remain internal, with no requirement for `apps/worker` to depend on a specific persistence factory implementation? [Modularity, Plan §Project Structure, Data Model §Persistence Models, Contract `outbox-publication-contract.md`]
- [x] **CHK111**: Does `apps/worker` contain only orchestration, configuration, stream listeners, and scheduled tasks without implementing business calculation logic? [Purity, Spec §Overview, Plan §Project Structure]
- [x] **CHK112**: Do all cross-module interactions depend strictly on public API packages (`*.api..`), forbidding imports of any `*.internal..` package? [Modular Monolith, Constitution Principle II, Plan §Constitution Check]

---

## 23. Database Change Decision & Schema Validation

- [x] **CHK113**: Is the decision **NO DATABASE CHANGE REQUIRED** objectively justified against all functional requirements? [Database, Plan §Database Changes]
- [x] **CHK114**: Does `platform.outbox_event` already contain all necessary columns (`outbox_event_id`, `message_id`, `aggregate_type`, `aggregate_id`, `event_type`, `event_version`, `payload`, `headers`, `occurred_at`, `published_at`, `publish_attempts`, `last_error`)? [Schema Validation, Migration `20260827000100`, Plan §Database Changes]
- [x] **CHK115**: Does `platform.processed_message` already contain `(consumer_name, message_id, processed_at, expires_at)` with primary key and expiry index? [Schema Validation, Migration `20260827000100`, Plan §Database Changes]
- [x] **CHK116**: Are all required indexes for outbox scanning (`outbox_unpublished_idx`) and job status discovery already present in the applied baseline migrations? [Index Validation, Migration `20260827000100`, Plan §Database Changes]

---

## 24. Concurrency Safety & Race Conditions

- [x] **CHK117**: Does the design explicitly handle two Worker instances receiving duplicate Backtest messages concurrently via `StartNextAttemptUseCase` atomic transitions and unique database constraints? [Concurrency, Plan §2, Quickstart §4]
- [x] **CHK118**: Does the design safely tolerate two Outbox Publisher instances selecting and physically publishing the same unpublished event, using conditional publication-state updates plus downstream idempotency rather than relying on a JVM-local lock or a short-lived PostgreSQL row lock? [Concurrency, Research §2, Plan §4, Contract `outbox-publication-contract.md`]
- [x] **CHK119**: Does the design handle races between the Stale Job Reconciler and a late-finishing Worker via F-005 state transition serialization? [Concurrency, Plan §5, Contract `trusted-worker-boundary.md`]
- [x] **CHK120**: Does the design safely handle concurrent Ranking Handler and Leaderboard Reconciler projections through the F-006 deterministic fingerprint/idempotency and durable concurrency mechanism, with unique constraints as secondary collision protection? [Concurrency, Plan §5, Contract `leaderboard-reconciliation-contract.md`]
- [x] **CHK121**: Does the design ensure that no system correctness guarantee depends solely on JVM-local locks? [Distributed Safety, Plan §Constitution Check]

---

## 25. Observability & MDC Context Propagation

- [x] **CHK122**: Does every consumer extract `correlationId`, `experimentId`, `jobId`, and `candidateId` and bind them to SLF4J MDC upon message receipt? [Observability, Spec §FR-035, Plan §Observability]
- [x] **CHK123**: Is `MDC.clear()` guaranteed to execute in a `finally` block on every worker thread return to prevent context leakage across pooled threads? [Observability, Spec §FR-035, Plan §Observability]
- [x] **CHK124**: Are Micrometer metrics specified for active Backtest jobs, retry counts, dead-letter counts, unpublished Outbox lag, and reconciler recovery counts? [Metrics, Spec §FR-036, Plan §Observability]
- [x] **CHK125**: Are observability requirements limited to grounded guarantees—structured correlation context, sanitized diagnostics, required metrics, and MDC cleanup—without inventing a mandatory INFO/WARN/ERROR mapping that is not specified by F-007? [Logging, Spec §FR-034–FR-036, Plan §Observability]
- [x] **CHK126**: Are logs and metrics verified to be free of sensitive credentials, API keys, or raw query dumps? [Security, Spec §FR-021, Contract `dead-letter-message.md`]

---

## 26. Configuration & Runtime Environment

- [ ] **CHK127**: Does the runtime configuration explicitly document secure Redis connection properties, including credentials and SSL/TLS when required, in addition to host/port, without hard-coded secrets? [Configurability, Plan §Technical Context, Quickstart §2]
  - Gap: The current Quickstart explicitly lists Redis host and port, but does not explicitly document credential/password or SSL/TLS properties.
  - Required correction: Add optional secure Redis connection properties using the project's Spring Boot configuration convention before implementation configuration is finalized.
- [x] **CHK128**: Are stream names, consumer group names, consumer worker names, and optional environment prefixes fully configurable? [Configurability, Spec §FR-001, Spec §FR-009]
- [x] **CHK129**: Are concurrency limits (max in-flight backtests, batch sizes, prefetch bounds) configurable per Worker instance? [Back-Pressure, Spec §FR-027, Quickstart §2]
- [x] **CHK130**: Are retry parameters (`maxAttempts`, `baseDelayMs`, `multiplier`, `maxDelayMs`, `jitter`) and timeout thresholds configurable without code changes? [Configurability, Spec §FR-017, Contract `retry-recovery-contract.md`]

---

## 27. Testability & Quickstart Coverage

- [x] **CHK131**: Does `quickstart.md` provide clear, runnable commands for executing unit, contract, architecture, and reliability test suites? [Completeness, Quickstart §1–§5]
- [x] **CHK132**: Are specific test scenarios documented for happy-path backtest & ranking, worker crash idempotency, and Redis total loss recovery? [Coverage, Quickstart §4]
- [x] **CHK133**: Are integration tests specified using real local PostgreSQL and Redis instances rather than mocking critical transaction/stream boundaries? [Test Integrity, Quickstart §1, Plan §Verification Plan]
- [x] **CHK134**: Are ArchUnit architecture tests defined to verify package boundaries and forbid unauthorized access to internal packages? [Architecture Tests, Quickstart §3, Plan §Verification Plan]
- [x] **CHK135**: Are test scenarios included for malformed messages, unsupported versions, and poison pill handling? [Resilience, Contract `redis-message-envelope.md`, Plan §Verification Plan]
- [x] **CHK136**: Does the test strategy avoid making live network calls to external crypto exchanges (Binance, etc.)? [Determinism, Constitution Principle III, Plan §Technical Context]

---

## 28. Success Criteria Quality & Objectivity

- [x] **CHK137**: Is SC-001 (Zero duplicate durable business outcomes under redelivery) objectively verifiable through automated integration tests? [Measurability, Spec §SC-001, Quickstart §4]
- [x] **CHK138**: Is SC-002 (Complete Redis-loss recovery from PostgreSQL durable truth) objectively testable via queue flush and reconciliation verification? [Measurability, Spec §SC-002, Quickstart §4]
- [x] **CHK139**: Is SC-003 (Retry budget exhaustion and durable FAILED state) objectively verifiable via fault-injection tests? [Measurability, Spec §SC-003]
- [x] **CHK140**: Does SC-010 objectively verify multi-worker concurrent processing without requiring arbitrary or invented throughput numbers? [Objective, Spec §SC-010, Plan §Technical Context]
- [x] **CHK141**: Are all acceptance scenarios expressed in Given/When/Then format with clear observable post-conditions? [Clarity, Spec §User Scenarios & Testing]

---

## 29. Constitution & ADR Compliance

- [x] **CHK142**: Does the design strictly comply with Constitution Principle I (Purity & Domain-Driven Design) and Principle II (Modular Monolith Boundaries)? [Constitution, Plan §Constitution Check]
- [x] **CHK143**: Does the design enforce Constitution Principle IV (Security & Ownership Integrity) by resolving ownership internally through `TrustedWorkerExperimentUseCase`? [Constitution, Plan §Constitution Check]
- [x] **CHK144**: Does the design uphold Constitution Principle V (Engineering Rigor & Durability) by guaranteeing PostgreSQL as the durable source of truth? [Constitution, Plan §Constitution Check]
- [x] **CHK145**: Is the architecture consistent with all accepted ADRs (ADR-0001 Modular Monolith, ADR-0006 Event-Driven Architecture with Transactional Outbox, ADR-0007 Redis Streams, ADR-0010 Deduplication)? [ADR Alignment, Plan §Constitution Check, Research §1–§7]

---

## Checklist Summary

- **Total checks**: 145
- **Passed**: 144
- **Open**: 1

### Critical Open Items
*None. No CRITICAL or HIGH checklist item remains open. CHK127 is a non-blocking configuration-documentation gap for optional secure Redis credentials/TLS settings.*

### Non-Critical Open Items

- **CHK127** — Secure Redis credential/SSL/TLS properties are not explicitly documented in the current Quickstart. This is a configuration-documentation gap, not a durability, ownership, or concurrency blocker.

### Database Decision
**CONSISTENT — NO DATABASE CHANGE REQUIRED**  
*The current schema supports duplicate-tolerant Outbox publication without a lease/claim column, completed-marker deduplication through the existing `platform.processed_message` primary key, durable Job retry/recovery discovery, Backtest/Evaluation completion evidence, deterministic Leaderboard reconciliation from existing F-006 state, and idempotent terminal Job progress. Existing repository migrations also provide the relevant Outbox, processed-message, Job recovery, Attempt recovery, Evaluation-ranking, and Leaderboard indexes. No new F-007 schema object is required by the corrected design; any implementation-time incompatibility must be handled by a new forward-only migration.*

### Scope Status

- **F-005 boundary**: **PASS** (`TrustedWorkerExperimentUseCase` owned by F-005; resolves owner internally; user-facing APIs preserved).
- **F-006 boundary**: **PASS** (`RunBacktestUseCase`, `EvaluateBacktestUseCase`, and `ProjectLeaderboardUseCase` reused cleanly).
- **F-009 boundary**: **PASS** (No public REST or WebSocket endpoints in F-007; transient progress events emitted).
- **Search deferral**: **PASS** (`search.requests.v1` schema reserved only; no Search coordinator or consumer implemented).
- **Redis/PostgreSQL durability**: **PASS** (PostgreSQL is authoritative truth; Redis is transient infrastructure; dual reconciliation handles total Redis loss).

---

## Readiness

No CRITICAL/HIGH durability, ownership, concurrency, recovery, or cross-module design gap remains open. CHK127 can be carried into implementation/configuration tasks.

**READY FOR `/speckit-tasks`**

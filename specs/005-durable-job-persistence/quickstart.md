# Quickstart & Verification Guide: F-005 Experiment Persistence and Ownership

**Feature**: F-005 — Experiment Persistence and Ownership  
**Branch**: `feature/005-experiment-persistence`  
**Directory**: `specs/005-durable-job-persistence`  
**Status**: Draft  
**Date**: 2026-08-30  

This guide provides runnable instructions to execute unit, architecture, and integration tests for F-005.

---

## 1. Prerequisites

- **Java 21** installed and configured (`JAVA_HOME` pointing to JDK 21).
- **Gradle 8.x** (wrapper provided via `./gradlew`).
- **Docker / Supabase CLI** (optional for local database integration tests).

---

## 2. Test Execution Commands

### 2.1. Fast Unit & Domain State Machine Tests
Runs pure Java domain tests verifying Experiment, Manifest freeze, Job state transitions, Execution Attempt monotonic numbering, and active idempotency conflict gate:

```bash
# Run unit tests for experiment module
./gradlew :modules:experiment:test

# Run unit tests for persistence module
./gradlew :modules:persistence:test
```

**Expected Outcome**: All domain state machines, validation rules, and JSON mapping unit tests pass in < 5 seconds with zero external dependencies.

---

### 2.2. Architecture & Dependency Boundary Tests
Verifies ArchUnit rules ensuring strict dependency direction (`persistence` depends on `experiment`, `experiment` depends only on `domain` and published contracts):

```bash
# Run ArchUnit architecture verification
./gradlew :architecture-tests:test
```

**Expected Outcome**: Zero module boundary violations, zero package cycle violations, and zero direct imports of internal persistence packages across modules.

---

### 2.3. Database Schema Contract Tests (SQL)
Runs pgTAP / SQL assertion tests against local Supabase database instance to verify database-level invariants (status constraints, foreign keys, unique indices, and forward migrations):

```bash
# Execute database baseline & user strategy/jobs test
supabase test db
```

**Expected Outcome**: Existing database assertions remain green and new F-005 SQL assertions verify:
- `execution_attempt.status` no longer accepts `RETRY_SCHEDULED`;
- `reproduces_experiment_id` lineage is valid;
- CREATED Manifest may have `fingerprint = NULL`, while the freeze transaction requires a non-empty fingerprint;
- idempotency supports durable `IN_PROGRESS` → `COMPLETED` claim state;
- legacy Attempt→Job backfill succeeds only for deterministic mappings and leaves zero orphan Attempts.

---

### 2.4. Persistence Integration Tests
Runs Spring JDBC persistence tests against isolated PostgreSQL/Supabase database:

```bash
# Requires local test database environment variables:
# DATABASE_URL, DATABASE_USERNAME, DATABASE_PASSWORD

./gradlew :modules:persistence:experimentIntegrationTest
```

**Expected Outcome**:
1. **Ownership Isolation**: User A creates Experiment/Job; User B querying by User A's ID receives empty/inaccessible result.
2. **Transaction Atomicity**: Rollback injection verifies coupled business state + Outbox writes and coupled Job + Attempt updates never half-commit.
3. **Monotonic Attempt Numbering**: Concurrent starters serialize on the parent Job row and receive sequential `attempt_no` values with zero collisions.
4. **Idempotency Atomic Claim**: exactly one concurrent first request receives `ACQUIRED`; same key + same hash replays; same key + different hash conflicts.
5. **Freeze Invariant**: CREATED Manifest may have no fingerprint; `CREATED → QUEUED` atomically stores fingerprint and freezes the Manifest.
6. **Cancellation Race**: `RETRY_SCHEDULED → CANCELLED` persists without Outbox; if requeue wins first, `QUEUED → CANCELLED` writes `JobCancelled`.
7. **Legacy Backfill**: deterministic Attempt→Job fixture migrates; ambiguous fixture aborts; zero orphans remain afterward.
8. **Reproduction Lineage**: `reproduces_experiment_id` is persisted and distinct from `derived_from_experiment_id`.

---

## 3. Reference Artifacts

- [Specification](spec.md)
- [Research & Decisions](research.md)
- [Data Model](data-model.md)
- [Job & Execution Attempt Contract](contracts/job-execution-attempt-contract.md)
- [Experiment & Manifest Contract](contracts/experiment-manifest-contract.md)
- [Persistence Ports Contract](contracts/persistence-ports-contract.md)
- [Idempotency & Outbox Contract](contracts/idempotency-outbox-contract.md)

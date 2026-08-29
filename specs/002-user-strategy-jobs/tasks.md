# Tasks: User-owned Strategies and Durable Jobs

**Input**: Design documents from `specs/002-user-strategy-jobs/`

**Tests**: Transactional SQL verification is required because this feature changes ownership, foreign keys and migration history.

## Phase 1: Setup

- [X] T001 Confirm branch, clean baseline migration, and current schema file inventory using `specs/002-user-strategy-jobs/quickstart.md`
- [X] T002 [P] Record the long-lived ownership and Job decision in `docs/adr/0012-user-strategy-job-ownership.md`
- [X] T003 [P] Add ADR-0012 to `docs/adr/README.md`

---

## Phase 2: Foundational Verification Design

- [X] T004 Define transactional assertions for the new tables, constraints, ownership paths and privileges in `supabase/tests/database/002_user_strategy_jobs_test.sql`

**Checkpoint**: The expected schema behavior is executable before the migration is finalized.

---

## Phase 3: User Story 1 — Manage private user Strategies (Priority: P1)

**Goal**: Add private, versioned Strategy definitions without changing the shared plugin catalog.

**Independent Test**: Two Auth users can use the same Strategy name, one user cannot create duplicate active names, invalid kind-specific versions fail, and ownership paths remain distinct.

- [X] T005 [US1] Create `strategy.user_strategy`, `strategy.user_strategy_version`, and `strategy.user_strategy_component` with constraints and indexes in `supabase/migrations/20260828000100_add_user_strategies_and_jobs.sql`
- [X] T006 [US1] Add optional User Strategy provenance to Experiment Manifest and preserve snapshot fields in `supabase/migrations/20260828000100_add_user_strategies_and_jobs.sql`
- [X] T007 [US1] Add new-table privilege revocations and table comments in `supabase/migrations/20260828000100_add_user_strategies_and_jobs.sql`

**Checkpoint**: User Strategy storage is independently reviewable and testable without Job support.

---

## Phase 4: User Story 2 — Track Jobs and execution attempts (Priority: P2)

**Goal**: Add one durable Job identity above retry Attempts and make legacy Attempt references valid.

**Independent Test**: Search and Backtest Job shape is enforced, wrong Experiment/Candidate combinations fail, retry Attempts share one Job, and orphan/mismatched Attempts fail.

- [X] T008 [US2] Create `experiment.job` with lifecycle, progress, ownership-path and recovery constraints in `supabase/migrations/20260828000100_add_user_strategies_and_jobs.sql`
- [X] T009 [US2] Backfill legacy Attempt Job rows and add Job/Candidate foreign keys in `supabase/migrations/20260828000100_add_user_strategies_and_jobs.sql`
- [X] T010 [US2] Add Job recovery and Experiment listing indexes in `supabase/migrations/20260828000100_add_user_strategies_and_jobs.sql`

**Checkpoint**: Every Attempt belongs to a durable Job and retry no longer conflates logical work with a Worker try.

---

## Phase 5: User Story 3 — Keep User profile separate from credentials (Priority: P3)

**Goal**: Make the authentication boundary explicit without adding credential columns.

**Independent Test**: Schema inspection finds no password/session/refresh-token columns and browser roles have no direct access to business tables.

- [X] T011 [US3] Document the unchanged Supabase Auth/User Profile boundary and application authorization invariant in `docs/database/data-dictionary.md`
- [X] T012 [US3] Cover credential-column absence and direct browser privilege denial in `supabase/tests/database/002_user_strategy_jobs_test.sql`

**Checkpoint**: The database supports ownership without becoming a second authentication store.

---

## Phase 6: Documentation and Cross-Cutting Consistency

- [X] T013 [P] Update physical ERD and ownership diagrams in `docs/database/erd.md` and `docs/database/drawio-erd.mmd`
- [X] T014 [P] Update database status, tables, invariants and decisions in `docs/database/README.md`, `docs/database/data-dictionary.md`, and `docs/database/decisions.md`
- [X] T015 [P] Separate User Strategy, Job and Execution Attempt in `docs/architecture/data-model-overview.md`
- [X] T016 Update feature validation state and runnable commands in `specs/002-user-strategy-jobs/quickstart.md`

---

## Phase 7: Validation

- [X] T017 Run static checks for placeholders, balanced Mermaid/Markdown fences, local links and `git diff --check`
- [X] T018 Run available non-mutating Supabase validation and record honest Planned/Verified state in `specs/002-user-strategy-jobs/quickstart.md`
- [X] T019 Review `git diff --stat`, `git status`, and confirm `supabase/migrations/20260827000100_create_database_baseline.sql` is unchanged

## Dependencies and Execution Order

- Setup (T001–T003) precedes schema implementation.
- Foundational verification design (T004) precedes T005–T012.
- User Story 1 (T005–T007) is required before User Story 2's manifest/Job integration can be fully verified.
- User Story 2 (T008–T010) depends on the existing Experiment/Candidate baseline but is otherwise independent from User Strategy tables.
- User Story 3 (T011–T012) uses the ownership model from User Story 1.
- Documentation tasks T013–T016 follow the final migration shape.
- Validation T017–T019 runs last.

## Parallel Opportunities

- T002 and T003 touch different ADR files but T003 should use the final ADR title.
- After the migration shape is stable, T013, T014 and T015 update separate documentation views and can proceed in parallel.
- Within implementation, User Strategy and Job DDL are conceptually independent, but they share one ordered migration and must be edited sequentially.

## Implementation Strategy

1. Deliver User Story 1 first: private Strategy ownership and immutable versions.
2. Add User Story 2: durable Job and legacy Attempt backfill.
3. Confirm User Story 3 without inventing credential storage or direct browser access.
4. Synchronize every documentation view, then validate without applying remote changes.

## Phase 8: Convergence

- [X] T020 Synchronize the public Job lifecycle to `SUCCEEDED` and all durable Job states in `docs/api/openapi.yaml`, while keeping Backtest Result and Experiment completion as `COMPLETED`, per Constitution IV (contradicts)
- [X] T021 Restore baseline-only verification by removing v2 Job dependencies from `supabase/tests/database/001_database_baseline_test.sql` and `specs/001-database-baseline/contracts/database-verification.md`, with v2 behavior remaining in `supabase/tests/database/002_user_strategy_jobs_test.sql`, per FR-016 (contradicts)
- [X] T022 Clarify database-trigger versus application-transaction responsibilities and state explicitly that Execution Attempt belongs only to Backtest Job in `docs/adr/0012-user-strategy-job-ownership.md`, `docs/database/decisions.md`, and `specs/002-user-strategy-jobs/data-model.md`, per plan: ownership and lifecycle decisions (partial)
- [X] T023 Update separate baseline/v2 validation order and evidence state in `specs/002-user-strategy-jobs/quickstart.md`, per SC-007 (partial)
- [X] T024 Run independent PostgreSQL baseline verification, PostgreSQL v2 verification, Gradle checks, contract consistency scans, and `git diff --check`, per Constitution V (partial)
- [X] T025 Attempt non-mutating Supabase dry-run and linked lint without applying remote changes, then record honest evidence or the exact environment blocker in `specs/002-user-strategy-jobs/quickstart.md`, per Constitution V (partial)

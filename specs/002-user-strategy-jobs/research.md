# Research: User-owned Strategies and Durable Jobs

## Decision 1 — Keep authentication data in Supabase Auth

**Decision**: Retain `auth.users` as the identity source and keep `platform.user_profile` as optional application metadata only. Do not add password, password hash, session or refresh-token columns.

**Rationale**: ADR-0011 already assigns authentication/session lifecycle to Supabase Auth. Duplicating credentials creates two sources of truth and expands the security surface without helping Strategy ownership.

**Alternatives considered**:

- Add password and refresh token to `user_profile`: rejected because it duplicates sensitive Auth state.
- Build a separate application user credential table: rejected because it supersedes ADR-0011 without a driver.

## Decision 2 — Separate shared plugin catalog from user-saved Strategy

**Decision**: Keep `strategy.strategy_version` as the shared, immutable plugin descriptor. Add `user_strategy`, `user_strategy_version`, and `user_strategy_component` for private user configurations.

**Rationale**: A plugin version answers “what implementation and parameter schema exists?” A user Strategy answers “what configuration did this user save?”. Separating them avoids pretending user data owns executable system plugins and lets two users save equivalent configurations independently.

**Alternatives considered**:

- Add `owner_user_id` and `scope` directly to `strategy_version`: rejected because it mixes system implementation catalog and mutable user organization.
- Store only Strategy JSON inside Experiment Manifest: rejected because users cannot manage or reuse named Strategy definitions outside one Experiment.
- Add sharing/member tables now: deferred because MVP requires private ownership only.

## Decision 3 — Version user Strategy and archive its mutable root

**Decision**: `user_strategy` holds owner, name and lifecycle. Each saved configuration is an immutable `user_strategy_version`; composite membership lives in child rows. Deletion is represented by archive for MVP.

**Rationale**: User-facing naming/lifecycle can change, while Experiment provenance must always resolve the exact configuration used. Archive preserves referenced versions.

**Alternatives considered**:

- Update one Strategy row in place: rejected because old Experiments silently change meaning.
- Hard-delete Strategy and cascade versions: rejected because it destroys reproduction evidence.

## Decision 4 — Add a durable Job above Execution Attempt

**Decision**: Add `experiment.job` for the logical Search/Backtest work item. Keep `execution_attempt` as a Worker try and link it to Job with a foreign key. Search Job has no Candidate; Backtest Job has exactly one Candidate from the same Experiment.

**Rationale**: Retry must not create a new logical job. Durable Job status/progress also supports recovery after Redis loss and gives REST/WebSocket a stable identifier.

**Alternatives considered**:

- Rename `execution_attempt` to Job: rejected because retry history and worker-level failures would be overwritten or conflated.
- Keep free-form `job_id` without a Job table: rejected because ownership, lifecycle and recovery state cannot be enforced or queried reliably.
- Put Job only in Redis: rejected because Redis is not the source of truth.

## Decision 5 — Enforce relational consistency where practical

**Decision**: Use composite foreign keys to prove that a Job Candidate belongs to its Experiment and an Attempt Candidate belongs to its Job. Use unique indexes for active Strategy names per owner and Backtest Job per Candidate.

**Rationale**: These are stable row relationships that PostgreSQL can enforce cheaply and unambiguously.

**Alternatives considered**:

- Rely on application checks for all relations: rejected because simple integrity mistakes could become durable corruption.
- Duplicate `owner_user_id` on Job and Strategy version: rejected because duplicated ownership can disagree with its aggregate root.

## Decision 6 — Keep authorization at the application boundary

**Decision**: Continue revoking direct access from `anon` and `authenticated`. The Java API must load/update Strategy by `(strategy_id, owner_user_id)` and load/stop Job through its Experiment owner. The schema supplies owner paths and constraints but does not claim to implement end-user authorization alone.

**Rationale**: This matches ADR-0011 and avoids a second browser-to-database business path. A foreign key proves identity consistency; it does not prove that the current caller is that identity.

**Alternatives considered**:

- Enable direct browser access with RLS: rejected for MVP because it changes the chosen application boundary.
- Trust the owner ID sent by the browser: rejected because an identifier is not authorization evidence.

## Decision 7 — Backfill before adding the Attempt foreign key

**Decision**: The forward migration validates that each existing `job_id` maps to one Candidate, inserts one Backtest Job per existing job ID using the Candidate's Experiment, and only then adds the foreign key.

**Rationale**: The applied baseline permits Attempt rows whose `job_id` has no parent. A safe upgrade must preserve them rather than assume the shared database is empty.

**Alternatives considered**:

- Add an unvalidated foreign key: rejected because old orphan state remains unresolved.
- Delete old Attempts: rejected because execution evidence must be preserved.
- Edit the baseline migration: prohibited after shared application.


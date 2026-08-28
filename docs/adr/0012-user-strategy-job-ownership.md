# ADR-0012: User Strategy Ownership and Durable Job Identity

**Status**: Proposed  
**Date**: 2026-08-28  
**Owners**: Tiến Luật  
**Extends**: ADR-0006 and ADR-0011

## Context

ADR-0011 assigns identity and session management to Supabase Auth and states that the Strategy catalog is shared. That catalog represents system plugin implementations such as MA, RSI or MACD; it does not represent a user's named parameter configuration.

The MVP now needs each user to save private Strategy configurations and reuse them in Experiments without allowing another user to read or run them. Updating one configuration in place would also change the meaning of historical Experiments.

The baseline schema also stores `job_id` directly on `execution_attempt` without a Job parent. This conflates one logical Search/Backtest operation with each Worker retry and leaves no durable aggregate for status, progress, recovery or authorization.

## Drivers and Quality Scenarios

- User A cannot read, change, archive or execute User B's Strategy.
- Two users can independently save equivalent Strategy names/configurations.
- Editing a Strategy creates a new immutable version; an old Experiment keeps its original meaning.
- A retry creates a new Execution Attempt under the same Job and does not create a duplicate result.
- Redis loss does not erase Job or Strategy truth.
- Password and refresh token remain outside business schemas.

## Decision

### Shared plugin catalog and private User Strategy

Keep `strategy.strategy_version` as the system-owned shared plugin catalog. Add:

- `strategy.user_strategy` for owner, display name, kind and active/archive lifecycle;
- `strategy.user_strategy_version` for exact versioned parameters, combination policy and fingerprint;
- `strategy.user_strategy_component` for ordered components of a composite version.

User Strategy versions are built as `DRAFT` and become immutable at `PUBLISHED`. A published version and its components cannot be changed or deleted. Updating a Strategy means publishing the next version. The mutable root is archived rather than hard-deleted.

MVP User Strategies are private. Sharing, organization membership, marketplace visibility and user-supplied executable code are outside scope.

`experiment_manifest.source_user_strategy_version_id` is optional provenance. The existing manifest snapshot remains authoritative, so rename/archive does not affect reproduction. The source Strategy and Experiment must have the same owner.

### Job and Execution Attempt

Add `experiment.job` as the durable logical unit of Search or Backtest work. Job owns type, status, progress, correlation identity, failure information and lifecycle timestamps.

- A Search Job belongs to an Experiment and has no Candidate.
- A Backtest Job belongs to one Candidate from the same Experiment.
- One Backtest Candidate has at most one logical Job.
- `execution_attempt` remains a Worker try; retry adds the next attempt number under the same Job.

Existing Attempt rows are backfilled into Job before the new foreign key is enabled. Migration aborts rather than guessing if one legacy Job ID maps to multiple Candidates or one Candidate maps to multiple logical Job IDs.

### Authorization boundary

Supabase Auth continues to own passwords, sessions and refresh tokens. `platform.user_profile` stores display metadata only.

The Java API must authorize User Strategy by authenticated `owner_user_id` and Job through `job → experiment → owner_user_id`. A client-provided Strategy ID or Job ID never grants access. Browser roles remain without direct business-table privileges; foreign keys protect integrity but do not replace application authorization.

## Alternatives Considered

- **Add owner and scope to `strategy_version`**: mixes executable plugin metadata with user organization and makes system upgrades/user edits harder to reason about.
- **Store Strategy only inside Experiment Manifest**: preserves runs but does not let users name, version and reuse configurations.
- **Use one mutable Strategy row**: simple but breaks historical provenance when parameters change.
- **Treat `execution_attempt` as Job**: loses the distinction between logical work and retry history.
- **Store Job only in Redis**: loses source-of-truth state and recovery when Redis is cleared.
- **Add sharing roles now**: expands MVP authorization and schema without a current requirement.

## Consequences

### Positive

- Plugin implementation ownership and user configuration ownership are explicit.
- Historical Experiments can resolve the exact published user configuration.
- Job status survives Worker/Redis restart and retry history remains visible.
- Database constraints prevent cross-Experiment Job/Candidate and Job/Attempt mismatches.
- No credential duplication is introduced.

### Negative

- Publishing composite versions requires a short multi-row transaction.
- Java repositories must always include authenticated ownership predicates.
- User Strategy adds lifecycle/version tables beyond a single JSON column.
- Backfill can intentionally stop deployment when legacy data is inconsistent.

## Validation Plan

- Run the transactional SQL contract in `supabase/tests/database/002_user_strategy_jobs_test.sql`.
- Verify two users can use the same active name but one user cannot duplicate it case-insensitively.
- Verify published versions/components reject mutation and archived roots reject new versions.
- Verify Search/Backtest Job shape, same-Experiment Candidate rules and Attempt foreign keys.
- Add Java integration tests proving cross-user Strategy and Job requests are rejected when application code exists.
- Clear Redis in a resilience test and verify Job/Strategy truth remains recoverable from PostgreSQL when Worker code exists.

## Evidence

Planned — migration, SQL assertions and documentation are prepared on `db-setup-v2`; remote dry-run/apply, Java authorization tests and Redis recovery evidence have not been collected.

## Risks and Mitigations

- **Risk**: Application queries by raw ID and omit owner filtering.  
  **Mitigation**: Repository methods accept authenticated owner identity and require cross-user integration tests before merge.
- **Risk**: A draft composite is partially constructed.  
  **Mitigation**: Build components transactionally; publication validates parent kind and minimum component count.
- **Risk**: Legacy Attempt data is inconsistent.  
  **Mitigation**: Validate one Candidate per legacy Job ID and abort the forward migration without discarding data.
- **Risk**: Version/history tables grow.  
  **Mitigation**: Index owner/version queries and retain referenced versions for reproducibility; define cleanup only for unreferenced drafts in a later feature.

## References

- [Feature specification](../../specs/002-user-strategy-jobs/spec.md)
- [Feature data model](../../specs/002-user-strategy-jobs/data-model.md)
- [ADR-0006: Queue and Worker](0006-queue-worker-backtesting.md)
- [ADR-0009: Reproducible Experiments](0009-reproducible-experiments.md)
- [ADR-0011: Supabase Auth and User Ownership](0011-supabase-auth-user-ownership.md)

## Supersession

- Supersedes: None
- Superseded by: None

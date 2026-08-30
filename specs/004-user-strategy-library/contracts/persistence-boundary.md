# Contract: Strategy Persistence Boundary

## Scope

The Strategy persistence adapter maps public Strategy-core ports to the existing PostgreSQL/Supabase schema. It does not own Strategy policy and is not imported by Strategy implementations.

## Gradle/module direction

```text
apps/api -> strategy-core, strategies, combination, persistence
strategies -> strategy-core, domain
combination -> strategy-core, domain
persistence -> strategy-core public output ports, domain
strategy-core -> domain
```

`persistence` must not depend on `strategy.internal`, `strategies`, or `combination`. The architecture dependency matrix must permit `persistence -> strategy` because persistence implements the Strategy owner's public output ports.

## Strategy catalog port

```text
registerOrVerify(StrategyDescriptor) -> durable StrategyDescriptor reference
findByKey(pluginId, implementationVersion) -> optional descriptor snapshot
findById(strategyVersionId) -> optional descriptor snapshot
```

`registerOrVerify` behavior:

1. query by plugin key/version;
2. if absent, insert the descriptor snapshot with the stable typed ULID declared by that trusted plugin version and its fingerprint;
3. if present and fingerprint/content matches, return the existing row;
4. if present but content conflicts, return `STRATEGY_CATALOG_CONFLICT`;
5. translate a concurrent unique race by re-reading and applying the same equality rule.

## User Strategy store port

All private operations require `ownerUserId` in their signature and SQL predicate.

Required capabilities:

- insert owner-scoped root and complete first draft atomically;
- list active roots for owner with bounded pagination;
- find root/version by `(id, owner_user_id)`;
- insert next complete draft only when expected latest version still matches;
- publish draft atomically only when owner/root/kind/lifecycle/catalog/components remain valid;
- archive active root conditionally by owner/status;
- resolve published single/Composite snapshot by version ID and owner;
- never expose a raw private `findById(id)` method to application services.

## Transaction boundaries

### Create first version

One transaction inserts root, version, and (for Composite) all components. Any failure rolls back all rows.

### Create next version

One transaction locks or conditionally verifies the owner-scoped root/latest version, inserts one version number, then all components. A uniqueness/stale-state race becomes `STRATEGY_CONFLICT`; the adapter never auto-renumbers and retries as a new user action.

### Publish

One transaction:

1. loads root/version by owner;
2. requires root `ACTIVE` and version `DRAFT`;
3. verifies kind and exact registered catalog references;
4. verifies at least two distinct components for Composite;
5. conditionally changes lifecycle to `PUBLISHED` with publication time;
6. re-reads the immutable snapshot;
7. rolls back if any invariant or conditional update fails.

The existing triggers remain the final immutability guard. Application validation remains authoritative for descriptor parameter rules.

## Existing table mapping

| Port data | Table |
|---|---|
| Trusted descriptor snapshot | `strategy.strategy_version` |
| Private root/owner/name/archive | `strategy.user_strategy` |
| Single/Composite draft or published version | `strategy.user_strategy_version` |
| Flat system components | `strategy.user_strategy_component` |

F-004 does not write `strategy.composite_version`, `strategy.composite_component`, Experiment, Job, Execution Attempt, Dataset, or market tables.

## JSONB mapping

Only the adapter serializes parameter definitions/values. Reads must reconstruct typed canonical values and reject corrupt or incompatible stored content with a stable integrity error. Serialization uses deterministic field names and exact decimal text; fingerprint generation remains in Strategy-core and is not delegated to PostgreSQL JSON rendering.

## Stable error translation

| Database condition | Application error |
|---|---|
| Owner-scoped row absent | `USER_STRATEGY_NOT_FOUND` |
| Active-name unique conflict | `USER_STRATEGY_NAME_CONFLICT` |
| Version number/lifecycle conditional conflict | `STRATEGY_CONFLICT` |
| Published-row/component mutation trigger | `IMMUTABLE_STRATEGY_VERSION` |
| Catalog key/version content conflict | `STRATEGY_CATALOG_CONFLICT` |
| FK/check/JSON corruption | `STRATEGY_PERSISTENCE_INTEGRITY` |
| Connectivity/other SQL failure | `STRATEGY_PERSISTENCE_UNAVAILABLE` |

Public errors contain no SQL text, table names, credentials, connection URLs, or stack traces.

## Migration and environment rule

- Do not edit `20260827000100_create_database_baseline.sql`.
- Do not edit `20260828000100_add_user_strategies_and_jobs.sql`.
- No F-004 migration is planned.
- If a real gap is found, stop and propose a new forward migration for review.
- Tests may reset/start local Supabase and run transactions that roll back.
- Remote `supabase db push` or any remote mutation requires separate explicit approval and is outside this plan.

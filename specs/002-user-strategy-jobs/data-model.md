# Data Model: User-owned Strategies and Durable Jobs

## Existing entities retained

### `auth.users`

Supabase-managed identity, password/session and refresh lifecycle. Business migrations reference `id uuid` only and never read or duplicate credentials.

### `platform.user_profile`

Optional one-to-one profile (`user_id`, display metadata, timestamps). No schema change is required for this feature.

### `strategy.strategy_version`

Shared system plugin snapshot: plugin identifier, implementation version, parameter schema/defaults, supported signals and fingerprint. It is not a user's saved Strategy.

## New Strategy entities

### `strategy.user_strategy`

Mutable aggregate root for one private saved Strategy.

| Field | Rule |
| --- | --- |
| `user_strategy_id` | ULID primary key |
| `owner_user_id` | Required FK to `auth.users.id` |
| `strategy_kind` | `SINGLE` or `COMPOSITE` |
| `name` | Required, non-blank |
| `description` | Optional |
| `status` | `ACTIVE` or `ARCHIVED` |
| `created_at`, `updated_at`, `archived_at` | UTC instants; archive timestamp must agree with status |

Invariant: active names are unique per owner ignoring case; different owners may use the same name.

### `strategy.user_strategy_version`

Immutable configuration snapshot belonging to one User Strategy.

| Field | Rule |
| --- | --- |
| `user_strategy_version_id` | ULID primary key |
| `user_strategy_id` | Required FK to aggregate root |
| `version_no` | Positive integer, unique within Strategy |
| `strategy_kind` | Frozen `SINGLE` or `COMPOSITE`; must match aggregate kind in application transaction |
| `strategy_version_id` | Required only for `SINGLE`; FK to shared plugin version |
| `parameters` | Exact parameters for `SINGLE`; empty object for `COMPOSITE` |
| `policy_id`, `policy_version`, `policy_parameters` | Required only for `COMPOSITE` |
| `lifecycle_status` | `DRAFT` while being assembled, then one-way `PUBLISHED` |
| `fingerprint` | Non-empty, unique within Strategy |
| `published_at`, `created_at` | UTC lifecycle instants |

Invariant: publication verifies parent kind and component count. Published content is
never updated/deleted. A change creates `version_no + 1`.

### `strategy.user_strategy_component`

Ordered plugin configuration inside a composite User Strategy version.

| Field | Rule |
| --- | --- |
| `user_strategy_version_id`, `position` | Composite primary key; position starts at zero |
| `strategy_version_id` | FK to shared plugin version |
| `parameters` | Exact component parameters |
| `weight` | Optional positive decimal |

Invariant: only composite versions have components and a usable composite has at least two; the application validates this in the creation transaction.

## Manifest provenance

`experiment.experiment_manifest` gains nullable `source_user_strategy_version_id`. It records which saved version initiated the Experiment. Existing frozen fields remain authoritative for reproduction, so archive or rename never changes an Experiment.

Application invariant: the source version must belong to the same user that owns the Experiment.

## New Job entity

### `experiment.job`

Durable identity and lifecycle for one logical long-running operation.

| Field | Rule |
| --- | --- |
| `job_id` | ULID primary key |
| `experiment_id` | Required FK to Experiment |
| `candidate_id` | Null for `SEARCH`; required for `BACKTEST`; composite FK guarantees the Candidate belongs to the Experiment |
| `job_type` | `SEARCH` or `BACKTEST` |
| `status` | `QUEUED`, `RUNNING`, `RETRY_SCHEDULED`, `SUCCEEDED`, `FAILED`, `CANCEL_REQUESTED`, `CANCELLED` |
| `correlation_id` | Required ULID used for cross-boundary tracing |
| `total_work`, `completed_work`, `failed_work` | Non-negative progress; completed + failed cannot exceed total |
| `best_score` | Optional exact decimal progress snapshot |
| `queued_at`, `started_at`, `finished_at`, `next_retry_at` | Optional lifecycle instants |
| `failure_code`, `failure_message` | Structured terminal/retry failure detail |
| `created_at`, `updated_at` | UTC audit instants |

Invariant: at most one Backtest Job exists per Candidate. A Search Experiment can have one top-level Search Job in MVP.

### `experiment.execution_attempt` (updated)

Represents one Worker try, not the logical Job. Existing columns remain; `job_id` becomes an FK. A composite FK over `(job_id, candidate_id)` prevents an Attempt from pointing at a Candidate different from its Backtest Job.

## State machines

### User Strategy

```text
ACTIVE -> ARCHIVED
```

Archive is terminal in MVP. Historical versions remain available for provenance.

### Job

```text
QUEUED -> RUNNING -> SUCCEEDED
                  -> RETRY_SCHEDULED -> QUEUED
                  -> FAILED
QUEUED/RUNNING -> CANCEL_REQUESTED -> CANCELLED
QUEUED -> CANCELLED
```

The database restricts valid state values. The application service validates allowed transitions atomically.

### Execution Attempt

```text
QUEUED -> RUNNING -> SUCCEEDED
                  -> RETRY_SCHEDULED
                  -> FAILED
                  -> CANCELLED
```

Retry creates another Attempt under the same Job with the next `attempt_no`.

## Ownership paths

```text
auth.users
  -> user_strategy
     -> user_strategy_version
        -> user_strategy_component

auth.users
  -> experiment
     -> job
        -> execution_attempt
```

Queries and commands must start from authenticated user identity and follow these paths. Supplying only a Strategy ID or Job ID is insufficient authorization.

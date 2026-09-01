# Data Model: F-008 News and Sentiment

## Ownership and relationships

```text
market_data.asset (read-only identity)
        ^
        | FK
news.news_item_asset >-- news.news_item --< news.sentiment_result
                              |                    |
                              | current work       | FK model_version
                              v                    v
                         durable lease     news.sentiment_model_release
```

`modules/news` owns all three News tables and their business meaning. The Asset foreign key permits association only; it does not grant News write access to Market Data.

## Java domain types

### NewsItem

| Field | Type | Rule |
|---|---|---|
| `newsId` | `NewsId` | Typed uppercase ULID |
| `source` | `NewsSource` | Nonblank canonical provider identifier |
| `sourceItemId` | optional text | Provider-local identity; never global identity |
| `url` | `CanonicalNewsUrl` | Logical uniqueness key |
| `title` | text | Sanitized and normalized, nonblank |
| `summary` | optional text | Public summary; size-bounded |
| `content` | text | Sanitized normalized analysis content |
| `language` | `LanguageCode` | Canonical lowercase; initial analysis supports `en` |
| `contentHash` | `ContentHash` | Versioned `sha256:` lowercase hex form |
| `publishedAt` | `Instant` | UTC boundary form |
| `crawledAt` | `Instant` | UTC boundary form |
| `analysisStatus` | enum | State machine below |
| `relatedAssets` | set of `AssetId` | Duplicate associations collapse |
| `analysisLease` | optional `AnalysisLease` | Present only while `ANALYZING` |

### SentimentModelRelease

| Field | Type | Rule |
|---|---|---|
| `modelVersion` | text | Global release identity and PK |
| `modelName` | text | Nonblank |
| `preprocessingVersion` | text | Nonblank, identifies exact tokenizer/preprocessing |
| `contractVersion` | text | Nonblank; initial value `sentiment-v1` |

The tuple is immutable. Any changed member requires a new `modelVersion`.

### SentimentResult

| Field | Type | Rule |
|---|---|---|
| `sentimentResultId` | typed ULID | Stable identity |
| `newsId` | `NewsId` | Existing parent |
| `contentHash` | `ContentHash` | Must equal the locked parent content hash at insert |
| `language` | `LanguageCode` | Nonblank |
| `modelVersion` | text | FK to one immutable release |
| `label` | enum | `POSITIVE`, `NEUTRAL`, `NEGATIVE` |
| `confidence` | `BigDecimal` | Exact `[0,1]`, scale compatible with `numeric(20,10)` |
| `polarityScore` | `BigDecimal` | Exact `[-1,1]` |
| `analyzedAt` | `Instant` | UTC |

Logical identity is `(newsId, contentHash, modelVersion)`. Accepted rows are immutable.

### AnalysisLease

| Field | Type | Rule |
|---|---|---|
| `leaseToken` | typed/random ULID | Unique per acquisition; stale-completion fence |
| `leaseOwner` | text | Nonblank Worker instance identity |
| `leaseExpiresAt` | `Instant` | Required while analyzing |
| `attemptCount` | integer | Nonnegative; incremented atomically immediately before dispatch |
| `nextEligibleAttempt` | optional `Instant` | Required for `FAILED_RETRYABLE` |
| `targetModelVersion` | text | Persisted release target; attempt budget is scoped to content + release |
| claimed content/model | hash + version | Completion must match |

## State model

```text
PENDING -> ANALYZING -> ANALYZED
              |
              +-> FAILED_RETRYABLE -> ANALYZING
              |                          |
              +--------------------------+-> FAILED

expired ANALYZING -> reclaimed ANALYZING under a new lease token
```

- `PENDING`: no lease; immediately eligible unless deferred.
- `ANALYZING`: all lease fields present; retry eligibility null.
- `FAILED_RETRYABLE`: no lease; a future eligibility time required.
- `ANALYZED`: no lease/retry timestamp; matching accepted result exists.
- `FAILED`: no lease/retry timestamp; terminal for the configured call budget or permanent input/release failure.

Open-circuit/readiness deferral does not consume an attempt. A dispatched POST consumes one even if its result is unknown or `503`.

## Forward migration design

Create exactly one reviewed migration after the two applied files; do not modify them.

### `news.news_item` additions

- `language text`
- `lease_owner text`
- `lease_token varchar(26)`
- `lease_expires_at timestamptz`
- `attempt_count integer not null default 0`
- `next_eligible_attempt timestamptz`
- `target_model_version text` referencing `sentiment_model_release(model_version)`

Replace the existing status check through the forward migration to include `FAILED_RETRYABLE`. Add checks for nonnegative attempts, canonical/nonblank language, complete lease fields only in `ANALYZING`, and required eligibility only in `FAILED_RETRYABLE`. Add partial indexes for due work and expired leases.

`target_model_version` makes the persisted attempt budget unambiguously apply to `newsId + contentHash + modelVersion`. New analyzable `en` items receive the active registered release when entering the workflow. Switching the configured release creates a new target cycle and resets its attempt/eligibility state without deleting historical results. Unsupported or legacy `und` items may remain without a target and are not claimable. Lease/start/complete SQL fences on this value.

### `news.sentiment_model_release`

```text
model_version           text PRIMARY KEY
model_name              text NOT NULL
preprocessing_version   text NOT NULL
contract_version        text NOT NULL
```

All values have nonblank checks. A `BEFORE UPDATE OR DELETE` trigger rejects mutation/deletion of established rows. Result foreign keys use restrictive deletion, never cascade.

### `news.sentiment_result` changes

- Add `language text`, safely backfill, then make non-null with canonical-language check.
- Add FK from the existing `model_version` to `sentiment_model_release(model_version)`.
- Retain unique `(news_item_id, content_hash, model_version)` and existing label/range checks and `numeric(20,10)` columns.
- Add a trigger that locks/reads the parent and rejects a mismatched content hash.
- Add immutable update/delete trigger.
- Prevent a parent content-hash mutation that would invalidate accepted results.

### Legacy data policy

- Existing News with unknown language may be set to `und`; it is not eligible for the English model until corrected or recollected.
- Before adding the result FK/not-null constraints, inspect every legacy `model_version`.
- Insert only explicitly reviewed release mappings embedded in an approved data migration.
- Abort the migration if any existing result lacks a truthful mapping; never infer provenance from the version string.

## Transactional operations

### Collect

Insert by canonical URL and rely on PostgreSQL uniqueness. A concurrent duplicate resolves the existing row. If accepted content/provenance conflicts, report a stable integrity outcome and never overwrite silently.

### Acquire/reclaim

In one short transaction, select eligible `PENDING`, due `FAILED_RETRYABLE`, or expired `ANALYZING` rows with `FOR UPDATE SKIP LOCKED`; assign a fresh token/owner/expiry and set `ANALYZING`. Acquisition alone does not increment attempts.

### Start attempt

After readiness, concurrency and circuit permission are reserved, atomically increment `attempt_count` using the exact lease token/hash fence. A zero-row update is stale work and no POST is sent.

### Complete

In one transaction, verify the exact lease/hash/release, insert or resolve the unique immutable result, verify any existing row is semantically identical, set `ANALYZED`, and clear lease/retry fields. Any result failure rolls back the state transition.

### Fail/defer

Fence by lease token/hash. A transient dispatched failure with budget remaining becomes `FAILED_RETRYABLE` with a future timestamp and cleared lease. Exhausted/permanent failure becomes `FAILED`. A pre-dispatch readiness/circuit deferral clears the lease and schedules eligibility without incrementing attempts.

## Query projections

Public projection: canonical News fields, related assets, `analysisStatus`, and optional `label`, `confidence`, `polarityScore` only when analyzed. Order by `(published_at DESC, id DESC)` and encode both in the cursor. Pair filtering uses `EXISTS` for base OR quote IDs so dual-linked items are not duplicated.

Protected audit projection: News/result IDs, language, content hash, model name/version, preprocessing/contract version, analyzed time and exact scores. Lease owner/token and credentials are not provenance output.

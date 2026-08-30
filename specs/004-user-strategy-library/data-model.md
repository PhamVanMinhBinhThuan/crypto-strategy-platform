# Data Model: Strategy Registry and User Strategy Library

## 1. Model boundaries

F-004 has three related but distinct models:

1. **Runtime Strategy model**: pure immutable values used to validate and evaluate one Strategy.
2. **Runtime registry model**: trusted platform plugins addressable by stable key and version.
3. **User Strategy model**: private owner-scoped configurations with immutable published versions.

F-003 remains owner of `Candle`, `TradingPair`, `Timeframe`, Dataset metadata, membership, and batched Candle reads. F-004 references those public values but does not redefine or persist them.

## 2. Identity and version values

| Value | Representation | Rule |
|---|---|---|
| `StrategyVersionId` | Typed ULID | Durable system plugin-version row identity; validates through shared `Ulids`. |
| `UserStrategyId` | Typed ULID | Private aggregate root identity. |
| `UserStrategyVersionId` | Typed ULID | Immutable/draft version row identity. |
| `OwnerUserId` | UUID | Supabase Auth user identity; never accepted as an editable owner field. |
| `StrategyPluginId` | Lowercase slug | Stable trusted plugin key such as `ma-crossover`; not a database primary key. |
| `StrategyImplementationVersion` | Semantic version | Version of behavior/descriptor; behavior-changing changes require a new value. |
| `CombinationPolicyId` | Lowercase slug | Initial value `majority-vote`. |
| `CombinationPolicyVersion` | Semantic version | Initial value `1.0.0`. |
| `UserStrategy.versionNo` | Positive integer | Monotonically increases within one root; uniqueness enforced per root. |
| `FingerprintVersion` | Fixed contract version | Initial value `strategy-v1`. |

## 3. Runtime Strategy entities

### `StrategyDescriptor`

| Field | Rule |
|---|---|
| `strategyVersionId` | Required typed ULID matching the durable catalog entry. |
| `pluginId` | Required stable slug. |
| `implementationVersion` | Required semantic version. |
| `contractVersion` | Required Strategy boundary version. |
| `displayName`, `description` | Non-blank display metadata; not part of behavior fingerprint. |
| `category` | Stable category such as `TREND`; descriptive, not executable. |
| `supportedSignals` | Non-empty subset of `BUY`, `SELL`, `HOLD`; the sample supports all three. |
| `requiredLookback` | Positive Candle count required before evaluation. |
| `parameterDefinitions` | Unique, name-sorted definitions. |
| `crossParameterConstraints` | Explicit deterministic relationships such as `fastPeriod < slowPeriod`. |
| `descriptorFingerprint` | Canonical fingerprint of behavior-relevant descriptor content. |

Registry uniqueness is `pluginId + implementationVersion`. `strategyVersionId`, descriptor fingerprint, and the semantic key must all agree with the durable catalog.

### `ParameterDefinition`

| Field | Rule |
|---|---|
| `name` | Stable validated name; unique within a descriptor. |
| `type` | `INTEGER`, `DECIMAL`, `BOOLEAN`, `TEXT`, or `ENUM`. |
| `required` | Required values without defaults must be supplied. |
| `defaultValue` | Optional value of the declared type and within all limits. |
| `minimum`, `maximum` | Optional inclusive bounds for integer/decimal values. |
| `allowedValues` | Required and non-empty for `ENUM`; absent otherwise. |
| `description` | Human-readable guidance; not part of runtime parameter values. |

### `StrategyParameterSet`

An immutable name-sorted map of typed values. It is produced only after:

1. rejecting unknown names;
2. checking supplied types;
3. filling declared defaults;
4. checking required values;
5. checking ranges/allowed values;
6. checking cross-parameter constraints.

Published snapshots always contain this complete set. Input omission is not retained as meaning.

### `StrategyContext`

| Field | Rule |
|---|---|
| `tradingPair` | Canonical F-003 `TradingPair`. |
| `timeframe` | Canonical F-003 `Timeframe`. |
| `candles` | Immutable ordered rolling window of canonical F-003 `Candle`; never a Dataset or reader. |
| `evaluationTime` | UTC `Instant` associated with the last evaluated closed Candle. |

Every Candle must share provider, trading-pair ID, and timeframe; open times are strictly increasing with no duplicate identity; evaluation uses closed Candles. Context size is bounded by the caller to the descriptor's required lookback. If fewer Candles are supplied, evaluation returns `INSUFFICIENT_DATA` and performs no read.

### `StrategyDecision`

| Field | Rule |
|---|---|
| `signal` | `BUY`, `SELL`, or `HOLD`. |
| `occurredAt` | UTC time of the evaluated Candle. |
| `strategyReference` | Exact plugin ID, implementation version, and durable Strategy version ID. |
| `reasonCode` | Stable non-UI diagnostic code. |
| `reason` | Short deterministic text suitable for demo/debug. |
| `evidence` | Immutable name-sorted typed scalar values; no secrets, markup, arbitrary objects, or provider payload. |

`HOLD` is a valid decision. Missing lookback is an error and is never encoded as `HOLD`.

### `StrategyPlugin` and `Strategy`

- `StrategyPlugin` exposes one descriptor and creates a Strategy from an already canonical parameter set.
- `Strategy` evaluates one immutable context and returns one decision.
- Both are thread-safe through stateless/immutable design or instance-per-execution creation.

## 4. User Strategy aggregate

### `UserStrategy`

Mutable organization root mapped to `strategy.user_strategy`.

| Field | Rule |
|---|---|
| `userStrategyId` | Typed ULID primary identity. |
| `ownerUserId` | Required immutable UUID. |
| `kind` | `SINGLE` or `COMPOSITE`; cannot change after a version exists. |
| `name` | Trimmed non-blank; unique among active roots for one owner, case-insensitive. |
| `description` | Optional owner metadata. |
| `status` | `ACTIVE` or `ARCHIVED`. |
| `archivedAt` | Null for active, required for archived. |
| `createdAt`, `updatedAt` | UTC audit instants. |

Different owners may use the same name. Archive is one-way, blocks new versions, and preserves published history.

### `UserStrategyVersion`

Configuration snapshot mapped to `strategy.user_strategy_version`.

| Field | Rule |
|---|---|
| `userStrategyVersionId` | Typed ULID primary identity. |
| `userStrategyId` | Required parent identity. |
| `versionNo` | Positive and unique under the parent. |
| `kind` | Frozen `SINGLE` or `COMPOSITE`, equal to parent kind. |
| `strategyVersionId` | Required only for `SINGLE`. |
| `parameters` | Complete canonical single parameters; empty for `COMPOSITE`. |
| `policyId`, `policyVersion`, `policyParameters` | Required only for `COMPOSITE`; initial policy has an empty canonical parameter set. |
| `lifecycleStatus` | `DRAFT` or `PUBLISHED`. |
| `fingerprint` | Required `strategy-v1` fingerprint. |
| `createdAt`, `publishedAt` | UTC instants consistent with lifecycle. |

A draft represents one complete validated configuration. Publication is one-way. Published rows cannot be updated or deleted. Editing means creating the next version.

### `UserStrategyComponent`

Flat system-plugin component mapped to `strategy.user_strategy_component`.

| Field | Rule |
|---|---|
| `userStrategyVersionId` | Composite version identity. |
| `position` | Non-negative storage/display position. |
| `strategyVersionId` | Required exact system catalog row; never another User Strategy or Composite. |
| `parameters` | Complete canonical component parameters. |
| `weight` | Unused/null for majority vote in F-004. |

A Composite has at least two components and no duplicate system Strategy version. Position is retained for storage/display but does not affect majority decision or canonical fingerprint.

### `StrategySnapshot`

Immutable application representation resolved for an authorized owner.

- Common: source User Strategy version ID, owner UUID, kind, version number, fingerprint.
- Single: exact plugin key, implementation version, durable Strategy version ID, complete parameters.
- Composite: exact policy key/version and a canonical order-independent list of exact system component references and complete parameters.

Root name, description, active/archive state, and current latest version are not fingerprint input and cannot change a resolved published snapshot.

## 5. Relationships

```text
Supabase Auth User (UUID)
    1
    └── owns 0..* UserStrategy (ULID)
                    1
                    └── contains 1..* UserStrategyVersion (ULID, versionNo)
                                         ├── SINGLE -> 1 StrategyDescriptor/strategy_version
                                         └── COMPOSITE -> 2..* UserStrategyComponent
                                                              └── each -> 1 StrategyDescriptor/strategy_version

Runtime StrategyRegistry
    └── pluginId + implementationVersion -> StrategyPlugin -> Strategy

F-003 CandleBatch (caller/test harness only)
    └── rolling closed Candle window -> StrategyContext -> StrategyDecision
```

## 6. Lifecycle and concurrency

### Root lifecycle

```text
ACTIVE ──archive(owner)──> ARCHIVED
ARCHIVED ──X──> ACTIVE
```

Archived roots are excluded from normal listings and reject new drafts/publications. Published versions remain owner-resolvable for provenance.

### Version lifecycle

```text
create complete validated configuration
                │
                v
              DRAFT ──publish(owner, expected state)──> PUBLISHED
                │                                      │
                └── replace with a new draft/version    └── immutable forever
```

Concurrent creation/publication based on the same prior state permits exactly one success. Unique `(user_strategy_id, version_no)` and conditional lifecycle change protect this rule. Persistence translates the stale operation to `STRATEGY_CONFLICT`; it does not silently retry with a new version number.

## 7. Canonical fingerprints

Format:

```text
strategy-v1:sha256:<64 lowercase hexadecimal characters>
```

Single fingerprint input includes kind, plugin key, implementation version, durable Strategy version ID, and the complete canonical parameter set.

Composite fingerprint input includes kind, policy key/version, canonical policy parameters, and components sorted by semantic Strategy key/version/durable ID. Component position, root display name, description, owner-facing archive status, and timestamps are excluded.

Canonical scalar encoding uses explicit type tags, length-prefixed UTF-8 text, normalized exact decimal text, and sorted field names. Any future encoding change creates a new fingerprint version rather than changing `strategy-v1`.

## 8. Existing PostgreSQL mapping

| Model | Existing table | Owner | Important enforcement |
|---|---|---|---|
| System descriptor | `strategy.strategy_version` | `strategy-core` | Unique plugin+version and fingerprint. |
| User Strategy root | `strategy.user_strategy` | `strategy-core` | Auth owner FK, active-name uniqueness, archive state. |
| User Strategy version | `strategy.user_strategy_version` | `strategy-core` | Kind shape, positive/unique version, lifecycle, immutable published trigger. |
| Composite component | `strategy.user_strategy_component` | `strategy-core` | System strategy FK, unique position/component, draft-only mutation. |

The baseline `strategy.composite_version` and `strategy.composite_component` represent the separate system-owned Composite catalog and are not written by the private User Strategy flow in F-004. No migration is planned. Existing migrations remain unchanged.

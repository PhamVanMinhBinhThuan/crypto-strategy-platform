# Data Model: F-015 Configurable Composite Search

## FrozenDatasetSummary

Owner-scoped read representation for selecting a dataset.

| Field | Rules |
|---|---|
| `datasetId` | Stable immutable identity |
| `provider` | Non-empty provider identity/version where available |
| `pair` | Canonical trading pair |
| `timeframe` | Supported canonical candle interval |
| `startTime` / `endTime` | UTC instants; half-open `[start, end)` and `start < end` |
| `candleCount` | Positive for a ready dataset |
| `checksum` | Canonical content checksum |
| `status` | Released dataset lifecycle state |
| `createdAt` | UTC creation instant |

An experiment references the immutable identity and freezes the full summary into its manifest. Dataset rows already referenced by experiments are never updated in place.

## StrategyPoolEntry

| Field | Rules |
|---|---|
| `artifactType` | Published built-in/user strategy or published composite |
| `strategyId` / `version` | Exact immutable registry identity |
| `displayName` | Presentation only; excluded from candidate identity where mutable |
| `parameterSchema` | Published typed schema for this exact version |
| `domains` | One normalized finite domain per searchable parameter; fixed parameters use one-value domains |

Pool identity is unique by `(artifactType, strategyId, version)`. Only artifacts executable through the existing strategy resolution boundary are eligible.

## ParameterDomain

Discriminated finite value domain:

- `choices`: non-empty ordered unique values matching the parameter type.
- `integerRange`: inclusive `min`, inclusive `max`, positive integer `step` with at least one value.
- `decimalRange`: canonical decimal `min`, `max`, positive `step`; values use exact decimal stepping and normalized text.

Domains are normalized to canonical value order before cardinality, hashing, and generation. Boolean/enum parameters normally use choices.

## CompositeSearchSpaceV2

| Field | Rules |
|---|---|
| `schemaVersion` | `2` |
| `pool` | Non-empty, canonically ordered unique entries |
| `minComponents` | At least one and no greater than `maxComponents` |
| `maxComponents` | No greater than pool size |
| `combinationPolicy` | Versioned policy descriptor; F-015 requires Majority Vote |
| `constraints` | Canonical supported constraints; every referenced component/parameter must exist |
| `cardinality` | Exact bounded integer when representable; otherwise safe capped/overflow form |
| `fingerprint` | Hash of canonical schema version, pool, domains, bounds, policy, and constraints |

Cardinality is the sum, for each permitted unique component subset, of the product of all domains belonging to selected components. Invalid candidates removed by constraints are excluded or safely accounted for by deterministic validation during traversal.

## CompositeCandidateDefinitionV2

| Field | Rules |
|---|---|
| `schemaVersion` | `2` |
| `generationIndex` | Monotonic zero-based logical position, unique in an experiment |
| `components` | Canonically ordered, non-empty exact strategy versions and exact parameter values |
| `combinationPolicy` | Exact policy ID/version and frozen configuration |
| `generator` | Generator ID/version reference |
| `fingerprint` | Deterministic hash of semantic candidate fields; excludes job/result/completion order |

Candidate definitions are immutable. A retry creates or advances an execution attempt for the same candidate, not a new definition.

## SearchRunV2

Existing durable Search Run extended/decoded with:

| Field | Rules |
|---|---|
| `searchSpace` | `CompositeSearchSpaceV2` for new runs; legacy flat space remains readable |
| `generatorState` | Versioned deterministic traversal state, including next logical position |
| `maximumCandidates` | Positive accepted budget |
| `topKTarget` | Positive leaderboard capacity; independent of execution window |
| `requestedConcurrency` | Positive requested window hint |
| `allocatedCount` | Number of immutable definitions durably accepted |
| `completedCount` | Successfully evaluated/ranked terminal candidates |
| `failedCount` | Terminally failed candidates |
| `activeCount` | `allocated - completed - failed`, never negative |
| `remainingCapacity` | `max(0, maximumCandidates - allocatedCount)` |
| `deadlineAt` | Frozen optional deadline |
| `noImprovementLimit` | Frozen optional terminal-outcome threshold |
| `status` / `terminalReason` | Durable lifecycle and explicit terminal explanation |
| `version` / `fence` | Optimistic concurrency and allocation ownership |

### State transitions

```text
QUEUED -> RUNNING -> STOP_REQUESTED -> STOPPED
                  -> SUCCEEDED
                  -> EXHAUSTED
                  -> FAILED
```

`RUNNING` may allocate only when every durable stop gate is false. Terminal states never allocate. In-flight completions may be reconciled after a stop request without reopening generation.

## ExperimentManifestV2

Freezes:

- dataset identity, provider, pair, timeframe, UTC range, candle count, checksum;
- every pool artifact identity/version/schema;
- normalized domains, component bounds, policy and constraints;
- generator ID/version, seed and initial state;
- maximum candidates, duration/deadline, no-improvement limit, Top-K;
- execution assumptions and metric/evaluator versions already required by existing manifests.
- one positive simulated initial capital plus fee/slippage rates in `[0, 1)`, shared by every Candidate Backtest in the Experiment.

Manifest version 1 remains immutable and readable. Reproduction of v2 copies the exact persisted candidate sequence rather than regenerating a time-limited run.

## SearchProgressView

| Field | Source |
|---|---|
| `allocated`, `completed`, `failed` | Durable run/candidate/job state |
| `active` | Derived authoritatively from durable counts |
| `remainingCapacity`, `maximumCandidates` | Durable budget |
| `bestScore` | Durable leaderboard/evaluation projection |
| `topK` | Frozen manifest target |
| `startedAt`, `elapsed`, `deadlineAt` | Durable timestamps plus current read time |
| `status`, `terminalReason` | Durable Search/Experiment lifecycle |
| `revision` | Monotonic snapshot/realtime recovery version |

## CompositeLeaderboardEntry

Existing deterministic rank/score projection enriched with:

- `candidateId`, `backtestId`, `evaluationId`, `candidateFingerprint`;
- compact component display summary;
- Total Return, Win Rate, Maximum Drawdown, Number of Trades;
- optional detail link/reference, not duplicated browser-derived evidence.

Ordering remains the Leaderboard owner's deterministic score/tie-break contract. Top-K replacement is idempotent by candidate/evaluation identity.

## Compatibility mapping

- Legacy flat `SearchSpace(parameters)` maps to one implicit component from the v1 manifest for reads only.
- Legacy candidates with only a parameter set resolve their component identity from the immutable v1 manifest.
- New Start Search requests always persist schema version 2.
- Queue envelopes remain backward readable; a v2 candidate payload carries the complete immutable candidate definition or a stable reference that the Backtest execution resolver loads authoritatively.

# REST API Contract: F-015 Search Configuration and Reads

All endpoints reuse F-009 authentication, owner-scoped not-found behavior, idempotency, error envelopes, request IDs, and versioning rules. Timestamps are ISO-8601 UTC instants. New write requests use the v2 shape; existing v1 responses remain readable during the compatibility window.

## List frozen datasets

`GET /api/v1/datasets?status=READY&pair={pair?}&timeframe={timeframe?}&cursor={cursor?}&limit={limit?}`

Response:

```json
{
  "items": [
    {
      "datasetId": "uuid",
      "provider": "binance",
      "pair": "BTC/USDT",
      "timeframe": "1h",
      "startTime": "2026-01-01T00:00:00Z",
      "endTime": "2026-07-01T00:00:00Z",
      "candleCount": 4344,
      "checksum": "sha256:...",
      "status": "READY",
      "createdAt": "2026-07-01T00:01:00Z"
    }
  ],
  "nextCursor": null
}
```

The result contains only datasets the caller may access. Ordering is deterministic (`createdAt DESC`, then stable identity).

## Create a frozen dataset

`POST /api/v1/datasets`

Headers: the released idempotency-key contract applies.

```json
{
  "pair": "BTC/USDT",
  "timeframe": "1h",
  "startTime": "2026-01-01T00:00:00Z",
  "endTime": "2026-07-01T00:00:00Z"
}
```

Returns `201` for a newly accepted snapshot or the released replay response for the same logical command. The response is a complete `FrozenDatasetSummary`. Validation failures identify `pair`, `timeframe`, `startTime`, or `endTime`; provider failures use safe retryable public errors.

## List available Search generators

`GET /api/v1/search/generators`

```json
{
  "items": [
    {
      "generatorId": "random-search",
      "version": "1.0.0",
      "displayName": "Random Search",
      "configurationSchema": {
        "seed": { "type": "integer", "required": true }
      }
    }
  ]
}
```

Only registered executable generators are returned. UI labels never activate an absent implementation.

## Start composite Search

`POST /api/v1/experiments`

The existing idempotency contract applies. F-015 adds request discriminator `configurationVersion: 2`.

```json
{
  "configurationVersion": 2,
  "datasetId": "uuid",
  "backtestConfiguration": {
    "initialCapital": "10000",
    "feeRate": "0.001",
    "slippageRate": "0.0005"
  },
  "searchSpace": {
    "schemaVersion": 2,
    "strategyPool": [
      {
        "artifactType": "BUILT_IN",
        "strategyId": "moving-average-crossover",
        "version": "1.0.0",
        "parameterDomains": {
          "fastPeriod": { "kind": "INTEGER_RANGE", "min": 10, "max": 30, "step": 10 },
          "slowPeriod": { "kind": "CHOICES", "values": [50, 100, 200] }
        }
      },
      {
        "artifactType": "BUILT_IN",
        "strategyId": "rsi",
        "version": "1.0.0",
        "parameterDomains": {
          "period": { "kind": "CHOICES", "values": [7, 14, 21] }
        }
      }
    ],
    "minComponents": 1,
    "maxComponents": 2,
    "combinationPolicy": { "policyId": "majority-vote", "version": "1.0.0", "configuration": {} },
    "constraints": [
      { "kind": "PARAMETER_LT", "left": "moving-average-crossover.fastPeriod", "right": "moving-average-crossover.slowPeriod" }
    ]
  },
  "generator": { "generatorId": "random-search", "version": "1.0.0", "seed": 42 },
  "stopConditions": {
    "maximumCandidates": 100,
    "maximumDurationSeconds": 43200,
    "maximumWithoutImprovement": 50
  },
  "topK": 10,
  "requestedConcurrency": 4
}
```

`backtestConfiguration` is additive and optional for compatibility. When omitted, the server uses the released defaults. Values are exact decimal strings; `initialCapital` must be positive and both rates must be in `[0, 1)`. The server freezes `LONG_ONLY`, `NEXT_CANDLE_OPEN`, final-position close, and `HALF_EVEN` for the MVP.

Accepted response:

```json
{
  "experimentId": "uuid",
  "searchRunId": "uuid",
  "status": "QUEUED",
  "configurationVersion": 2,
  "configurationFingerprint": "sha256:...",
  "monitorPath": "/search/uuid"
}
```

The server resolves and freezes dataset and strategy artifacts. Client-supplied display names, checksums, schemas, cardinalities, or lifecycle states are ignored/rejected rather than trusted.

## Read Search progress

`GET /api/v1/experiments/{experimentId}` returns the existing experiment representation plus:

```json
{
  "search": {
    "allocated": 20,
    "active": 4,
    "completed": 15,
    "failed": 1,
    "remainingCapacity": 80,
    "maximumCandidates": 100,
    "topK": 10,
    "bestScore": "84.10000000",
    "startedAt": "2026-09-05T01:00:00Z",
    "deadlineAt": "2026-09-05T13:00:00Z",
    "status": "RUNNING",
    "terminalReason": null,
    "revision": 27
  }
}
```

`active = allocated - completed - failed`. Counts are authoritative and non-negative. `remainingCapacity` refers to unallocated budget, not predicted successful results.

## Read composite leaderboard

`GET /api/v1/experiments/{experimentId}/leaderboard`

Each existing item adds the authoritative fields below:

```json
{
  "rank": 1,
  "candidateId": "uuid",
  "backtestId": "uuid",
  "evaluationId": "uuid",
  "score": "84.10000000",
  "candidateFingerprint": "sha256:...",
  "candidateSummary": "Moving Average Crossover + RSI",
  "metrics": {
    "totalReturn": "1.42500000",
    "winRate": "0.58200000",
    "maximumDrawdown": "0.12400000",
    "numberOfTrades": 1245
  }
}
```

The list length is at most the manifest Top-K. Sharpe Ratio is not part of F-015.

## Read candidate detail

`GET /api/v1/experiments/{experimentId}/candidates/{candidateId}`

Returns the immutable candidate definition, generator/generation index, exact component strategy versions and parameters, combination policy, fingerprint, dataset provenance, Backtest identity/status, authoritative metrics when available, and metric version. It never returns secret provider payloads or internal exception details.

## Stop and reproduce

Existing F-009/F-010 endpoints and command semantics remain authoritative. F-015 requires them to accept v2 experiments without changing idempotency, ownership, stop-race, or async reproduction rules.

## Compatibility

- Requests without `configurationVersion: 2` retain the released legacy mapping during the compatibility window.
- Existing response fields are not removed or retyped.
- New fields are additive and clients must tolerate absence for historical records.
- Unsupported v2 policies/generators/domains return stable validation codes; they never silently downgrade to v1.

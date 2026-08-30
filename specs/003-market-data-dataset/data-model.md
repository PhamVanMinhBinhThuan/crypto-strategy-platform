# Data Model: Market Data and Dataset

**Feature**: F-003 Market Data and Dataset  
**Date**: 2026-08-29

## Ownership and placement

Market Data is the single semantic owner of Asset, Trading Pair, Candle, Timeframe, provider normalization, and Dataset evidence. Stable provider-neutral values are placed in `modules/domain` because later Backtest and News capabilities may depend on `domain` but are not allowed to depend on `market-data`. This placement grants read/use access only; Market Data alone owns creation policy, durable Market writes, and the five `market.*` tables.

Dataset/application models, use cases, ports, errors, and realtime events remain in `modules/market-data`. Physical row mappings remain in `modules/persistence`.

## Identity conventions

| Identity | Canonical form | Durable mapping | Rule |
|---|---|---|---|
| Asset ID | Typed uppercase Crockford ULID | `market.asset.asset_id varchar(26)` | Application generated; immutable |
| Trading Pair ID | Typed uppercase Crockford ULID | `market.trading_pair.trading_pair_id varchar(26)` | Application generated; immutable |
| Candle ID | Typed uppercase Crockford ULID | `market.candle.candle_id varchar(26)` | Surrogate durable reference; immutable |
| Candle natural key | provider + pair + timeframe + open time | Existing unique constraint | Logical deduplication identity |
| Dataset Version ID | Typed uppercase Crockford ULID | `market.dataset_version.dataset_version_id varchar(26)` | Stable downstream Dataset identity |
| Authenticated User ID | UUID | Supabase `auth.users.id` | Not owned or used for Market ownership by F-003 |

Typed business IDs validate their full 26-character format at construction. Raw strings do not cross public Market ports as accepted identities.

## Canonical entities and values

### Asset

Represents one canonical traded or quoted asset.

| Field | Meaning | Validation |
|---|---|---|
| `assetId` | Stable Market business identity | Valid typed ULID |
| `symbol` | Canonical code such as `BTC` | Nonblank uppercase alphanumeric |
| `name` | Optional display name | Blank is normalized to absent |
| `active` | Whether new Market operations may select it | Required boolean |

Identity is `assetId`; business uniqueness is `symbol`.

### Trading Pair

Represents the ordered base/quote market scope.

| Field | Meaning | Validation |
|---|---|---|
| `tradingPairId` | Stable Market business identity | Valid typed ULID |
| `baseAsset` | Asset being priced | Required canonical Asset reference |
| `quoteAsset` | Asset in which price is expressed | Required canonical Asset reference; different from base |
| `canonicalSymbol` | `BASE/QUOTE`, such as `BTC/USDT` | Derived from Asset symbols; never accepted from provider payload unchanged |
| `active` | Whether new Market operations may select it | Required boolean |

Business uniqueness is ordered `(baseAssetId, quoteAssetId)`. The baseline compact `symbol` column is a persistence mapping such as `BTCUSDT`; it is never exposed as the canonical pair value.

### Market Provider

A stable uppercase provider code. F-003 defines `BINANCE` and a test-only fixture provider. Provider values participate in Candle identity and checksum input. Provider-specific host, symbol, interval, payload, and errors are not part of this value.

### Timeframe

| Code | Fixed UTC duration |
|---|---:|
| `1m` | 1 minute |
| `5m` | 5 minutes |
| `15m` | 15 minutes |
| `30m` | 30 minutes |
| `1h` | 1 hour |
| `2h` | 2 hours |
| `4h` | 4 hours |
| `1d` | 24 hours |

Timeframe owns:

- code validation;
- fixed duration;
- UTC boundary/alignment checks;
- next expected open time;
- exclusive interval end from an aligned open time.

Requests with non-aligned boundaries are rejected rather than rounded.

### Candle Key

| Field | Rule |
|---|---|
| `provider` | Canonical uppercase provider |
| `tradingPair` | Canonical `BASE/QUOTE` value |
| `timeframe` | One supported Timeframe |
| `openTime` | Aligned UTC instant |

The Candle Key is the canonical logical identity used for historical overlap, realtime deduplication, reconnect merge, and persistence conflict handling.

### Candle

Represents one canonical OHLCV state for an interval. It does not require a surrogate Candle ID until persisted.

| Field | Meaning | Validation/canonicalization |
|---|---|---|
| `key` | Natural identity | Required Candle Key |
| `closeTime` | Exclusive canonical interval boundary | Exactly `openTime + timeframe`; after open time |
| `open` | Opening price | Exact decimal; nonnegative; fits `numeric(30,12)` without rounding |
| `high` | Highest price | Exact decimal; ≥ open, low, close |
| `low` | Lowest price | Exact decimal; ≤ open, high, close |
| `close` | Closing/current price | Exact decimal; nonnegative |
| `volume` | Base volume | Exact decimal; nonnegative |
| `closed` | Finality | Required; only `true` can persist or join a Dataset |

Equivalent decimal values ignore insignificant trailing zero scale. Provider timestamps are parsed as UTC. Binance's inclusive final millisecond is validated against the interval, then normalized to the exclusive boundary.

### Persisted Candle

Combines a `candleId` with one accepted closed Candle. It is write-once. The same natural key and same content resolves idempotently to the existing record. The same key with different canonical content is an integrity conflict and never updates the record.

## Query and realtime models

### Historical Candle Query

| Field | Rule |
|---|---|
| `provider` | Supported configured provider |
| `tradingPair` | Active canonical pair |
| `timeframe` | Supported Timeframe |
| `startTime` | Inclusive, aligned UTC instant |
| `endTime` | Exclusive, aligned UTC instant; after start |
| `collectionCutoff` | Explicit UTC instant used to prove historical closure |

Every returned Candle has `openTime ∈ [startTime,endTime)`. A historical interval is closed only when its exclusive canonical end is at or before `collectionCutoff`.

### Realtime Candle Query

Contains provider, Trading Pair, and Timeframe. The registry key is exactly this tuple. Multiple consumers of the same key share one upstream subscription.

### Candle Update

Contains canonical Candle plus a canonical provider-event instant used only to order competing updates for the same Candle Key. Provider-event time is not Candle chronology, Dataset membership, or checksum input.

Rules:

- a newer open update may replace an older open update for the same key;
- exact duplicates collapse;
- a closed update is final;
- no open/older update may replace a closed update;
- only closed updates pass to durable persistence.

### Connection State

```text
CONNECTING -> CONNECTED
CONNECTED -> RECONNECTING
RECONNECTING -> CONNECTED
CONNECTING|CONNECTED|RECONNECTING -> DISCONNECTED
```

`DISCONNECTED` follows final cancellation, unrecoverable configuration/mapping failure, or exhausted policy. A transient transport loss first reports `RECONNECTING`. The adapter must not report `CONNECTED` after reconnect until overlap backfill and continuity verification succeed.

### Candle Subscription

A closeable handle associated with one internal consumer. Closing the final handle for a registry key releases the upstream stream and cancels pending reconnect/backfill. It contains no browser connection or public WebSocket protocol state.

## Dataset models

### Dataset Version

An immutable accepted snapshot for one market scope.

| Field | Durable column | Validation |
|---|---|---|
| `datasetVersionId` | `dataset_version_id` | Typed ULID |
| `version` | `version` | Java `String`; exact Dataset/checksum canonicalization contract ID `candle-v1` for F-003 |
| `provider` | `provider` | Canonical uppercase provider |
| `tradingPairId` | `trading_pair_id` | Existing canonical pair reference |
| `timeframe` | `timeframe` | Supported canonical code |
| `normalizationVersion` | `normalization_version` | Nonblank provider mapping provenance |
| `rangeStart` | `range_start` | Inclusive aligned UTC instant |
| `rangeEnd` | `range_end` | Exclusive aligned UTC instant; after start |
| `candleCount` | `candle_count` | Positive; equals membership size |
| `checksum` | `checksum` | Lowercase `sha256:` plus 64 hex characters; globally unique |
| `createdAt` | `created_at` | UTC instant assigned at finalization |

There is no separate Dataset root entity in the baseline. `datasetVersionId` is the stable Dataset identity used downstream.

The `version` column is not a numeric business revision and is not modeled as an enum. F-003 uses the `String` value `candle-v1` to identify the canonical Dataset/checksum contract. `normalizationVersion` independently identifies provider-to-canonical mapping provenance. Introducing a separate Dataset business-revision concept requires a later contract/schema decision.

### Dataset Snapshot and paged Candle reads

`DatasetSnapshot` contains only Dataset Version metadata from the table above. It never contains a Candle or membership collection.

Membership is read through:

```text
DatasetCandleReader.readCandles(datasetId, fromSequence, batchSize) -> CandleBatch
```

- `fromSequence` is zero-based and inclusive;
- `batchSize` is in `[1,5000]`;
- members include their `sequenceNo` and persisted canonical Candle and are ordered ascending;
- `CandleBatch.nextSequence` is the first sequence not returned;
- `CandleBatch.hasMore` states whether another nonempty batch exists;
- `fromSequence == candleCount` yields an empty terminal batch; a larger value is invalid.

Finalized Dataset membership is immutable, so sequence continuation is stable without an opaque cursor. Integrity verification streams these bounded batches into validation and checksum state rather than storing membership in `DatasetSnapshot`.

### Dataset Membership

| Field | Durable column | Validation |
|---|---|---|
| `datasetVersionId` | `dataset_version_id` | Matches parent Dataset Version |
| `sequenceNo` | `sequence_no` | Zero-based contiguous integer |
| `candleId` | `candle_id` | Unique within Dataset; references accepted closed Candle |

Membership order is strictly ascending Candle `openTime`. Every member matches parent provider/pair/timeframe and has `openTime ∈ [rangeStart,rangeEnd)`. The next member opens exactly one Timeframe after the previous; every expected interval appears once.

### Dataset Checksum Contract

`candle-v1` computes SHA-256 over a versioned canonical byte stream described in [checksum-candle-v1.md](contracts/checksum-candle-v1.md). Dataset provenance is compared separately during idempotency/integrity verification.

### Dataset construction lifecycle

```text
REQUESTED
  -> FETCHED_COMPLETE
  -> VALIDATED_COMPLETE
  -> FINALIZED

Any pre-finalization state -> REJECTED
```

These are application-flow states, not new database status values. Only `FINALIZED` is exposed as a Dataset Version. A transaction failure exposes no accepted partial Dataset.

Finalized Dataset Version and Membership have no update/delete transition through F-003 ports.

## Relationships

```text
Asset(base)  ─┐
              ├── Trading Pair ──< Persisted Candle
Asset(quote) ─┘                        ^
                                      |
Dataset Version ──< Dataset Membership+
```

Downstream `experiment.experiment_manifest.dataset_version_id` may reference Dataset Version. That reference does not grant Experiment write access to Market tables.

## Cross-record invariants

The application/persistence transaction verifies what the existing row-local constraints cannot:

1. Only closed Candles persist.
2. A Dataset member has the same provider, pair, timeframe, and range as its parent.
3. Sequence numbers are contiguous from zero and agree with ascending open time.
4. Every expected interval is present exactly once.
5. `candleCount` equals membership size.
6. Recomputed `candle-v1` checksum equals stored checksum.
7. Existing checksum is reusable only if version, normalization provenance, scope, range, count, and full membership are equal.
8. Same Candle natural key with different canonical content is rejected.
9. Finalized metadata/membership expose no mutation operation.

## Persistence mapping notes

- All SQL fully qualifies `market.*`.
- Compact `trading_pair.symbol` is mapped from base+quote for storage; reads reconstruct the canonical pair through Asset joins.
- Exact decimals must fit `numeric(30,12)` without rounding.
- Timestamps use `timestamptz` and UTC `Instant` conversion.
- Dataset Version plus membership finalizes in one transaction. The approved implementation may include new Candle inserts in the same transaction to satisfy atomic construction.
- Existing PK/UQ/check/FK constraints remain unchanged; no migration is planned.

## Unsupported revision behavior

The current schema cannot store two canonical Candle values with the same natural key. A provider correction or new normalization that changes accepted OHLCV at that key produces an integrity conflict. A future Candle-revision model requires a separate specification, architecture decision, and forward migration; F-003 never overwrites existing evidence.

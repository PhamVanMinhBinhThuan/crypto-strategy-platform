# Contract: Market Data Capability Boundary v1

**Contract ID**: `market-data-provider-v1`  
**Owner**: `modules/market-data`  
**Consumers**: composition roots and later capability integrations through published APIs only

## Boundary rules

- All inputs/outputs use canonical domain values.
- No Binance symbol, interval enum, array tuple, JSON model, HTTP status/body, WebSocket frame, SDK type, or transport exception crosses this boundary.
- Provider and persistence dependencies are output ports owned by Market Data.
- Public browser REST/WebSocket DTOs are not part of this contract.
- Contract changes that alter canonical meaning, closure, ordering, or error semantics require a new version and ADR review when architectural meaning changes.

## Input use cases

### Load historical Candles

```text
loadHistoricalCandles(HistoricalCandleQuery) -> HistoricalCandleBatch
```

Preconditions:

- provider and pair are configured/active;
- Timeframe is one of the eight canonical values;
- start/end are aligned UTC instants and `start < end`;
- range is `[start,end)`;
- explicit collection cutoff is present;
- configured page/range bounds are not exceeded.

Postconditions:

- the operation returns only after all pages complete;
- Candles are canonical, sorted by open time, exact-duplicate-free, and scoped to the query;
- historical Candles are closed at the supplied cutoff;
- partial provider pages are never returned as a successful batch;
- unresolved interval gaps return `MARKET_DATA_GAP`.

### Create Dataset

```text
createDataset(CreateDatasetCommand) -> DatasetVersion
```

The command contains the historical query, normalization version, and checksum contract version. The use case obtains complete canonical Candles, validates continuity, builds zero-based membership, calculates checksum, and requests atomic finalization.

Equivalent concurrent/repeated commands return the existing Dataset only when full provenance and ordered membership match. Otherwise the result is an integrity conflict.

### Get/verify Dataset

```text
getDataset(DatasetVersionId) -> DatasetSnapshot
readCandles(DatasetVersionId datasetId, int fromSequence, int batchSize) -> CandleBatch
verifyDataset(DatasetVersionId) -> DatasetIntegrityResult
```

`DatasetSnapshot` contains metadata only and never embeds Candles or membership. Its `version` field is a `String` containing the Dataset/checksum canonicalization contract ID (`candle-v1` for F-003); it is not a numeric revision or enum.

`DatasetCandleReader.readCandles` has the following contract:

- `datasetId` is the immutable Dataset Version identity;
- `fromSequence` is the zero-based inclusive membership sequence to read and must be nonnegative;
- `batchSize` must be between 1 and 5,000 inclusive;
- results are ordered by ascending `sequenceNo` and contain at most `batchSize` members;
- `CandleBatch` contains `datasetId`, `fromSequence`, ordered members with their sequence numbers and persisted canonical Candles, `nextSequence`, and `hasMore`;
- `nextSequence` is the first sequence not returned and is safe as the next call's `fromSequence`;
- `fromSequence == candleCount` returns an empty terminal batch with `hasMore=false`; a sequence beyond `candleCount` is `INVALID_MARKET_QUERY`;
- unsupported batch sizes and negative sequences are `INVALID_MARKET_QUERY`;
- Dataset immutability makes continuation stable; no opaque cursor or snapshot-wide Candle materialization is used.

Verification reads successive bounded `CandleBatch` values and incrementally recalculates scope, range, continuity, count, and checksum. Missing/corrupt evidence is never returned as a valid Dataset.

### Subscribe to realtime Candles

```text
subscribeCandles(RealtimeCandleQuery, CandleUpdateHandler, ConnectionStateHandler)
    -> CandleSubscription
```

The query contains provider, pair, and Timeframe. The closeable handle belongs to one internal consumer.

Behavior:

- identical provider/pair/timeframe queries share one upstream stream within the process;
- connection state is delivered through the stable state contract;
- open updates may be emitted transiently;
- closed updates are final and persist idempotently;
- duplicate/stale updates collapse;
- an open/older update never replaces a closed update;
- closing the final handle releases upstream state and cancels pending recovery.

## Provider output port

```text
MarketDataProvider
  providerId() -> MarketProvider
  normalizationVersion() -> String
  loadHistorical(HistoricalCandleQuery) -> HistoricalCandleBatch
  subscribe(RealtimeCandleQuery, ProviderUpdateHandler, ProviderStateHandler)
      -> CandleSubscription
```

Provider implementations prove closure differently:

- Binance historical: validated interval end at or before explicit cutoff;
- Binance realtime: final-kline flag;
- Fixture: versioned fixture metadata.

The provider contract does not require every provider to expose a native historical finality flag.

## Historical pagination semantics

- Cursor is the next expected canonical open time.
- Provider page size and maximum page/range count are bounded configuration.
- The adapter must make progress; repeated non-progress pages fail.
- Overlapping boundary records are intentional and deduplicated by Candle Key.
- Failure after one or more pages fails the entire provider operation.

## Realtime state contract

| State | Meaning |
|---|---|
| `CONNECTING` | Establishing the first upstream stream |
| `CONNECTED` | Stream active and any required recovery continuity verified |
| `RECONNECTING` | Transport lost; bounded reconnect/backfill active |
| `DISCONNECTED` | Subscription cancelled or recovery/configuration irrecoverably stopped |

Allowed transitions:

```text
CONNECTING -> CONNECTED
CONNECTED -> RECONNECTING
RECONNECTING -> CONNECTED
CONNECTING|CONNECTED|RECONNECTING -> DISCONNECTED
```

The adapter must not emit `CONNECTED` after a reconnect until historical overlap backfill, buffered update merge, deduplication, and gap validation succeed.

## Stable errors

| Code | Meaning | Retry classification |
|---|---|---|
| `INVALID_MARKET_QUERY` | Unsupported/invalid provider, pair, timeframe, range, cutoff, or bound | Never retry unchanged request |
| `MARKET_PROVIDER_UNAVAILABLE` | Timeout, I/O, eligible provider `5xx`, or disconnected provider | Retry only under bounded policy |
| `MARKET_PROVIDER_RATE_LIMITED` | Provider `429`/`418` or equivalent | Honor provider delay; bounded retry |
| `MARKET_DATA_GAP` | Required interval continuity cannot be restored | Retry only through explicit recovery/new request |
| `MARKET_DATA_MAPPING_FAILED` | Provider payload cannot form a canonical Candle | Do not retry same payload |
| `MARKET_DATA_INTEGRITY_CONFLICT` | Same Candle/checksum identity has different accepted content/provenance | Do not overwrite; operator/review action |
| `DATASET_NOT_FOUND` | Requested Dataset Version does not exist | Not retryable unless caller expects eventual creation |
| `DATASET_INTEGRITY_FAILED` | Stored count/order/scope/checksum is inconsistent | Do not use Dataset |

Errors may retain a safe provider code and market scope for internal diagnostics. They never contain raw provider bodies, credentials, database details, class names, or stack traces in caller-facing form.

## Binance mapping contract

| Canonical value | Binance representation |
|---|---|
| `BTC/USDT` | configured/validated compact symbol `BTCUSDT` for REST; lowercase in stream name |
| canonical Timeframe code | matching Binance interval code |
| UTC `Instant` | epoch milliseconds on provider boundary |
| exact decimal | provider numeric string parsed directly to exact decimal |
| canonical close time | exclusive interval boundary after provider close timestamp validation |
| realtime `closed` | Binance final-kline flag |

The REST adapter uses public `GET /api/v3/klines`; the stream adapter uses kline streams. Endpoint hosts are runtime configuration, not canonical domain values.

## Contract verification suite

Run the same behavioral suite against:

1. Binance adapter with deterministic fake REST/stream transports and raw versioned fixtures.
2. Provider-neutral fixture provider.

The suite covers canonical mapping, full pagination, closure, stable errors, duplicate/conflict rules, subscription lifecycle, connection states, reconnect/backfill, and cancellation. It never calls live Binance.

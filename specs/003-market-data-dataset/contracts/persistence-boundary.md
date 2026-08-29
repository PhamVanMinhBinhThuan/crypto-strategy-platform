# Contract: Market Data Persistence Boundary v1

**Owner of ports and data**: `modules/market-data`  
**Adapter implementation**: `modules/persistence`  
**Physical schema**: existing PostgreSQL `market` schema

## Boundary rules

- Market Data application code depends only on these output ports.
- JDBC, SQL, DataSource, transactions, row mappers, constraint names, and PostgreSQL exceptions remain inside `modules/persistence`.
- Every physical table reference is schema-qualified.
- The adapter exposes no Dataset/Candle update or delete operation.
- Only closed Candles are accepted by durable operations.
- No applied migration is edited and no new migration is planned.

## Market reference data port

Responsibilities:

- resolve Asset by canonical symbol or ID;
- resolve Trading Pair by ordered base/quote or ID;
- atomically create missing active Asset/Trading Pair references owned by Market Data when a configured canonical pair is first accepted;
- map compact storage symbol to/from canonical base/quote Asset relationship;
- reject a compact symbol or pair relationship that conflicts with an existing record.

The canonical pair is always reconstructed from joined base/quote Assets. `trading_pair.symbol` is not returned as canonical `BASE/QUOTE`.

## Closed Candle store port

Conceptual operations:

```text
saveClosed(Candle) -> PersistedCandle
saveClosedBatch(List<Candle>) -> List<PersistedCandle>
findRange(provider, tradingPairId, timeframe, startInclusive, endExclusive)
    -> ordered List<PersistedCandle>
```

### Insert/idempotency behavior

For each natural key `(provider,tradingPairId,timeframe,openTime)`:

1. Reject open or invalid Candle before SQL.
2. Attempt insert with an application-generated typed Candle ULID.
3. If the natural-key conflict occurs, load the accepted row.
4. Return it only when close time and OHLCV are canonically equal.
5. Otherwise return `MARKET_DATA_INTEGRITY_CONFLICT` and never issue an update.

Batch results preserve canonical open-time order. A batch used for Dataset finalization participates in the same transaction as Dataset metadata/membership.

## Dataset store port

Conceptual operations:

```text
finalizeAtomically(DatasetFinalization) -> DatasetSnapshot
find(DatasetVersionId) -> DatasetSnapshot?
findByChecksum(Checksum) -> DatasetSnapshot?
readCandles(DatasetVersionId datasetId, int fromSequence, int batchSize) -> CandleBatch
verify(DatasetVersionId) -> DatasetIntegrityResult
```

`DatasetSnapshot` is metadata-only. `readCandles` is implemented by the separate `DatasetCandleReader` port and uses a zero-based inclusive `fromSequence`, a `batchSize` from 1 through 5,000, and deterministic `ORDER BY sequence_no`. It returns at most the requested number of members plus `nextSequence`/`hasMore`; it never loads complete membership into Dataset metadata.

`DatasetFinalization` contains:

- any new closed Candles;
- Dataset Version identity/version/provenance/range/count/checksum;
- zero-based ordered membership by canonical Candle.

### Atomic finalization

One PostgreSQL transaction:

1. resolves/creates Market references;
2. inserts or validates every closed Candle idempotently;
3. inserts `dataset_version`;
4. inserts every `dataset_candle` row;
5. reloads the accepted metadata and verifies membership through bounded `DatasetCandleReader` pages before commit.

Any error rolls back every new record in that finalization. No accepted partial Dataset is visible.

### Equivalent/concurrent finalization

The baseline globally enforces unique checksum. On checksum conflict:

1. reload the committed winner;
2. compare Dataset contract version, provider, Trading Pair, Timeframe, normalization version, range, count, checksum, and exact ordered membership through bounded pages;
3. return the winner only when all values match;
4. otherwise return `MARKET_DATA_INTEGRITY_CONFLICT`.

This comparison is required even though the digest matches. A digest never authorizes metadata aliasing.

## Read-time integrity verification

Dataset metadata reads never materialize membership. Integrity verification repeatedly calls `readCandles(datasetId, fromSequence, batchSize)`, uses `ORDER BY sequence_no`, incrementally feeds the checksum digest, and verifies:

- sequence begins at zero and is contiguous;
- no Candle repeats;
- Candle open times strictly increase by one Timeframe;
- every Candle is closed and matches parent provider/pair/timeframe/range;
- stored `candle_count` equals membership size;
- recalculated checksum matches stored checksum;
- the checksum contract and normalization version are supported.

Failure returns `DATASET_INTEGRITY_FAILED`; corrupt evidence is not returned as a valid Dataset.

## Existing physical mappings

| Canonical model | Existing table/constraint |
|---|---|
| Asset | `market.asset`; unique uppercase symbol |
| Trading Pair | `market.trading_pair`; unique base+quote and compact symbol; base != quote |
| Persisted Candle | `market.candle`; unique provider+pair+timeframe+open time; OHLCV/time checks |
| Dataset Version | `market.dataset_version`; unique checksum; range/count/checksum checks |
| Membership | `market.dataset_candle`; PK dataset+sequence; unique dataset+candle |

Use UTC `Instant`/`timestamptz` mapping and exact decimal/`numeric(30,12)` mapping without rounding.

## Immutability and corrections

- Public ports have no update/delete/member mutation method.
- Same-key/different-value Candle correction is rejected.
- Finalized Dataset metadata and membership are write-once.
- Direct privileged SQL is an operational trust boundary and is not a second application write path.
- A true Candle-revision model needs a separate spec/ADR and forward migration.

## Transaction/failure tests

The local PostgreSQL integration suite must verify:

- exact duplicate and conflicting Candle insert;
- overlapping/concurrent Candle batches;
- full rollback after a membership failure;
- concurrent equivalent Dataset finalization;
- same checksum with different provenance/membership conflict;
- ordered range reads and all integrity checks;
- no persistence API for mutating accepted evidence.

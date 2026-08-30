# Quickstart Validation: Market Data and Dataset

This guide describes the commands and expected evidence after F-003 implementation. It does not authorize implementation, remote database mutation, or live Binance testing during planning.

## Prerequisites

- JDK 21
- Repository Gradle wrapper
- For the separate persistence suite only: Docker-compatible runtime and Supabase CLI using the repository's local configuration
- No Binance API key, Supabase service-role key, Redis, Strategy, Backtest, or authentication fixture is required

## 1. Confirm governance gates

Before implementation merge, verify:

- ADR-0001, ADR-0002, ADR-0003, ADR-0007, and ADR-0009 are `Accepted`;
- the F-002 architecture rule permits typed ULID platform business IDs while retaining UUID for authenticated user identity;
- the approved scope rejects conflicting provider corrections and makes no schema change.

Planning may be reviewed before these gates close; implementation must not silently bypass them.

## 2. Run the default offline verification

PowerShell:

```powershell
.\gradlew.bat clean check
```

POSIX shell:

```bash
./gradlew clean check
```

Expected outcome:

- all domain, Market Data, provider-contract, checksum, composition, and architecture tests pass;
- no live Binance, database, Redis, queue, or authentication dependency is contacted;
- all existing F-002 tests remain green.

## 3. Run focused deterministic suites

```bash
./gradlew :modules:domain:test
./gradlew :modules:market-data:test
./gradlew :modules:persistence:test
./gradlew :apps:api:test
./gradlew :architecture-tests:test
```

Expected evidence:

- canonical Asset/Pair/Timeframe/Candle validation;
- historical closure using explicit cutoff;
- UTC and exact decimal normalization;
- Binance raw fixture mapping and stable error translation;
- full bounded pagination with overlap/no-progress/partial-failure cases;
- provider contract parity between Binance-with-fake-transports and fixture provider;
- shared realtime subscription, open/closed ordering, disconnect/backfill, and final-subscriber cancellation;
- deterministic membership and `candle-v1` golden checksum vectors;
- 100 shuffled/overlapping construction repetitions with one logical result;
- architecture rules reject provider/persistence leakage and accept typed Market ULIDs.

## 4. Validate controlled realtime recovery

Run the focused Market Data recovery tests with fixed clock, scheduler, backoff, jitter, REST pages, and stream frames:

```bash
./gradlew :modules:market-data:test --tests "*RealtimeRecovery*"
```

Expected outcome:

- states follow `CONNECTED -> RECONNECTING -> CONNECTED`;
- overlap backfill begins from the last confirmed closed Candle;
- buffered stream and REST records merge without duplicate closed Candles;
- a stale/open update never replaces a closed Candle;
- unresolved continuity reports `MARKET_DATA_GAP`;
- controlled recovery completes within 30 simulated seconds after availability returns;
- closing the final subscriber cancels pending recovery.

## 5. Validate Dataset/checksum determinism

```bash
./gradlew :modules:market-data:test --tests "*Dataset*" --tests "*Checksum*"
```

Expected outcome:

- membership is zero-based, contiguous, complete, and ordered by open time;
- equivalent decimals/UTC instants produce identical canonical bytes;
- shuffled/duplicate/overlapping input produces the same digest;
- changing checksum-relevant data or contract marker changes the digest;
- accepted evidence has no mutation path;
- same checksum with mismatched provenance/membership is an integrity conflict.

See [checksum-candle-v1.md](contracts/checksum-candle-v1.md).

## 6. Run local PostgreSQL/Supabase integration evidence

Start/reset only the local stack; never use shared-development credentials for this suite:

```bash
supabase start
supabase db reset
./gradlew marketDataIntegrationTest
supabase test db
```

Expected outcome:

- repository migrations create the authoritative schema;
- JDBC mapping preserves ULID, exact decimal, and UTC values;
- exact duplicate Candle inserts are idempotent;
- conflicting Candle content is rejected without overwrite;
- Dataset Version and membership finalize atomically;
- forced membership failure rolls back the finalization;
- concurrent equivalent finalization returns one logical Dataset;
- count/order/scope/checksum integrity verification passes for valid evidence and rejects controlled tampering;
- existing SQL baseline tests remain green.

See [database-verification.md](contracts/database-verification.md).

## 7. Configuration validation

The API composition root binds these environment-backed groups after implementation:

```text
PLATFORM_MARKET_DATA_PROVIDER
PLATFORM_MARKET_DATA_BINANCE_REST_BASE_URL
PLATFORM_MARKET_DATA_BINANCE_WEBSOCKET_BASE_URL
PLATFORM_MARKET_DATA_BINANCE_CONNECT_TIMEOUT
PLATFORM_MARKET_DATA_BINANCE_REQUEST_TIMEOUT
PLATFORM_MARKET_DATA_BINANCE_PAGE_SIZE
PLATFORM_MARKET_DATA_BINANCE_MAX_PAGES
PLATFORM_MARKET_DATA_BINANCE_RETRY_*
PLATFORM_MARKET_DATA_BINANCE_RECONNECT_*
PLATFORM_MARKET_DATA_NORMALIZATION_VERSION
PLATFORM_MARKET_DATA_CHECKSUM_CONTRACT_VERSION
```

Exact Spring relaxed-binding names are finalized during tasks/implementation. Configuration tests must show:

- fixture provider works with no network endpoint;
- production Binance selection accepts only bounded valid HTTPS/WSS configuration;
- page size above 1,000, nonpositive bounds, unsupported versions, or secret-bearing URLs fail fast without printing values;
- no API key is requested for public kline data.

## 8. Review scope and changed files

The implementation review should find changes only in the modules/files identified by [plan.md](plan.md). In particular:

- no database migration change;
- no public REST controller or browser WebSocket protocol;
- no `modules/contracts` or `apps/worker` dependency;
- no Strategy, Backtest, Experiment, Search, Evaluation, Leaderboard, News, auth, Redis, queue, wallet, order, or live-trading behavior;
- no raw Binance type outside `marketdata.internal.provider.binance`;
- no JDBC/table mapping outside `persistence.internal.marketdata`.

## 9. Evidence recording

Record actual commit, environment, configuration profile, command output, test counts, recovery timeline, and checksum fixture version. Keep status `Planned` until real execution exists. Do not invent benchmark, log, database, or network evidence.

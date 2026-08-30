# Implementation Plan: Market Data and Dataset

**Branch**: `feature/003-market-data-dataset` | **Date**: 2026-08-29 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/003-market-data-dataset/spec.md`

## Summary

Implement F-003 as the Market Data capability on the existing modular-monolith foundation. Stable provider-neutral market values live in `modules/domain` for later cross-capability consumption, while `modules/market-data` remains their semantic owner and contains the application services, provider/persistence ports, validation, normalization, deterministic Dataset construction, Binance historical/realtime adapter, and process-local shared subscriptions. `modules/persistence` implements the owner ports with Spring JDBC against the existing `market` schema. `apps/api` supplies typed configuration and composition only; it adds no public controller or browser WebSocket protocol.

The plan uses Java 21 `HttpClient`/`WebSocket` plus internal JSON mapping instead of a Binance SDK. Historical retrieval completes pagination and validation before persistence. Realtime updates use provider finality, share upstream streams by provider/pair/timeframe, persist only closed Candles, and recover disconnect gaps through overlapping REST backfill. Dataset metadata and ordered membership finalize atomically with SHA-256 checksum contract `candle-v1`; metadata reads return `DatasetSnapshot`, while membership is streamed in bounded `CandleBatch` pages. No Redis, queue, Strategy, Backtest, authentication, public transport, or database migration is introduced.

## Technical Context

**Language/Version**: Java 21

**Primary Dependencies**: Spring Boot dependency management 3.5.16; Java 21 `java.net.http.HttpClient` and `WebSocket`; Jackson JSON mapping internal to the Binance adapter; Spring JDBC/transaction APIs in `modules/persistence`; PostgreSQL JDBC runtime driver; no Binance SDK and no Spring WebFlux

**Storage**: Existing PostgreSQL/Supabase `market.asset`, `market.trading_pair`, `market.candle`, `market.dataset_version`, and `market.dataset_candle` tables; no schema change or additional migration

**Testing**: JUnit 5, existing ArchUnit suite, deterministic fake REST/WebSocket transports, fixed clock/scheduler/jitter, golden checksum fixtures, in-memory port fakes, existing SQL baseline tests, and an opt-in local-Supabase PostgreSQL integration-test source set using the repository migrations

**Target Platform**: JVM/Linux deployment; Windows/macOS/Linux development supported through the existing Gradle wrapper; Binance public market-data-only HTTPS/WSS endpoints in deployed environments

**Project Type**: Multi-project modular-monolith capability with one Spring Boot composition root and separate persistence adapter module

**Performance Goals**: Controlled realtime recovery completes within 30 seconds after provider availability returns; one upstream stream is shared per provider/pair/timeframe within a process; historical pages are bounded to Binance's maximum of 1,000 records and a configured maximum page/range count; no unverified production throughput claim

**Constraints**: Exact decimal semantics compatible with `numeric(30,12)` without rounding; UTC `Instant` semantics; deterministic half-open historical ranges; no ambient clock in closure validation; no live Binance call in automated tests; no secrets; only closed Candles persist or enter Datasets; finalized evidence is write-once; no direct capability access to JDBC/table mappings; no edit to the applied migration

**Scale/Scope**: One real provider (Binance) plus one deterministic fixture provider; eight canonical Timeframes; historical Dataset creation and upstream realtime subscriptions only; process-local subscription sharing; no browser fan-out, Redis cache, queue, or Worker orchestration

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-checked after Phase 1 design.*

| Principle/Gate | Pre-design status | Design evidence and required action |
|---|---|---|
| Specification-first and ADR governance | PASS | Approved F-003 spec has testable outcomes. ADR-0001, ADR-0002, ADR-0003, ADR-0007, and ADR-0009 are `Accepted`. ADR-0004 remains a downstream F-009 public-transport decision. |
| Single module/data ownership | PASS | Market Data is the semantic and durable-data owner. Stable values in `domain` do not grant other capabilities write ownership. Application code uses published ports; `persistence` implements output ports; `apps/api` composes public factories. |
| Dependency direction | PASS with foundation correction gate | `domain` depends only on JDK; `market-data → domain`; `persistence → domain + market-data public ports`; `apps/api → market-data + persistence`. No `worker`, `contracts`, Strategy, or Backtest edge is added. F-002's overly broad raw-UUID architecture rule must be narrowed to permit typed ULID business IDs while retaining UUID for authenticated user identity. |
| Reproducibility and immutable evidence | PASS | Ordered membership, checksum contract, golden fixtures, atomic finalize, read-time integrity verification, and no mutation ports preserve Dataset evidence. Conflicting Candle corrections are rejected because the current schema has no revision model. |
| Versioned contracts/provider isolation | PASS | `market-data-provider-v1`, normalization version, Dataset/checksum contract version, canonical values, internal Binance DTOs, and common provider contract tests prevent provider leakage. |
| Safety, reliability, observability, evidence | PASS | No credentials are required for public Binance market endpoints; typed bounded retry/reconnect config, stable errors/states, correlation-compatible logging, deterministic failure tests, gap recovery, and no partial accepted Dataset are planned. |
| Exact decimal, UTC, deterministic identity/order | PASS with identity gate | `BigDecimal`, `Instant`, canonical Timeframe alignment, uppercase provider, canonical pair, typed uppercase ULIDs, deterministic sorting and checksum framing. Identifier correction requires review before implementation. |
| Durable source of truth and cache/queue recovery | PASS | PostgreSQL is the only durable Market truth. F-003 introduces no Redis; transient open Candle/subscription state is process-local and rebuildable from provider plus persisted closed Candles. |
| Database migration governance | PASS | Existing schema is sufficient. Applied migration remains unchanged. Any future Candle revision model or changed checksum uniqueness requires a new specification/ADR and reviewed forward migration. |
| Scope/security boundaries | PASS | No public controller, browser WebSocket, direct browser database access, auth ownership, real trading, Strategy, Backtest, queue, or unrelated infrastructure. |

### Required planning and merge gates

1. **Resolved**: ADR-0001, ADR-0002, ADR-0003, ADR-0007, and ADR-0009 are accepted; implementation must conform to them.
2. Review and correct F-002's canonical-identity contract/test so typed platform business ULIDs are allowed while Supabase user/owner identity remains UUID. Do not work around the rule with hidden dual IDs or untyped strings.
3. Confirm that F-003 rejects same-identity/different-value provider corrections. Supporting revised OHLCV for an accepted Candle is outside this schema and requires a separate revision policy plus forward migration.
4. Treat identical checksum with different stored provenance or membership as an integrity conflict. Do not weaken the baseline global checksum uniqueness inside F-003.

No Constitution violation is justified or carried into implementation. The remaining identity and integrity items above are implementation/review gates that must resolve before merge.

## Project Structure

### Documentation (this feature)

```text
specs/003-market-data-dataset/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── market-data-boundary.md
│   ├── persistence-boundary.md
│   ├── checksum-candle-v1.md
│   └── database-verification.md
└── tasks.md                    # generated later by /speckit-tasks
```

### Source Code (repository root)

```text
modules/domain/
└── src/
    ├── main/java/com/cryptostrategy/platform/domain/api/market/
    │   ├── Asset.java
    │   ├── AssetId.java
    │   ├── AssetSymbol.java
    │   ├── TradingPair.java
    │   ├── TradingPairId.java
    │   ├── Candle.java
    │   ├── CandleId.java
    │   ├── CandleKey.java
    │   ├── DatasetVersionId.java
    │   ├── MarketProvider.java
    │   └── Timeframe.java
    └── test/java/com/cryptostrategy/platform/domain/api/market/

modules/market-data/
└── src/
    ├── main/java/com/cryptostrategy/platform/marketdata/
    │   ├── api/
    │   │   ├── model/
    │   │   ├── error/
    │   │   ├── event/
    │   │   ├── port/in/
    │   │   ├── port/out/
    │   │   └── MarketDataModuleFactory.java
    │   └── internal/
    │       ├── application/
    │       ├── validation/
    │       ├── normalization/
    │       ├── checksum/
    │       ├── realtime/
    │       ├── observability/
    │       └── provider/
    │           ├── binance/
    │           └── fixture/
    ├── test/java/com/cryptostrategy/platform/marketdata/
    └── test/resources/fixtures/market-data/
        ├── binance-rest/v1/
        ├── binance-stream/v1/
        └── checksum/candle-v1/

modules/persistence/
└── src/
    ├── main/java/com/cryptostrategy/platform/persistence/
    │   ├── api/MarketDataPersistenceFactory.java
    │   └── internal/marketdata/
    │       ├── JdbcMarketReferenceDataAdapter.java
    │       ├── JdbcCandleStoreAdapter.java
    │       ├── JdbcDatasetStoreAdapter.java
    │       ├── row/
    │       └── MarketDataSql.java
    ├── test/java/com/cryptostrategy/platform/persistence/internal/marketdata/
    └── src/marketDataIntegrationTest/java/com/cryptostrategy/platform/persistence/marketdata/

apps/api/
└── src/
    ├── main/java/com/cryptostrategy/platform/api/config/MarketDataConfiguration.java
    ├── main/resources/application.yml
    └── test/java/com/cryptostrategy/platform/api/config/MarketDataConfigurationTest.java

architecture-tests/
└── src/test/java/com/cryptostrategy/platform/architecture/
    ├── ModuleBoundaryTest.java
    └── PurityAndCycleTest.java

gradle/libs.versions.toml
modules/domain/build.gradle.kts
modules/market-data/build.gradle.kts
modules/persistence/build.gradle.kts
apps/api/build.gradle.kts
```

**Structure Decision**: Reuse the three existing F-002 modules and API composition root. Provider-neutral cross-capability values are physically in `domain`; Market Data models/use cases/ports and Binance behavior are in `market-data`; JDBC implementations are in `persistence`; only configuration/wiring is in `apps/api`. Public contracts use `..api..`; all implementations remain under the owning module's `..internal..`. `modules/contracts`, `apps/worker`, and all unrelated capability modules remain unchanged.

## Module and Dependency Plan

```text
apps/api
  ├──> market-data public input ports/factory
  └──> persistence public factory
            │
            └──> market-data output ports

market-data ──> domain canonical market values
persistence ──> domain canonical values

domain ──> JDK only
```

### `modules/domain`

- Define immutable provider-neutral Asset, Trading Pair, Timeframe, Candle, natural Candle key, and typed Market ULID values.
- Use `BigDecimal` and `Instant`; enforce structural OHLCV/time/alignment invariants without Spring, database, network, provider JSON, or ambient clock.
- Keep Market Data as semantic owner. Other capabilities may consume these stable values but cannot write Market tables or import Market Data internals.
- Update architecture fixtures/rules to recognize typed ULID business identifiers and continue requiring UUID for authenticated user identity.

### `modules/market-data`

- Publish input ports: load historical Candles, create/get/verify Dataset, subscribe/unsubscribe canonical realtime Candles.
- Publish output ports before their consumers: Market Data provider, market reference persistence, `ClosedCandleStore` persistence/range lookup, atomic Dataset store, paginated `DatasetCandleReader`, and Market-record ID generation.
- Implement application services for historical collection, Dataset construction, and shared realtime subscription coordination.
- Keep provider and persistence results canonical before application logic sees them; no Binance DTO or JDBC row escapes its adapter.
- Implement versioned validation/normalization, checksum `candle-v1`, error taxonomy, connection state, deterministic deduplication, completeness checks, and recovery merge.
- Implement Binance REST/WebSocket adapters and a fixture provider behind the same provider port.

### `modules/persistence`

- Depend only on `domain`, Market Data public output ports/models, and Spring JDBC/transaction APIs.
- Fully qualify `market.*` SQL and map compact stored pair symbols back through base/quote Asset joins; never expose `BTCUSDT` as canonical `BTC/USDT`.
- Insert closed Candles idempotently by natural identity; reload and compare exact canonical content after conflict; reject different content without update.
- Finalize `dataset_version` plus all `dataset_candle` rows in one transaction. Expose no update/delete/member mutation operation.
- Return metadata-only `DatasetSnapshot` values and implement `DatasetCandleReader.readCandles(datasetId, fromSequence, batchSize)` with deterministic `ORDER BY sequence_no` and a maximum batch size of 5,000.
- On concurrent checksum conflict, reload the winner and return it only if provenance, count, and ordered membership are equivalent.
- Recompute/verify count, contiguous sequence, scope, order, and checksum when loading finalized evidence.

### `apps/api`

- Add typed configuration and composition for provider selection, Binance transport, retry/reconnect bounds, normalization/checksum versions, persistence factories, public historical/realtime input ports, and the final public `MarketDataModuleFactory` surface.
- Use public module factories so composition never imports `marketdata.internal` or `persistence.internal`.
- Add no REST controller, WebSocket endpoint, browser subscription rule, or authentication behavior.

### Unchanged modules

- `modules/contracts`: no HTTP/WebSocket/queue DTO is introduced by F-003.
- `apps/worker`: no ingestion job, scheduler, or queue consumer is introduced.
- Strategy, Backtest, Experiment, Search, Evaluation, Leaderboard, News, and Web remain outside the change set.

## Application and Data Flows

### Historical Dataset creation

1. Validate canonical provider, pair, supported Timeframe, aligned `[start,end)` range, and explicit collection cutoff.
2. Resolve or create Market-owned Asset/Trading Pair references through the output port; provider/storage symbols remain adapter details.
3. Page the provider by time cursor with bounded page size/count. Accumulate complete provider input before durable writes.
4. Normalize pair, timeframe, UTC instants, exact decimals, and closure; reject mapping/semantic failures and unproven open Candles.
5. Sort by `openTime`, collapse exact duplicates, reject conflicting identities, and require one Candle for every expected interval.
6. Create zero-based membership and compute the published `candle-v1` checksum over canonical ordered Candle content.
7. Atomically persist any new closed Candles, Dataset metadata, and complete membership; resolve equivalent concurrent retries to the same snapshot.
8. Reload and verify accepted evidence through metadata plus bounded membership pages before returning `DatasetSnapshot`.

### Realtime subscription and recovery

1. Key internal subscriptions by canonical provider/pair/timeframe and reference-count consumers.
2. Open/reuse one upstream Binance stream; map updates to canonical Candles and stable connection states.
3. Emit open updates transiently; route closed updates through the application-owned `ClosedCandleStore` port defined before recovery services. Persist only the first accepted closed update per Candle identity. Never regress closed to open/older state.
4. On disconnect, report `RECONNECTING`, schedule bounded exponential backoff, and start REST backfill from the last confirmed closed Candle with intentional overlap.
5. Buffer concurrent stream updates, merge by Candle identity/provider event order, validate interval continuity, persist recovered closed Candles idempotently, then report `CONNECTED`. Closed beats open; otherwise a later provider-event instant wins. Equal event instants with different canonical content fail deterministically as `MARKET_DATA_INTEGRITY_CONFLICT` rather than relying on arrival order.
6. Report `MARKET_DATA_GAP` rather than recovered state if continuity cannot be restored; cancel pending recovery and close upstream transport when the last subscriber leaves.

## Validation and Normalization Decisions

- Canonical provider is uppercase (`BINANCE`); canonical Trading Pair is `BASE/QUOTE`; Binance stream/REST compact symbols are internal mappings.
- All eight Timeframes are fixed UTC intervals; `1d` means 24 hours UTC. Requests must align and are never silently rounded.
- Historical membership uses `openTime ∈ [start,end)`. Binance inclusive millisecond close timestamps are validated, then canonical `closeTime` is the exclusive interval boundary `openTime + timeframe` so all providers share one meaning.
- Binance REST requests map `[start,end)` to `startTime=start` and `endTime=end-1ms`, omit any non-UTC `timeZone` override, and advance the canonical page cursor to `lastAcceptedOpenTime + timeframe`. Tests prove these rules for `1d` as well as intraday intervals.
- Historical closure requires canonical interval end at or before the explicit cutoff. Realtime closure uses the provider final-kline flag. Open updates remain transient.
- Decimal parsing is direct to exact decimal. After canonical scale normalization, values must have at most 18 integer digits and 12 fractional digits and must fit total precision 30; overflow or excess scale is rejected before JDBC so PostgreSQL never rounds accepted evidence. Canonical duplicate comparison is numeric and scale-insensitive (`1.0` equals `1.000`), never Java `BigDecimal.equals` semantics.
- Exact duplicate Candle keys and values collapse; same key/different canonical content is `MARKET_DATA_INTEGRITY_CONFLICT` and never updates the accepted row.
- Retry applies only to transient I/O/timeouts, selected `5xx`, and provider rate-limit responses. Invalid query/mapping failures are not retried; `Retry-After` is honored without leaking raw provider errors.

## Dataset and Checksum Plan

- `dataset_version_id` is the stable downstream Dataset Version identity; no separate Dataset root is added.
- Typed uppercase Crockford ULIDs match the applied `varchar(26)` baseline. UUID remains exclusive to Supabase user identity.
- `dataset_version.version` remains a Java `String` and records the Dataset/checksum canonicalization contract ID `candle-v1`; it is not a numeric business revision or enum. `normalization_version` separately records provider mapping provenance. A separate Dataset business-version concept requires a future contract/schema decision.
- Checksum input begins with the checksum-contract marker and then length-safe, fixed-field canonical Candle records sorted by `openTime`: provider, canonical pair, timeframe, open time, close time, open, high, low, close, volume.
- Encode UTF-8; normalize UTC instants to one published ISO representation; normalize decimals to plain scale-insensitive strings with zero as `0`; hash with SHA-256 and lowercase `sha256:<64 hex>`.
- Golden input/digest fixtures freeze the contract. A checksum-contract change changes the marker and digest.
- The baseline's globally unique checksum is the logical snapshot idempotency key. Existing checksum is reusable only when provenance, count, and full ordered membership match; otherwise return an integrity conflict.
- Different normalization provenance producing identical Candle checksum cannot create a second row under the baseline. F-003 reports the conflict; changing this behavior requires an explicit schema/contract decision outside F-003.
- Accepted Dataset and Candle records are write-once through F-003 ports. Conflicting provider correction at an existing Candle key is rejected; a true Candle-revision model is future work requiring a forward migration.

### Dataset read and pagination contract

- `DatasetSnapshot` is metadata-only and contains no Candle or membership collection.
- Membership reads use `DatasetCandleReader.readCandles(datasetId, fromSequence, batchSize) -> CandleBatch`.
- `fromSequence` is zero-based, inclusive, and nonnegative. `batchSize` is in `[1,5000]`.
- `CandleBatch` contains the Dataset ID, requested starting sequence, ordered sequence/member records, `nextSequence`, and `hasMore`.
- `nextSequence` is the first sequence not returned. `fromSequence == candleCount` yields an empty terminal batch; values greater than `candleCount` are invalid.
- Dataset immutability makes continuation stable. Reads and integrity verification use `ORDER BY sequence_no`; verification incrementally updates continuity, count, and checksum state without materializing full membership in metadata.

## Observability and Correlation Plan

- Reuse F-002's `correlationId`/MDC convention at the API composition boundary and capture the correlation value explicitly before work crosses asynchronous provider/reconnect callbacks.
- Propagate correlation context through historical, Dataset, realtime, provider, and persistence calls without adding it to canonical Candle identity, Dataset checksum input, or durable Market rows.
- Emit structured lifecycle logs for provider request/retry/rate-limit/reconnect, Dataset finalization/integrity, and persistence conflict/rollback using safe provider, market scope, state, attempt, duration, and correlation fields.
- Never log raw Binance payloads, secret-bearing URLs, credentials, SQL text/parameters, caller-facing stack traces, or full Dataset membership.
- Deterministic tests capture log events and prove propagation, redaction, MDC cleanup, and callback restoration using fixed correlation IDs.

## Configuration Plan

Typed configuration under `platform.market-data`:

| Property group | Purpose |
|---|---|
| `provider` | Select `binance` or deterministic fixture at composition time |
| `binance.rest-base-url` | Public market-data REST endpoint; overridable for controlled tests |
| `binance.websocket-base-url` | Public market-data stream endpoint; overridable for controlled tests |
| `binance.connect-timeout`, `request-timeout` | Positive bounded transport timeouts |
| `binance.page-size` | Positive value capped at 1,000 |
| `binance.max-pages` | Positive guard against unbounded range/pagination |
| `binance.retry.*` | Maximum attempts/elapsed time, initial delay, multiplier, maximum delay, deterministic jitter seam |
| `binance.reconnect.*` | Initial/max delay and recovery bounds compatible with the controlled 30-second scenario |
| `normalization-version` | Provider-to-canonical mapping provenance |
| `checksum-contract-version` | Must resolve to supported `candle-v1` implementation |

Production defaults may target Binance's public market-data-only hosts and require no credential. Configuration validation rejects unsafe schemes, nonpositive bounds, excessive page size, unsupported provider/version, and secret-bearing URLs. Logs expose only safe provider/status/rate-weight metadata and correlation context.

## Test and Evidence Plan

### Default offline `clean check`

- Domain unit tests: symbols, distinct pair assets, all Timeframes/alignment, ULID validation, exact decimals including every `numeric(30,12)` boundary, Candle time/OHLCV/finality/key invariants, and independence from a non-UTC JVM default timezone.
- Market Data unit tests: range/cutoff validation, normalization, closure filtering, sort/dedup/conflict, completeness, membership, checksum golden vectors, exactly 100 shuffled/overlapping repetitions, and scale-insensitive duplicates.
- Provider contract tests: same suite against Binance with fake transports and fixture provider; no live network.
- Binance fixtures: exact `[start,end)` request translation, UTC daily behavior, pagination, overlap, empty/current page, malformed tuples, decimal/timestamp/OHLC errors, 400/418/429/5xx/timeout, raw/combined stream events, fragmentation, duplicate/stale/equal-event-time/closed-then-open events, disconnect/recovery/cancellation.
- Realtime state tests: shared reference count, connection transitions, fixed scheduler/backoff/jitter, overlap merge, recovered-gap exact-once, and an explicit virtual-time assertion that controlled recovery completes within 30 seconds after availability returns.
- Application-service tests with fake ports: atomic outcome, concurrent equivalent/conflicting operations, no partial accepted Dataset.
- Architecture tests: allowed Gradle edges, no cross-module `internal` imports, typed ULID rule, no Spring/JDBC/provider/persistence dependency in canonical/application/checksum code, and no Binance type outside its internal adapter package.
- Observability tests: reuse F-002 correlation IDs across synchronous and asynchronous boundaries, capture safe structured fields, prove redaction and MDC cleanup, and add no endpoint.
- API composition test: typed configuration validation and public historical/Dataset/realtime factory wiring only; no endpoint.

### Local PostgreSQL/Supabase integration evidence

- Use the repository's local Supabase configuration and immutable migrations; do not duplicate schema DDL in Java tests.
- Run a dedicated `marketDataIntegrationTest` source set separately from the Docker-independent default check.
- Verify ULID/numeric/timestamptz mapping under UTC and non-UTC JVM defaults, numeric precision/scale rejection before rounding, three-pass scale-insensitive duplicate insertion, conflicting Candle rejection, ordered half-open Candle range reads, overlapping/concurrent insert, paginated ordered membership, rollback, concurrent equivalent Dataset finalization, checksum/provenance conflict, integrity reads, and absence of mutator APIs.
- Retain existing SQL baseline verification and add no remote mutation or live Supabase requirement.

## Implementation Sequencing

1. **Governance/foundation gate**: verify the accepted ADRs; approve typed ULID correction to F-002 spec/architecture rule; confirm no schema change and correction limitation.
2. **Domain foundation**: add canonical values/invariants and focused architecture tests.
3. **Public boundaries**: add Market Data historical/Dataset/realtime input ports plus provider, `ClosedCandleStore`, range-read, Dataset-store, and paginated-reader output ports before application services consume them.
4. **Application core**: add validation, normalization, historical service, Dataset/checksum logic, shared realtime coordinator, fake ports, and unit tests.
5. **Provider adapter**: add internal JDK/Jackson Binance transports, exact UTC half-open request translation, pagination, error translation, realtime registry/recovery through `ClosedCandleStore`, versioned fixtures, and common provider contract tests.
6. **Persistence adapter**: add JDBC/transaction dependencies, SQL adapters/factory, idempotent/atomic behavior, and local PostgreSQL integration source set.
7. **Composition/configuration**: finish the public module factory, add `apps/api` dependencies, typed properties, structured logging/correlation propagation, public-factory wiring, and context tests; add no endpoint.
8. **Verification**: run default check, local database integration, SQL baseline, architecture suite, determinism repetitions, and recovery timeline; record real evidence only after execution.

## Post-Design Constitution Re-check

The Phase 1 design preserves module/data ownership, exact decimal and UTC rules, deterministic versioned checksum/evidence, provider isolation, durable PostgreSQL truth, safe configuration, and scoped verification. It adds no database migration or unrelated infrastructure. Applicable ADRs are accepted; implementation merge remains blocked until the typed-ULID foundation correction and all verification gates listed above are resolved and reviewed.

## Complexity Tracking

No Constitution violation is proposed. The public factory entry points and dedicated local-database integration source set are necessary boundary/evidence mechanisms within existing modules, not new deployment components.

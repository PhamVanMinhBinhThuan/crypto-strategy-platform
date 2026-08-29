# Tasks: Market Data and Dataset

**Input**: Design documents from `/specs/003-market-data-dataset/`

**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/`

**Tests**: Required by the approved specification. For each behavior, add the test first, confirm it fails for the intended reason, then implement.

**Organization**: Tasks are grouped by user story and ordered by dependency direction: `domain` → `market-data` ports/application → provider and persistence adapters → `apps/api` composition.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel with adjacent `[P]` tasks because it changes different files and has no unfinished dependency.
- **[Story]**: Maps a task to its owning user story.
- Every task names its exact module/package/file area.

---

## Phase 1: Setup and Governance Gates

**Purpose**: Resolve merge gates and prepare the existing F-002 modules without changing the database schema.

- [x] T001 Record the owner-approved `Accepted` status in `docs/adr/0001-modular-monolith.md`, `docs/adr/0002-module-boundaries.md`, `docs/adr/0003-market-data-adapter.md`, `docs/adr/0007-postgresql-redis-ownership.md`, and `docs/adr/0009-reproducible-experiments.md`
- [x] T002 Align `specs/003-market-data-dataset/data-model.md`, `specs/003-market-data-dataset/contracts/market-data-boundary.md`, and `specs/003-market-data-dataset/contracts/persistence-boundary.md` on metadata-only `DatasetSnapshot` and `DatasetCandleReader.readCandles(datasetId, fromSequence, batchSize)` returning `CandleBatch` with maximum `batchSize=5000`; define snapshot `version` as String contract ID `candle-v1`
- [X] T003 Add only the planned Jackson, Spring JDBC/transaction, PostgreSQL runtime, and test dependencies while preserving module edges in `gradle/libs.versions.toml`, `modules/domain/build.gradle.kts`, `modules/market-data/build.gradle.kts`, `modules/persistence/build.gradle.kts`, and `apps/api/build.gradle.kts`
- [X] T004 [P] Configure the opt-in `marketDataIntegrationTest` source set and task, isolated from default `check` and wired to local Supabase properties only, in `modules/persistence/build.gradle.kts`

**Checkpoint**: Governance decisions are recorded, the paginated Dataset contract is authoritative, and builds can host F-003 code and deterministic tests.

---

## Phase 2: Foundational Canonical Model and Boundaries

**Purpose**: Establish provider-neutral values and enforce F-002/F-003 dependency rules before any user story implementation.

**⚠️ CRITICAL**: User-story work starts only after this phase passes.

- [ ] T005 Update the F-002 identifier architecture test and its positive/negative fixtures so typed Market business IDs may wrap validated ULID strings while authenticated `User`/`UserId` fields still require `UUID`, in `architecture-tests/src/test/java/com/cryptostrategy/platform/architecture/PurityAndCycleTest.java` and `architecture-tests/src/test/java/com/cryptostrategy/platform/architecture/fixtures/`
- [ ] T006 [P] Add failing validation/round-trip tests for typed uppercase Crockford ULIDs in `modules/domain/src/test/java/com/cryptostrategy/platform/domain/api/market/MarketUlidTest.java`
- [X] T007 Implement the shared ULID validation/generation primitive and typed `AssetId`, `TradingPairId`, `CandleId`, and `DatasetVersionId` wrappers in `modules/domain/src/main/java/com/cryptostrategy/platform/domain/internal/identity/MarketUlid.java` and `modules/domain/src/main/java/com/cryptostrategy/platform/domain/api/market/{AssetId,TradingPairId,CandleId,DatasetVersionId}.java`
- [ ] T008 [P] Add failing tests for uppercase asset symbols and provider identifiers in `modules/domain/src/test/java/com/cryptostrategy/platform/domain/api/market/AssetAndProviderTest.java`
- [X] T009 Implement `AssetSymbol`, `Asset`, and `MarketProvider` without Spring/provider dependencies in `modules/domain/src/main/java/com/cryptostrategy/platform/domain/api/market/{AssetSymbol,Asset,MarketProvider}.java`
- [ ] T010 [P] Add failing tests for all eight canonical timeframe codes, exact durations, UTC alignment, and interval arithmetic in `modules/domain/src/test/java/com/cryptostrategy/platform/domain/api/market/TimeframeTest.java`
- [X] T011 Implement `Timeframe` in `modules/domain/src/main/java/com/cryptostrategy/platform/domain/api/market/Timeframe.java`
- [ ] T012 [P] Add failing tests proving `TradingPair` stores distinct `baseAsset` and `quoteAsset` values and derives `canonicalSymbol()` as `BASE/QUOTE` in `modules/domain/src/test/java/com/cryptostrategy/platform/domain/api/market/TradingPairTest.java`
- [X] T013 Implement `TradingPair(baseAsset, quoteAsset)` and `canonicalSymbol()` in `modules/domain/src/main/java/com/cryptostrategy/platform/domain/api/market/TradingPair.java`
- [ ] T014 Add failing tests for `CandleKey` identity, UTC `Instant` boundaries, exact `BigDecimal` OHLCV, high/low consistency, timeframe alignment, and closed/open state in `modules/domain/src/test/java/com/cryptostrategy/platform/domain/api/market/CandleTest.java`
- [X] T015 Implement immutable `CandleKey` and `Candle` invariants without ambient time in `modules/domain/src/main/java/com/cryptostrategy/platform/domain/api/market/{CandleKey,Candle}.java`
- [ ] T016 Add architecture rules for `domain` JDK purity, public `..api..` boundaries, forbidden cross-module `..internal..` imports, allowed Gradle dependency direction, and Binance/JDBC isolation in `architecture-tests/src/test/java/com/cryptostrategy/platform/architecture/{ModuleBoundaryTest,PurityAndCycleTest}.java` after T005 updates the shared identity rule
- [X] T017 [P] Define stable Market Data error codes/results, realtime connection events, and the append-only `ClosedCandleStore` output port—including ordered half-open range lookup—before any ingestion or recovery service consumes it, in `modules/market-data/src/main/java/com/cryptostrategy/platform/marketdata/api/error/{MarketDataErrorCode,MarketDataException}.java`, `modules/market-data/src/main/java/com/cryptostrategy/platform/marketdata/api/event/{ConnectionState,CandleUpdate}.java`, and `modules/market-data/src/main/java/com/cryptostrategy/platform/marketdata/api/port/out/ClosedCandleStore.java`
- [ ] T018 [P] Add reusable deterministic clocks, schedulers, ID generators, and in-memory port fakes for later tests in `modules/market-data/src/test/java/com/cryptostrategy/platform/marketdata/support/`

**Checkpoint**: Canonical Market values are independently tested, ULID/UUID ownership is explicit, and module-boundary tests protect the dependency direction.

---

## Phase 3: User Story 1 — Build a Reproducible Historical Dataset (Priority: P1) 🎯 MVP

**Goal**: Construct and inspect an immutable, deterministic Dataset from complete closed canonical Candles without loading its membership into metadata.

**Independent Test**: With the fixture provider and in-memory persistence ports, create the same Dataset from differently ordered/overlapping batches; assert one metadata-only snapshot, identical checksum/provenance, and complete ordered membership when traversed through successive `CandleBatch` pages.

### Tests for User Story 1

- [ ] T019 [P] [US1] Add failing API-model tests proving `DatasetSnapshot` contains metadata only, contains no Candle collection, and exposes `version` as Java `String` with Dataset/checksum contract meaning `candle-v1` rather than a numeric revision or enum, in `modules/market-data/src/test/java/com/cryptostrategy/platform/marketdata/api/model/DatasetSnapshotTest.java`
- [ ] T020 [P] [US1] Add failing contract tests for `DatasetCandleReader.readCandles(datasetId, fromSequence, batchSize)`: zero-based inclusive sequence, batch sizes 1 and 5000 accepted, 0/5001 rejected, ordered members, `nextSequence`, `hasMore`, empty terminal batch at `candleCount`, beyond-end rejection, and no duplicate/omitted Candle across pages, in `modules/market-data/src/test/java/com/cryptostrategy/platform/marketdata/api/port/out/DatasetCandleReaderContractTest.java`
- [ ] T021 [P] [US1] Add failing request-validation tests for provider/pair/timeframe scope, aligned UTC `[start,end)`, explicit cutoff, positive page/range bounds, and supported versions in `modules/market-data/src/test/java/com/cryptostrategy/platform/marketdata/internal/validation/HistoricalQueryValidatorTest.java`
- [ ] T022 [P] [US1] Add immutable golden input, exact byte-stream, and digest fixtures for `candle-v1` under `modules/market-data/src/test/resources/fixtures/market-data/checksum/candle-v1/`
- [ ] T023 [US1] Add failing checksum tests for input shuffling, decimal scale/zero normalization, UTC offset equivalence, checksum-relevant changes, contract marker changes, and excluded surrogate metadata in `modules/market-data/src/test/java/com/cryptostrategy/platform/marketdata/internal/checksum/CandleV1ChecksumTest.java`
- [ ] T024 [P] [US1] Add failing Dataset construction tests for sorting, exact-duplicate collapse, same-key conflict, contiguous zero-based membership, mixed-scope rejection, and gap detection in `modules/market-data/src/test/java/com/cryptostrategy/platform/marketdata/internal/application/DatasetAssemblerTest.java`
- [ ] T025 [US1] Add failing use-case tests proving exactly 100 shuffled/overlapping constructions produce one checksum/membership, repeated/concurrent-equivalent idempotency, immutable finalized evidence, and metadata-plus-paged-membership retrieval in `modules/market-data/src/test/java/com/cryptostrategy/platform/marketdata/internal/application/DatasetServiceTest.java`

### Implementation for User Story 1

- [X] T026 [P] [US1] Implement metadata-only `DatasetSnapshot` with `String version`, `DatasetMembership`, `DatasetFinalization`, and `CandleBatch(datasetId, fromSequence, members, nextSequence, hasMore)` models—without a Candle list in snapshot metadata—in `modules/market-data/src/main/java/com/cryptostrategy/platform/marketdata/api/model/`
- [X] T027 [P] [US1] Define `LoadHistoricalCandlesUseCase`, `CreateDatasetUseCase`, `GetDatasetUseCase`, and `VerifyDatasetUseCase` input ports in `modules/market-data/src/main/java/com/cryptostrategy/platform/marketdata/api/port/in/`
- [X] T028 [P] [US1] Define append-only `DatasetStore` plus `DatasetCandleReader.readCandles(datasetId, fromSequence, batchSize)` with maximum batch size 5000 in `modules/market-data/src/main/java/com/cryptostrategy/platform/marketdata/api/port/out/{DatasetStore,DatasetCandleReader}.java`; expose no update/delete/member mutation method
- [X] T029 [US1] Implement historical query, closed-Candle, scope, range-alignment, continuity, deterministic deduplication, and membership-sequence validators in `modules/market-data/src/main/java/com/cryptostrategy/platform/marketdata/internal/validation/`
- [X] T030 [US1] Implement the exact UTF-8 length-framed `candle-v1` canonicalizer and SHA-256 formatter in `modules/market-data/src/main/java/com/cryptostrategy/platform/marketdata/internal/checksum/{CanonicalCandleEncoder,CandleV1Checksum}.java`
- [X] T031 [US1] Implement provider-neutral historical collection plus deterministic Dataset assembly and atomic finalization orchestration in `modules/market-data/src/main/java/com/cryptostrategy/platform/marketdata/internal/application/{HistoricalCandleService,DatasetAssembler,DatasetService}.java`
- [X] T032 [US1] Expose historical and Dataset input use cases through public construction only in `modules/market-data/src/main/java/com/cryptostrategy/platform/marketdata/api/MarketDataModuleFactory.java`

**Checkpoint**: US1 works with deterministic fakes, produces metadata-only snapshots, and retrieves arbitrarily large membership page by page.

---

## Phase 4: User Story 2 — Obtain Canonical Historical and Realtime Candles (Priority: P1)

**Goal**: Supply canonical historical and realtime Candles through a provider-neutral port, with bounded pagination and deterministic reconnect recovery.

**Independent Test**: Run one provider contract against the fixture provider and Binance adapter using only fixed HTTP/WebSocket fixtures, fake time/scheduling, and no live network; compare canonical outputs, errors, state transitions, and recovery results.

### Tests for User Story 2

- [ ] T033 [P] [US2] Add versioned Binance REST fixtures for valid, overlapping, empty, malformed, unsupported, rate-limited, and partial-failure pages under `modules/market-data/src/test/resources/fixtures/market-data/binance-rest/v1/`
- [ ] T034 [P] [US2] Add versioned Binance raw/combined stream fixtures for open, final, duplicate, stale, fragmented, disconnect, and recovery events under `modules/market-data/src/test/resources/fixtures/market-data/binance-stream/v1/`
- [ ] T035 [US2] Add a provider-neutral contract suite covering historical normalization, closure, sorting/deduplication, stable errors, subscription lifecycle, states, and cancellation in `modules/market-data/src/test/java/com/cryptostrategy/platform/marketdata/contract/MarketDataProviderContract.java`
- [ ] T036 [P] [US2] Add failing Binance mapper tests for configured compact symbols, interval codes, epoch-millisecond UTC conversion under UTC and a non-UTC JVM default zone, exclusive canonical close time, scale-insensitive exact decimals, `numeric(30,12)` boundary/overflow/excess-scale rejection, tuple validation, and final-kline mapping in `modules/market-data/src/test/java/com/cryptostrategy/platform/marketdata/internal/provider/binance/BinanceCandleMapperTest.java`
- [ ] T037 [P] [US2] Add failing historical pagination tests proving `[start,end)` maps to `startTime=start` and `endTime=end-1ms`, no non-UTC `timeZone` is sent, the cursor advances to `lastAcceptedOpenTime + timeframe`, UTC `1d` behavior, page-size/page-count bounds, overlap collapse, out-of-order pages, no-progress detection, incomplete final pages, and fail-whole-operation behavior in `modules/market-data/src/test/java/com/cryptostrategy/platform/marketdata/internal/provider/binance/BinanceHistoricalProviderTest.java`
- [ ] T038 [P] [US2] Add failing provider-error tests for 400, 418, 429 with retry delay, eligible 5xx, timeout/I/O, malformed payload, and safe diagnostics in `modules/market-data/src/test/java/com/cryptostrategy/platform/marketdata/internal/provider/binance/BinanceErrorTranslatorTest.java`
- [ ] T039 [P] [US2] Add failing realtime transport tests for frame handling, finality, duplicate/stale updates, closed-over-open precedence, later-provider-event precedence, equal-event-time conflicting content rejection independent of arrival order, and cancellation in `modules/market-data/src/test/java/com/cryptostrategy/platform/marketdata/internal/provider/binance/BinanceStreamProviderTest.java`
- [ ] T040 [P] [US2] Add failing shared-subscription tests proving one upstream stream per provider/pair/timeframe and reference-counted release in `modules/market-data/src/test/java/com/cryptostrategy/platform/marketdata/internal/realtime/SharedSubscriptionRegistryTest.java`
- [ ] T041 [US2] Add failing reconnect tests for bounded exponential backoff/jitter, last-confirmed overlap backfill, buffered-update merge, continuity verification before `CONNECTED`, duplicate suppression, exhausted recovery, cancellation, and an explicit fixed-scheduler assertion that recovery completes within 30 virtual seconds after provider availability returns in `modules/market-data/src/test/java/com/cryptostrategy/platform/marketdata/internal/realtime/RealtimeRecoveryCoordinatorTest.java`

### Implementation for User Story 2

- [X] T042 [P] [US2] Define canonical historical/realtime queries, batches, handler/subscription types, public `SubscribeCandlesUseCase`, and `MarketDataProvider` output port in `modules/market-data/src/main/java/com/cryptostrategy/platform/marketdata/api/model/`, `modules/market-data/src/main/java/com/cryptostrategy/platform/marketdata/api/port/in/SubscribeCandlesUseCase.java`, and `modules/market-data/src/main/java/com/cryptostrategy/platform/marketdata/api/port/out/MarketDataProvider.java`
- [X] T043 [P] [US2] Implement the deterministic provider-neutral fixture provider for the shared contract suite in `modules/market-data/src/main/java/com/cryptostrategy/platform/marketdata/internal/provider/fixture/FixtureMarketDataProvider.java`
- [X] T044 [P] [US2] Implement package-private Binance REST/WebSocket transport interfaces and JDK 21 transport clients in `modules/market-data/src/main/java/com/cryptostrategy/platform/marketdata/internal/provider/binance/transport/`
- [ ] T045 [US2] Implement internal Binance DTOs, symbol/interval mapping, tuple/frame parsing, local-timezone-independent UTC normalization, exact decimal conversion with pre-JDBC `numeric(30,12)` fit checks, deterministic equal-event-time conflict handling, and finality mapping in `modules/market-data/src/main/java/com/cryptostrategy/platform/marketdata/internal/provider/binance/{dto,BinanceCandleMapper}.java`
- [X] T046 [US2] Implement bounded REST historical pagination with exact `startTime`/`endTime=endExclusive-1ms`, UTC interval semantics, canonical interval cursor progression, and full-batch validation behind `MarketDataProvider` in `modules/market-data/src/main/java/com/cryptostrategy/platform/marketdata/internal/provider/binance/BinanceHistoricalProvider.java`
- [X] T047 [US2] Implement stable provider error translation and bounded retry/rate-limit policy in `modules/market-data/src/main/java/com/cryptostrategy/platform/marketdata/internal/provider/binance/{BinanceErrorTranslator,BinanceRetryPolicy}.java`
- [X] T048 [US2] Implement normalized Binance realtime subscription handling and connection-state emission in `modules/market-data/src/main/java/com/cryptostrategy/platform/marketdata/internal/provider/binance/BinanceStreamProvider.java`
- [X] T049 [US2] Implement process-local shared upstream subscriptions with per-consumer handles in `modules/market-data/src/main/java/com/cryptostrategy/platform/marketdata/internal/realtime/SharedSubscriptionRegistry.java`
- [ ] T050 [US2] Implement `RealtimeSubscriptionService` plus bounded reconnect, REST overlap backfill, deterministic buffered merge, closed-Candle deduplication/persistence through the foundational `ClosedCandleStore` port from T017, continuity gating, and shutdown in `modules/market-data/src/main/java/com/cryptostrategy/platform/marketdata/internal/application/RealtimeSubscriptionService.java` and `modules/market-data/src/main/java/com/cryptostrategy/platform/marketdata/internal/realtime/RealtimeRecoveryCoordinator.java`

**Checkpoint**: US2 passes the same deterministic contract for fixture and Binance providers; no Binance type or live network escapes the adapter.

---

## Phase 5: User Story 3 — Persist Valid Candles Without Duplication (Priority: P1)

**Goal**: Persist canonical Market references, closed Candles, Dataset metadata, and membership against the existing `market.*` baseline with idempotency and atomicity.

**Independent Test**: Against an isolated local Supabase database, save overlapping/duplicate/conflicting Candle batches, finalize equivalent Datasets, and page membership; assert exact decimals/UTC, one natural-key row, no overwrite, and no partial transaction.

### Tests for User Story 3

- [X] T051 [P] [US3] Add static SQL/API tests requiring schema-qualified `market.*` statements and proving no accepted Candle/Dataset update, delete, or membership mutation API exists in `modules/persistence/src/test/java/com/cryptostrategy/platform/persistence/internal/marketdata/MarketDataSqlContractTest.java`
- [ ] T052 [P] [US3] Add local-database integration tests for Asset and Trading Pair resolve/create, base/quote reconstruction, compact-symbol conflict, and ULID mapping in `modules/persistence/src/marketDataIntegrationTest/java/com/cryptostrategy/platform/persistence/marketdata/JdbcMarketReferenceDataAdapterIT.java`
- [ ] T053 [P] [US3] Add local-database integration tests for `numeric(30,12)` maximum valid integer/fractional boundaries, pre-SQL overflow/excess-scale rejection without rounding, UTC round trips under UTC and non-UTC JVM defaults, three reprocessings of scale-equivalent values such as `1.0`/`1.000`, same-key/different-value conflict, open/invalid rejection, ordered provider/pair/timeframe `[start,end)` range reads, overlap, and concurrency in `modules/persistence/src/marketDataIntegrationTest/java/com/cryptostrategy/platform/persistence/marketdata/JdbcCandleStoreAdapterIT.java`
- [X] T054 [P] [US3] Add application tests proving only validated closed Candles reach `ClosedCandleStore` and failed batches produce no partial call in `modules/market-data/src/test/java/com/cryptostrategy/platform/marketdata/internal/application/ClosedCandleIngestionServiceTest.java`
- [ ] T055 [US3] Add local-database integration tests for atomic Dataset metadata/membership finalization, rollback on member failure, equivalent concurrent winner, checksum/provenance conflict, and immutability in `modules/persistence/src/marketDataIntegrationTest/java/com/cryptostrategy/platform/persistence/marketdata/JdbcDatasetStoreAdapterIT.java`
- [ ] T056 [US3] Add local-database contract tests for `readCandles(datasetId, fromSequence, batchSize)` proving inclusive zero-based `fromSequence`, batch sizes 1/5000 accepted and 0/5001 rejected, ascending `sequence_no`, `nextSequence`/`hasMore`, empty terminal/beyond-end behavior, stable continuation, and complete no-duplicate traversal without materializing all members in `modules/persistence/src/marketDataIntegrationTest/java/com/cryptostrategy/platform/persistence/marketdata/JdbcDatasetCandleReaderIT.java`

### Implementation for User Story 3

- [X] T057 [P] [US3] Define the Market reference persistence port in `modules/market-data/src/main/java/com/cryptostrategy/platform/marketdata/api/port/out/MarketReferenceDataStore.java`; reuse the already-defined T017 `ClosedCandleStore` port rather than redefining it after realtime recovery
- [X] T058 [US3] Implement closed-Candle admission orchestration before persistence in `modules/market-data/src/main/java/com/cryptostrategy/platform/marketdata/internal/application/ClosedCandleIngestionService.java`
- [X] T059 [P] [US3] Implement schema-qualified constants and exact row mappings for Asset, Trading Pair, Candle, Dataset metadata, and membership in `modules/persistence/src/main/java/com/cryptostrategy/platform/persistence/internal/marketdata/MarketDataSql.java` and `modules/persistence/src/main/java/com/cryptostrategy/platform/persistence/internal/marketdata/row/`
- [X] T060 [US3] Implement Market reference resolve/create and canonical base/quote reconstruction in `modules/persistence/src/main/java/com/cryptostrategy/platform/persistence/internal/marketdata/JdbcMarketReferenceDataAdapter.java`
- [X] T061 [US3] Implement closed-Candle single/batch insert, pre-JDBC precision/scale validation, natural-key conflict reload, scale-insensitive numeric canonical comparison, no-update conflict handling, and ordered schema-qualified provider/pair/timeframe half-open range lookup in `modules/persistence/src/main/java/com/cryptostrategy/platform/persistence/internal/marketdata/JdbcCandleStoreAdapter.java`
- [X] T062 [US3] Implement transactional Dataset finalization, contiguous membership insert, checksum-winner comparison, rollback, and metadata-only reads in `modules/persistence/src/main/java/com/cryptostrategy/platform/persistence/internal/marketdata/JdbcDatasetStoreAdapter.java`
- [X] T063 [US3] Implement `DatasetCandleReader.readCandles(datasetId, fromSequence, batchSize)` with inclusive zero-based sequence, `batchSize` range 1–5000, ascending `sequence_no`, `nextSequence`, `hasMore`, empty terminal batch, and beyond-end rejection in `modules/persistence/src/main/java/com/cryptostrategy/platform/persistence/internal/marketdata/JdbcDatasetCandleReader.java`
- [X] T064 [US3] Expose the JDBC adapters through `modules/persistence/src/main/java/com/cryptostrategy/platform/persistence/api/MarketDataPersistenceFactory.java`
- [X] T065 [US3] Add and pass persistence-to-public-port dependency rules in `architecture-tests/src/test/java/com/cryptostrategy/platform/architecture/ModuleBoundaryTest.java`

**Checkpoint**: US3 persists only closed canonical records, rejects corrections, finalizes atomically, and pages Dataset members using the unchanged baseline schema.

---

## Phase 6: User Story 4 — Detect Incomplete or Inconsistent Dataset Input (Priority: P2)

**Goal**: Reject incomplete/mixed input and refuse to return stored Dataset evidence whose metadata, order, scope, count, or checksum is inconsistent.

**Independent Test**: Use fakes for construction failures and controlled local-database tampering for read failures; each case must return the specified stable error and no valid/partial Dataset.

### Tests for User Story 4

- [ ] T066 [P] [US4] Add acceptance tests for empty input, missing interval, mixed provider/pair/timeframe, out-of-range member, open final Candle, and unaligned range in `modules/market-data/src/test/java/com/cryptostrategy/platform/marketdata/internal/application/DatasetCompletenessAcceptanceTest.java`
- [ ] T067 [P] [US4] Add paged verifier tests using successive batches up to size 5000 for duplicate/missing sequence, repeated Candle, wrong scope/range, unsupported String contract version, count mismatch, checksum mismatch, and incremental verification without full membership materialization in `modules/market-data/src/test/java/com/cryptostrategy/platform/marketdata/internal/application/DatasetIntegrityVerifierTest.java`
- [ ] T068 [US4] Add controlled local-database tamper tests proving corrupt evidence is returned as `DATASET_INTEGRITY_FAILED`, never as a valid snapshot, in `modules/persistence/src/marketDataIntegrationTest/java/com/cryptostrategy/platform/persistence/marketdata/DatasetIntegrityTamperIT.java`

### Implementation for User Story 4

- [ ] T069 [US4] Complete gap/mixed-scope failure reporting with `MARKET_DATA_GAP` versus `MARKET_DATA_INTEGRITY_CONFLICT` in `modules/market-data/src/main/java/com/cryptostrategy/platform/marketdata/internal/validation/{DatasetCompletenessValidator,CandleScopeValidator}.java`
- [X] T070 [US4] Implement `DatasetIntegrityVerifier` as a bounded loop over `DatasetSnapshot` plus `DatasetCandleReader` pages, recalculating sequence, scope, continuity, count, and checksum without retaining all Candles in snapshot metadata, in `modules/market-data/src/main/java/com/cryptostrategy/platform/marketdata/internal/application/DatasetIntegrityVerifier.java`
- [X] T071 [US4] Integrate integrity verification into get/verify use cases and map missing/corrupt evidence to `DATASET_NOT_FOUND`/`DATASET_INTEGRITY_FAILED` in `modules/market-data/src/main/java/com/cryptostrategy/platform/marketdata/internal/application/DatasetService.java`
- [X] T072 [US4] Add transaction-failure translation that preserves stable Market Data errors without leaking SQL details in `modules/persistence/src/main/java/com/cryptostrategy/platform/persistence/internal/marketdata/MarketDataPersistenceExceptionTranslator.java`

**Checkpoint**: US4 rejects incomplete construction and detects persisted corruption while keeping Dataset reads bounded and deterministic.

---

## Phase 7: Configuration, Composition, and Cross-Cutting Verification

**Purpose**: Wire the completed capability into the existing API composition root and prove scope, safety, and deterministic operation.

- [ ] T073 Add failing public-factory tests proving `MarketDataModuleFactory` exposes `LoadHistoricalCandlesUseCase`, Dataset use cases, and `SubscribeCandlesUseCase` without exposing Binance, JDBC, or other internal types in `modules/market-data/src/test/java/com/cryptostrategy/platform/marketdata/api/MarketDataModuleFactoryTest.java`
- [ ] T074 Finalize public construction of historical, Dataset, provider, realtime/recovery, and persistence-port collaborators in `modules/market-data/src/main/java/com/cryptostrategy/platform/marketdata/api/MarketDataModuleFactory.java`; composition roots must not import `marketdata.internal`
- [ ] T075 [P] Add deterministic structured-log tests for fixed F-002 correlation IDs across historical, Dataset, provider retry/reconnect callback, and persistence boundaries, including MDC cleanup/restoration and redaction of payloads, credential-bearing URLs, SQL details, stack traces, and full membership, in `modules/market-data/src/test/java/com/cryptostrategy/platform/marketdata/internal/observability/MarketDataObservabilityTest.java` and `apps/api/src/test/java/com/cryptostrategy/platform/api/config/MarketDataCorrelationConfigurationTest.java`
- [ ] T076 Implement correlation-context capture/propagation and safe structured lifecycle logging without adding correlation to domain identity/checksum/persistence in `modules/market-data/src/main/java/com/cryptostrategy/platform/marketdata/internal/observability/{CorrelationContext,MarketDataEventLogger}.java`, provider/application boundary calls under `modules/market-data/src/main/java/com/cryptostrategy/platform/marketdata/internal/`, and `apps/api/src/main/java/com/cryptostrategy/platform/api/config/MarketDataConfiguration.java`
- [ ] T077 [P] Add failing configuration-binding tests for provider selection, public HTTPS/WSS hosts, timeout/page/retry/reconnect bounds, normalization/checksum versions, unsafe/secret-bearing URLs, and redacted failures in `apps/api/src/test/java/com/cryptostrategy/platform/api/config/MarketDataConfigurationTest.java`
- [X] T078 Implement typed `platform.market-data` configuration and public-factory composition—without controller, browser WebSocket, scheduler, or auth behavior—in `apps/api/src/main/java/com/cryptostrategy/platform/api/config/MarketDataConfiguration.java` and `apps/api/src/main/resources/application.yml`
- [X] T079 [P] Update the implementation verification guide with public input ports, the exact `readCandles(datasetId, fromSequence, batchSize)` contract/max 5000, correlation evidence, non-UTC checks, numeric boundaries, and local Supabase lifecycle in `specs/003-market-data-dataset/quickstart.md`
- [X] T080 Run `./gradlew clean check` with network access disabled and record/fix any F-003 failures in `modules/domain/src/`, `modules/market-data/src/`, `modules/persistence/src/test/`, `apps/api/src/test/`, or `architecture-tests/src/test/` without weakening assertions
- [ ] T081 Run the approved local sequence `supabase start`, `supabase db reset`, `./gradlew marketDataIntegrationTest`, and `supabase test db`; record results and verify `git diff -- supabase/migrations/20260827000100_create_database_baseline.sql modules/contracts apps/worker` is empty before handoff

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 — Setup/Governance**: T001 and T002 are complete documentation gates. T003 precedes compilation; T004 follows T003 because both modify `modules/persistence/build.gradle.kts` and precedes PostgreSQL integration tests.
- **Phase 2 — Foundation**: Depends on Phase 1 and blocks all user stories. T005 precedes T016 because both modify `PurityAndCycleTest.java`. T017 defines `ClosedCandleStore` before any ingestion/recovery task. Other tests marked `[P]` may start together; each implementation follows its matching test.
- **US1 (Phase 3)**: Depends on Phase 2. It uses deterministic fakes and has no dependency on Binance or JDBC, so it is the recommended MVP.
- **US2 (Phase 4)**: Depends on Phase 2. It may proceed in parallel with US1 after shared query/error contracts are agreed; it does not depend on persistence.
- **US3 (Phase 5)**: Depends on Phase 2 and the US1 persistence/Dataset port shapes (T026–T028). Candle persistence work can proceed in parallel with US2; Dataset finalization depends on T029–T031.
- **US4 (Phase 6)**: Depends on US1 validators/checksum/reader contracts and US3 adapters for tamper integration tests.
- **Phase 7 — Composition/Verification**: T073 depends on T032, T050, and T064; T074 follows its failing factory test. T075/T076 cover observability. T077 may run in parallel with T075 after public factory shapes settle; T078 depends on T074, T076, and T077. Final checks T080/T081 depend on all selected story phases.

### User Story Dependencies

```text
Phase 1 Setup
    ↓
Phase 2 Canonical Foundation
    ├──→ US1 Dataset core (MVP) ──────┐
    ├──→ US2 Provider adapters        ├──→ Composition and full verification
    └──→ US3 Candle persistence ──────┘
             ↑              ↓
       US1 port shapes     US4 integrity verification
```

- **US1** is independently demonstrable with fixture data and in-memory ports.
- **US2** is independently contract-testable with fixture transports and no database/live Binance.
- **US3** is independently testable against local Supabase with canonical fixture Candles and no Binance.
- **US4** adds construction/read integrity outcomes and intentionally reuses US1/US3 contracts.

### Within Each Story

- Add each test/fixture first and confirm the intended failure.
- Define canonical/public models and ports before application services.
- Implement application behavior before infrastructure adapters.
- Keep provider and JDBC details under their owning `internal` packages.
- Complete the story's independent test before moving its checkpoint.

---

## Parallel Execution Examples

### Foundation

After T003/T004 and T005/T016 complete their same-file sequences, separate developers can execute T006, T008, T010, and T012 in parallel because they target distinct tests/files. T017 and T018 may also proceed in parallel with those domain tests. Implementations T007, T009, T011, and T013 follow their corresponding tests; T014/T015 then compose those types into Candle.

### User Story 1

After T019–T021 establish expected APIs, checksum fixtures/tests (T022–T023), Dataset assembly tests (T024), and port/model definitions (T026–T028) can be divided across independent file areas. T029–T031 integrate those results.

### User Story 2

REST fixtures/mapping/pagination (T033, T036–T038) and stream fixtures/lifecycle/sharing (T034, T039–T041) can proceed as two tracks. They converge in T048–T050.

### User Story 3

After T004, foundational `ClosedCandleStore` T017, and Market reference port T057, Market-reference work (T052/T060), Candle work (T053/T061), and Dataset paging test preparation (T056) can proceed in parallel against isolated test cases. Dataset finalization (T055/T062) follows the canonical Dataset contracts.

---

## Implementation Strategy

### MVP First

1. Complete Phase 1 and Phase 2.
2. Complete US1 using the fixture provider and in-memory persistence ports.
3. Stop and verify deterministic checksum, metadata-only `DatasetSnapshot`, and complete paginated membership traversal.
4. Add US2 and US3 as independent adapters, then add US4 integrity hardening.

### Scope Guardrails

- Do not add Strategy, Backtest, Experiment execution, F-004/F-005/F-006 behavior, public REST/WebSocket delivery, authentication/user ownership, Redis, queues, workers, schedulers, or live-network tests.
- Do not edit `supabase/migrations/20260827000100_create_database_baseline.sql` or add a migration.
- Keep `DatasetSnapshot.version` as `String`; do not introduce a Dataset version enum.
- Use ULID for Market business entities and retain UUID only for Supabase user identity.
- Never load all Dataset Candles into `DatasetSnapshot`; use `DatasetCandleReader` and bounded `CandleBatch` pages.

## Notes

- `[P]` means the task has no incomplete dependency and targets distinct files from adjacent parallel tasks.
- Commit after each task or small logical group, and keep default tests deterministic/offline.
- No task authorizes a database redesign or unrelated capability implementation.

# Implementation Plan: F-008 News and Sentiment

**Branch**: `feature/008-news-sentiment` | **Date**: 2026-08-30 | **Spec**: [spec.md](spec.md)

**Input**: Approved feature specification at `specs/008-news-sentiment/spec.md`

## Summary

F-008 adds a Java-owned News capability and an independently deployed, stateless Python FastAPI inference service. Java collects, sanitizes, normalizes, hashes, deduplicates, associates, stores, leases, retries, validates, and exposes News/Sentiment state. Python eagerly loads one immutable Multi-channel LSTM-CNN release bundle and performs only model-specific preprocessing and inference. PostgreSQL is the durable source of truth; `apps/worker` is the sole asynchronous orchestrator and makes one outbound inference call per dispatched lease attempt.

The plan implements ADR-0008 without changing its boundary: `modules/news` owns the domain and ports, `modules/persistence` implements durable adapters, `apps/api` exposes authenticated read models, `apps/worker` composes providers and the HTTP inference adapter, and `apps/sentiment` has no database, Redis, crawler, or Strategy responsibility. The approved clarification governs retry behavior: Resilience4j supplies `CircuitBreaker` and `TimeLimiter` only; persisted Worker scheduling supplies the 5-second and 30-second retries.

## Technical Context

**Language/Version**: Java 21; Spring Boot 3.5.x; Python 3.11 target (exact minor locked with the validated TensorFlow bundle)

**Primary Dependencies**: Existing Gradle/Spring/Jackson/JDBC foundation; JDK `HttpClient` and StAX; jsoup for HTML-to-text sanitation; Resilience4j CircuitBreaker and TimeLimiter; FastAPI, Uvicorn, Pydantic, TensorFlow/Keras, NumPy

**Storage**: PostgreSQL/Supabase schemas `news` and read-only references to canonical `market_data` asset identities; immutable filesystem/container model bundle for Python; no Python database or Redis connection

**Testing**: JUnit 5, Spring Boot Test, PostgreSQL integration tests, ArchUnit, MockWebServer; pytest, HTTPX, pytest-asyncio, jsonschema; shared JSON Schema fixtures

**Target Platform**: Linux containers for Java Worker/API and one-worker-per-process Python inference replicas; local Windows/Linux developer execution

**Project Type**: Modular Java monolith plus a separately deployable internal Python service

**Performance Goals**: Accepted News visible without waiting for inference; degraded state visible within 5 seconds; bounded inference concurrency; deterministic newest-first pagination; no Worker thread blocked for retry delays

**Constraints**: Java normalizes and sanitizes; Python is stateless and internal-only; exact decimal strings across HTTP; maximum three conservative dispatch reservations and therefore at most three actual calls by default; 2-second connect timeout, one TimeLimiter-owned 30-second response deadline, 120-second lease/model-startup deadline; per-Worker 50% circuit threshold after 10 calls and 30-second open interval; fatal Python startup failure exits nonzero; forward migration only

**Scale/Scope**: Multiple independently configured News providers, asynchronous at-least-once analysis, one active configured sentiment release, historical immutable results by content/model identity, public News list plus protected audit provenance

## Constitution Check

*GATE: Passed before research and re-checked after design.*

| Principle / constraint | Plan evidence | Status |
|---|---|---|
| Specification first; Accepted ADRs govern | Approved F-008 spec and Accepted ADR-0008 are the design authority; no new architectural direction is introduced | PASS |
| One capability owner | `modules/news` owns News Item, Sentiment Result, lifecycle, provider and inference ports; Python owns only inference runtime behavior | PASS |
| Dependency direction | News depends only on `modules/domain`; persistence depends on News; API/Worker compose public ports; no import of another module's internals | PASS |
| Versioned contracts and provider isolation | `sentiment-v1` schemas/fixtures are shared; each provider maps its payload behind `NewsProvider` | PASS |
| PostgreSQL durable truth | Lease, attempt, eligibility, release and result state are persisted; Redis is not required for correctness | PASS |
| At-least-once, bounded retry, duplicate safety | Lease fencing plus unique `(news_item_id, content_hash, model_version)` and Worker-only persisted retry | PASS |
| Exact values and deterministic identity | `BigDecimal`, canonical decimal strings, UTC instants, typed ULIDs, versioned normalization/hash and stable pagination | PASS |
| Security and failure isolation | Browser never calls Python; internal service auth and size limits; News remains visible and Market Data/Backtest remain operational on inference failure | PASS |
| Evidence before “Verified” | Unit, contract, PostgreSQL, architecture, resilience, failure-isolation and release-gated real-bundle smoke tests are planned; no result is claimed yet | PASS |
| Applied migrations are immutable | One new forward migration and non-production dry-run; existing migrations remain byte-unchanged | PASS |
| No Strategy/Backtest integration | Explicitly excluded except non-regression/failure-isolation evidence | PASS |

Post-design re-check: the lease token strengthens FR-024/FR-027; the release table implements FR-031; the offline model-release gate prevents runtime training/download. No Constitution violation or new ADR is required. A new ADR would be required if implementation moves orchestration or storage into Python, makes Redis/Outbox the source of truth, or changes module dependency direction.

## Project Structure

### Documentation (this feature)

```text
specs/008-news-sentiment/
|-- spec.md
|-- plan.md
|-- research.md
|-- data-model.md
|-- quickstart.md
`-- contracts/
    |-- README.md
    |-- canonical-news-v1.md
    |-- persistence-boundary.md
    |-- public-news-api.md
    `-- sentiment-v1/
        |-- analyze-request.schema.json
        |-- analyze-success.schema.json
        |-- error-response.schema.json
        |-- health.schema.json
        `-- fixtures/
            |-- valid-request.json
            |-- valid-success.json
            |-- transient-error.json
            `-- ready.json
```

`tasks.md` is intentionally not created by this planning step.

### Source Code (repository root)

```text
modules/news/
`-- src/{main,test}/java/com/cryptostrategy/platform/news/
    |-- api/{model,port/in,port/out,error}/
    `-- internal/{application,normalization,provider,validation,observability}/

modules/contracts/
`-- src/main/java/com/cryptostrategy/platform/contracts/sentiment/v1/

modules/persistence/
`-- src/{main,test,newsIntegrationTest}/java/com/cryptostrategy/platform/persistence/
    |-- api/NewsPersistenceFactory.java
    `-- internal/news/

apps/api/
`-- src/{main,test}/java/com/cryptostrategy/platform/api/news/

apps/worker/
`-- src/{main,test}/java/com/cryptostrategy/platform/worker/
    |-- config/
    `-- news/{collection,analysis,sentiment}/

apps/sentiment/
|-- pyproject.toml
|-- app/{api,core,model}/
|-- tools/
`-- tests/{unit,contract,smoke}/

supabase/migrations/<new-forward-news-sentiment-migration>.sql
supabase/tests/<new-news-sentiment-schema-test>.sql
architecture-tests/src/test/java/.../
docs/api/openapi.yaml
infra/compose/
```

**Structure Decision**: Extend the existing modular-monolith layout. News business types and interfaces stay in `modules/news`; Java transport DTOs and canonical schema resources live in `modules/contracts`, but News does not depend on that module. The Worker owns the HTTP adapter and scheduling. Python remains a standalone process. No generic HTTP utility or shared business-model module is added.

## Component and Boundary Plan

### Java: News domain and application

Create typed, framework-free capability models:

- `NewsId`, `SentimentResultId`, `ContentHash`, `CanonicalNewsUrl`, `NewsSource`, `LanguageCode`.
- `NewsItem` with normalized title/content, publication/crawl instants, source/URL, hash, related `AssetId`s and `AnalysisStatus`.
- `AnalysisStatus`: `PENDING`, `ANALYZING`, `FAILED_RETRYABLE`, `ANALYZED`, `FAILED`.
- `AnalysisLease` with unique lease token, owner, expiry, attempt count, next eligibility and claimed content/model identity.
- `SentimentLabel`, `SentimentModelRelease`, and immutable `SentimentResult`; use `BigDecimal`, never `double`, at Java capability boundaries.
- `NewsListQuery`, deterministic cursor/page types, provider candidate types, and semantic inference request/outcome types with no HTTP/Jackson/FastAPI concepts.

Publish narrow input ports for collection, acquiring/recovering work, atomically starting an attempt, completing/failing analysis, listing public News, and retrieving protected audit provenance. Publish output ports for `NewsProvider`, News storage, analysis-work storage, News queries, sentiment-release storage, asset-reference resolution, and `SentimentInferencePort`.

Implement shared application policy in `modules/news`: provider-neutral validation, HTML/script sanitization, Unicode/whitespace normalization, URL canonicalization, versioned hash encoding, logical URL deduplication, asset association, state-transition rules, failure classification inputs, stable pagination, and strict response/provenance validation. `NewsModuleFactory` follows existing F-003/F-004 factory composition patterns.

Multiple configured provider adapters implement the same `NewsProvider` port and are injected as a collection. Provider DTOs, pagination, authentication, rate-limit mapping and transport errors remain adapter-private. Plan the first production RSS/Atom adapter plus a deterministic fixture adapter; selecting any proprietary provider, credentials, license and retention terms is a delivery gate rather than an invented contract.

### Java: persistence adapters and migration

Add News-specific JDBC adapters and a `NewsPersistenceFactory`, using the existing `JdbcTemplate`/`TransactionTemplate` pattern. Do not expose table rows outside persistence. Explicit operations insert/find canonical URLs, associate existing Assets, claim work using `FOR UPDATE SKIP LOCKED`, reserve a unique lease token, fence mutations by token/hash/model, consume a conservative dispatch-attempt reservation immediately before transport handoff, schedule retries, atomically complete results, and provide public/audit projections.

All PostgreSQL work follows one lock order: register or verify the Sentiment Model Release in its own short transaction first; then lock News Items in News ID order; then insert/read Sentiment Results; then lock News Item Asset rows in Asset ID order when an operation includes associations. Result-completion code MUST already hold the parent News Item lock before a parent-hash trigger reads it. No transaction may insert a release while holding a News/result lock. Runtime `register-or-verify` uses insert-on-conflict followed by tuple comparison and returns a stable conflict for different provenance.

Lease claims use a configurable bounded batch, default 25, ordered by effective eligibility instant ascending and News ID ascending. Effective eligibility is `lease_expires_at` for expired `ANALYZING` rows and `COALESCE(next_eligible_attempt, crawled_at)` otherwise. Claim/completion transactions use scoped configurable PostgreSQL lock and statement timeouts (defaults 2 seconds and 5 seconds). SQLSTATE `40P01` and `40001` are translated to bounded recoverable persistence outcomes; persistence code performs no hidden in-transaction retry loop, allowing lease expiry/Worker scheduling to recover safely.

Create one forward migration after the two applied migrations. It adds News language, durable lease fields and a persisted target model release, expands the status check, creates `news.sentiment_model_release`, adds result language and the release foreign key, and creates eligibility/recovery indexes and integrity/immutability triggers. Existing result rows require an explicit reviewed release mapping; the migration aborts rather than inventing provenance. See [data-model.md](data-model.md).

### Java: Worker orchestration and HTTP adapter

`apps/worker` composes collection schedules and analysis coordination. A cycle checks an independently probed/cached readiness state, claims a bounded ordered batch, acquires a local concurrency permit, acquires a circuit permission, atomically consumes one dispatch-attempt reservation, hands exactly one request to the transport, validates the outcome, and completes or durably schedules failure. Readiness rejection occurs before claim. If concurrency/circuit admission fails, the Worker releases/reschedules the lease without consuming a reservation. If attempt start becomes stale or database-rejected after a circuit permit was acquired, it explicitly releases the unused circuit permit and local concurrency permit.

The reservation-to-network boundary is deliberately conservative: after the fenced database increment succeeds, a process crash leaves the reservation consumed even if it cannot be proven that the HTTP request left the process. Reclaim uses the remaining budget, and actual calls can therefore be fewer than—but never exceed—the configured reservations. This is the accepted resolution of the cross-resource atomicity gap; no Outbox is introduced.

The Worker-local adapter uses Java 21 asynchronous `HttpClient`, bounded responses, correlation headers, optional environment service token, a local concurrency permit, Resilience4j `CircuitBreaker`, and `TimeLimiter`. JDK `HttpClient` owns the 2-second connect timeout; TimeLimiter is the single end-to-end 30-second response deadline and performs best-effort future cancellation. A timeout records and persists one outcome; any late completion is ignored. There is no Resilience4j Retry, blocking sleep, recursive call, synchronous retry loop, or second response-deadline race.

Circuit breaker and concurrency state are per Worker process; aggregate deployment capacity equals the per-process limit multiplied by Worker replicas. The readiness probe is low-rate, separately scheduled, and not decorated by the inference circuit. Circuit accounting is:

| Outcome | Attempt reservation | Circuit record | Durable action |
|---|---:|---|---|
| Cached/probed not ready or no work after readiness preflight | No | None | End cycle or defer without acquiring a circuit permit |
| Local concurrency unavailable or circuit/half-open rejection | No | None | Release/defer lease; open circuit defers no earlier than its remaining/default interval |
| Stale/DB-rejected attempt start after circuit permit | No | Explicit unused-permit release | No HTTP call; release/defer as applicable |
| Compatible valid `2xx` | Yes | Success once | Complete result; later DB failure does not rewrite circuit outcome |
| Connection failure, TimeLimiter timeout, `429`, dispatched `503 NOT_READY`, eligible `5xx` | Yes | Failure once | `FAILED_RETRYABLE` with 5/30 delay or terminal on exhausted budget |
| Invalid/malformed/oversized/mismatched `2xx` | Yes | Failure once | Permanent `FAILED` for unchanged content/release |
| Authentication, unsupported language/release, invalid unchanged input, other permanent `4xx` | Yes | Ignored | Permanent `FAILED` without retry |

Every admitted circuit permit receives exactly one success, failure, or unused release. `Retry-After` can only push eligibility later.

### Java: API and Market Data composition

Keep authenticated `GET /news-items`. `apps/api` resolves a Trading Pair through the existing public Market Data reference boundary, passes base and quote `AssetId`s to News, and returns each item once if related to either. The default response exposes `analysisStatus` and only `label`, `confidence`, and `polarityScore` for analyzed items.

Provide provenance through a separate internal service-token/network-protected audit endpoint and application port. Do not claim an operator role that the current authentication foundation does not define. Update repository OpenAPI during implementation.

### Python: FastAPI service and routes

Create an application factory with FastAPI lifespan, strict Pydantic edge schemas, stable safe errors, optional service-token authentication, and body/text limits. Routes are `POST /api/v1/sentiment/analyze`, `GET /health/live`, and `GET /health/ready`.

The application imports only lightweight FastAPI/configuration modules before serving liveness. Startup schedules exactly one off-event-loop loader; TensorFlow/Keras is lazily imported inside that loader, which validates manifest/checksums, loads tokenizer/model, validates shapes/finite output, and performs one warm-up. State is `LOADING -> READY` for a healthy process. Readiness is `503` while loading and the deadline defaults to 120 seconds. Artifact/load/warm-up failure or deadline expiry records a safe terminal metric/log and terminates the process nonzero; the supervisor restarts/replaces it. A timed-out background load can never publish READY in the dying process, avoiding an indefinitely live process with an unkillable TensorFlow thread. There is no request-triggered initialization or reload loop. Use one Uvicorn worker per process and scale with replicas.

### Python: Multi-channel model release and inference

The upstream repository is pinned at research commit `fd1163a88d04e61e2b19a34e07da99e10acb6288`. It supplies legacy architecture/training code but no usable trained weights or serialized vocabulary. Model preparation is therefore an offline, reproducible release gate, never startup/default CI.

The offline workflow must receive verified matching artifacts or train an approved release from licensed data, modernize the documented architecture to a serialization-safe compatible TensorFlow/Keras implementation, and emit a bundle containing model, frozen vocabulary/tokenizer, manifest, checksums, label map, preprocessing/padding policy, source commit, training provenance, and dependency-lock identity. Distribution is blocked until GPL-3.0 and dataset/weight rights are reviewed. Release acceptance additionally requires uploading the immutable bundle to the approved artifact location, pinning its digest and exact model/preprocessing/contract versions in Compose/deployment configuration, and recording a real process cold-start smoke result within the 120-second default deadline.

Each Python process serves exactly one release. Model replacement is a sequential deployment of a new single-release process. Java registers the new release before assigning News work, resets attempt/eligibility state for that new target release, and retains prior immutable results. Concurrent multi-model serving in one Python process is outside F-008.

The initial upstream-faithful preprocessor lowercases and uses a frozen whitespace vocabulary. Missing/incompatible vocabulary produces a safe terminal startup failure and nonzero exit; the service never falls back, downloads, or rebuilds. Preserve max length 400 and pre-padding/pre-truncation unless a new release versions that behavior. PAD/OOV IDs must be fixed before training.

Inference joins title/content with a versioned separator, maps indices `0=POSITIVE`, `1=NEGATIVE`, `2=NEUTRAL`, applies a deterministic tie rule, sets confidence to max probability and polarity to `pPositive - pNegative`, and serializes scale-10 canonical decimals with half-even rounding. Invalid shapes, non-finite values, negatives, or non-normalized output fail safely.

## Contract Plan

The files in [contracts/](contracts/README.md) are the language-neutral `sentiment-v1` design source. During implementation, canonical copies move into `modules/contracts` resources so Java and Python tests load the same physical artifacts. Java transport DTOs live in `modules/contracts`; semantic types remain News-owned.

Requests contain request/News identity, normalized English title/content, `en`, content hash, and exact release expectation. Success echoes all identity/provenance and returns label, exact decimal strings and UTC time. Java performs semantic echo validation after schema validation. Safe errors classify transient/permanent outcomes without content, paths or stack traces. Health represents loading, ready, failed and timed-out states without artifact paths.

## Verification Plan

### Java evidence

- News unit tests: IDs/invariants, transitions, canonicalization/hash goldens, sanitization, language/limits, duplicates, decimals, fake-clock retry, response validation, and a provider contract suite against at least two adapters.
- PostgreSQL tests: migration/backfill guard, checks/FKs/triggers/indexes, deterministic bounded claim ordering, concurrent URL uniqueness, exclusive/reclaimed leases, stale fencing, reservation accounting including crash-before-transport, atomic completion/rollback, duplicate result, two sequential releases/attempt reset, concurrent identical/conflicting release registration, global lock-order stress, forced `40P01`/`40001` translation, immutability, exact values, and pair filtering. H2 is not constraint evidence; tests use barriers and bounded database timeouts rather than sleeps.
- Worker tests use MockWebServer only with deterministic Dispatchers, barriers/latches, controlled executors, injectable Clock/ID sources, and explicit circuit state transitions. They never use wall-clock sleeps or unreachable-host timing. Tests cover the entire outcome matrix, exact single permit callback, unused half-open permit release, process-local limits, one transport handoff per reservation, reservation-before-dispatch crash, cancellation/late completion, 5/30 eligibility, `Retry-After`, recovery, malformed responses, correlation and redaction.
- API/security tests: authentication, all states, lightweight analyzed sentiment, no fabricated neutral, pagination/filtering and no provenance/lease leakage.
- Architecture/failure-isolation tests: dependency rules and continued API, News, Market Data and technical Backtest operation when inference fails.

### Python evidence

- Default tests inject a deterministic `InferenceEngine`, Clock, ID source, and executor; coordinate concurrency with events rather than sleeps; and assert TensorFlow/Keras is not imported before the loader is invoked.
- Contract/route tests run shared valid/invalid fixtures, strict sizes/auth, echo, `en`/release mismatch, errors and decimals.
- Runtime tests cover exactly-once load, checksum/tokenizer/model mismatch, warm-up failure, fake-clock timeout, nonzero fatal-exit request, no late READY, health transitions and deterministic capacity.
- Preprocessing/inference goldens cover Unicode/case/whitespace/OOV, separator, pre-padding/truncation 400, labels/ties, confidence/polarity, rounding, bounds and NaN/Inf.
- A release-gated real-bundle smoke test starts a fresh process, proves liveness precedes TensorFlow/artifact loading, measures readiness within 120 seconds, and validates digest/shape/contract without downloading, training, or claiming accuracy. Default CI remains model-free, but this smoke evidence and the pinned bundle are mandatory for release readiness.

### Cross-runtime evidence

Both runtimes execute the same schema/fixture matrix for fields, IDs, hashes, UTC times, decimals, release/language compatibility, errors and health. Java rejects every echo/provenance mismatch. Model quality requires separate reviewable evaluation and is not inferred from label counts.

## Delivery Sequence

1. Freeze `sentiment-v1`, canonical normalization/hash rules, projections and initial provider choice.
2. Add the forward migration and PostgreSQL verification.
3. Build News domain/application ports and unit/contract suites.
4. Build JDBC adapters and Worker lease orchestration with a fake inference port.
5. Build transport DTOs, HTTP adapter and resilience tests.
6. Build public API and protected audit composition.
7. Build FastAPI routes/runtime against a fake engine and shared fixtures.
8. Produce the separately reviewed immutable model bundle, upload it, pin its digest/release metadata, and record the real cold-start smoke result.
9. Compose containers with fatal-startup restart/replacement behavior, observability and failure-isolation verification; keep evidence `Planned` until tests run.

## Complexity Tracking

No Constitution violations require justification. The second runtime and HTTP boundary are mandated by Accepted ADR-0008. The lease token, release table and shared contracts reduce concurrency/provenance risk within that architecture.

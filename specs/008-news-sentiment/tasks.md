# Tasks: F-008 News and Sentiment

**Input**: Design documents from `specs/008-news-sentiment/`

**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/`, `quickstart.md`

**Tests**: The approved specification requires unit, contract, PostgreSQL integration, architecture, resilience, failure-isolation, security, and release-gated real-bundle smoke evidence. Test tasks precede their corresponding implementation tasks.

**Organization**: Tasks are ordered by shared setup/foundation and then by the four approved user stories. The requested Database, Java Domain & Ports, Java Collection & Orchestration, Python Sentiment Service, Java Sentiment Client, and Cross-cutting workstreams are identified within those phases.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel because it changes different files and does not depend on an incomplete task in the same phase.
- **[US1]–[US4]**: Maps the task to an approved user story.
- Every task names the intended file or files.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Prepare dependency, source-set, and runtime skeletons without implementing feature behavior.

- [X] T001 Add pinned jsoup, Resilience4j CircuitBreaker/TimeLimiter, MockWebServer, and PostgreSQL test dependencies to `gradle/libs.versions.toml`
- [X] T002 Configure `modules/news` to expose `modules/domain` and use jsoup/JUnit dependencies in `modules/news/build.gradle.kts`
- [X] T003 [P] Configure `modules/contracts` JSON resources and Jackson test support in `modules/contracts/build.gradle.kts`
- [X] T004 Configure the News dependency and PostgreSQL-backed `newsIntegrationTest` source set/task in `modules/persistence/build.gradle.kts`
- [X] T005 Configure News, Contracts, Persistence, JDK HTTP, Resilience4j, Jackson, and MockWebServer dependencies in `apps/worker/build.gradle.kts`
- [X] T006 [P] Create the pinned Python application/test dependency manifest and package roots in `apps/sentiment/pyproject.toml`, `apps/sentiment/app/__init__.py`, and `apps/sentiment/tests/__init__.py`

**Checkpoint**: Java modules resolve their intended dependencies and the Python package can be installed without loading a model.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Establish the database, Java domain/ports, and shared wire contract required by every story.

**Critical**: Complete this phase before user-story implementation.

### Workstream 1 — Database: forward migration

- [X] T007 Add failing pgTAP assertions for News language, all five statuses, lease-field consistency, nonnegative attempts, target release, and due/expired indexes in `supabase/tests/database/003_news_sentiment_test.sql`
- [X] T008 Extend the failing pgTAP suite with model-release PK/nonblank/FK checks, result language/ranges/unique identity, hash integrity, restrictive FKs, and result/release immutability cases in `supabase/tests/database/003_news_sentiment_test.sql`
- [X] T009 Implement the new forward-only News/Sentiment migration, leaving both applied migrations unchanged, in `supabase/migrations/20260830000100_add_news_sentiment_workflow.sql`
- [X] T010 Add empty-baseline success, explicit legacy model mapping, unmapped-legacy abort, and concurrent conflicting-release migration fixtures to `supabase/tests/database/003_news_sentiment_test.sql`
- [X] T011 Add a static migration contract test that checks applied migration checksums remain unchanged and the F-008 migration is ordered last in `modules/persistence/src/test/java/com/cryptostrategy/platform/persistence/internal/news/NewsMigrationContractTest.java`

### Workstream 2 — Java Domain & Ports

- [X] T012 [P] Add failing invariant tests for News IDs, URLs, hashes, language, statuses, leases, exact decimals, release identity, and immutable results in `modules/news/src/test/java/com/cryptostrategy/platform/news/api/model/NewsDomainModelTest.java`
- [X] T013 Implement `NewsId`, `SentimentResultId`, `ContentHash`, `CanonicalNewsUrl`, `NewsSource`, and `LanguageCode` in `modules/news/src/main/java/com/cryptostrategy/platform/news/api/model/`
- [X] T014 Implement `AnalysisStatus`, `AnalysisLease`, `RelatedNewsAsset`, and canonical `NewsItem` in `modules/news/src/main/java/com/cryptostrategy/platform/news/api/model/`
- [X] T015 Implement `SentimentLabel`, `SentimentModelRelease`, `SentimentResult`, `SentimentAnalysisRequest`, and `SentimentAnalysisOutcome` with `BigDecimal`/`Instant` semantics in `modules/news/src/main/java/com/cryptostrategy/platform/news/api/model/`
- [X] T016 [P] Add compile-time/public-contract tests for collection, work lifecycle, query, provider, persistence, asset resolution, and inference ports in `modules/news/src/test/java/com/cryptostrategy/platform/news/api/port/NewsPortContractTest.java`
- [X] T017 Define input ports and commands for collection, acquire/start/complete/fail analysis, public listing, and audit lookup in `modules/news/src/main/java/com/cryptostrategy/platform/news/api/port/in/`
- [X] T018 Define output ports for providers, News storage, analysis work, release storage, public/audit queries, asset resolution, and semantic inference in `modules/news/src/main/java/com/cryptostrategy/platform/news/api/port/out/`
- [X] T019 [P] Add failing factory-composition tests for injected ports, `Clock`, normalization policy, retry policy, and release configuration in `modules/news/src/test/java/com/cryptostrategy/platform/news/api/NewsModuleFactoryTest.java`
- [X] T020 Implement `NewsModuleFactory` and stable News error types without framework/transport dependencies in `modules/news/src/main/java/com/cryptostrategy/platform/news/api/NewsModuleFactory.java` and `modules/news/src/main/java/com/cryptostrategy/platform/news/api/error/`
- [X] T021 [P] Extend module boundary tests to forbid News dependencies on Contracts, Spring HTTP, persistence internals, Market Data internals, Strategy, Backtest, Search, Evaluation, Leaderboard, and app packages in `architecture-tests/src/test/java/com/cryptostrategy/platform/architecture/NewsArchitectureTest.java`

### Workstream 6 — Shared contract foundation

- [X] T022 Copy the approved `sentiment-v1` JSON Schemas and seed fixtures into canonical runtime resources at `modules/contracts/src/main/resources/contracts/sentiment-v1/`
- [X] T023 [P] Add failing Java schema/fixture inventory tests, including exact-decimal and closed-object rules, in `modules/contracts/src/test/java/com/cryptostrategy/platform/contracts/sentiment/v1/SentimentV1SchemaTest.java`
- [X] T024 Implement immutable Java transport DTOs for analyze request/success/error and health payloads in `modules/contracts/src/main/java/com/cryptostrategy/platform/contracts/sentiment/v1/`

**Checkpoint**: The forward migration, domain boundary, and one language-neutral wire contract are ready for story work.

---

## Phase 3: User Story 1 — Collect Trustworthy Canonical News (Priority: P1) MVP

**Goal**: Collect safe canonical News from multiple independent providers, deduplicate by canonical URL, persist it, and keep it visible without Sentiment availability.

**Independent Test**: Run deterministic valid/duplicate/HTML/script/reordered/malformed fixtures through two provider implementations and verify one durable canonical News Item per URL with stable hash, provenance, related Assets, and `PENDING` state while Sentiment is stopped.

### Workstream 3 — Java Collection & Deduplication

- [X] T025 [P] [US1] Add golden tests for HTML sanitation, Unicode/whitespace normalization, URL canonicalization, size limits, and `news-canonical-v1` hashes in `modules/news/src/test/java/com/cryptostrategy/platform/news/internal/normalization/CanonicalNewsNormalizerTest.java`
- [X] T026 [P] [US1] Add a reusable provider contract suite for valid, duplicate, reordered, malformed, timeout, and conflicting-URL fixtures in `modules/news/src/test/java/com/cryptostrategy/platform/news/provider/NewsProviderContract.java`
- [X] T027 [US1] Implement `CanonicalNewsNormalizer`, `CanonicalNewsUrlV1`, and `NewsContentHashV1` from the approved canonical contract in `modules/news/src/main/java/com/cryptostrategy/platform/news/internal/normalization/`
- [X] T028 [P] [US1] Implement the deterministic fixture provider adapter in `modules/news/src/main/java/com/cryptostrategy/platform/news/internal/provider/fixture/FixtureNewsProvider.java`
- [X] T029 [P] [US1] Implement the RSS/Atom transport, mapper, and error translator using JDK HTTP/StAX behind `NewsProvider` in `modules/news/src/main/java/com/cryptostrategy/platform/news/internal/provider/rss/`
- [X] T030 [US1] Run the common provider contract suite against fixture and RSS/Atom adapters in `modules/news/src/test/java/com/cryptostrategy/platform/news/provider/ConfiguredNewsProvidersTest.java`
- [X] T031 [P] [US1] Add failing collection-service tests for multi-provider isolation, sanitization, unsupported language, duplicate URL, content conflict, related-asset mapping, and Sentiment-independent acceptance in `modules/news/src/test/java/com/cryptostrategy/platform/news/internal/application/NewsCollectionServiceTest.java`
- [X] T032 [US1] Implement provider-neutral `NewsCollectionService` and stable acceptance/rejection outcomes in `modules/news/src/main/java/com/cryptostrategy/platform/news/internal/application/NewsCollectionService.java`
- [X] T033 [P] [US1] Add PostgreSQL tests for concurrent canonical-URL insertion, source identity, association uniqueness, conflict safety, and `PENDING` visibility in `modules/persistence/src/newsIntegrationTest/java/com/cryptostrategy/platform/persistence/news/NewsCollectionPersistenceIntegrationTest.java`
- [X] T034 [US1] Implement News row mappings, SQL, exception translation, item store, and idempotent asset association in `modules/persistence/src/main/java/com/cryptostrategy/platform/persistence/internal/news/NewsRows.java`, `NewsSql.java`, `NewsPersistenceExceptionTranslator.java`, and `JdbcNewsItemStoreAdapter.java`
- [X] T035 [US1] Compose configured providers, read-only Asset resolution, collection schedule, and safe collection metrics/logging in `apps/worker/src/main/java/com/cryptostrategy/platform/worker/config/NewsWorkerConfiguration.java`, `apps/worker/src/main/java/com/cryptostrategy/platform/worker/news/collection/NewsCollectionScheduler.java`, and `apps/worker/src/main/resources/application.yml`

**Checkpoint**: US1 is independently demonstrable with the Sentiment service absent.

---

## Phase 4: User Story 2 — Analyze English News Without Blocking the Platform (Priority: P1)

**Goal**: Analyze pending English News asynchronously with durable leases, strict response validation, bounded Worker retry, circuit isolation, and idempotent result persistence.

**Independent Test**: Seed pending News, run the Worker against MockWebServer, and verify success, duplicate delivery, timeout/429/5xx/permanent failure, 5/30-second eligibility, pre-dispatch zero-reservation deferral, crash/reclaim, stale-response rejection, and platform availability without a real ML model.

### Workstream 3 — Worker Lease Handling and Persistence

- [X] T036 [P] [US2] Add PostgreSQL concurrency tests for `SKIP LOCKED` exclusivity, expired reclaim, unique lease tokens, and stale completion rejection in `modules/persistence/src/newsIntegrationTest/java/com/cryptostrategy/platform/persistence/news/AnalysisLeaseIntegrationTest.java`
- [X] T037 [P] [US2] Add PostgreSQL tests for conservative attempt reservation, crash after reservation before transport handoff, no-reservation deferral, 5/30 eligibility, terminal failure, atomic completion rollback, duplicate-result equivalence, and per-target-release budget reset in `modules/persistence/src/newsIntegrationTest/java/com/cryptostrategy/platform/persistence/news/SentimentResultIntegrationTest.java`
- [X] T038 [P] [US2] Add PostgreSQL runtime tests for concurrent identical/conflicting model-release register-or-verify and sequential release replacement in `modules/persistence/src/newsIntegrationTest/java/com/cryptostrategy/platform/persistence/news/SentimentModelReleaseConcurrencyIntegrationTest.java`
- [X] T039 [P] [US2] Add barrier-driven PostgreSQL stress tests for release-to-News-to-result lock order, eligibility-then-News-ID batch ordering, bounded batch size, and recoverable `40P01`/`40001` translation without sleeps in `modules/persistence/src/newsIntegrationTest/java/com/cryptostrategy/platform/persistence/news/NewsDeadlockRecoveryIntegrationTest.java`
- [X] T040 [US2] Implement fenced deterministic claim/reclaim, conservative attempt reservation, deferral, retry, failure, and completion SQL with scoped lock/statement timeouts in `modules/persistence/src/main/java/com/cryptostrategy/platform/persistence/internal/news/JdbcAnalysisWorkStoreAdapter.java`
- [X] T041 [US2] Implement short-transaction immutable model-release register-or-verify before News locking and idempotent Sentiment Result persistence in `modules/persistence/src/main/java/com/cryptostrategy/platform/persistence/internal/news/JdbcSentimentModelReleaseStore.java`
- [X] T042 [US2] Translate PostgreSQL `40P01`/`40001` to bounded recoverable outcomes and expose News adapters through `modules/persistence/src/main/java/com/cryptostrategy/platform/persistence/internal/news/NewsPersistenceExceptionTranslator.java` and `modules/persistence/src/main/java/com/cryptostrategy/platform/persistence/api/NewsPersistenceFactory.java`
- [X] T043 [P] [US2] Add failing state-machine and fake-clock retry-policy tests in `modules/news/src/test/java/com/cryptostrategy/platform/news/internal/application/NewsAnalysisServiceTest.java`
- [X] T044 [P] [US2] Add failing semantic response tests for every request/News/hash/language/release echo mismatch, invalid label/decimal/range/time, and duplicate equivalence in `modules/news/src/test/java/com/cryptostrategy/platform/news/internal/validation/SentimentResponseValidatorTest.java`
- [X] T045 [US2] Implement state-transition/retry policy and analysis application service in `modules/news/src/main/java/com/cryptostrategy/platform/news/internal/application/NewsAnalysisService.java`
- [X] T046 [US2] Implement strict semantic outcome validation in `modules/news/src/main/java/com/cryptostrategy/platform/news/internal/validation/SentimentResponseValidator.java`

### Workstream 5 — Java Sentiment Client and Resilience

- [X] T047 [P] [US2] Add configuration validation tests for endpoint, service token, 2-second connect timeout, TimeLimiter-owned 30-second response deadline, per-Worker concurrency/circuit state, three-reservation budget, 120-second lease, 50%/10-call/30-second circuit defaults, and 5/30 delays in `apps/worker/src/test/java/com/cryptostrategy/platform/worker/config/NewsWorkerPropertiesTest.java`
- [X] T048 [US2] Implement typed per-Worker configuration properties without a Resilience4j Retry or second response-timeout setting in `apps/worker/src/main/java/com/cryptostrategy/platform/worker/config/NewsWorkerProperties.java` and `apps/worker/src/main/resources/application.yml`
- [X] T049 [P] [US2] Add MockWebServer contract tests for v1 mapping, authorization/correlation headers, body limits, safe error mapping, and response echo validation in `apps/worker/src/test/java/com/cryptostrategy/platform/worker/news/sentiment/HttpSentimentInferenceAdapterTest.java`
- [X] T050 [P] [US2] Add deterministic MockWebServer resilience tests for the complete circuit outcome matrix, exactly one success/failure/unused-permit callback, no-work-before-circuit behavior, half-open stale-start permit release, process-local limits, cancellation/late completion, `Retry-After`, and no internal retry using Dispatchers, barriers/latches, controlled executors, explicit circuit transitions, and no sleeps/unreachable-host timing in `apps/worker/src/test/java/com/cryptostrategy/platform/worker/news/sentiment/SentimentClientResilienceTest.java`
- [X] T051 [US2] Implement v1 mapping and bounded asynchronous JDK HTTP transport with connect timeout only and best-effort cancellation under the TimeLimiter deadline in `apps/worker/src/main/java/com/cryptostrategy/platform/worker/news/sentiment/SentimentContractMapper.java` and `HttpSentimentInferenceAdapter.java`
- [X] T052 [US2] Implement the exact outcome-recording matrix, per-process concurrency admission, Resilience4j CircuitBreaker/TimeLimiter, and explicit unused-permit release while excluding Retry in `apps/worker/src/main/java/com/cryptostrategy/platform/worker/news/sentiment/SentimentClientGuard.java`
- [X] T053 [P] [US2] Add deterministic Worker coordinator tests for readiness-before-claim, ordered bounded claims, permit-before-reservation ordering, crash immediately after reservation and before transport handoff, zero-reservation deferrals, persistence failure after HTTP success, crash recovery, and late stale responses in `apps/worker/src/test/java/com/cryptostrategy/platform/worker/news/analysis/NewsAnalysisCoordinatorTest.java`
- [X] T054 [US2] Implement nonblocking readiness probing, ordered lease polling, conservative reservation/coordinating, persisted retry eligibility, and no sleep/recursive retry in `apps/worker/src/main/java/com/cryptostrategy/platform/worker/news/analysis/NewsAnalysisCoordinator.java` and `NewsAnalysisScheduler.java`
- [X] T055 [US2] Add safe structured correlation/News logging and per-Worker analysis/lease/retry/circuit metrics without article bodies or credentials in `apps/worker/src/main/java/com/cryptostrategy/platform/worker/news/analysis/NewsAnalysisObservability.java`

**Checkpoint**: US2 passes entirely against a fake HTTP service and never loads TensorFlow.

---

## Phase 5: User Story 3 — Browse News by Trading Pair and Analysis State (Priority: P2)

**Goal**: Expose authenticated, deterministic News browsing with base-or-quote filtering, explicit analysis states, lightweight sentiment, and protected provenance.

**Independent Test**: Seed dual/single/unrelated Asset associations and all states, page through `GET /news-items`, and verify correct filtering, ordering, exact strings, no fabricated sentiment, and no provenance/lease leakage.

- [X] T056 [P] [US3] Add PostgreSQL query tests for base-or-quote matching, dual-link deduplication, status projection, exact decimals, and `(publishedAt, newsId)` pagination in `modules/persistence/src/newsIntegrationTest/java/com/cryptostrategy/platform/persistence/news/NewsQueryIntegrationTest.java`
- [X] T057 [US3] Implement public and protected audit projections in `modules/persistence/src/main/java/com/cryptostrategy/platform/persistence/internal/news/JdbcNewsQueryAdapter.java` and `JdbcSentimentAuditStore.java`
- [X] T058 [P] [US3] Add News query service tests for cursor validation, stable ordering, supplied base/quote Asset IDs, and non-analyzed sentiment suppression in `modules/news/src/test/java/com/cryptostrategy/platform/news/internal/application/NewsQueryServiceTest.java`
- [X] T059 [US3] Implement `NewsQueryService`, `NewsListQuery`, opaque cursor, public page, and audit result models in `modules/news/src/main/java/com/cryptostrategy/platform/news/internal/application/NewsQueryService.java` and `modules/news/src/main/java/com/cryptostrategy/platform/news/api/model/`
- [X] T060 [P] [US3] Add authenticated controller tests for all analysis states, lightweight analyzed sentiment, no neutral fabrication, pair filtering, pagination, and field non-disclosure in `apps/api/src/test/java/com/cryptostrategy/platform/api/news/NewsControllerTest.java`
- [X] T061 [P] [US3] Add internal service-token audit endpoint authorization and provenance tests in `apps/api/src/test/java/com/cryptostrategy/platform/api/news/NewsAuditControllerTest.java`
- [X] T062 [US3] Compose Market Data pair resolution with News query ports and implement public DTO/controller mapping in `apps/api/src/main/java/com/cryptostrategy/platform/api/config/NewsConfiguration.java`, `apps/api/src/main/java/com/cryptostrategy/platform/api/news/NewsController.java`, and `NewsResponse.java`
- [X] T063 [US3] Implement the network/service-token-protected audit controller without browser exposure in `apps/api/src/main/java/com/cryptostrategy/platform/api/news/NewsAuditController.java` and `apps/api/src/main/java/com/cryptostrategy/platform/api/config/SecurityConfiguration.java`
- [X] T064 [US3] Update the public News and protected audit API definitions with exact decimal strings and provenance separation in `docs/api/openapi.yaml`

**Checkpoint**: US3 is independently testable using persisted fixtures; Python availability does not affect browsing.

---

## Phase 6: User Story 4 — Operate and Replace the Sentiment Model Safely (Priority: P2)

**Goal**: Provide a stateless FastAPI service with strict v1 routes, eager one-time model initialization, deterministic preprocessing/inference mapping, exact release validation, and replaceable immutable artifacts.

**Independent Test**: Use a fake inference engine to verify contracts and health transitions, then opt in to one reviewed bundle smoke test; serving a new release must preserve the old Java result and reject incompatible expected releases.

### Workstream 4 — Python FastAPI and Model Runtime

- [ ] T065 [P] [US4] Add strict Pydantic/schema parity tests for valid and invalid shared request/response/error/health fixtures in `apps/sentiment/tests/contract/test_sentiment_v1_contract.py`
- [ ] T066 [P] [US4] Add FastAPI route tests with injected Clock/ID/executor for auth, limits, `en`/release compatibility, provenance echo, canonical decimals, safe errors, and no URL fetching or translation in `apps/sentiment/tests/unit/test_sentiment_routes.py`
- [ ] T067 [P] [US4] Add deterministic fake-clock/runtime tests for liveness before TensorFlow import, exactly one loader, readiness, checksum/manifest/tokenizer/model/warm-up failure, nonzero exit on the 120-second deadline, and no late READY using events rather than sleeps in `apps/sentiment/tests/unit/test_model_runtime.py`
- [ ] T068 [P] [US4] Add deterministic preprocessing/inference goldens for NFC/case/whitespace/OOV, title-content separator, pre-padding/pre-truncation 400, class/tie mapping, polarity/confidence, half-even decimals, bounds, NaN/Inf, and event-controlled capacity in `apps/sentiment/tests/unit/test_multichannel_engine.py`
- [ ] T069 [US4] Implement typed settings, injected Clock/ID/executor, safe errors, service-token authentication, and redacted structured logging in `apps/sentiment/app/core/config.py`, `runtime_dependencies.py`, `errors.py`, `auth.py`, and `logging.py`
- [X] T070 [US4] Implement strict analyze/error/health Pydantic schemas matching `sentiment-v1` in `apps/sentiment/app/api/schemas/sentiment.py`, `error.py`, and `health.py`
- [X] T071 [P] [US4] Define framework-neutral `InferenceEngine`, release manifest, runtime state, and artifact checksum contracts without importing TensorFlow in `apps/sentiment/app/model/protocol.py`, `manifest.py`, and `runtime_state.py`
- [X] T072 [US4] Implement the frozen whitespace tokenizer, PAD/OOV handling, max-length 400 pre-padding/pre-truncation, and versioned join rule in `apps/sentiment/app/model/tokenizer.py` and `preprocessing.py`
- [X] T073 [US4] Implement serialization-safe Multi-channel LSTM-CNN loading/inference with TensorFlow/Keras imported only inside the off-event-loop loader in `apps/sentiment/app/model/multichannel_engine.py`
- [X] T074 [US4] Implement one-time off-event-loop manifest/checksum/model/tokenizer load, warm-up, fatal nonzero exit on terminal failure/timeout, late-ready fencing, and bounded inference capacity in `apps/sentiment/app/model/runtime.py`
- [X] T075 [US4] Implement `POST /api/v1/sentiment/analyze`, `/health/live`, `/health/ready`, and the lightweight FastAPI application/lifespan bootstrap in `apps/sentiment/app/api/routes/sentiment.py`, `health.py`, and `apps/sentiment/app/main.py`
- [X] T076 [P] [US4] Add a dependency-boundary test that rejects PostgreSQL, Supabase, Redis, crawler, request-URL-fetch imports, and TensorFlow/Keras import before loader invocation in `apps/sentiment/tests/unit/test_stateless_boundary.py`
- [X] T077 [US4] Record GPL-3.0, dataset/weight provenance, tokenizer compatibility, and distribution approval gates for the pinned upstream commit in `apps/sentiment/artifacts/RELEASE-GATES.md`
- [X] T078 [US4] Implement the offline-only release-bundle builder/verifier that requires approved model/vocabulary inputs and records source commit, preprocessing, labels, training provenance, dependency identity, and checksums in `apps/sentiment/tools/build_release_bundle.py` and `apps/sentiment/artifacts/manifest.schema.json`
- [ ] T079 [US4] Add an environment-gated real-bundle smoke test that never downloads/trains and validates manifest/digest/load/warm-up/shape/contract behavior in `apps/sentiment/tests/smoke/test_release_bundle.py`
- [ ] T080 [US4] Add a fresh-process cold-start test proving liveness precedes TensorFlow/artifact loading, readiness completes within 120 seconds, and timeout exits nonzero under supervisor replacement in `apps/sentiment/tests/smoke/test_cold_start_process.py`
- [ ] T081 [US4] Produce and upload the approved immutable model/tokenizer bundle, pin its digest and exact release metadata for deployment, and record its release evidence in `apps/sentiment/artifacts/active-release.lock.json` and `specs/008-news-sentiment/evidence.md`

**Checkpoint**: US4 default tests pass using a fake engine; F-008 release readiness additionally requires T079-T081 to pass with the approved digest-pinned bundle.

---

## Phase 7: Cross-Cutting Contract, Compose, Security, and Evidence

**Purpose**: Prove both runtimes agree and the integrated deployment preserves isolation and observability.

### Workstream 6 — Contract Tests and Docker Compose

- [ ] T082 [P] Expand canonical valid/invalid/error/health fixture coverage and ensure both runtimes load the same inventory in `modules/contracts/src/main/resources/contracts/sentiment-v1/fixtures/`
- [X] T083 Add Java/Python contract parity execution and fixture-drift checks to `.github/workflows/ci.yml`
- [ ] T084 [P] Create the internal-only, one-Uvicorn-worker Python container with digest-pinned immutable bundle mount, lightweight liveness, readiness, and nonzero fatal-startup behavior in `apps/sentiment/Dockerfile`
- [ ] T085 Add the Sentiment service, private networking, readiness, restart/replacement policy for fatal startup, token injection, pinned bundle mount, and per-Worker Java endpoint/limit configuration to `infra/compose/docker-compose.yml`
- [ ] T086 [P] Add a Compose integration test for slow load, fatal timeout, process replacement, readiness recovery, and no late READY from the terminated process in `apps/sentiment/tests/smoke/test_compose_startup_recovery.py`
- [ ] T087 [P] Add API/Worker/Python security tests for secret/body/path/SQL/stack-trace redaction and browser-to-Python denial in `apps/api/src/test/java/com/cryptostrategy/platform/api/news/NewsSecurityIntegrationTest.java`, `apps/worker/src/test/java/com/cryptostrategy/platform/worker/news/NewsLoggingSecurityTest.java`, and `apps/sentiment/tests/unit/test_safe_logging.py`
- [ ] T088 Add an end-to-end failure-isolation test proving News visibility, API liveness, Market Data and technical Backtest continuity, five-second degraded observability, per-Worker admission behavior, and recovery without duplicate results in `apps/worker/src/test/java/com/cryptostrategy/platform/worker/news/NewsSentimentFailureIsolationTest.java`
- [ ] T089 [P] Add collection/work/status/latency/circuit/label/release/load/readiness/fatal-startup metrics without presenting label distribution as accuracy in `apps/worker/src/main/java/com/cryptostrategy/platform/worker/news/NewsMetrics.java` and `apps/sentiment/app/core/metrics.py`
- [ ] T090 Run and document the non-production migration dry-run, deterministic Java/Python suites, mandatory release-gated cold-start/Compose smoke evidence, and ADR/Constitution/exclusion review in `specs/008-news-sentiment/quickstart.md` and `specs/008-news-sentiment/evidence.md`

**Checkpoint**: Cross-runtime compatibility, deployment isolation, security, and actual verification evidence are reviewable.

---

## Dependencies & Execution Order

### Phase dependencies

- **Phase 1 — Setup**: Starts immediately.
- **Phase 2 — Foundational**: Depends on Phase 1 and blocks all story phases.
- **Phase 3 — US1**: Depends on Phase 2; delivers the recommended MVP.
- **Phase 4 — US2**: Depends on Phase 2 and uses persisted News supplied by US1 for end-to-end acceptance. Unit/resilience work can begin with fixtures before US1 completes.
- **Phase 5 — US3**: Depends on Phase 2; query/API work can proceed with seeded fixtures, while the final journey depends on US1 persistence.
- **Phase 6 — US4**: Depends on the shared contract portion of Phase 2 and otherwise proceeds independently of Java collection/client work.
- **Phase 7 — Cross-cutting**: Depends on the story components included in the intended release.

### User story dependency graph

```text
Setup -> Database + Domain/Ports + Contract
                     |        |        |
                     v        v        v
                    US1 ----> US2      US4
                     |         |        |
                     `----> US3         |
                          \     |       /
                           Cross-cutting
```

- **US1 (P1)**: Independently valuable after foundation; no Python dependency.
- **US2 (P1)**: Domain/persistence/client tests use fixtures and MockWebServer; full acceptance consumes News from US1.
- **US3 (P2)**: Independently testable with seeded rows; full user journey benefits from US1.
- **US4 (P2)**: Independent after shared contracts; Java integration is proven in cross-cutting parity tests.

### Critical task dependencies

- T009 follows T007-T008; T010-T011 validate T009.
- T013-T015 follow T012; T017-T018 follow the domain models; T020 follows T019 and the ports.
- T024 follows T022-T023.
- T027 follows T025; T030 follows T026, T028 and T029; T032 follows T027 and T031; T034 follows T033.
- T040-T042 follow T036-T039; T045-T046 follow T043-T044.
- T051-T052 follow T047-T050; T054 follows T040-T053; T055 follows T054.
- T057 follows T056; T059 follows T058; T062-T063 follow T057-T061; T064 follows the public/audit implementation.
- T069-T075 follow their corresponding T065-T068 tests; T074 depends on T071-T073; T075 depends on T069-T074.
- T078 follows the T077 release review; T079-T080 add release-gated tests after T074-T078; T081 requires T077-T080 and is mandatory for release readiness while default fake-engine CI remains model-free.
- T083 follows T065 and T082; T084-T086 follow T074-T081; T087-T090 follow the relevant story tasks and produce actual evidence rather than planned claims.

## Parallel Opportunities

### Setup/Foundation

- T003 and T006 can run while Java dependency catalog/build work proceeds.
- Database tests T007-T008, domain tests T012/T016/T019/T021, and contract tests T023 can be authored in parallel before their implementations.

### US1 parallel example

```text
T025 canonicalization/hash tests
T026 provider contract suite
T028 fixture provider adapter
T029 RSS/Atom adapter
T033 PostgreSQL collection tests
```

### US2 parallel example

```text
T036 lease concurrency tests
T037 result/attempt transaction tests
T038 concurrent release registration tests
T039 deadlock/serialization/claim-order tests
T043 state/retry tests
T044 response validation tests
T047 properties tests
T049 HTTP contract tests
T050 deterministic resilience tests
T053 Worker coordinator/crash-window tests
```

### US3 parallel example

```text
T056 persistence query tests
T058 News query service tests
T060 public API tests
T061 audit API tests
```

### US4 parallel example

```text
T065 shared contract tests
T066 route/security tests
T067 runtime/health tests
T068 preprocessing/inference tests
T071 inference protocol/manifest types
T076 stateless-boundary test
```

## Implementation Strategy

### MVP first

1. Complete Setup and Foundational phases.
2. Complete US1 collection and persistence.
3. Stop and run the US1 independent test with Sentiment absent.
4. This is the smallest deployable increment: trustworthy visible News with durable pending analysis state.

### Incremental delivery

1. **US1**: Safe, deduplicated, Sentiment-independent News.
2. **US2**: Durable asynchronous analysis against a fake/compatible service.
3. **US3**: Authenticated pair-filtered public browsing and protected audit provenance.
4. **US4**: Independently deployable Python runtime plus mandatory reviewed, produced, uploaded, and digest-pinned single-release model bundle.
5. **Cross-cutting**: Shared-contract CI, fatal-startup Compose recovery, failure isolation, security, metrics, and recorded evidence.

### Workstream ownership map

| Requested workstream | Primary tasks |
|---|---|
| Database forward migration and deadlock safety | T007-T011, T033-T042, T056-T057 |
| Java Domain & Ports / ArchUnit | T012-T021 |
| Java Collection & Orchestration | T025-T046, T053-T055 |
| Python Sentiment Service | T065-T081 |
| Java Sentiment Client | T047-T052 |
| Contracts and Docker Compose | T022-T024, T082-T090 |

## Notes

- A `[P]` task must still respect the explicit dependencies above.
- Tests should fail for the intended reason before the paired implementation begins.
- PostgreSQL—not H2—is the evidence source for constraints, triggers, `SKIP LOCKED`, and concurrency.
- Default Java tests never load TensorFlow; default Python tests use injected Clock/ID/executor dependencies and a deterministic fake engine without importing TensorFlow before the loader.
- MockWebServer and database concurrency tests use Dispatchers, barriers/latches, controlled executors, explicit circuit transitions, and bounded database timeouts; wall-clock sleeps and unreachable-host timing are forbidden.
- No task grants Python PostgreSQL/Redis access or adds Strategy/Backtest integration.
- Producing, uploading, digest-pinning, and cold-start testing the real single-release model bundle is a release gate after verified weights/vocabulary or approved training data plus licensing/provenance review.

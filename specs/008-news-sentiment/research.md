# Research: F-008 News and Sentiment

## Decision 1: Preserve ADR-0008 runtime and ownership boundary

**Decision**: Java owns News collection, canonicalization, deduplication, lifecycle, persistence ports, validation and public state. Python owns only model loading, model-specific preprocessing and inference. The Worker calls Python over internal HTTP.

**Rationale**: This is the Accepted ADR-0008 decision and isolates Python dependency, startup, memory and inference failures from Market Data and technical Backtest behavior.

**Alternatives rejected**: TensorFlow in Java; Python crawling or writing PostgreSQL; browser-to-Python calls; shared table ownership.

## Decision 2: Use a PostgreSQL lease state machine

**Decision**: Extend `news.news_item` with language, a unique lease token, owner/expiry, attempt count and next eligibility. Claim with `FOR UPDATE SKIP LOCKED`; fence completion by lease token, content hash and model release.

**Rationale**: PostgreSQL remains the source of truth and the workflow survives Worker/cache restarts. A unique token prevents a stale execution using a reused Worker identity from completing a newer lease.

**Alternatives rejected**: Reusing Backtest jobs; Redis-only work state; an Outbox for this feature; owner/expiry without a fencing token.

## Decision 3: Persist retry orchestration; never retry inside the HTTP call

**Decision**: Resilience4j provides CircuitBreaker and TimeLimiter only. The Worker persists `FAILED_RETRYABLE` and future eligibility at 5 and 30 seconds. Every permitted leased execution dispatches exactly one POST.

**Rationale**: This is the approved FR-034/FR-036 clarification and avoids multiplying the three-call budget or holding Worker threads during backoff.

**Alternatives rejected**: Resilience4j Retry, JDK client retries, `Thread.sleep`, recursive immediate retry.

## Decision 4: Separate permission-to-dispatch from attempt consumption

**Decision**: Check readiness and reserve local concurrency/circuit permission before the atomic attempt-start operation. Once the POST is dispatched, the attempt remains consumed, including a raced `503`.

**Rationale**: Open-circuit/readiness deferral must consume zero attempts, while each actual outbound call must consume one before dispatch.

**Alternatives rejected**: Incrementing after response; decrementing attempts after `503`; decorating after increment where circuit rejection consumes an attempt.

## Decision 5: Represent model identity through an immutable release table

**Decision**: Create `news.sentiment_model_release(model_version PK, model_name, preprocessing_version, contract_version)` and reference it from results. Release and accepted result rows are immutable.

**Rationale**: A global model version maps to one exact release tuple without concurrency-sensitive duplication checks on every result.

**Alternatives rejected**: Repeating provenance on every result; deriving metadata from version strings; mutable release rows.

## Decision 6: Abort unsafe legacy provenance migration

**Decision**: Backfill existing News language as `und` where necessary, but require explicit reviewed mappings for every legacy result model version before adding the release FK/not-null constraints. Abort when truthful mapping is unavailable.

**Rationale**: The baseline does not contain model name, preprocessing version or contract version, so they cannot be inferred safely.

**Alternatives rejected**: Inventing generic values or parsing model-version text.

## Decision 7: Keep providers independent behind one port

**Decision**: Configure a list of independent `NewsProvider` adapters. Each maps its private payload to provider-neutral candidates; shared News services perform sanitation, canonicalization, hashing and deduplication. Plan an RSS/Atom adapter and deterministic fixture adapter initially.

**Rationale**: Satisfies FR-054 and prevents provider shape or transport rules from leaking into canonical logic.

**Alternatives rejected**: Provider-specific domain models; one switch statement in the collector; an unrelated generic HTTP module.

**Delivery gate**: Any proprietary production provider requires an explicit API/pagination/authentication/license/retention choice before implementation tasks are finalized.

## Decision 8: Compose Trading Pair filtering outside News ownership

**Decision**: `apps/api` resolves a pair through the existing public Market Data reference boundary and passes base/quote `AssetId`s to News. News queries its own association table using those IDs and cannot create/mutate Market Data records.

**Rationale**: Enables approved base-or-quote filtering while preserving module dependency and data ownership.

**Alternatives rejected**: News importing Market Data internals or joining to gain write authority.

## Decision 9: Use strict shared schemas and semantic validation

**Decision**: `sentiment-v1` JSON Schemas and fixtures are language-neutral source artifacts; both Java and Python run them. Scores are canonical decimal strings compatible with PostgreSQL `numeric(20,10)`. Java additionally validates all echoed identity/provenance.

**Rationale**: Schema checks cannot prove response correspondence to the request; semantic validation closes that gap and exact strings avoid binary floating-point drift.

**Alternatives rejected**: Independent DTO-only contracts; JSON numeric scores; trusting HTTP 200 responses.

Contract v1 fixes normalized title at 1,000 Unicode code points and content at 100,000 code points, with a 256 KiB default HTTP body ceiling. Deployment settings may reduce admission capacity only when clients receive the same stable size error; increasing the versioned field maxima requires contract review.

## Decision 10: Eagerly initialize one immutable Python runtime bundle

**Decision**: FastAPI lifespan starts one off-event-loop load/warm-up. The service is live during loading and ready only after manifest/checksum/tokenizer/model/shape validation and warm-up. Default deadline is 120 seconds; failure/timeout is sticky until restart.

**Rationale**: Heavy TensorFlow initialization must not block liveness or leak into the first analysis request. Sticky failure prevents inconsistent late state.

**Alternatives rejected**: Lazy initialization, download/train at startup, repeated background reload, multiple Uvicorn workers sharing no memory.

## Decision 11: Treat model production as an offline release gate

**Decision**: Pin upstream MultiChannel commit `fd1163a88d04e61e2b19a34e07da99e10acb6288`, but do not claim it is deployable. Produce or convert an approved model bundle offline, with frozen vocabulary, manifest, checksums, label/preprocessing configuration, source/training provenance and locked dependencies.

**Rationale**: Inspection found legacy Python 2/Keras training code, an empty weights placeholder and no serialized vocabulary. Exact inference cannot be reproduced from upstream alone.

**Alternatives rejected**: Rebuilding a vocabulary from runtime input; assuming weights exist; training during startup/default CI; mutable artifact downloads.

**Release gates**: verified matching weights/vocabulary or licensed retraining data; reproducible artifact build; GPL-3.0 and dataset/weight licensing review; measured model evaluation kept distinct from runtime label metrics.

## Decision 12: Preserve upstream preprocessing semantics as a versioned release

**Decision**: The initial compatible release uses lowercase plus a frozen whitespace vocabulary, max length 400, and pre-padding/pre-truncation. PAD/OOV IDs are fixed before training. Class indices are `POSITIVE=0`, `NEGATIVE=1`, `NEUTRAL=2`; confidence is `max(p)` and polarity is `pPositive-pNegative`.

**Rationale**: These are the recoverable upstream semantics. The English release uses its frozen whitespace tokenizer and has no runtime segmenter or translation dependency. Changing tokenization later changes the preprocessing/model release and requires bundled dictionaries/checksums.

**Alternatives rejected**: Silent tokenizer fallback; dynamic vocabulary; changing padding or class order without a new release.

## Decision 13: Keep UI output lightweight and provenance protected

**Decision**: Public `GET /news-items` returns analysis state and optional label/confidence/polarity only. A separately protected internal service-token/network endpoint returns release/hash provenance.

**Rationale**: Implements FR-040 and avoids coupling the UI to internal ML release data. The current auth foundation does not establish an operator role, so the plan does not invent one.

**Alternatives rejected**: Full provenance in public DTOs; claiming role-based audit access without an authorization policy.

## Decision 14: Test runtimes independently and contracts jointly

**Decision**: Java resilience tests use a mock HTTP server, Python default tests use a fake inference engine, both execute shared fixtures, and a real model smoke test is opt-in.

**Rationale**: Default CI stays fast, deterministic, secret-free and independent of TensorFlow artifacts/providers while still proving the cross-runtime boundary.

**Alternatives rejected**: Loading the Python model from Java tests; mutable model downloads in CI; treating H2 as PostgreSQL constraint evidence.

## Primary references

- `docs/adr/0008-sentiment-service-boundary.md`
- `docs/adr/0002-module-boundaries.md`
- `docs/adr/0006-queue-worker-backtesting.md`
- `docs/adr/0007-postgresql-redis-ownership.md`
- `.specify/memory/constitution.md`
- `supabase/migrations/20260827000100_create_database_baseline.sql`
- MultiChannel repository and inspected source at `https://github.com/ntienhuy/MultiChannel`

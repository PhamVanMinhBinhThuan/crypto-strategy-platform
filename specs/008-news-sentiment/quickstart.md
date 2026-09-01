# Verification Quickstart: F-008 News and Sentiment

This document describes the planned developer and CI verification flow. Commands that target F-008 components become runnable after implementation; no result is claimed by this planning artifact.

## Prerequisites

- Java 21 and the repository Gradle wrapper
- Docker-compatible PostgreSQL/Supabase environment for schema/concurrency tests
- Python 3.11 virtual environment for the selected locked TensorFlow release
- No model artifact is required for default Java or Python tests

## Default Java verification

```powershell
./gradlew :modules:news:test
./gradlew :apps:worker:test
./gradlew :apps:api:test
./gradlew :architecture-tests:test
```

Expected coverage includes canonicalization, state transitions, shared contract fixtures, exactly one HTTP call per dispatched lease, CircuitBreaker/TimeLimiter behavior, lightweight public responses, and module dependency rules. Worker tests use a mock HTTP server and must not load Python/TensorFlow.

## PostgreSQL verification

Start an isolated non-production PostgreSQL instance, apply the existing migrations followed by the new F-008 forward migration, then run the planned SQL and `newsIntegrationTest` suites.

```powershell
./gradlew :modules:persistence:newsIntegrationTest
```

The environment-specific task must require explicit database variables. Never point migration verification at a shared or production database without approval. Verify the empty-baseline path and a populated legacy fixture that aborts without an explicit model-release mapping.

## Default Python verification

From `apps/sentiment`, create a virtual environment and install the hash-locked application/test dependencies produced during implementation, then run:

```powershell
python -m pytest tests/unit tests/contract
```

These suites inject a deterministic inference double. They must not train, download, load TensorFlow model artifacts, access PostgreSQL/Redis, or fetch News URLs.

## Cross-runtime contract verification

Both Java and Python suites load the same canonical schemas and fixture inventory derived from `specs/008-news-sentiment/contracts/sentiment-v1`. Verify:

- strict acceptance/rejection parity;
- identity, hash, language and release echo validation;
- canonical exact-decimal encoding;
- safe transient/permanent errors;
- liveness/readiness states.

Any schema or fixture update must fail one side until both mappings conform; copied divergent fixture sets are not accepted.

## Local fake-service resilience scenario

Run the Worker integration profile against its mock sentiment endpoint, then exercise success, timeout, `429`, `5xx`, malformed response, open circuit and recovery. Assert persisted eligibility rather than elapsed thread sleep:

- first transient dispatched failure: next eligible at `now + 5s`;
- second: `now + 30s`;
- third: terminal `FAILED` under defaults;
- pre-dispatch unready/open circuit: no attempt consumed;
- each dispatched lease execution: exactly one POST.

## Optional real-model smoke test

Only after a reviewed immutable bundle exists, provide its path and explicitly enable the smoke profile:

```powershell
$env:SENTIMENT_MODEL_BUNDLE = '<reviewed-absolute-bundle-path>'
$env:RUN_SENTIMENT_MODEL_SMOKE = 'true'
python -m pytest tests/smoke
```

The smoke test validates manifest/checksums, tokenizer/model compatibility, warm-up, shape, finite probabilities and response contract. It does not measure or claim model accuracy. Missing artifacts skip/fail the opt-in profile according to CI policy and never trigger a download or training run.

## Failure-isolation acceptance check

With News already collected, stop or time out the fake/real sentiment service and verify that:

- Java API liveness remains healthy;
- accepted News remains queryable with a non-analyzed/degraded state;
- Market Data remains callable;
- existing technical Backtest fixtures remain unaffected;
- analysis degradation is observable within five seconds;
- restarting the service permits due work to resume without duplicate logical results.

## Evidence policy

Record test command, commit, environment/configuration and actual result. Keep all F-008 evidence `Planned` until the corresponding command or review has produced a real, reviewable artifact. Label distribution and a successful smoke inference are not model-accuracy evidence.

# F-015 100-Candidate Durable Integration

## Database setup

Executed on 2026-09-05 against an isolated PostgreSQL 17 Docker container bound to `127.0.0.1:55432`. A minimal local Supabase compatibility bootstrap created the `auth` schema/users table and the `anon`/`authenticated` roles. All 13 repository migrations were then applied in filename order through:

```text
Get-Content -Raw <migration> | docker exec -i crypto-f015-test-db \
  psql -q -v ON_ERROR_STOP=1 -U postgres -d crypto_f015
```

Result: `ALL_MIGRATIONS_APPLIED`, including `20260905000100_f015_composite_search.sql`.

## Command

```text
DATABASE_URL=jdbc:postgresql://127.0.0.1:55432/crypto_f015
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=<local isolated test value>
./gradlew --no-daemon --no-configuration-cache \
  :modules:persistence:experimentIntegrationTest \
  --tests '*FiniteSearchExperimentIntegrationTest.oneHundredCandidatesRefillThroughAWindowOfFourWithoutDuplicates*'
```

Result: `BUILD SUCCESSFUL in 2m 7s`. The JUnit test body took 3.821 seconds; the remaining duration was Gradle configuration/compilation and Windows/OneDrive overhead.

## Frozen test configuration

- Generator: registered Random Search baseline
- Candidate budget: 100
- Finite domain: integer `period` values 1 through 100
- Requested/per-experiment active window: 4
- Global active bound: 20
- Top-K: 10
- Durable storage: PostgreSQL candidate, job, generator state, coordination and outbox tables
- Terminal fixture outcome: each allocated Backtest job settles exactly once as a terminal failure, forcing refill until the budget is exhausted

## Verified results

- Exactly 100 candidate rows were persisted.
- Exactly 100 distinct candidate fingerprints were persisted.
- Peak active Backtest jobs was exactly 4 and never exceeded the configured window.
- Exactly 100 durable `BACKTEST_JOB` outbox events were persisted.
- Failed terminal outcomes reached exactly 100.
- Search Run and Experiment reached authoritative `COMPLETED` status after maximum-candidate exhaustion.
- Top-K 10 did not cap allocation or active work.

The test transaction rolls back its fixture graph, so no test Experiment data remains after execution.

## Defects found by the real PostgreSQL run

The first executions exposed and led to fixes for two PostgreSQL-only defects that unit/H2 checks did not catch:

1. `pg_advisory_xact_lock()` returns `void`; allocation now executes the statement instead of mapping it to `Long`.
2. A Java text-block concatenation produced `selectsearch_run_id`; the authoritative progress query now preserves the required token boundary.

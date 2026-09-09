# Quickstart Validation: F-015

## Prerequisites

- Java 21 and the repository's Gradle wrapper.
- Node.js 22 and dependencies installed under `apps/web`.
- Docker-compatible runtime for PostgreSQL/Redis integration tests.
- For live dataset verification, configured market-data provider credentials/network access; deterministic tests use the market-data test adapter, never production seed rows presented as live data.

## 1. Static and unit validation

```powershell
./gradlew :modules:search:test :modules:experiment-execution:test :apps:worker:test :apps:api:test :architecture-tests:test
Set-Location apps/web
npm run format:check
npm run lint
npm run typecheck
npm test
```

Expected:

- canonical composite space/cardinality and deterministic Random candidate tests pass;
- Top-K/concurrency separation and completion/reconciliation refill tests pass;
- public request/read compatibility and architecture boundaries pass;
- Search form, monitor, leaderboard, and async-state component tests pass.

## 2. Durable 100-candidate integration

Run the F-015 integration source set/task documented by the implementation and retain its generated report under `docs/evidence/f015/`.

Expected assertions:

- 100 unique candidate fingerprints and 100 terminal outcomes;
- active work never exceeds the configured per-experiment/global limit;
- completion refills the window until the candidate budget is reached;
- Top-10 contains no more than ten deterministic entries and Top-K did not cap processing;
- every candidate uses the same dataset checksum;
- no duplicate Candidate, Backtest result, Evaluation, or Leaderboard identity.

## 3. Browser end-to-end flow

Start the supported API, Worker, dependencies, and production web app, then run the F-015 Playwright project/scenario.

Expected flow:

1. Choose pair/timeframe/start/end.
2. Create or select the frozen dataset and inspect its provenance.
3. Select at least two strategies and configure domains.
4. Choose one-to-two components, Majority Vote, Random Search, seed, 100 candidates, and Top-10.
5. Start and navigate to the experiment monitor.
6. Observe authoritative counts, stop/realtime states, and final leaderboard.
7. Open a candidate and its Backtest result.
8. Reproduce the terminal experiment and verify evidence status.

## 4. Scale evidence

Run the documented performance profile outside the default fast test suite:

- 1,000 deterministic candidates at baseline and increased worker capacity.
- 10,000-candidate controlled generation/allocation/backpressure profile.

Record date, commit, CPU/memory, Java/Node versions, database/Redis settings, dataset size, strategy pool/domains, worker concurrency, counts, elapsed time, throughput, peak active/pending work, duplicates, failures, and whether every assertion passed. Do not describe synthetic compute-only timing as end-to-end worker scaling.

## 5. Full repository gate

```powershell
Set-Location ../..
./gradlew check
Set-Location apps/web
npm run check
```

Expected: all checks pass without modifying historical specs, old migrations, or unrelated user files.


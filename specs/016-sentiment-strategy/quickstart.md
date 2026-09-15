# Quickstart: F-016 Sentiment Strategy

## Prerequisites

- Java 21, Node 22 and the repository Gradle wrapper.
- PostgreSQL/Supabase and Redis configured through `docs/SETUP.md`.
- Analyzed News exists for the Dataset base asset.
- Python Sentiment is required for new analysis, not frozen replay.

## Focused verification

```bash
./gradlew :modules:strategy-core:test :modules:strategies:test :modules:news:test :modules:backtesting:test
./gradlew :apps:api:test :apps:worker:test
npm --prefix apps/web run lint
npm --prefix apps/web run typecheck
npm --prefix apps/web test
```

## Manual flow

1. Start the stack using `docs/run.md`.
2. Confirm analyzed News exists, then choose “Use as Strategy”.
3. Configure Sentiment in Composer and open Backtest.
4. Run finite Search alone or in a Composite.
5. Open Result evidence and verify score, thresholds, article count, model and snapshot fingerprint.
6. Stop Python Sentiment and reproduce the Result; it must use the frozen snapshot.
7. Run a technical-only Backtest; it must still complete.

## Expected evidence

- Five system strategies in catalog.
- Known fixtures produce BUY/HOLD/SELL and exclude future News.
- Repeated frozen runs have identical decision/trade/result fingerprints.
- All Search Candidates share one snapshot/model release.
- Sentiment outage does not block technical execution or frozen replay.

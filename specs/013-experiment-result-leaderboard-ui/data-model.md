# Phase 1 Data Model: F-013 UI State

This is browser-facing state only; it creates no database schema or durable browser business model.

## Backtest lookup and result

`BacktestLookup` is `none | resultId | backtestId | invalid`. Conflicting/malformed input dispatches no request. No identifier conversion exists.

`BacktestResultViewModel` contains:

- identity/status/completed UTC instant;
- exactly total return, win rate, maximum drawdown, number of trades;
- initial capital, final capital, total fees;
- all released trade fields in authoritative order;
- Experiment/Candidate/Job/Attempt identities and four fingerprints;
- supplied assumptions.

All decimal financial fields remain authoritative strings. A UI cell may derive a shortened presentation string, but the original full string remains in the view model and must be available through accessible disclosure/text. Query state is `idle | loading | refreshing | success | empty-identifier | inaccessible | dependency-blocked | retryable-failure | terminal-failure`; retryable errors may carry safe normalized `retryAfterSeconds` metadata from F-011.

## Experiment configuration draft

| Field | Source | Validation |
|---|---|---|
| Name | User | Required; preserved on failure |
| Dataset ID | Known ID / fixture option | Required; no production catalog invented |
| Generator ID/version/seed | Fixture until public discovery | Required contract shape |
| Strategy/plugin/version | Public Strategy descriptor | Required |
| Parameter ranges/options | Descriptor schema | min ≤ max; allowed options; cross-constraints where expressible |
| Stop conditions | User | At least one positive finite bound |
| Top-K | User | integer 1–100; initial 10 |

Form state is independent of Experiment snapshot. Validation blocks dispatch, not backend behavior.

## Experiment and Job snapshots

Experiment fields follow F-009: identity, name, dataset, status, job IDs, derivation/reproduction links, UTC instants, safe failure.

```text
CREATED → QUEUED → RUNNING → COMPLETED
                    └──────→ FAILED
QUEUED/RUNNING → STOP_REQUESTED → STOPPED
```

This is presentation eligibility, not browser orchestration. Only REST/events change durable rendered state.

Job fields: Job/Experiment/optional Candidate identities, type, released status, work counts, optional best-score string, UTC instants, retry instant, safe failure. Fixtures cover relevant `QUEUED`, `RUNNING`, `RETRY_SCHEDULED`, `SUCCEEDED`, `FAILED`, `CANCEL_REQUESTED`, `CANCELLED`. Counts remain authoritative; a visual ratio is presentation only and raw counts remain accessible.

## Leaderboard snapshot

Fields: Experiment/revision identities, monotonic revision, configured Top-K, ranking version, fingerprint, UTC created time, opaque cursor/has-more, ordered entries. Entry fields are only rank, evaluation result ID, backtest result ID, score string, maximum drawdown string, evaluation fingerprint. Preserve server rank/order. Limit is 1–100 and capped by configured Top-K.

## Command state

Start, Stop, Reproduce independently own `idle | submitting | accepted | uncertain | conflict | dependency-unavailable | retryable-failure | terminal-failure`, logical key, payload reference, safe error/correlation ID.

- New deliberate command generates one key.
- Timeout/reset becomes uncertain; same payload/key retries.
- Rapid repeat while submitting is suppressed.
- Deliberately changed/new command gets a new key.
- Stop conflict refreshes durable Experiment.
- Start/Reproduce dependency gate preserves form/original evidence.

## Realtime state

Connection is `disconnected | connecting | connected | reconnecting`, supplemented by freshness `fresh | stale | recovering` and retry/exhaustion metadata. Each subscription independently owns target, ID, `pending | active | error | released`, marker, and safe error. A bounded recent event-ID set and rendered Leaderboard revision are ephemeral and cleared with private-state cleanup.

## Required deterministic fixture catalog

- Backtest: normal, zero/many trades, large/small decimals, inaccessible, parity blocked, retryable/terminal failure.
- Experiment: CREATED, QUEUED, RUNNING, STOP_REQUESTED, STOPPED, COMPLETED, FAILED, F-010 unavailable.
- Job: every relevant released state, failure, retry schedule.
- Leaderboard: empty, entries, pages/cursors, stale/new revision.
- Realtime: confirmation/error, progress, completion, leaderboard update, disconnect, reconnecting/success/exhaustion/manual retry, 4001, duplicate/stale.

Outcomes/events are predeclared; no browser business pipeline produces them.

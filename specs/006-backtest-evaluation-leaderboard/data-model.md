# Data Model: F-006 Backtest, Evaluation and Leaderboard

## Ownership and identity

| Entity | Owner | Identity | Mutability |
| --- | --- | --- | --- |
| Backtest Assumptions | Backtesting | contract ID/version inside frozen Manifest | Immutable after Manifest freeze |
| Position | Backtesting runtime | none; scoped to one run | Mutable runtime state, never durable outcome |
| Trade | Backtesting | typed `TradeId` ULID + `(resultId, sequence)` | Immutable after Result acceptance |
| Backtest Result | Backtesting | typed `BacktestResultId` ULID; one canonical result per Candidate | Immutable |
| Evaluation Result | Evaluation | typed `EvaluationResultId` ULID + Result/metric/ranking versions | Immutable |
| Leaderboard Revision | Leaderboard | typed `LeaderboardRevisionId` ULID + `(experimentId, revisionNo)` | Immutable |
| Leaderboard Entry | Leaderboard | `(revisionId, rank)` | Immutable with revision |
| Reproduction Run | Experiment | new run/result IDs linked to originals | Immutable evidence; capability verification reports are referenced, not owned |

All new business IDs reuse the shared typed ULID infrastructure. Existing `ExperimentId`, `CandidateId`, `JobId`, `AttemptId`, `DatasetVersionId`, `StrategyReference` and user UUID remain authoritative.

## Backtest Assumptions

Required fields:

- `contractVersion`: canonical assumptions contract, initially `backtest-assumptions-v1`;
- `initialCapital`: positive money, scale 12;
- `positionMode`: `LONG_ONLY`;
- `maxConcurrentPositions`: exactly 1;
- `capitalAllocation`: `ALL_AVAILABLE`;
- `executionRule`: `NEXT_CANDLE_OPEN`;
- `feeRate`: rate in `[0,1)`, scale 10;
- `slippageRate`: rate in `[0,1)`, scale 10;
- `invalidSignalPolicy`: `IGNORE` for BUY-while-open and SELL-while-flat;
- `endOfDatasetPolicy`: `FORCE_CLOSE_AT_FINAL_CLOSE`;
- `roundingMode`: `HALF_EVEN`;
- `tradeScale`: 12;
- `metricScale`: 10.

Changing any field changes the assumptions canonical encoding and requires a new Experiment/Manifest rather than reading a new default.

## Position

Runtime fields:

- entry decision time and execution time;
- entry execution price after adverse slippage;
- quantity calculated as `availableCash / (buyFillPrice × (1 + feeRate))`, rounded only at the canonical output boundary, so entry notional plus entry fee never exceeds available cash;
- entry notional and entry fee;
- frozen Strategy decision reference/reason for audit;
- last processed Candle identity/sequence.

Only one Position may be open. It transitions `FLAT → LONG → FLAT`. HOLD does not change state. Invalid BUY/SELL combinations are recorded only as deterministic ignored decisions, not Trades.

## Trade

Fields:

- typed ID, Backtest Result ID and zero-based sequence;
- entry/exit decision and execution timestamps;
- entry/exit prices, quantity, entry fee, exit fee and total fee, all scale 12;
- gross proceeds, realized P&L and post-trade cash, scale 12;
- exit reason: Strategy SELL or forced final close;
- canonical Strategy references responsible for decisions.

Validation:

- entry occurs before exit;
- all prices/quantity/fees are non-negative and quantity is positive;
- sequence is contiguous within Result;
- timestamps and prices must originate from Dataset Candles under the selected execution rule.

## Backtest Result

Fields:

- typed ID;
- Experiment, Candidate, Backtest Job and successful Attempt identities;
- Manifest fingerprint and frozen Dataset/Strategy provenance fingerprints;
- assumptions contract/version and canonical assumptions;
- initial/final capital, total fees and ordered immutable Trades;
- immutable `EquityCurveSummary`: point count, peak equity, trough equity, peak/trough sequence evidence and `equity-curve-v1` digest, calculated online without retaining every point;
- `backtest-v1` fingerprint;
- completion instant and optional original Result reference for reproduction.

Invariants:

- Job is BACKTEST and all Experiment/Candidate/Attempt identities form one lineage;
- Attempt status is `SUCCEEDED` before the durable Result transaction;
- at most one canonical business Result exists per Candidate;
- Result fingerprint excludes runtime Worker identity and completion timing;
- failed validation creates no Result or child Trade.

## Evaluation Result

Fields:

- typed ID, Experiment ID and Backtest Result ID;
- `metricVersion` and `rankingVersion`;
- Total Return, Win Rate, Maximum Drawdown and Number of Trades;
- normalized return/win/drawdown scores;
- overall score in `[0,1]`;
- Leaderboard eligibility (`numberOfTrades >= 5`);
- `evaluation-v1` fingerprint and evaluation instant.

Formulas:

- `TotalReturn = (finalCapital - initialCapital) / initialCapital`;
- `WinRate = winningTrades / numberOfTrades`, with zero Trades producing zero; a win requires realized net P&L after both fees greater than zero, so break-even is not a win;
- Maximum Drawdown is the largest peak-to-trough loss ratio derived from the Backtesting-owned `EquityCurveSummary`; equity is valued after each closed Candle as cash plus open-position quantity multiplied by that Candle close;
- `returnScore = clamp(TotalReturn, 0, 1)`;
- `winRateScore = clamp(WinRate, 0, 1)`;
- `drawdownScore = 1 - clamp(MaximumDrawdown, 0, 1)`;
- `overallScore = 0.45 × returnScore + 0.30 × winRateScore + 0.25 × drawdownScore`.

All output metrics use scale 10 and `HALF_EVEN`. Evaluation identity/uniqueness includes both metric and ranking versions.

## Leaderboard Revision and Entry

Revision fields:

- typed ID, Experiment ID, positive revision number and Top K = 10;
- ranking version;
- ordered immutable entries;
- `leaderboard-v1` fingerprint and creation instant.

Entry fields:

- revision/Experiment/Evaluation identities;
- contiguous rank from 1;
- exact score, Maximum Drawdown and evaluation fingerprint tie-break evidence.

Only eligible Evaluations from the same Experiment participate. Ordering is score descending, drawdown ascending, `evaluationFingerprint` ascending. Identical canonical ordered content does not create another revision.

## Capability-local transactions

- Backtesting atomically writes one Backtest Result, its Trades and its EquityCurveSummary.
- Evaluation atomically writes one Evaluation Result for an accepted Backtest Result.
- Leaderboard atomically writes one Revision and all of its Entries.
- Failure of a later capability leaves the earlier accepted immutable outcome intact and retryable; “no partial outcome” applies inside each capability transaction, not to the entire cross-capability graph.

## Reproduction

A reproduction uses a durable Reproduction Run owned only by Experiment and never overwrites the original Result. Backtesting, Evaluation and Leaderboard contribute immutable verification reports through public contracts. Success requires:

1. Dataset provenance/count/checksum and Strategy provenance/fingerprint match frozen inputs;
2. canonical assumptions and versions match;
3. ordered Trade sequence matches field-for-field after canonical decimal normalization;
4. all four metrics and overall score match;
5. EquityCurveSummary evidence/digest and relevant `backtest-v1`, `evaluation-v1` and, when projected, `leaderboard-v1` fingerprints match.

A mismatch produces structured comparison evidence and no claim of successful reproduction.

## Lifecycle

```text
Frozen Manifest + Candidate + SUCCEEDED Attempt
                    │
                    ▼
             validate/stream Dataset
                    │ invalid → fail fast, persist no outcome
                    ▼
             build Trades + Result
                    │
                    ▼
                Evaluation
                    │ eligible (>=5 Trades)
                    ▼
             Leaderboard Revision
```

Transaction failure at any persistence step rolls back the entire new outcome graph. Existing immutable artifacts remain unchanged.

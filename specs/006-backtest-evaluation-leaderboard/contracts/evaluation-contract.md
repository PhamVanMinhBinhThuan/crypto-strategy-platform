# Evaluation Public Contract

## Input

An immutable accepted Backtest Result with ordered Trades, capital summary and `backtest-v1` fingerprint, plus explicit `metricVersion` and `rankingVersion`.

## Output

An immutable Evaluation Result containing:

- Total Return;
- Win Rate;
- Maximum Drawdown;
- Number of Trades;
- normalized component scores;
- overall score `[0,1]`;
- Leaderboard eligibility;
- `evaluation-v1` fingerprint.

## Rules

- Use scale 10 and `HALF_EVEN` for canonical outputs.
- Zero Trades yields Win Rate 0 and is ineligible; `initialCapital <= 0` is rejected before Evaluation; no NaN/infinity is allowed.
- Initial capital must be positive before division.
- Maximum Drawdown is derived from the immutable Backtesting-owned `EquityCurveSummary`, whose `equity-curve-v1` digest and peak/trough evidence were calculated online from deterministic cash/position valuation.
- A win is a Trade whose realized net P&L after entry and exit fees is greater than zero; a break-even Trade is not a win.
- Fixed clamp normalization and weights are part of `rankingVersion`.
- Fewer than five Trades remains a persisted Evaluation but cannot enter Leaderboard.
- The same Result/metric/ranking versions are idempotent; a changed ranking version may create a distinct Evaluation projection without overwriting the old one.

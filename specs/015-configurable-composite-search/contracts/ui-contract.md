# UI Contract: F-015 Search & Leaderboard

The production route extends UI-04 using the F-011 application shell and shared HTTP/realtime clients. The screenshot and prototype guide hierarchy and density only.

## Routes

- `/search`: new Search configuration workflow.
- `/search/{experimentId}`: authoritative experiment monitor and leaderboard.
- Existing Backtest detail route: target for a leaderboard row's released Backtest identity.

A successful Start Search navigates to `/search/{experimentId}`. The configuration form is not duplicated below an existing experiment monitor.

## Configuration order

1. Pair.
2. Timeframe.
3. Start UTC and End UTC.
4. Create frozen dataset or select a compatible existing frozen dataset.
5. Simulated initial capital, transaction fee, and slippage.
6. Strategy pool.
7. Parameter domains grouped by selected strategy.
8. Component bounds, Majority Vote policy, and supported constraints.
9. Registered generator and seed.
10. Stop conditions and Top-K.
11. Review frozen summary and Start Search.

Capital is labeled as simulated quote-asset capital, never as an exchange wallet balance. Fee and slippage use percentage inputs in the browser and exact decimal rates at the API boundary.

The Start action remains disabled until every authoritative prerequisite is selected and client validation passes. Server validation remains final and is mapped back to the appropriate section without discarding input.

## Dataset presentation

The selected-dataset card shows pair, timeframe, `[start, end)` UTC, provider, candle count, checksum (abbreviated with accessible full value), status, and creation time. “Create dataset” means fetch-and-freeze; it is not a candidate Backtest command.

## Strategy/domain presentation

- Each eligible artifact has a checkbox/control, immutable version, and type badge.
- Selecting an artifact reveals controls generated from its published parameter schema.
- Domain inputs distinguish fixed value, choices, and range/step.
- Pool/component counts and estimated/exact cardinality update as a preview only; Start uses server validation.
- Weighted Vote, Domain-Guided Search, Genetic Search, Pause, and Resume are absent unless released contracts make them executable.

## Monitor presentation

The page preserves the prototype's pipeline/status hierarchy but uses authoritative fields:

- status and terminal reason;
- allocated, active, completed, failed, remaining capacity, and maximum;
- elapsed/deadline where configured;
- best score and Top-K;
- Generate → Backtest → Evaluate → Rank → Leaderboard lifecycle labels without pretending there is only one “current candidate” when multiple workers are active.

The worker monitor is replaced by aggregate active-work status unless a released worker-read contract exists. No fake `worker_01` rows appear.

## Leaderboard presentation

Columns:

- Rank
- Composite candidate summary
- Score
- Total Return
- Win Rate
- Maximum Drawdown
- Trades
- Actions

Sharpe Ratio is omitted. Numeric values are tabular/monospaced where the design system supports it. Candidate summary opens authoritative detail; Backtest action opens the released result route.

## Async and accessibility states

Implement initial loading, refreshing, empty, validation error, ownership-safe not-found, conflict, retryable dependency error, degraded realtime, terminal failure, stopped, exhausted, and completed states. Preserve the last durable snapshot while reconnecting. Status uses text/icon semantics and keyboard focus reaches every configuration group and table action.

## Fixture boundary

Fixture mode implements the same HTTP/realtime request/response shapes for datasets, strategies, generators, Search start, progress, leaderboard, and candidate detail. It is visibly labeled and may replay deterministic snapshots/events, but browser code does not simulate candidate generation, Backtest, Evaluation, or Ranking.

export const F013_SCENARIOS = [
  "backtest-normal",
  "backtest-zero-trades",
  "backtest-many-trades",
  "backtest-extreme-decimals",
  "backtest-inaccessible",
  "backtest-result-id-blocked",
  "backtest-retryable",
  "backtest-terminal",
  "backtest-authentication-required",
  "backtest-rate-limited",
  "experiment-created",
  "experiment-queued",
  "experiment-running",
  "experiment-stop-requested",
  "experiment-stopped",
  "experiment-completed",
  "experiment-failed",
  "search-coordinator-unavailable",
  "leaderboard-empty",
  "leaderboard-page",
  "realtime-recovery",
  "realtime-exhausted",
  "search-config-loading",
  "search-config-empty",
  "search-config-invalid",
  "search-progress-degraded",
  "search-no-improvement",
  "candidate-detail",
  "composite-leaderboard"
] as const;
export type F013Scenario = (typeof F013_SCENARIOS)[number];
export const isF013Scenario = (value: string): value is F013Scenario =>
  (F013_SCENARIOS as readonly string[]).includes(value);

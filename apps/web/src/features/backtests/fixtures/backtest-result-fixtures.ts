export const normalBacktestResult = {
  backtestResultId: "result-013",
  backtestId: "backtest-013",
  status: "COMPLETED",
  metrics: {
    totalReturn: "18.427500000000000001",
    winRate: "0.625",
    maximumDrawdown: "0.0831",
    numberOfTrades: 2
  },
  initialCapital: "10000.00000000",
  finalCapital: "11842.7500000000001",
  totalFees: "12.37500000",
  completedAt: "2026-09-03T04:12:00Z",
  trades: [
    {
      tradeId: "trade-001",
      sequence: 0,
      side: "LONG",
      entryTime: "2026-08-01T00:00:00Z",
      entryPrice: "65000.123456789",
      exitTime: "2026-08-02T00:00:00Z",
      exitPrice: "67250.987654321",
      quantity: "0.10000000",
      entryFee: "3.25000617",
      exitFee: "3.36254938",
      totalFee: "6.61255555",
      profitLoss: "218.47386420",
      postTradeCash: "10218.47386420",
      exitReason: "STRATEGY_SELL"
    },
    {
      tradeId: "trade-002",
      sequence: 1,
      side: "LONG",
      entryTime: "2026-08-04T00:00:00Z",
      entryPrice: "67000.00",
      exitTime: "2026-08-08T00:00:00Z",
      exitPrice: "69000.00",
      quantity: "0.20000000",
      entryFee: "2.00",
      exitFee: "3.76244445",
      totalFee: "5.76244445",
      profitLoss: "1624.27613580",
      postTradeCash: "11842.7500000000001",
      exitReason: "FORCED_FINAL_CLOSE"
    }
  ],
  provenance: {
    experimentId: "experiment-013",
    candidateId: "candidate-013",
    jobId: "job-013",
    successfulAttemptId: "attempt-013",
    manifestFingerprint: "sha256:manifest013",
    datasetFingerprint: "sha256:dataset013",
    strategyFingerprint: "sha256:strategy013",
    resultFingerprint: "sha256:result013"
  },
  assumptions: {
    assumptionsVersion: "1",
    initialCapital: "10000.00000000",
    feeRate: "0.001",
    slippageRate: "0.0005",
    positionMode: "LONG_ONLY",
    executionPriceRule: "NEXT_CANDLE_OPEN",
    forceCloseAtEnd: true,
    roundingMode: "HALF_EVEN"
  }
} as const;
export const zeroTradeBacktestResult = {
  ...normalBacktestResult,
  backtestResultId: "result-zero",
  metrics: { totalReturn: "0", winRate: "0", maximumDrawdown: "0", numberOfTrades: 0 },
  finalCapital: normalBacktestResult.initialCapital,
  totalFees: "0",
  trades: []
} as const;
export const manyTradeBacktestResult = {
  ...normalBacktestResult,
  backtestResultId: "result-many",
  metrics: { ...normalBacktestResult.metrics, numberOfTrades: 6 },
  trades: [
    normalBacktestResult.trades[0],
    normalBacktestResult.trades[1],
    { ...normalBacktestResult.trades[0], tradeId: "trade-003", sequence: 2 },
    { ...normalBacktestResult.trades[1], tradeId: "trade-004", sequence: 3 },
    { ...normalBacktestResult.trades[0], tradeId: "trade-005", sequence: 4 },
    { ...normalBacktestResult.trades[1], tradeId: "trade-006", sequence: 5 }
  ]
} as const;
export const extremeDecimalBacktestResult = {
  ...normalBacktestResult,
  backtestResultId: "result-extreme",
  metrics: {
    ...normalBacktestResult.metrics,
    totalReturn: "999999999999999999.000000000000000001",
    maximumDrawdown: "0.0000000000000000001"
  }
} as const;
export const backtestFixtureErrors = {
  inaccessible: {
    ok: false,
    error: { code: "RESOURCE_NOT_FOUND", message: "Resource inaccessible", retryable: false }
  },
  resultIdBlocked: {
    ok: false,
    error: {
      code: "BLOCKED_BACKTEST_RESULT_READ_BY_RESULT_ID",
      message: "Result lookup is awaiting upstream parity.",
      retryable: false
    }
  },
  retryable: {
    ok: false,
    error: { code: "DEPENDENCY_UNAVAILABLE", message: "Try again later.", retryable: true }
  },
  terminal: {
    ok: false,
    error: { code: "BACKTEST_FAILED", message: "Backtest failed safely.", retryable: false }
  },
  authentication: {
    ok: false,
    error: {
      code: "AUTHENTICATION_REQUIRED",
      message: "Your session has expired. Please sign in again.",
      retryable: false
    }
  },
  rateLimited: {
    ok: false,
    error: {
      code: "RATE_LIMIT_EXCEEDED",
      message: "Wait before retrying.",
      retryable: true,
      retryAfterSeconds: 30
    }
  }
} as const;

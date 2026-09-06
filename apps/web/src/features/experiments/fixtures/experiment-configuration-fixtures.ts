export const knownDataset = {
  datasetId: "dataset-btc-1h",
  label: "BTC/USDT 1h frozen dataset"
} as const;
export const frozenDatasetFixture = {
  datasetId: "dataset-btc-1h",
  version: "candle-v1",
  provider: "FIXTURE_EXCHANGE",
  pair: "BTC/USDT",
  timeframe: "1h",
  normalizationVersion: "fixture-normalization-v1",
  startTime: "2026-08-01T00:00:00Z",
  endTime: "2026-09-01T00:00:00Z",
  membershipCount: 744,
  checksum: `sha256:${"d".repeat(64)}`,
  status: "READY",
  createdAt: "2026-09-01T00:00:01Z"
} as const;
export const generatorPage = {
  items: [{ generatorId: "random-search", version: "1.0.0", displayName: "Random Search" }]
} as const;
export const emptyUserStrategyPage = { items: [], nextCursor: null, hasMore: false } as const;
export const fixtureGenerator = {
  generatorId: "random-search",
  version: "1.0.0",
  seed: 20260903,
  source: "FIXTURE_ONLY"
} as const;
export const strategyDescriptorPage = {
  items: [
    {
      strategyId: "ma-crossover",
      strategyVersionId: "strategy-version-013",
      version: "1.0.0",
      contractVersion: "1",
      displayName: "Moving Average Crossover",
      description: "Deterministic crossover strategy",
      category: "TREND",
      supportedSignals: ["BUY", "SELL"],
      requiredLookback: 3,
      parameters: [
        {
          name: "fastPeriod",
          type: "INTEGER",
          required: true,
          defaultValue: "12",
          minimum: "2",
          maximum: "50",
          searchRangeHint: { minimum: "5", maximum: "20", step: "5" },
          allowedValues: [],
          description: "Fast moving-average period"
        },
        {
          name: "slowPeriod",
          type: "INTEGER",
          required: true,
          defaultValue: "64",
          minimum: "10",
          maximum: "200",
          searchRangeHint: { minimum: "20", maximum: "100", step: "10" },
          allowedValues: [],
          description: "Slow moving-average period"
        },
        {
          name: "priceSource",
          type: "ENUM",
          required: true,
          defaultValue: "CLOSE",
          minimum: null,
          maximum: null,
          searchRangeHint: null,
          allowedValues: ["OPEN", "CLOSE"],
          description: "Input price"
        }
      ],
      constraints: [{ lowerParameter: "fastPeriod", upperParameter: "slowPeriod" }],
      descriptorFingerprint: "strategy-descriptor-v2:ma-crossover:1.0.0"
    }
  ],
  nextCursor: null,
  hasMore: false
} as const;
export const acceptedStart = {
  ok: true,
  data: { experimentId: "experiment-fixture-new", jobId: "job-fixture-new", status: "QUEUED" }
} as const;
export const acceptedReproduce = {
  ok: true,
  data: { experimentId: "experiment-fixture-copy", jobId: "job-fixture-copy", status: "QUEUED" }
} as const;
export const commandFixtureErrors = {
  authentication: {
    ok: false,
    error: { code: "AUTHENTICATION_REQUIRED", message: "Session expired.", retryable: false }
  },
  rateLimited: {
    ok: false,
    error: { code: "RATE_LIMIT_EXCEEDED", message: "Wait.", retryable: true, retryAfterSeconds: 12 }
  },
  dependency: {
    ok: false,
    error: {
      code: "DEPENDENCY_UNAVAILABLE",
      message: "BLOCKED_SEARCH_COORDINATOR",
      retryable: true
    }
  },
  uncertain: {
    ok: false,
    error: { code: "TRANSPORT_UNCERTAIN", message: "Outcome is unknown.", retryable: true }
  }
} as const;

export const runningExperiment = {
  experimentId: "experiment-013",
  name: "BTC trend search",
  status: "RUNNING",
  datasetId: "dataset-btc-1h",
  jobIds: ["job-search-013"],
  derivedFromExperimentId: null,
  reproducesExperimentId: null,
  startedAt: "2026-09-03T02:00:00Z",
  completedAt: null,
  failure: null,
  searchProgress: {
    allocated: 46,
    active: 2,
    completed: 42,
    failed: 2,
    remainingCapacity: 54,
    configuredMaximum: 100,
    topK: 10,
    bestScore: "0.8734000000",
    startedAt: "2026-09-03T02:00:00Z",
    terminalReason: null
  },
  createdAt: "2026-09-03T01:59:00Z"
} as const;
export const runningJob = {
  jobId: "job-search-013",
  experimentId: "experiment-013",
  candidateId: null,
  type: "SEARCH",
  status: "RUNNING",
  totalWork: 100,
  completedWork: 42,
  failedWork: 2,
  bestScore: "0.873400000000000001",
  queuedAt: "2026-09-03T01:59:00Z",
  startedAt: "2026-09-03T02:00:00Z",
  finishedAt: null,
  nextRetryAt: null,
  failure: null,
  createdAt: "2026-09-03T01:59:00Z",
  updatedAt: "2026-09-03T04:00:00Z"
} as const;
export const candidatePage = {
  items: [
    {
      candidateId: "candidate-013",
      experimentId: "experiment-013",
      generationIndex: 42,
      definition: { strategyId: "ma-crossover", parameters: { fastPeriod: 12, slowPeriod: 64 } },
      generatorState: { seed: 20260903 },
      fingerprint: "sha256:candidate013",
      createdAt: "2026-09-03T03:50:00Z"
    }
  ],
  nextCursor: null,
  hasMore: false
} as const;
export const experimentStates = [
  "CREATED",
  "QUEUED",
  "RUNNING",
  "STOP_REQUESTED",
  "STOPPED",
  "COMPLETED",
  "FAILED"
].map((status, index) => ({
  ...runningExperiment,
  experimentId: `experiment-${status.toLowerCase()}`,
  status,
  startedAt: ["CREATED", "QUEUED"].includes(status) ? null : runningExperiment.startedAt,
  completedAt: ["STOPPED", "COMPLETED", "FAILED"].includes(status) ? "2026-09-03T05:00:00Z" : null,
  failure:
    status === "FAILED"
      ? { code: "JOB_EXECUTION_TIMEOUT", message: "The Search job exceeded its execution window." }
      : null,
  createdAt: `2026-09-03T0${index}:00:00Z`
})) as readonly unknown[];
export const jobStates = [
  "QUEUED",
  "RUNNING",
  "RETRY_SCHEDULED",
  "SUCCEEDED",
  "FAILED",
  "CANCEL_REQUESTED",
  "CANCELLED"
].map((status) => ({
  ...runningJob,
  jobId: `job-${status.toLowerCase()}`,
  status,
  nextRetryAt: status === "RETRY_SCHEDULED" ? "2026-09-03T05:10:00Z" : null,
  failure:
    status === "FAILED" ? { code: "JOB_RETRY_EXHAUSTED", message: "Retry budget exhausted." } : null
})) as readonly unknown[];
export const completionNotification = {
  eventType: "BACKTEST_COMPLETED",
  eventVersion: 1,
  eventId: "event-completed-013",
  occurredAt: "2026-09-03T04:01:00Z",
  correlationId: "fixture-correlation",
  subscriptionId: "experiment-experiment-013",
  payload: {
    experimentId: "experiment-013",
    candidateId: "candidate-013",
    backtestResultId: "result-013"
  }
} as const;
export const experimentFixtureErrors = {
  authentication: {
    ok: false,
    error: { code: "AUTHENTICATION_REQUIRED", message: "Session expired.", retryable: false }
  },
  inaccessible: {
    ok: false,
    error: { code: "RESOURCE_NOT_FOUND", message: "Resource inaccessible", retryable: false }
  },
  rateLimited: {
    ok: false,
    error: { code: "RATE_LIMIT_EXCEEDED", message: "Wait.", retryable: true, retryAfterSeconds: 20 }
  },
  dependency: {
    ok: false,
    error: {
      code: "DEPENDENCY_UNAVAILABLE",
      message: "BLOCKED_SEARCH_COORDINATOR",
      retryable: true
    }
  }
} as const;

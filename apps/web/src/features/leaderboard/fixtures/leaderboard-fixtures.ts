export const leaderboardPage = {
  experimentId: "experiment-013",
  revisionId: "leaderboard-revision-7",
  revision: 7,
  topK: 25,
  rankingPolicyVersion: "1.0.0",
  fingerprint: "sha256:leaderboard7",
  createdAt: "2026-09-03T04:10:00Z",
  nextCursor: null,
  hasMore: false,
  items: [
    {
      rank: 1,
      candidateId: "candidate-013",
      candidateFingerprint: "sha256:candidate013",
      candidateSummary: "ma-crossover + rsi",
      evaluationResultId: "evaluation-001",
      backtestResultId: "result-013",
      score: "0.873400000000000001",
      maximumDrawdown: "0.0831",
      evaluationFingerprint: "sha256:evaluation001",
      metrics: {
        totalReturn: "0.425",
        winRate: "0.582",
        maximumDrawdown: "0.0831",
        numberOfTrades: 1245,
        metricVersion: "metric-v1"
      }
    },
    {
      rank: 2,
      evaluationResultId: "evaluation-002",
      backtestResultId: "result-014",
      score: "0.8199",
      maximumDrawdown: "0.0612",
      evaluationFingerprint: "sha256:evaluation002"
    }
  ]
} as const;
export const emptyLeaderboard = {
  ...leaderboardPage,
  revisionId: "leaderboard-revision-1",
  revision: 1,
  items: []
} as const;
export const cursorLeaderboard = {
  ...leaderboardPage,
  nextCursor: "opaque+cursor==",
  hasMore: true
} as const;
export const staleLeaderboard = {
  ...leaderboardPage,
  revisionId: "leaderboard-revision-6",
  revision: 6
} as const;
export const newerLeaderboard = {
  ...leaderboardPage,
  revisionId: "leaderboard-revision-8",
  revision: 8
} as const;
export const leaderboardFixtureErrors = {
  inaccessible: {
    ok: false,
    error: { code: "RESOURCE_NOT_FOUND", message: "Resource inaccessible", retryable: false }
  },
  authentication: {
    ok: false,
    error: { code: "AUTHENTICATION_REQUIRED", message: "Session expired.", retryable: false }
  },
  rateLimited: {
    ok: false,
    error: { code: "RATE_LIMIT_EXCEEDED", message: "Wait.", retryable: true, retryAfterSeconds: 15 }
  }
} as const;

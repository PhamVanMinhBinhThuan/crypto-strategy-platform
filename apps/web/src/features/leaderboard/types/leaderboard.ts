export type LeaderboardEntry = Readonly<{
  rank: number;
  evaluationResultId: string;
  backtestResultId: string;
  score: string;
  maximumDrawdown: string;
  evaluationFingerprint: string;
  candidateId?: string | null;
  candidateFingerprint?: string | null;
  candidateSummary?: string | null;
  metrics?: Readonly<{
    totalReturn: string;
    winRate: string;
    maximumDrawdown: string;
    numberOfTrades: number;
    metricVersion: string;
  }> | null;
}>;
export type LeaderboardSnapshot = Readonly<{
  experimentId: string;
  revisionId: string;
  revision: number;
  topK: number;
  rankingPolicyVersion: string;
  fingerprint: string;
  createdAt: string;
  items: readonly LeaderboardEntry[];
  nextCursor: string | null;
  hasMore: boolean;
}>;
export const capLeaderboardLimit = (value: number, configured = 100) =>
  Math.min(configured, 100, Math.max(1, Math.trunc(value || 10)));

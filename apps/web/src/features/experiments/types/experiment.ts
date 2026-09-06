export type ExperimentStatus =
  "CREATED" | "QUEUED" | "RUNNING" | "STOP_REQUESTED" | "STOPPED" | "COMPLETED" | "FAILED";
export type Failure = Readonly<{ code: string; message: string }>;
export type Experiment = Readonly<{
  experimentId: string;
  name: string;
  status: ExperimentStatus;
  datasetId: string;
  dataset?: Readonly<{
    datasetId: string;
    provider: string;
    pair: string;
    timeframe: string;
    startTime: string;
    endTime: string;
    candleCount: number;
    checksum: string;
    normalizationVersion: string;
  }>;
  jobIds: readonly string[];
  searchJob?: Job | null;
  derivedFromExperimentId: string | null;
  reproducesExperimentId: string | null;
  startedAt: string | null;
  completedAt: string | null;
  failure: Failure | null;
  searchProgress?: Readonly<{
    allocated: number;
    active: number;
    completed: number;
    failed: number;
    remainingCapacity: number;
    configuredMaximum: number;
    topK: number;
    bestScore: string | null;
    startedAt: string | null;
    terminalReason: string | null;
  }> | null;
  createdAt: string;
}>;
export type Job = Readonly<{
  jobId: string;
  experimentId: string;
  candidateId: string | null;
  type: "SEARCH" | "BACKTEST";
  status: string;
  totalWork: number;
  completedWork: number;
  failedWork: number;
  bestScore: string | null;
  queuedAt: string;
  startedAt: string | null;
  finishedAt: string | null;
  nextRetryAt: string | null;
  failure: Failure | null;
  createdAt: string;
  updatedAt: string;
}>;
export type Candidate = Readonly<{
  candidateId: string;
  experimentId: string;
  generationIndex: number;
  definition: Readonly<Record<string, unknown>>;
  generatorState: Readonly<Record<string, unknown>> | null;
  fingerprint: string;
  createdAt: string;
}>;
export type CandidatePage = Readonly<{
  items: readonly Candidate[];
  nextCursor: string | null;
  hasMore: boolean;
}>;
export type ExperimentHistoryItem = Readonly<{
  experimentId: string;
  name: string;
  status: ExperimentStatus;
  dataset: Readonly<{ provider: string; pair: string; timeframe: string; candleCount: number }>;
  progress: Readonly<{ processed: number; total: number; succeeded: number; failed: number }>;
  startedAt: string | null;
  completedAt: string | null;
  createdAt: string;
}>;
export type ExperimentHistoryPage = Readonly<{
  items: readonly ExperimentHistoryItem[];
  nextCursor: string | null;
  hasMore: boolean;
  totalCount: number;
}>;
export type CandidatePipelineView = "RESULTS" | "FAILED" | "ALL";
export type EligibilityReason = Readonly<{
  code: "MINIMUM_TRADES_NOT_MET" | "UNSPECIFIED";
  actualTrades: number | null;
  requiredTrades: number | null;
}>;
export type CandidatePipelineItem = Readonly<{
  candidateId: string;
  generationIndex: number;
  definition: Readonly<Record<string, unknown>>;
  candidateSummary: string;
  candidateFingerprint: string;
  createdAt: string;
  backtest: Readonly<{
    jobId: string | null;
    status: string;
    backtestResultId: string | null;
    startedAt: string | null;
    finishedAt: string | null;
    attemptNo: number | null;
    nextRetryAt: string | null;
    retryable: boolean;
    failure: Failure | null;
  }>;
  evaluation: Readonly<{
    status: string;
    evaluationResultId: string | null;
    score: string | null;
    totalReturn: string | null;
    winRate: string | null;
    maximumDrawdown: string | null;
    numberOfTrades: number | null;
    metricVersion: string | null;
    eligible: boolean | null;
    eligibilityReason: EligibilityReason | null;
    evaluatedAt: string | null;
  }>;
  ranking: Readonly<{ status: string; rank: number | null; rankingVersion: string | null }>;
  failureStage: string | null;
}>;
export type CandidatePipelinePage = Readonly<{
  items: readonly CandidatePipelineItem[];
  nextCursor: string | null;
  hasMore: boolean;
  resultCount: number;
  failedCount: number;
  totalCount: number;
}>;
export type CandidateDetail = Readonly<{
  candidateId: string;
  generationIndex: number;
  definition: Readonly<Record<string, unknown>>;
  generatorState: Readonly<Record<string, unknown>> | null;
  candidateFingerprint: string;
  dataset: Readonly<{
    datasetId: string;
    version: string;
    checksum: string;
    provider: string;
    pair: string;
    timeframe: string;
    normalizationVersion: string;
    startTime: string;
    endTime: string;
    candleCount: number;
  }>;
  backtestResultId: string | null;
  backtestStatus: string;
  metrics: Readonly<{
    totalReturn: string;
    winRate: string;
    maximumDrawdown: string;
    numberOfTrades: number;
    metricVersion: string;
  }> | null;
  backtest: CandidatePipelineItem["backtest"];
  evaluation: CandidatePipelineItem["evaluation"];
  ranking: CandidatePipelineItem["ranking"];
  failureStage: string | null;
}>;
export const terminalExperiment = (status: ExperimentStatus) =>
  ["STOPPED", "COMPLETED", "FAILED"].includes(status);

export type ExperimentStatus =
  "CREATED" | "QUEUED" | "RUNNING" | "STOP_REQUESTED" | "STOPPED" | "COMPLETED" | "FAILED";
export type Failure = Readonly<{ code: string; message: string }>;
export type Experiment = Readonly<{
  experimentId: string;
  name: string;
  status: ExperimentStatus;
  datasetId: string;
  jobIds: readonly string[];
  derivedFromExperimentId: string | null;
  reproducesExperimentId: string | null;
  startedAt: string | null;
  completedAt: string | null;
  failure: Failure | null;
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
export const terminalExperiment = (status: ExperimentStatus) =>
  ["STOPPED", "COMPLETED", "FAILED"].includes(status);

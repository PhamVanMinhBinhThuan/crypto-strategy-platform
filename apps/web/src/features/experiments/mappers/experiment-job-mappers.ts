import { z } from "zod";
const utc = z.string().datetime(),
  nullableUtc = utc.nullable(),
  failure = z.object({ code: z.string(), message: z.string() }).strict().nullable();
const searchProgress = z
  .object({
    allocated: z.number().int().nonnegative(),
    active: z.number().int().nonnegative(),
    completed: z.number().int().nonnegative(),
    failed: z.number().int().nonnegative(),
    remainingCapacity: z.number().int().nonnegative(),
    configuredMaximum: z.number().int().positive(),
    topK: z.number().int().positive(),
    bestScore: z.string().nullable(),
    startedAt: nullableUtc,
    terminalReason: z.string().nullable()
  })
  .strict()
  .nullable()
  .optional();
const job = z.object({
  jobId: z.string(),
  experimentId: z.string(),
  candidateId: z.string().nullable(),
  type: z.enum(["SEARCH", "BACKTEST"]),
  status: z.string(),
  totalWork: z.number().int().positive(),
  completedWork: z.number().int().nonnegative(),
  failedWork: z.number().int().nonnegative(),
  bestScore: z.string().nullable(),
  queuedAt: utc,
  startedAt: nullableUtc,
  finishedAt: nullableUtc,
  nextRetryAt: nullableUtc,
  failure,
  createdAt: utc,
  updatedAt: utc
});
export const experimentSchema = z
  .object({
    experimentId: z.string(),
    name: z.string(),
    status: z.enum([
      "CREATED",
      "QUEUED",
      "RUNNING",
      "STOP_REQUESTED",
      "STOPPED",
      "COMPLETED",
      "FAILED"
    ]),
    datasetId: z.string(),
    dataset: z
      .object({
        datasetId: z.string(),
        provider: z.string(),
        pair: z.string(),
        timeframe: z.string(),
        startTime: utc,
        endTime: utc,
        candleCount: z.number().int().nonnegative(),
        checksum: z.string(),
        normalizationVersion: z.string()
      })
      .strict()
      .optional(),
    jobIds: z.array(z.string()),
    searchJob: job.nullable().optional(),
    derivedFromExperimentId: z.string().nullable(),
    reproducesExperimentId: z.string().nullable(),
    startedAt: nullableUtc,
    completedAt: nullableUtc,
    failure,
    searchProgress,
    createdAt: utc
  })
  .strict();
export const jobSchema = z
  .object(job.shape)
  .strict();
export const candidateSchema = z
  .object({
    candidateId: z.string(),
    experimentId: z.string(),
    generationIndex: z.number().int().nonnegative(),
    definition: z.record(z.string(), z.unknown()),
    generatorState: z.record(z.string(), z.unknown()).nullable(),
    fingerprint: z.string(),
    createdAt: utc
  })
  .strict();
export const candidatePageSchema = z.object({
  items: z.array(candidateSchema),
  nextCursor: z.string().nullable(),
  hasMore: z.boolean()
});
export const mapExperiment = (v: unknown) => experimentSchema.parse(v);
export const mapJob = (v: unknown) => jobSchema.parse(v);
export const mapCandidatePage = (v: unknown) => candidatePageSchema.parse(v);
export const experimentHistoryPageSchema = z.object({
  items: z.array(z.object({
    experimentId: z.string(),
    name: z.string(),
    status: experimentSchema.shape.status,
    dataset: z.object({
      provider: z.string(), pair: z.string(), timeframe: z.string(),
      candleCount: z.number().int().nonnegative()
    }).strict(),
    progress: z.object({
      processed: z.number().int().nonnegative(), total: z.number().int().nonnegative(),
      succeeded: z.number().int().nonnegative(), failed: z.number().int().nonnegative()
    }).strict(),
    startedAt: nullableUtc,
    completedAt: nullableUtc,
    createdAt: utc
  }).strict()),
  nextCursor: z.string().nullable(),
  hasMore: z.boolean(),
  totalCount: z.number().int().nonnegative()
}).strict();
export const mapExperimentHistoryPage = (value: unknown) => experimentHistoryPageSchema.parse(value);
const pipelineStageFailure = z.object({ code: z.string(), message: z.string() }).strict().nullable();
const eligibilityReason = z
  .object({
    code: z.enum(["MINIMUM_TRADES_NOT_MET", "UNSPECIFIED"]),
    actualTrades: z.number().int().nonnegative().nullable(),
    requiredTrades: z.number().int().positive().nullable()
  })
  .strict()
  .nullable();
const pipelineBacktestSchema = z
  .object({
    jobId: z.string().nullable(),
    status: z.string(),
    backtestResultId: z.string().nullable(),
    startedAt: nullableUtc,
    finishedAt: nullableUtc,
    attemptNo: z.number().int().positive().nullable(),
    nextRetryAt: nullableUtc,
    retryable: z.boolean(),
    failure: pipelineStageFailure
  })
  .strict();
const pipelineEvaluationSchema = z
  .object({
    status: z.string(),
    evaluationResultId: z.string().nullable(),
    score: z.string().nullable(),
    totalReturn: z.string().nullable(),
    winRate: z.string().nullable(),
    maximumDrawdown: z.string().nullable(),
    numberOfTrades: z.number().int().nullable(),
    metricVersion: z.string().nullable(),
    eligible: z.boolean().nullable(),
    eligibilityReason,
    evaluatedAt: nullableUtc
  })
  .strict();
const pipelineRankingSchema = z
  .object({
    status: z.string(),
    rank: z.number().int().positive().nullable(),
    rankingVersion: z.string().nullable()
  })
  .strict();
export const candidatePipelinePageSchema = z
  .object({
    items: z.array(
      z
        .object({
          candidateId: z.string(),
          generationIndex: z.number().int().nonnegative(),
          definition: z.record(z.string(), z.unknown()),
          candidateSummary: z.string(),
          candidateFingerprint: z.string(),
          createdAt: utc,
          backtest: pipelineBacktestSchema,
          evaluation: pipelineEvaluationSchema,
          ranking: pipelineRankingSchema,
          failureStage: z.string().nullable()
        })
        .strict()
    ),
    nextCursor: z.string().nullable(),
    hasMore: z.boolean(),
    resultCount: z.number().int().nonnegative(),
    failedCount: z.number().int().nonnegative(),
    totalCount: z.number().int().nonnegative()
  })
  .strict();
export const mapCandidatePipelinePage = (value: unknown) => candidatePipelinePageSchema.parse(value);

export const candidateDetailSchema = z
  .object({
    candidateId: z.string(),
    generationIndex: z.number().int().nonnegative(),
    definition: z.record(z.string(), z.unknown()),
    generatorState: z.record(z.string(), z.unknown()).nullable(),
    candidateFingerprint: z.string(),
    dataset: z
      .object({
        datasetId: z.string(),
        version: z.string(),
        checksum: z.string(),
        provider: z.string(),
        pair: z.string(),
        timeframe: z.string(),
        normalizationVersion: z.string(),
        startTime: utc,
        endTime: utc,
        candleCount: z.number().int().nonnegative()
      })
      .strict(),
    backtestResultId: z.string().nullable(),
    backtestStatus: z.string(),
    metrics: z
      .object({
        totalReturn: z.string(),
        winRate: z.string(),
        maximumDrawdown: z.string(),
        numberOfTrades: z.number().int().nonnegative(),
        metricVersion: z.string()
      })
      .strict()
      .nullable(),
    backtest: pipelineBacktestSchema,
    evaluation: pipelineEvaluationSchema,
    ranking: pipelineRankingSchema,
    failureStage: z.string().nullable()
  })
  .strict();
export const mapCandidateDetail = (value: unknown) => candidateDetailSchema.parse(value);

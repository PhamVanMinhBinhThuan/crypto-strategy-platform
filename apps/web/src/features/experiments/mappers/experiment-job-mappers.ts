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
    jobIds: z.array(z.string()),
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
  .object({
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
  })
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

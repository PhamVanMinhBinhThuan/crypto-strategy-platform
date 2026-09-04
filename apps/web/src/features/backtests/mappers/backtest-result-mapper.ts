import { z } from "zod";
import type { BacktestResultViewModel } from "../types/backtest-result";
const decimal = z.string().regex(/^-?\d+(?:\.\d+)?$/);
const trade = z
  .object({
    tradeId: z.string().min(1),
    sequence: z.number().int().nonnegative(),
    side: z.string().min(1),
    entryTime: z.string().datetime(),
    entryPrice: decimal,
    exitTime: z.string().datetime(),
    exitPrice: decimal,
    quantity: decimal,
    entryFee: decimal,
    exitFee: decimal,
    totalFee: decimal,
    profitLoss: decimal,
    postTradeCash: decimal,
    exitReason: z.string().min(1)
  })
  .strict();
const provenanceParameter = z.object({ type: z.string(), value: z.string() }).strict();
const strategyReference = z
  .object({
    strategyVersionId: z.string().min(1),
    pluginId: z.string().min(1),
    implementationVersion: z.string().min(1)
  })
  .strict();
const strategyEvidence = z
  .object({
    kind: z.enum(["SINGLE", "COMPOSITE"]),
    singleStrategy: strategyReference.nullable(),
    parameters: z.record(z.string(), provenanceParameter),
    compositePolicyId: z.string().nullable(),
    compositePolicyVersion: z.string().nullable(),
    components: z.array(
      z
        .object({
          strategy: strategyReference,
          parameters: z.record(z.string(), provenanceParameter)
        })
        .strict()
    ),
    sourceUserStrategyVersionId: z.string().nullable(),
    fingerprint: z.string().min(1)
  })
  .strict();
const datasetEvidence = z
  .object({
    datasetVersionId: z.string().min(1),
    version: z.string().min(1),
    checksum: z.string().min(1),
    provider: z.string().min(1),
    tradingPair: z.string().min(1),
    timeframe: z.string().min(1),
    normalizationVersion: z.string().min(1),
    rangeStart: z.string().datetime(),
    rangeEnd: z.string().datetime(),
    candleCount: z.number().int().nonnegative()
  })
  .strict();
const candidateEvidence = z
  .object({
    candidateId: z.string().min(1),
    generationIndex: z.number().int().nonnegative(),
    definition: z.record(z.string(), z.unknown()),
    fingerprint: z.string().min(1),
    createdAt: z.string().datetime()
  })
  .strict();
const schema = z
  .object({
    backtestResultId: z.string().min(1),
    backtestId: z.string().min(1).nullish(),
    status: z.literal("COMPLETED"),
    metrics: z
      .object({
        totalReturn: decimal,
        winRate: decimal,
        maximumDrawdown: decimal,
        numberOfTrades: z.number().int().nonnegative()
      })
      .strict(),
    trades: z.array(trade),
    provenance: z
      .object({
        experimentId: z.string(),
        candidateId: z.string(),
        jobId: z.string(),
        successfulAttemptId: z.string(),
        manifestFingerprint: z.string(),
        datasetFingerprint: z.string(),
        strategyFingerprint: z.string(),
        resultFingerprint: z.string(),
        manifestVersion: z.string().nullable(),
        dataset: datasetEvidence.nullable(),
        strategy: strategyEvidence.nullable(),
        candidate: candidateEvidence.nullable(),
        softwareVersion: z.string().nullable(),
        gitCommit: z.string().nullable()
      })
      .strict(),
    assumptions: z
      .object({
        assumptionsVersion: z.string(),
        initialCapital: decimal,
        feeRate: decimal,
        slippageRate: decimal,
        positionMode: z.string(),
        executionPriceRule: z.string(),
        forceCloseAtEnd: z.boolean(),
        roundingMode: z.string()
      })
      .strict(),
    initialCapital: decimal,
    finalCapital: decimal,
    totalFees: decimal,
    completedAt: z.string().datetime()
  })
  .strict()
  .superRefine((value, context) => {
    if (value.metrics.numberOfTrades !== value.trades.length)
      context.addIssue({ code: "custom", message: "Trade count does not match Trade history." });
    const ids = new Set<string>();
    const sequences = new Set<number>();
    value.trades.forEach((item, index) => {
      if (
        ids.has(item.tradeId) ||
        sequences.has(item.sequence) ||
        item.sequence !== index ||
        Date.parse(item.entryTime) >= Date.parse(item.exitTime)
      )
        context.addIssue({ code: "custom", message: "Trade evidence ordering is invalid." });
      ids.add(item.tradeId);
      sequences.add(item.sequence);
    });
  });
export function mapBacktestResult(value: unknown): BacktestResultViewModel {
  const parsed = schema.parse(value);
  return {
    ...parsed,
    backtestId: parsed.backtestId
      ? (parsed.backtestId as NonNullable<BacktestResultViewModel["backtestId"]>)
      : undefined,
    backtestResultId: parsed.backtestResultId as BacktestResultViewModel["backtestResultId"]
  };
}

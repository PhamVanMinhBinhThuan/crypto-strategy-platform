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
const schema = z
  .object({
    backtestResultId: z.string().min(1),
    backtestId: z.string().min(1),
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
        resultFingerprint: z.string()
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
  .strict();
export function mapBacktestResult(value: unknown): BacktestResultViewModel {
  const parsed = schema.parse(value);
  return {
    ...parsed,
    backtestId: parsed.backtestId as BacktestResultViewModel["backtestId"],
    backtestResultId: parsed.backtestResultId as BacktestResultViewModel["backtestResultId"]
  };
}

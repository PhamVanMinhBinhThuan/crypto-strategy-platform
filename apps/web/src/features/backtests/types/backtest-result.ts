import type { PublicError } from "@/src/foundation/http/contracts";
export type BacktestId = string & { readonly __kind: "BacktestId" };
export type BacktestResultId = string & { readonly __kind: "BacktestResultId" };
export type BacktestLookup =
  | { kind: "none" }
  | { kind: "invalid"; message: string }
  | { kind: "backtestId"; id: BacktestId }
  | { kind: "resultId"; id: BacktestResultId };
export type TradeViewModel = Readonly<{
  tradeId: string;
  sequence: number;
  side: string;
  entryTime: string;
  entryPrice: string;
  exitTime: string;
  exitPrice: string;
  quantity: string;
  entryFee: string;
  exitFee: string;
  totalFee: string;
  profitLoss: string;
  postTradeCash: string;
  exitReason: string;
}>;
export type BacktestResultViewModel = Readonly<{
  backtestResultId: BacktestResultId;
  backtestId: BacktestId;
  status: "COMPLETED";
  metrics: Readonly<{
    totalReturn: string;
    winRate: string;
    maximumDrawdown: string;
    numberOfTrades: number;
  }>;
  trades: readonly TradeViewModel[];
  provenance: Readonly<
    Record<
      | "experimentId"
      | "candidateId"
      | "jobId"
      | "successfulAttemptId"
      | "manifestFingerprint"
      | "datasetFingerprint"
      | "strategyFingerprint"
      | "resultFingerprint",
      string
    >
  >;
  assumptions: Readonly<{
    assumptionsVersion: string;
    initialCapital: string;
    feeRate: string;
    slippageRate: string;
    positionMode: string;
    executionPriceRule: string;
    forceCloseAtEnd: boolean;
    roundingMode: string;
  }>;
  initialCapital: string;
  finalCapital: string;
  totalFees: string;
  completedAt: string;
}>;
export type BacktestQueryState =
  | { status: "idle" }
  | { status: "loading" }
  | { status: "refreshing" }
  | { status: "empty-identifier" }
  | { status: "success"; snapshot: BacktestResultViewModel }
  | {
      status: "inaccessible" | "dependency-blocked" | "retryable-failure" | "terminal-failure";
      error: PublicError;
      snapshot?: BacktestResultViewModel;
    };
export function parseBacktestLookup(values: {
  resultId?: string;
  backtestId?: string;
}): BacktestLookup {
  const resultId = values.resultId?.trim(),
    backtestId = values.backtestId?.trim();
  if (resultId && backtestId)
    return { kind: "invalid", message: "Provide only one result identifier." };
  if (!resultId && !backtestId) return { kind: "none" };
  const value = resultId ?? backtestId ?? "";
  if (!/^[A-Za-z0-9_-]{6,128}$/.test(value))
    return { kind: "invalid", message: "The identifier is malformed." };
  return resultId
    ? { kind: "resultId", id: value as BacktestResultId }
    : { kind: "backtestId", id: value as BacktestId };
}

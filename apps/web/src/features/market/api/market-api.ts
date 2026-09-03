import type { ApiClient, ApiResult } from "@/src/foundation/http/contracts";
import { requestPublic } from "../../shared/feature-api";
import { candlePageSchema } from "./schemas";
import type { MarketPair, MarketTimeframe } from "../model/market-catalog";
import type { z } from "zod";
export type CandlePage = z.infer<typeof candlePageSchema>;
export function listCandles(
  client: ApiClient,
  query: {
    pair: MarketPair;
    timeframe: MarketTimeframe;
    startTime: string;
    endTime: string;
    limit?: number;
  }
): Promise<ApiResult<CandlePage>> {
  const params = new URLSearchParams({
    pair: query.pair,
    timeframe: query.timeframe,
    startTime: query.startTime,
    endTime: query.endTime,
    limit: String(query.limit ?? 200)
  });
  return requestPublic(client, candlePageSchema, `/api/v1/candles?${params}`);
}

import type { ApiClient, ApiResult } from "@/src/foundation/http/contracts";
import { mapBacktestResult } from "../mappers/backtest-result-mapper";
import type {
  BacktestId,
  BacktestResultId,
  BacktestResultViewModel
} from "../types/backtest-result";
const inaccessible = {
  ok: false,
  error: { code: "RESOURCE_NOT_FOUND", message: "Resource inaccessible", retryable: false }
} as const;
const mapped = (result: ApiResult<unknown>): ApiResult<BacktestResultViewModel> => {
  if (!result.ok)
    return ["RESOURCE_NOT_FOUND", "FORBIDDEN"].includes(result.error.code) ? inaccessible : result;
  try {
    return { ...result, data: mapBacktestResult(result.data) };
  } catch {
    return {
      ok: false,
      error: {
        code: "INVALID_RESPONSE",
        message: "The service returned an invalid result.",
        retryable: false
      }
    };
  }
};
export const createBacktestResultService = (api: ApiClient) => ({
  async readByBacktestId(id: BacktestId) {
    return mapped(await api.request(`/api/v1/backtests/${encodeURIComponent(id)}/result`));
  },
  async readByResultId(id: BacktestResultId): Promise<ApiResult<BacktestResultViewModel>> {
    return mapped(await api.request(`/api/v1/backtest-results/${encodeURIComponent(id)}`));
  }
});

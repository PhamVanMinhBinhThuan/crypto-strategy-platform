import type { ApiClient, ApiResult } from "../http/contracts";
export class MockApiClient implements ApiClient {
  constructor(private readonly responses = new Map<string, unknown>()) {}
  async request<T>(path: string): Promise<ApiResult<T>> {
    return this.responses.has(path)
      ? { ok: true, data: this.responses.get(path) as T, correlationId: "fixture-correlation" }
      : {
          ok: false,
          error: {
            code: "FIXTURE_NOT_FOUND",
            message: "No fixture is configured for this request.",
            retryable: false
          }
        };
  }
}

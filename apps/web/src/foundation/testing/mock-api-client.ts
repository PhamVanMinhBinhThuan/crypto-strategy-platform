import type { ApiClient, ApiResult } from "../http/contracts";
export class MockApiClient implements ApiClient {
  readonly requests: { path: string; init: RequestInit }[] = [];
  constructor(private readonly responses = new Map<string, unknown | ApiResult<unknown>>()) {}
  respond(path: string, value: unknown | ApiResult<unknown>) {
    this.responses.set(path, value);
    return this;
  }
  async request<T>(path: string, init: RequestInit = {}): Promise<ApiResult<T>> {
    this.requests.push({ path, init });
    const key = `${(init.method ?? "GET").toUpperCase()} ${path}`;
    const value = this.responses.get(key) ?? this.responses.get(path);
    const isResult =
      typeof value === "object" && value !== null && "ok" in (value as Record<string, unknown>);
    return value !== undefined
      ? isResult
        ? (value as ApiResult<T>)
        : { ok: true, data: value as T, correlationId: "fixture-correlation" }
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

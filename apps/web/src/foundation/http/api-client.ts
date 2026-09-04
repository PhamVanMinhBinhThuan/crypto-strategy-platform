import type { ApiClient, ApiResult } from "./contracts";
import type { AuthClient } from "../auth/contracts";
import { mapPublicError, normalizeRetryAfter } from "./error-mapper";
export function createApiClient(
  baseUrl: string,
  auth: AuthClient,
  fetcher: typeof fetch = fetch,
  recover?: () => Promise<unknown>
): ApiClient {
  return {
    async request<T>(path: string, init: RequestInit = {}): Promise<ApiResult<T>> {
      const session = await auth.session();
      const correlationId = crypto.randomUUID();
      const response = await fetcher(`${baseUrl}${path}`, {
        ...init,
        headers: {
          Accept: "application/json",
          "X-Correlation-ID": correlationId,
          ...(session ? { Authorization: `Bearer ${session.accessToken}` } : {}),
          ...init.headers
        }
      });
      const responseCorrelation = response.headers.get("X-Correlation-ID") ?? correlationId;
      const body = await response.json().catch(() => null);
      if (response.status === 401 && recover) await recover();
      return response.ok
        ? { ok: true, data: body as T, correlationId: responseCorrelation }
        : {
            ok: false,
            error: mapPublicError(
              response.status,
              body,
              responseCorrelation,
              normalizeRetryAfter(response.headers.get("Retry-After"))
            )
          };
    }
  };
}

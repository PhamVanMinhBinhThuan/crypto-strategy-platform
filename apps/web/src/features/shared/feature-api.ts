import type { z } from "zod";
import type { ApiClient, ApiResult } from "@/src/foundation/http/contracts";

export async function requestPublic<T>(
  client: ApiClient,
  schema: z.ZodType<T>,
  path: string,
  init?: RequestInit
): Promise<ApiResult<T>> {
  const result = await client.request<unknown>(path, init);
  if (!result.ok) return result;
  const parsed = schema.safeParse(result.data);
  return parsed.success
    ? { ...result, data: parsed.data }
    : {
        ok: false,
        error: {
          code: "INVALID_PUBLIC_RESPONSE",
          message: "The service returned an invalid response.",
          correlationId: result.correlationId,
          retryable: false
        }
      };
}

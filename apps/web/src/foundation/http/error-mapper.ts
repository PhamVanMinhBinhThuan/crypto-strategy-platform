import type { PublicError } from "./contracts";
const retryable = new Set([429, 502, 503, 504]);
export function mapPublicError(status: number, body: unknown, correlationId?: string): PublicError {
  const record = typeof body === "object" && body ? (body as Record<string, unknown>) : {};
  return {
    code: typeof record.code === "string" ? record.code : "REQUEST_FAILED",
    message:
      status === 401
        ? "Your session has expired. Please sign in again."
        : typeof record.message === "string"
          ? record.message
          : "The service could not complete this request.",
    correlationId,
    retryable: retryable.has(status)
  };
}

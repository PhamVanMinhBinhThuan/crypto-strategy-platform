import type { PublicError } from "./contracts";
const retryable = new Set([429, 502, 503, 504]);
export function normalizeRetryAfter(value: string | null): number | undefined {
  if (!value || !/^\d+$/.test(value.trim())) return undefined;
  const seconds = Number(value);
  return Number.isSafeInteger(seconds) && seconds >= 0 ? seconds : undefined;
}
export function mapPublicError(
  status: number,
  body: unknown,
  correlationId?: string,
  retryAfterSeconds?: number
): PublicError {
  const record = typeof body === "object" && body ? (body as Record<string, unknown>) : {};
  const details =
    typeof record.details === "object" && record.details
      ? (record.details as Record<string, unknown>)
      : {};
  const fieldErrors = Array.isArray(details.fieldErrors)
    ? details.fieldErrors.flatMap((value) => {
        if (!value || typeof value !== "object") return [];
        const item = value as Record<string, unknown>;
        return typeof item.field === "string" && typeof item.reason === "string"
          ? [{ field: item.field, reason: item.reason }]
          : [];
      })
    : [];
  return {
    code: typeof record.code === "string" ? record.code : "REQUEST_FAILED",
    message:
      status === 401
        ? "Your session has expired. Please sign in again."
        : typeof record.message === "string"
          ? record.message
          : "The service could not complete this request.",
    correlationId,
    retryable: retryable.has(status),
    ...(retryAfterSeconds === undefined ? {} : { retryAfterSeconds }),
    ...(fieldErrors.length === 0 ? {} : { fieldErrors })
  };
}

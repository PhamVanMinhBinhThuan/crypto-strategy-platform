export type PublicError = Readonly<{
  code: string;
  message: string;
  correlationId?: string;
  retryable: boolean;
}>;
export type ApiResult<T> =
  { ok: true; data: T; correlationId?: string } | { ok: false; error: PublicError };
export interface ApiClient {
  request<T>(path: string, init?: RequestInit): Promise<ApiResult<T>>;
}

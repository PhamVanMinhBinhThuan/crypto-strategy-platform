export type AsyncState<T> =
  | { kind: "loading" }
  | { kind: "success"; data: T }
  | { kind: "empty" }
  | { kind: "error"; message: string; retryable: boolean }
  | { kind: "degraded"; data?: T; message: string };

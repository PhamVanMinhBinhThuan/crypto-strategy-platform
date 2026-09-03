import type { PublicError } from "@/src/foundation/http/contracts";
export type ExactDecimal = string;
export type UtcInstant = string;
export type InaccessibleState = Readonly<{
  status: "inaccessible";
  message: "Resource inaccessible";
}>;
export type DependencyGateState = Readonly<{
  status: "dependency-blocked";
  code: string;
  message: string;
}>;
export type RateLimitState<T> = Readonly<{
  status: "rate-limited";
  retryAfterSeconds?: number;
  snapshot?: T;
}>;
export type AsyncSnapshot<T> =
  | { status: "idle" | "loading"; snapshot?: T }
  | { status: "success"; snapshot: T }
  | {
      status: "inaccessible" | "retryable-failure" | "terminal-failure";
      error: PublicError;
      snapshot?: T;
    };

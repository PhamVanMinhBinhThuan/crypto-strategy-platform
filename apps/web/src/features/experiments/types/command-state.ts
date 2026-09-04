import type { PublicError } from "@/src/foundation/http/contracts";
export type CommandState =
  | { status: "idle" }
  | { status: "submitting"; key: string }
  | { status: "accepted"; key: string }
  | {
      status:
        | "uncertain"
        | "conflict"
        | "dependency-unavailable"
        | "retryable-failure"
        | "terminal-failure";
      key: string;
      error: PublicError;
    };
export const newCommandKey = () => crypto.randomUUID();

export type ExperimentCommandState =
  | Exclude<CommandState, { status: "accepted" }>
  | {
      status: "accepted";
      key: string;
      experimentId: string;
      jobId: string;
      acceptedStatus: string;
    };

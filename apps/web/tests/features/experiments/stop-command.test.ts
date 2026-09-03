import { describe, expect, it } from "vitest";
import type { CommandState } from "@/src/features/experiments/types/command-state";
const classify = (code: string, retryable = false): CommandState["status"] =>
  code.includes("STATE") || code.includes("CONFLICT")
    ? "conflict"
    : code === "TRANSPORT_UNCERTAIN"
      ? "uncertain"
      : retryable
        ? "retryable-failure"
        : "terminal-failure";
describe("Stop command states", () => {
  it.each([
    ["accepted", "accepted"],
    ["INVALID_STATE_TRANSITION", "conflict"],
    ["TRANSPORT_UNCERTAIN", "uncertain"],
    ["RESOURCE_NOT_FOUND", "terminal-failure"],
    ["AUTHENTICATION_REQUIRED", "terminal-failure"],
    ["RATE_LIMIT_EXCEEDED", "retryable-failure"],
    ["JOB_INTERNAL_ERROR", "terminal-failure"]
  ] as const)("maps %s", (code, state) =>
    expect(code === "accepted" ? "accepted" : classify(code, code === "RATE_LIMIT_EXCEEDED")).toBe(
      state
    )
  );
});

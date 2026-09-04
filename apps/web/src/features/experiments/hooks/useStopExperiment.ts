"use client";
import { useRef, useState } from "react";
import type { ApiClient } from "@/src/foundation/http/contracts";
import { createExperimentCommandService } from "../service/experiment-command-service";
import { newCommandKey, type CommandState } from "../types/command-state";
export function useStopExperiment(api: ApiClient, id: string, onConflict: () => void) {
  const [state, setState] = useState<CommandState>({ status: "idle" });
  const pending = useRef(false);
  const key = useRef<string | undefined>(undefined);
  const execute = async (retry = false) => {
    if (pending.current) return;
    pending.current = true;
    if (!retry || !key.current) key.current = newCommandKey();
    setState({ status: "submitting", key: key.current });
    const r = await createExperimentCommandService(api).stop(id, key.current);
    pending.current = false;
    if (r.ok) {
      setState({ status: "accepted", key: key.current });
      return;
    }
    const status =
      r.error.code.includes("STATE") || r.error.code.includes("CONFLICT")
        ? "conflict"
        : r.error.code === "TRANSPORT_UNCERTAIN"
          ? "uncertain"
          : r.error.retryable
            ? "retryable-failure"
            : "terminal-failure";
    setState({ status, key: key.current, error: r.error });
    if (status === "conflict") onConflict();
  };
  return { state, execute, retry: () => execute(true) };
}

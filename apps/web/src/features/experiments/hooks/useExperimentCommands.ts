"use client";
import { useRef, useState } from "react";
import type { ApiClient } from "@/src/foundation/http/contracts";
import type { ExperimentDraft } from "../types/experiment-configuration";
import { createExperimentCommandService } from "../service/experiment-command-service";
import { newCommandKey, type CommandState } from "../types/command-state";
export function useExperimentCommands(api: ApiClient) {
  const [start, setStart] = useState<CommandState>({ status: "idle" });
  const [reproduce, setReproduce] = useState<CommandState>({ status: "idle" });
  const startKey = useRef<string | undefined>(undefined),
    reproduceKey = useRef<string | undefined>(undefined),
    busy = useRef({ start: false, reproduce: false });
  const run = async (
    kind: "start" | "reproduce",
    draft?: ExperimentDraft,
    id?: string,
    retry = false
  ) => {
    if (busy.current[kind]) return;
    busy.current[kind] = true;
    const ref = kind === "start" ? startKey : reproduceKey;
    if (!retry || !ref.current) ref.current = newCommandKey();
    const setter = kind === "start" ? setStart : setReproduce;
    setter({ status: "submitting", key: ref.current });
    const service = createExperimentCommandService(api);
    const r =
      kind === "start"
        ? await service.start(draft!, ref.current)
        : await service.reproduce(id!, ref.current);
    busy.current[kind] = false;
    if (r.ok) {
      setter({ status: "accepted", key: ref.current });
      return;
    }
    const status =
      r.error.code === "DEPENDENCY_UNAVAILABLE" || r.error.code === "BLOCKED_SEARCH_COORDINATOR"
        ? "dependency-unavailable"
        : r.error.code === "TRANSPORT_UNCERTAIN"
          ? "uncertain"
          : r.error.retryable
            ? "retryable-failure"
            : "terminal-failure";
    setter({ status, key: ref.current, error: r.error });
  };
  return {
    start,
    reproduce,
    startExperiment: (d: ExperimentDraft, retry = false) => run("start", d, undefined, retry),
    reproduceExperiment: (id: string, retry = false) => run("reproduce", undefined, id, retry)
  };
}

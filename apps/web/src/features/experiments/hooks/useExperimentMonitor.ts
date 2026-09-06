"use client";
import { useCallback, useEffect, useRef, useState } from "react";
import type { ApiClient } from "@/src/foundation/http/contracts";
import { createExperimentService } from "../service/experiment-service";
import type { Experiment } from "../types/experiment";
import { forgetExperiment } from "@/src/foundation/navigation/resource-history";
import { useRouter } from "next/navigation";
export function useExperimentMonitor(api: ApiClient, id?: string) {
  const router = useRouter();
  const [experiment, setExperiment] = useState<Experiment>();
  const [status, setStatus] = useState<"idle" | "loading" | "success" | "error">("idle");
  const [error, setError] = useState<string>();
  const requestVersion = useRef(0);
  const loadedExperimentId = useRef<string>();
  const refresh = useCallback(async () => {
    if (!id) return;
    const currentRequest = ++requestVersion.current;
    if (loadedExperimentId.current !== id) setExperiment(undefined);
    setStatus("loading");
    const service = createExperimentService(api);
    const exp = await service.readExperiment(id);
    if (currentRequest !== requestVersion.current) return;
    if (!exp.ok) {
      if (exp.error.code === "RESOURCE_NOT_FOUND") {
        if (forgetExperiment(id)) router.replace("/search");
        setExperiment(undefined);
        loadedExperimentId.current = undefined;
      }
      setError(
        exp.error.retryable
          ? "Experiment đang tạm thời không khả dụng. Vui lòng thử lại."
          : "Không thể tải Experiment."
      );
      setStatus("error");
      return;
    }
    loadedExperimentId.current = id;
    setExperiment(exp.data);
    setError(undefined);
    setStatus("success");
  }, [api, id, router]);
  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- experiment identity starts an external API synchronization
    void refresh();
  }, [refresh]);
  return { experiment, status, error, refresh };
}

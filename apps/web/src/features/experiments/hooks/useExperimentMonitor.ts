"use client";
import { useCallback, useEffect, useState } from "react";
import type { ApiClient } from "@/src/foundation/http/contracts";
import { createExperimentService } from "../service/experiment-service";
import type { Candidate, Experiment, Job } from "../types/experiment";
export function useExperimentMonitor(api: ApiClient, id?: string) {
  const [experiment, setExperiment] = useState<Experiment>();
  const [jobs, setJobs] = useState<Job[]>([]);
  const [candidates, setCandidates] = useState<Candidate[]>([]);
  const [status, setStatus] = useState<"idle" | "loading" | "success" | "error">("idle");
  const [error, setError] = useState<string>();
  const refresh = useCallback(async () => {
    if (!id) return;
    setStatus("loading");
    const service = createExperimentService(api);
    const exp = await service.readExperiment(id);
    if (!exp.ok) {
      setError(
        exp.error.retryable
          ? "Experiment đang tạm thời không khả dụng. Vui lòng thử lại."
          : "Không thể tải Experiment."
      );
      setStatus("error");
      return;
    }
    setExperiment(exp.data);
    const reads = await Promise.all(exp.data.jobIds.map((j) => service.readJob(j)));
    setJobs(reads.flatMap((r) => (r.ok ? [r.data] : [])));
    const page = await service.readCandidates(id);
    if (page.ok) setCandidates([...page.data.items]);
    setStatus("success");
  }, [api, id]);
  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- experiment identity starts an external API synchronization
    void refresh();
  }, [refresh]);
  return { experiment, jobs, candidates, status, error, refresh };
}

"use client";
import { useCallback, useEffect, useState } from "react";
import type { ApiClient } from "@/src/foundation/http/contracts";
import { createReproductionVerificationService } from "../service/reproduction-verification-service";
import type { ReproductionVerificationState } from "../types/reproduction-verification";

export function useReproductionVerification(api: ApiClient, experimentId?: string) {
  const [state, setState] = useState<ReproductionVerificationState>({ status: "idle" });
  const refresh = useCallback(async () => {
    if (!experimentId) return;
    setState((current) => (current.status === "success" ? current : { status: "loading" }));
    const result = await createReproductionVerificationService(api).read(experimentId);
    setState(
      result.ok
        ? { status: "success", snapshot: result.data }
        : {
            status: "error",
            message: result.error.retryable
              ? "Verification is temporarily unavailable."
              : "Unable to read reproduction verification."
          }
    );
  }, [api, experimentId]);

  useEffect(() => {
    if (!experimentId) return;
    const timer = window.setTimeout(() => void refresh(), 0);
    return () => window.clearTimeout(timer);
  }, [experimentId, refresh]);

  useEffect(() => {
    if (state.status !== "success" || !["PENDING", "RUNNING"].includes(state.snapshot.status))
      return;
    const timer = window.setInterval(() => void refresh(), 2_000);
    return () => window.clearInterval(timer);
  }, [refresh, state]);

  return { state, refresh };
}

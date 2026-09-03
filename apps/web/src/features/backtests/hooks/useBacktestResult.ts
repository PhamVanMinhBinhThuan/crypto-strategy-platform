"use client";
import { useCallback, useEffect, useRef, useState } from "react";
import type { ApiClient } from "@/src/foundation/http/contracts";
import { createBacktestResultService } from "../service/backtest-result-service";
import type { BacktestLookup, BacktestQueryState } from "../types/backtest-result";
export function useBacktestResult(api: ApiClient, lookup: BacktestLookup) {
  const lookupKind = lookup.kind;
  const lookupId =
    lookup.kind === "backtestId" || lookup.kind === "resultId" ? lookup.id : undefined;
  const lookupMessage = lookup.kind === "invalid" ? lookup.message : undefined;
  const [state, setState] = useState<BacktestQueryState>({ status: "idle" });
  const snapshot = useRef<
    Extract<BacktestQueryState, { status: "success" }>["snapshot"] | undefined
  >(undefined);
  const [eligibleAt, setEligibleAt] = useState(0);
  const load = useCallback(async () => {
    if (lookupKind === "none") {
      setState({ status: "empty-identifier" });
      return;
    }
    if (lookupKind === "invalid") {
      setState({
        status: "terminal-failure",
        error: { code: "INVALID_LOOKUP", message: lookupMessage!, retryable: false }
      });
      return;
    }
    setState({ status: snapshot.current ? "refreshing" : "loading" });
    const service = createBacktestResultService(api);
    const result =
      lookupKind === "backtestId"
        ? await service.readByBacktestId(
            lookupId as Extract<BacktestLookup, { kind: "backtestId" }>["id"]
          )
        : await service.readByResultId(
            lookupId as Extract<BacktestLookup, { kind: "resultId" }>["id"]
          );
    if (result.ok) {
      snapshot.current = result.data;
      setEligibleAt(0);
      setState({ status: "success", snapshot: result.data });
      return;
    }
    const status =
      result.error.code === "RESOURCE_NOT_FOUND"
        ? "inaccessible"
        : result.error.code.startsWith("BLOCKED_")
          ? "dependency-blocked"
          : result.error.retryable
            ? "retryable-failure"
            : "terminal-failure";
    if (result.error.retryAfterSeconds !== undefined)
      setEligibleAt(Date.now() + result.error.retryAfterSeconds * 1000);
    setState({
      status,
      error: result.error,
      ...(snapshot.current ? { snapshot: snapshot.current } : {})
    });
  }, [api, lookupId, lookupKind, lookupMessage]);
  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- route identity starts an external API synchronization
    void load();
  }, [load]);
  const canRetry = eligibleAt === 0;
  useEffect(() => {
    if (!eligibleAt) return;
    const delay = Math.max(0, eligibleAt - Date.now());
    const timer = setTimeout(() => setEligibleAt(0), delay);
    return () => clearTimeout(timer);
  }, [eligibleAt]);
  return { state, retry: load, canRetry };
}

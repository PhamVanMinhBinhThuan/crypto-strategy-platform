"use client";

import { useSyncExternalStore } from "react";
import { registerPrivateStateCleanup } from "@/src/foundation/auth/logout";

type ResourceKind = "experiment" | "backtest";
const EVENT = "crypto-strategy-resource-history";
const keys: Record<ResourceKind, string> = {
  experiment: "crypto-strategy:last-experiment-url",
  backtest: "crypto-strategy:last-backtest-url"
};
const fallbacks: Record<ResourceKind, string> = { experiment: "/search", backtest: "/backtests" };
const identifier = /^[A-Za-z0-9_-]{6,128}$/;

function validRememberedUrl(kind: ResourceKind, value: string | null): value is string {
  if (!value) return false;
  try {
    const parsed = new URL(value, "https://local.invalid");
    if (parsed.origin !== "https://local.invalid") return false;
    if (kind === "experiment") {
      const match = parsed.pathname.match(/^\/search\/([^/]+)$/);
      return Boolean(match && identifier.test(decodeURIComponent(match[1])) &&
        [null, "results", "failed", "all"].includes(parsed.searchParams.get("view")) &&
        !parsed.searchParams.has("candidateId"));
    }
    return parsed.pathname === "/backtests" && identifier.test(parsed.searchParams.get("resultId") ?? "") &&
      !parsed.searchParams.has("backtestId");
  } catch {
    return false;
  }
}

function read(kind: ResourceKind) {
  if (typeof window === "undefined") return fallbacks[kind];
  try {
    const value = window.sessionStorage.getItem(keys[kind]);
    return validRememberedUrl(kind, value) ? value : fallbacks[kind];
  } catch {
    return fallbacks[kind];
  }
}

function subscribe(listener: () => void) {
  if (typeof window === "undefined") return () => undefined;
  const onStorage = (event: StorageEvent) => {
    if (Object.values(keys).includes(event.key ?? "")) listener();
  };
  window.addEventListener(EVENT, listener);
  window.addEventListener("storage", onStorage);
  return () => {
    window.removeEventListener(EVENT, listener);
    window.removeEventListener("storage", onStorage);
  };
}

function write(kind: ResourceKind, value?: string) {
  if (typeof window === "undefined") return;
  try {
    if (value && validRememberedUrl(kind, value)) window.sessionStorage.setItem(keys[kind], value);
    else window.sessionStorage.removeItem(keys[kind]);
    window.dispatchEvent(new Event(EVENT));
  } catch {
    // Navigation still works through the current URL when storage is unavailable.
  }
}

export function useRememberedResource(kind: ResourceKind) {
  return useSyncExternalStore(subscribe, () => read(kind), () => fallbacks[kind]);
}

export function rememberExperiment(experimentId: string, view = "results") {
  if (!identifier.test(experimentId)) return;
  const normalizedView = ["results", "failed", "all"].includes(view) ? view : "results";
  write("experiment", `/search/${encodeURIComponent(experimentId)}?view=${normalizedView}`);
}

export function forgetExperiment(expectedExperimentId?: string) {
  if (expectedExperimentId && !read("experiment").startsWith(
    `/search/${encodeURIComponent(expectedExperimentId)}?`)) return false;
  write("experiment");
  return true;
}

export function rememberBacktestResult(resultId: string) {
  if (!identifier.test(resultId)) return;
  write("backtest", `/backtests?resultId=${encodeURIComponent(resultId)}`);
}

export function forgetBacktestResult(expectedResultId?: string) {
  if (expectedResultId && read("backtest") !==
    `/backtests?resultId=${encodeURIComponent(expectedResultId)}`) return false;
  write("backtest");
  return true;
}

export function candidateReturnUrl(experimentId: string, candidateId: string, view: string) {
  const normalizedView = ["results", "failed", "all"].includes(view) ? view : "results";
  const params = new URLSearchParams({ view: normalizedView, candidateId });
  return `/search/${encodeURIComponent(experimentId)}?${params.toString()}`;
}

export function safeExperimentReturnUrl(
  value: string | undefined,
  experimentId: string,
  candidateId: string
) {
  const fallback = candidateReturnUrl(experimentId, candidateId, "results");
  if (!value) return fallback;
  try {
    const parsed = new URL(value, "https://local.invalid");
    if (parsed.origin !== "https://local.invalid" ||
      parsed.pathname !== `/search/${encodeURIComponent(experimentId)}`) return fallback;
    const returnCandidate = parsed.searchParams.get("candidateId");
    if (returnCandidate && returnCandidate !== candidateId) return fallback;
    return candidateReturnUrl(
      experimentId,
      candidateId,
      parsed.searchParams.get("view") ?? "results"
    );
  } catch {
    return fallback;
  }
}

if (typeof window !== "undefined") {
  registerPrivateStateCleanup(() => {
    write("experiment");
    write("backtest");
  });
}

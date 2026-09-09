"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import type { ApiClient } from "@/src/foundation/http/contracts";
import { createExperimentService } from "../service/experiment-service";
import type { CandidatePipelinePage, CandidatePipelineView } from "../types/experiment";

type ViewState = {
  page?: CandidatePipelinePage;
  cursor?: string;
  history: Array<string | undefined>;
  error?: string;
  loading: boolean;
  stale: boolean;
};

const views: readonly CandidatePipelineView[] = ["RESULTS", "FAILED", "ALL"];
const emptyView = (): ViewState => ({ history: [], loading: false, stale: false });
const emptyViews = (): Record<CandidatePipelineView, ViewState> => ({
  RESULTS: emptyView(),
  FAILED: emptyView(),
  ALL: emptyView()
});

export function useCandidatePipeline(
  api: ApiClient,
  experimentId: string | undefined,
  view: CandidatePipelineView
) {
  const [states, setStates] = useState(emptyViews);
  const [counts, setCounts] = useState({ resultCount: 0, failedCount: 0, totalCount: 0 });
  const [countsLoaded, setCountsLoaded] = useState(false);
  const requestVersions = useRef<Record<CandidatePipelineView, number>>({
    RESULTS: 0,
    FAILED: 0,
    ALL: 0
  });
  const previousExperiment = useRef(experimentId);

  const load = useCallback(
    async (
      targetView: CandidatePipelineView,
      targetCursor?: string,
      targetHistory?: Array<string | undefined>
    ) => {
      if (!experimentId) return;
      const request = ++requestVersions.current[targetView];
      setStates((current) => ({
        ...current,
        [targetView]: { ...current[targetView], loading: true, error: undefined }
      }));
      const result = await createExperimentService(api).readCandidatePipeline(
        experimentId,
        targetView,
        targetCursor
      );
      if (request !== requestVersions.current[targetView]) return;
      if (result.ok) {
        setCounts({
          resultCount: result.data.resultCount,
          failedCount: result.data.failedCount,
          totalCount: result.data.totalCount
        });
        setCountsLoaded(true);
        setStates((current) => ({
          ...current,
          [targetView]: {
            page: result.data,
            cursor: targetCursor,
            history: targetHistory ?? current[targetView].history,
            loading: false,
            stale: false
          }
        }));
      } else {
        setStates((current) => ({
          ...current,
          [targetView]: {
            ...current[targetView],
            loading: false,
            error: "Candidate progress is temporarily unavailable."
          }
        }));
      }
    },
    [api, experimentId]
  );

  useEffect(() => {
    if (previousExperiment.current === experimentId) return;
    previousExperiment.current = experimentId;
    for (const item of views) requestVersions.current[item]++;
    setStates(emptyViews());
    setCounts({ resultCount: 0, failedCount: 0, totalCount: 0 });
    setCountsLoaded(false);
  }, [experimentId]);

  const active = states[view];
  useEffect(() => {
    if (!experimentId || active.page || active.loading) return;
    // eslint-disable-next-line react-hooks/set-state-in-effect -- an empty view starts its external API synchronization
    void load(view);
  }, [active.loading, active.page, experimentId, load, view]);

  const next = () => {
    if (!active.page?.nextCursor || active.loading) return;
    void load(view, active.page.nextCursor, [...active.history, active.cursor]);
  };
  const previous = () => {
    if (!active.history.length || active.loading) return;
    const target = active.history.at(-1);
    void load(view, target, active.history.slice(0, -1));
  };
  const notifyUpdate = useCallback(() => {
    setStates((current) => {
      const next = { ...current };
      for (const item of views) {
        if (item !== view && current[item].page) {
          next[item] = { ...current[item], stale: true };
        }
      }
      return next;
    });
    const activeView = states[view];
    void load(view, activeView.cursor, activeView.history);
  }, [load, states, view]);

  return {
    ...active,
    counts,
    countsLoaded,
    pageNumber: active.history.length + 1,
    canPrevious: active.history.length > 0,
    next,
    previous,
    refresh: () => load(view, active.cursor, active.history),
    notifyUpdate
  };
}

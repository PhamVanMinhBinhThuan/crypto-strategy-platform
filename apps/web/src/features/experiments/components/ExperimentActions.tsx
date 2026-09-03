"use client";
import { useRef, useState } from "react";
import type { ApiClient } from "@/src/foundation/http/contracts";
import type { Experiment } from "../types/experiment";
import { useStopExperiment } from "../hooks/useStopExperiment";
export function ExperimentActions({
  api,
  experiment,
  onRefresh
}: {
  api: ApiClient;
  experiment: Experiment;
  onRefresh: () => void;
}) {
  const [confirm, setConfirm] = useState(false);
  const trigger = useRef<HTMLButtonElement>(null);
  const { state, execute, retry } = useStopExperiment(api, experiment.experimentId, onRefresh);
  const stoppable = ["QUEUED", "RUNNING"].includes(experiment.status);
  return (
    <section className="actions" aria-live="polite">
      <button
        ref={trigger}
        className="button danger"
        disabled={!stoppable || state.status === "submitting"}
        onClick={() => setConfirm(true)}
      >
        Stop Experiment
      </button>
      {confirm && (
        <div className="dialog-backdrop">
          <div role="dialog" aria-modal="true" aria-labelledby="stop-title" className="dialog">
            <h2 id="stop-title">Stop this experiment?</h2>
            <p>Completed candidates and leaderboard evidence will be preserved.</p>
            <div>
              <button
                className="button danger"
                onClick={() => {
                  setConfirm(false);
                  void execute();
                  trigger.current?.focus();
                }}
              >
                Confirm stop
              </button>
              <button
                className="button secondary"
                onClick={() => {
                  setConfirm(false);
                  trigger.current?.focus();
                }}
              >
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}
      {state.status === "accepted" && (
        <p role="status">Stop requested. Waiting for authoritative STOPPED state.</p>
      )}
      {["uncertain", "retryable-failure"].includes(state.status) && (
        <button className="button secondary" onClick={() => void retry()}>
          Retry with the same command key
        </button>
      )}
      {state.status === "conflict" && (
        <p role="alert">State changed; refreshing the authoritative experiment.</p>
      )}
    </section>
  );
}

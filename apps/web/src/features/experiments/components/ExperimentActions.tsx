"use client";
import Link from "next/link";
import { useRef, useState } from "react";
import type { ApiClient } from "@/src/foundation/http/contracts";
import type { Experiment } from "../types/experiment";
import { useExperimentCommands } from "../hooks/useExperimentCommands";
import { useReproductionVerification } from "../hooks/useReproductionVerification";
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
  const commands = useExperimentCommands(api);
  const verification = useReproductionVerification(
    api,
    experiment.reproducesExperimentId ? experiment.experimentId : undefined
  );
  const stoppable = ["QUEUED", "RUNNING"].includes(experiment.status);
  const reproducible = ["COMPLETED", "STOPPED"].includes(experiment.status);
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
      <button
        className="button secondary"
        disabled={!reproducible || commands.reproduce.status === "submitting"}
        onClick={() => void commands.reproduceExperiment(experiment.experimentId)}
      >
        Reproduce Experiment
      </button>
      {commands.reproduce.status === "accepted" && (
        <p role="status">
          Reproduction accepted as a new linked Experiment; verification starts as PENDING.{" "}
          <Link href={`/search?id=${encodeURIComponent(commands.reproduce.experimentId)}`}>
            Open reproduced Experiment {commands.reproduce.experimentId}
          </Link>
        </p>
      )}
      {["dependency-unavailable", "uncertain", "retryable-failure"].includes(
        commands.reproduce.status
      ) && (
        <button
          className="button secondary"
          onClick={() => void commands.reproduceExperiment(experiment.experimentId, true)}
        >
          Retry reproduction with the same command key
        </button>
      )}
      {commands.reproduce.status === "terminal-failure" && (
        <p role="alert">Reproduction could not be accepted.</p>
      )}
      {experiment.reproducesExperimentId && (
        <article className="panel" aria-live="polite">
          <h2>Reproduction verification</h2>
          <p className="muted">
            Linked reproduction of Experiment{" "}
            <span className="mono">{experiment.reproducesExperimentId}</span>.
          </p>
          {verification.state.status === "loading" && <p role="status">Loading verdict…</p>}
          {verification.state.status === "error" && (
            <p role="alert">{verification.state.message}</p>
          )}
          {verification.state.status === "success" && (
            <>
              <p>
                Verdict: <strong>{verification.state.snapshot.status}</strong>
              </p>
              {["PENDING", "RUNNING"].includes(verification.state.snapshot.status) && (
                <p role="status">Comparing ordered trades, metrics and fingerprints…</p>
              )}
              {["MATCHED", "MISMATCHED"].includes(verification.state.snapshot.status) && (
                <dl>
                  <div>
                    <dt>Ordered trades</dt>
                    <dd>{verification.state.snapshot.tradesMatched ? "MATCHED" : "MISMATCHED"}</dd>
                  </div>
                  <div>
                    <dt>Metrics</dt>
                    <dd>{verification.state.snapshot.metricsMatched ? "MATCHED" : "MISMATCHED"}</dd>
                  </div>
                  <div>
                    <dt>Fingerprints</dt>
                    <dd>
                      {verification.state.snapshot.fingerprintsMatched ? "MATCHED" : "MISMATCHED"}
                    </dd>
                  </div>
                </dl>
              )}
              {verification.state.snapshot.status === "MISMATCHED" && (
                <pre className="mono">
                  {JSON.stringify(verification.state.snapshot.differences, null, 2)}
                </pre>
              )}
              {verification.state.snapshot.failure && (
                <p role="alert">
                  {verification.state.snapshot.failure.code}:{" "}
                  {verification.state.snapshot.failure.message}
                </p>
              )}
            </>
          )}
        </article>
      )}
    </section>
  );
}

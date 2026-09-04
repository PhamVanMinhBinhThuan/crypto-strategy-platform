import type { Job } from "../types/experiment";
export function JobProgressList({ jobs }: { jobs: readonly Job[] }) {
  return (
    <section className="panel">
      <div className="section-heading">
        <h2>Job progress</h2>
        <span>
          {jobs.length} public job{jobs.length === 1 ? "" : "s"}
        </span>
      </div>
      {jobs.map((j) => {
        const ratio = Math.min(
          100,
          Math.max(0, ((j.completedWork + j.failedWork) / j.totalWork) * 100)
        );
        return (
          <article className="job-row" key={j.jobId}>
            <div>
              <strong>{j.type}</strong>
              <span className="status">{j.status}</span>
              <small className="mono">{j.jobId}</small>
            </div>
            <div
              className="progress"
              role="progressbar"
              aria-valuenow={j.completedWork + j.failedWork}
              aria-valuemin={0}
              aria-valuemax={j.totalWork}
              aria-label={`${j.completedWork} completed, ${j.failedWork} failed, ${j.totalWork} total`}
            >
              <span style={{ width: `${ratio}%` }} />
            </div>
            <p>
              {j.completedWork} completed · {j.failedWork} failed · {j.totalWork} total{" "}
              {j.bestScore && (
                <>
                  · best <span className="numeric">{j.bestScore}</span>
                </>
              )}
            </p>
            {j.nextRetryAt && (
              <p>
                Retry scheduled <time dateTime={j.nextRetryAt}>{j.nextRetryAt}</time>
              </p>
            )}
            {j.failure && (
              <p role="alert">
                {j.failure.code}: {j.failure.message}
              </p>
            )}
          </article>
        );
      })}
    </section>
  );
}

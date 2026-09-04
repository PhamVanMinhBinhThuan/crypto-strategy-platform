import Link from "next/link";
import type { LeaderboardSnapshot } from "../types/leaderboard";
export function LeaderboardTable({ snapshot }: { snapshot: LeaderboardSnapshot }) {
  return (
    <section className="panel">
      <div className="section-heading">
        <h2>Leaderboard</h2>
        <span>
          Revision {snapshot.revision} · {snapshot.rankingPolicyVersion}
        </span>
      </div>
      {snapshot.items.length === 0 ? (
        <p className="empty-copy">
          No strategy candidates evaluated yet. Awaiting evaluation outcomes.
        </p>
      ) : (
        <div
          className="table-scroll"
          tabIndex={0}
          role="region"
          aria-label="Scrollable experiment leaderboard"
        >
          <table>
            <thead>
              <tr>
                <th>Rank</th>
                <th>Evaluation Result ID</th>
                <th>Backtest Result ID</th>
                <th>Score</th>
                <th>Maximum Drawdown</th>
                <th>Evaluation Fingerprint</th>
                <th>
                  <span className="sr-only">Action</span>
                </th>
              </tr>
            </thead>
            <tbody>
              {snapshot.items.map((e) => (
                <tr key={e.evaluationResultId}>
                  <td className="numeric">{e.rank}</td>
                  <td className="mono">{e.evaluationResultId}</td>
                  <td className="mono">{e.backtestResultId}</td>
                  <td className="numeric" title={e.score}>
                    {e.score}
                  </td>
                  <td className="numeric" title={e.maximumDrawdown}>
                    {e.maximumDrawdown}
                  </td>
                  <td className="mono">{e.evaluationFingerprint}</td>
                  <td>
                    <Link
                      className="table-action"
                      href={`/backtests?resultId=${encodeURIComponent(e.backtestResultId)}`}
                    >
                      View Backtest
                    </Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}

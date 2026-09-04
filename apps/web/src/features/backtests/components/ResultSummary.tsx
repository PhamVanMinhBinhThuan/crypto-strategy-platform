import type { BacktestResultViewModel } from "../types/backtest-result";
const Full = ({ value }: { value: string }) => (
  <span className="numeric" title={value} aria-label={value}>
    {value}
  </span>
);
export function ResultSummary({ result }: { result: BacktestResultViewModel }) {
  const metrics = [
    ["Total Return", result.metrics.totalReturn],
    ["Win Rate", result.metrics.winRate],
    ["Maximum Drawdown", result.metrics.maximumDrawdown],
    ["Number of Trades", String(result.metrics.numberOfTrades)]
  ];
  return (
    <>
      <header className="feature-header">
        <div>
          <p className="eyebrow">Immutable evidence</p>
          <h1>Backtest Results</h1>
          <p className="muted">
            Result {result.backtestResultId} · completed{" "}
            <time dateTime={result.completedAt}>{result.completedAt}</time>
          </p>
        </div>
        <span className="status status-success">COMPLETED</span>
      </header>
      <section className="metric-grid" aria-label="Released performance metrics">
        {metrics.map(([label, value]) => (
          <article className="metric-card" key={label}>
            <span>{label}</span>
            <strong>
              <Full value={value} />
            </strong>
          </article>
        ))}
      </section>
      <section className="capital-grid" aria-label="Capital summary">
        {[
          ["Initial capital", result.initialCapital],
          ["Final capital", result.finalCapital],
          ["Total fees", result.totalFees]
        ].map(([label, value]) => (
          <div key={label}>
            <dt>{label}</dt>
            <dd>
              <Full value={value} />
            </dd>
          </div>
        ))}
      </section>
    </>
  );
}

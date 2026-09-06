import { strategyName } from "@/src/features/experiments/components/candidate-presentation";
import type { BacktestResultViewModel } from "../types/backtest-result";
import {
  formatBacktestPercent,
  formatMoney,
  formatUtcDateTime,
  quoteCurrency,
  subtractDecimals,
  valueTone
} from "./backtest-presentation";

export function ResultSummary({ result }: { result: BacktestResultViewModel }) {
  const dataset = result.provenance.dataset;
  const candidate = result.provenance.candidate;
  const strategy = resultStrategyName(result);
  const candidateNumber = candidate ? candidate.generationIndex + 1 : null;
  const currency = quoteCurrency(dataset?.tradingPair);
  const netProfit = subtractDecimals(result.finalCapital, result.initialCapital);
  const metrics = [
    {
      label: "Total return",
      display: formatBacktestPercent(result.metrics.totalReturn, true),
      raw: result.metrics.totalReturn,
      tone: valueTone(result.metrics.totalReturn)
    },
    {
      label: "Win rate",
      display: formatBacktestPercent(result.metrics.winRate),
      raw: result.metrics.winRate,
      tone: ""
    },
    {
      label: "Maximum drawdown",
      display: formatBacktestPercent(result.metrics.maximumDrawdown),
      raw: result.metrics.maximumDrawdown,
      tone: ""
    },
    {
      label: "Trades",
      display: new Intl.NumberFormat("en-US").format(result.metrics.numberOfTrades),
      raw: String(result.metrics.numberOfTrades),
      tone: ""
    }
  ];

  return (
    <section className="backtest-overview" aria-labelledby="backtest-result-heading">
      <header className="feature-header backtest-header">
        <div>
          <p className="eyebrow">Backtest result</p>
          <h1 id="backtest-result-heading">
            {strategy === "Backtest" ? "Backtest result" : `${strategy} backtest`}
          </h1>
          {candidateNumber && (
            <p className="backtest-candidate-label">Candidate #{candidateNumber}</p>
          )}
          <p className="muted backtest-result-context">
            {dataset && (
              <>
                <span>{dataset.tradingPair}</span>
                <span aria-hidden="true">·</span>
                <span>{dataset.timeframe}</span>
                <span aria-hidden="true">·</span>
              </>
            )}
            <span>Completed </span>
            <time dateTime={result.completedAt}>{formatUtcDateTime(result.completedAt)}</time>
          </p>
        </div>
        <span className="status status-success backtest-status">Completed</span>
      </header>

      <section className="backtest-metric-grid" aria-label="Backtest performance">
        {metrics.map((metric) => (
          <article className="backtest-metric-card" key={metric.label}>
            <span>{metric.label}</span>
            <strong className={`backtest-number ${metric.tone}`} title={metric.raw}>
              {metric.display}
            </strong>
          </article>
        ))}
      </section>

      <dl className="backtest-capital-grid" aria-label="Capital summary">
        {[
          ["Initial capital", formatMoney(result.initialCapital, currency), result.initialCapital, ""],
          ["Final capital", formatMoney(result.finalCapital, currency), result.finalCapital, ""],
          ["Net profit", formatMoney(netProfit, currency, true), netProfit, valueTone(netProfit)],
          ["Total fees", formatMoney(result.totalFees, currency), result.totalFees, ""]
        ].map(([label, display, raw, tone]) => (
          <div key={label}>
            <dt>{label}</dt>
            <dd className={`backtest-number ${tone}`} title={raw}>{display}</dd>
          </div>
        ))}
      </dl>
    </section>
  );
}

function resultStrategyName(result: BacktestResultViewModel) {
  const definition = result.provenance.candidate?.definition;
  if (definition) return strategyName(definition);
  const evidence = result.provenance.strategy;
  if (!evidence) return "Backtest";
  const pluginIds = evidence.singleStrategy
    ? [evidence.singleStrategy.pluginId]
    : evidence.components.map((component) => component.strategy.pluginId);
  const names = pluginIds.map((pluginId) => pluginId
    .replaceAll("-", " ")
    .replaceAll("_", " ")
    .replace(/\b\w/g, (letter) => letter.toUpperCase()));
  return names.join(" + ") || "Backtest";
}

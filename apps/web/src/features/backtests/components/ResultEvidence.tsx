import { TechnicalDetails } from "@/src/features/experiments/components/TechnicalDetails";
import {
  candidateComponents,
  parameterSummary,
  strategyName
} from "@/src/features/experiments/components/candidate-presentation";
import type { BacktestResultViewModel } from "../types/backtest-result";
import {
  formatBacktestPercent,
  formatCount,
  formatUtcDateTime,
  humanizeBacktestValue
} from "./backtest-presentation";

export function ResultEvidence({ result }: { result: BacktestResultViewModel }) {
  const { provenance } = result;
  const definition = provenance.candidate?.definition;
  const componentCount = definition ? candidateComponents(definition).length : 0;
  const strategyTitle = definition ? strategyName(definition) : strategyFallback(result);
  const parameters = definition ? parameterSummary(definition) : "";
  const combinationPolicy =
    definition && componentCount > 1
      ? combinationPolicyLabel(definition, provenance.strategy?.compositePolicyId)
      : null;
  const dataset = provenance.dataset;

  return (
    <section className="backtest-setup" aria-labelledby="backtest-setup-heading">
      <div className="section-heading">
        <div>
          <p className="eyebrow">Configuration</p>
          <h2 id="backtest-setup-heading">Backtest setup</h2>
        </div>
        <span>Frozen inputs used to produce this result.</span>
      </div>

      <div className="backtest-setup-grid">
        <article className="panel backtest-setup-card">
          <h3>Strategy configuration</h3>
          <p className="backtest-setup-primary">{strategyTitle}</p>
          {parameters ? (
            <p className="muted">{parameters}</p>
          ) : (
            <p className="muted">Detailed strategy parameters are unavailable.</p>
          )}
          {combinationPolicy && (
            <dl className="backtest-fact-list">
              <div>
                <dt>Combination policy</dt>
                <dd>{combinationPolicy}</dd>
              </div>
            </dl>
          )}
        </article>

        <article className="panel backtest-setup-card">
          <h3>Frozen dataset</h3>
          {dataset ? (
            <dl className="backtest-fact-list">
              <div>
                <dt>Market</dt>
                <dd>
                  {dataset.provider} · {dataset.tradingPair} · {dataset.timeframe}
                </dd>
              </div>
              <div>
                <dt>UTC range</dt>
                <dd>
                  {formatUtcDateTime(dataset.rangeStart)} – {formatUtcDateTime(dataset.rangeEnd)}
                </dd>
              </div>
              <div>
                <dt>Candles</dt>
                <dd className="backtest-number">{formatCount(dataset.candleCount)}</dd>
              </div>
            </dl>
          ) : (
            <p className="muted">Detailed frozen dataset provenance is unavailable.</p>
          )}
        </article>

        <article className="panel backtest-setup-card">
          <h3>Execution assumptions</h3>
          <dl className="backtest-fact-list">
            <div>
              <dt>Transaction fee</dt>
              <dd className="backtest-number">
                {formatBacktestPercent(result.assumptions.feeRate)}
              </dd>
            </div>
            <div>
              <dt>Slippage</dt>
              <dd className="backtest-number">
                {formatBacktestPercent(result.assumptions.slippageRate)}
              </dd>
            </div>
            <div>
              <dt>Position mode</dt>
              <dd>{humanizeBacktestValue(result.assumptions.positionMode)}</dd>
            </div>
            <div>
              <dt>Orders execute at</dt>
              <dd>{humanizeBacktestValue(result.assumptions.executionPriceRule)}</dd>
            </div>
            <div>
              <dt>Close position at dataset end</dt>
              <dd>{result.assumptions.forceCloseAtEnd ? "Yes" : "No"}</dd>
            </div>
          </dl>
        </article>
      </div>
    </section>
  );
}

export function ResultTechnicalDetails({ result }: { result: BacktestResultViewModel }) {
  const { provenance } = result;
  const dataset = provenance.dataset;
  const definition = provenance.candidate?.definition;
  const technicalValues: ReadonlyArray<readonly [string, string | null | undefined]> = [
    ["Backtest result ID", result.backtestResultId],
    ["Backtest ID", result.backtestId],
    ["Experiment ID", provenance.experimentId],
    ["Candidate ID", provenance.candidateId],
    ["Job ID", provenance.jobId],
    ["Successful attempt ID", provenance.successfulAttemptId],
    ["Manifest fingerprint", provenance.manifestFingerprint],
    ["Dataset fingerprint", provenance.datasetFingerprint],
    ["Strategy fingerprint", provenance.strategyFingerprint],
    ["Candidate fingerprint", provenance.candidate?.fingerprint],
    ["Result fingerprint", provenance.resultFingerprint],
    ["Dataset ID", dataset?.datasetVersionId],
    ["Dataset checksum", dataset?.checksum],
    ["Dataset version", dataset?.version],
    ["Normalization version", dataset?.normalizationVersion],
    ["Dataset range start", dataset?.rangeStart],
    ["Dataset range end", dataset?.rangeEnd],
    ["Manifest version", provenance.manifestVersion],
    ["Strategy implementations", strategyImplementations(result)],
    ["Software version", provenance.softwareVersion],
    ["Git commit", provenance.gitCommit],
    ["Assumptions version", result.assumptions.assumptionsVersion],
    ["Raw fee rate", result.assumptions.feeRate],
    ["Raw slippage rate", result.assumptions.slippageRate],
    ["Raw position mode", result.assumptions.positionMode],
    ["Raw execution price rule", result.assumptions.executionPriceRule],
    ["Force close at end", String(result.assumptions.forceCloseAtEnd)],
    ["Raw rounding mode", result.assumptions.roundingMode],
    ["Raw total return", result.metrics.totalReturn],
    ["Raw win rate", result.metrics.winRate],
    ["Raw maximum drawdown", result.metrics.maximumDrawdown],
    ["Raw initial capital", result.initialCapital],
    ["Raw final capital", result.finalCapital],
    ["Raw total fees", result.totalFees]
  ];

  return (
    <section
      className="panel backtest-technical"
      aria-label="Technical details and reproducibility"
    >
      <p className="muted backtest-technical-intro">
        Immutable identifiers and inputs can be used to audit or reproduce this result.
      </p>
      <TechnicalDetails
        summaryLabel="Technical details & reproducibility"
        values={technicalValues}
        jsonValues={definition ? ([["Immutable candidate definition", definition]] as const) : []}
      />
    </section>
  );
}

function strategyFallback(result: BacktestResultViewModel) {
  const strategy = result.provenance.strategy;
  if (!strategy) return "Strategy details unavailable";
  if (strategy.singleStrategy) return humanizePluginId(strategy.singleStrategy.pluginId);
  const names = strategy.components.map((component) =>
    humanizePluginId(component.strategy.pluginId)
  );
  return names.join(" + ") || "Strategy details unavailable";
}

function strategyImplementations(result: BacktestResultViewModel) {
  const strategy = result.provenance.strategy;
  if (!strategy) return null;
  const implementations = strategy.singleStrategy
    ? [strategy.singleStrategy]
    : strategy.components.map((component) => component.strategy);
  return (
    implementations
      .map((item) => `${item.pluginId}@${item.implementationVersion} (${item.strategyVersionId})`)
      .join(", ") || null
  );
}

function combinationPolicyLabel(
  definition: Readonly<Record<string, unknown>>,
  fallback?: string | null
) {
  const value = definition.combinationPolicy;
  if (value && typeof value === "object") {
    const policyId = (value as Record<string, unknown>).policyId;
    if (typeof policyId === "string") return humanizePluginId(policyId);
  }
  return fallback ? humanizePluginId(fallback) : "Configured policy";
}

function humanizePluginId(value: string) {
  return value
    .replaceAll("-", " ")
    .replaceAll("_", " ")
    .replace(/\b\w/g, (letter) => letter.toUpperCase());
}

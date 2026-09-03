import type { BacktestResultViewModel } from "../types/backtest-result";
export function ResultEvidence({ result }: { result: BacktestResultViewModel }) {
  const evidence = {
    "Manifest fingerprint": result.provenance.manifestFingerprint,
    "Dataset fingerprint": result.provenance.datasetFingerprint,
    "Strategy fingerprint": result.provenance.strategyFingerprint,
    "Result fingerprint": result.provenance.resultFingerprint
  };
  const assumptions = {
    "Fee rate": result.assumptions.feeRate,
    "Slippage rate": result.assumptions.slippageRate,
    "Position mode": result.assumptions.positionMode,
    "Execution price rule": result.assumptions.executionPriceRule,
    "Rounding mode": result.assumptions.roundingMode
  };
  return (
    <section className="evidence-grid">
      <article className="panel">
        <h2>Provenance</h2>
        <dl>
          {Object.entries(evidence).map(([k, v]) => (
            <div key={k}>
              <dt>{k}</dt>
              <dd className="mono">{v}</dd>
            </div>
          ))}
        </dl>
      </article>
      <article className="panel">
        <h2>Execution assumptions</h2>
        <dl>
          {Object.entries(assumptions).map(([k, v]) => (
            <div key={k}>
              <dt>{k}</dt>
              <dd className="numeric">{v}</dd>
            </div>
          ))}
        </dl>
      </article>
    </section>
  );
}

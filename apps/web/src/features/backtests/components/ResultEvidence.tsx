import type { BacktestResultViewModel } from "../types/backtest-result";
export function ResultEvidence({ result }: { result: BacktestResultViewModel }) {
  const { provenance } = result;
  const evidence = {
    "Experiment ID": provenance.experimentId,
    "Candidate ID": provenance.candidateId,
    "Job ID": provenance.jobId,
    "Successful attempt ID": provenance.successfulAttemptId,
    "Manifest fingerprint": provenance.manifestFingerprint,
    "Dataset fingerprint": provenance.datasetFingerprint,
    "Strategy fingerprint": provenance.strategyFingerprint,
    "Result fingerprint": provenance.resultFingerprint
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
      {provenance.dataset && provenance.strategy && provenance.candidate ? (
        <>
          <article className="panel">
            <h2>Dataset evidence</h2>
            <dl>
              <div>
                <dt>Dataset version</dt>
                <dd className="mono">{provenance.dataset.version}</dd>
              </div>
              <div>
                <dt>Dataset checksum</dt>
                <dd className="mono">{provenance.dataset.checksum}</dd>
              </div>
              <div>
                <dt>Market scope</dt>
                <dd>
                  {provenance.dataset.provider} · {provenance.dataset.tradingPair} ·{" "}
                  {provenance.dataset.timeframe}
                </dd>
              </div>
              <div>
                <dt>Candles</dt>
                <dd className="numeric">{provenance.dataset.candleCount}</dd>
              </div>
            </dl>
          </article>
          <article className="panel">
            <h2>Strategy and candidate evidence</h2>
            <dl>
              <div>
                <dt>Strategy kind</dt>
                <dd>{provenance.strategy.kind}</dd>
              </div>
              {provenance.strategy.singleStrategy && (
                <div>
                  <dt>Strategy implementation</dt>
                  <dd className="mono">
                    {provenance.strategy.singleStrategy.pluginId}@
                    {provenance.strategy.singleStrategy.implementationVersion}
                  </dd>
                </div>
              )}
              <div>
                <dt>Strategy parameters</dt>
                <dd className="mono">
                  {Object.entries(provenance.strategy.parameters)
                    .map(([name, parameter]) => `${name}=${parameter.value}`)
                    .join(", ") || "none"}
                </dd>
              </div>
              <div>
                <dt>Candidate definition</dt>
                <dd className="mono">{JSON.stringify(provenance.candidate.definition)}</dd>
              </div>
              <div>
                <dt>Candidate fingerprint</dt>
                <dd className="mono">{provenance.candidate.fingerprint}</dd>
              </div>
            </dl>
          </article>
          <article className="panel">
            <h2>Reproduction comparison inputs</h2>
            <p className="muted">
              A linked reproduction compares ordered trades, metrics and these immutable
              fingerprints before reporting MATCHED or MISMATCHED.
            </p>
            <dl>
              <div>
                <dt>Manifest version</dt>
                <dd className="mono">{provenance.manifestVersion}</dd>
              </div>
              <div>
                <dt>Software / commit</dt>
                <dd className="mono">
                  {provenance.softwareVersion} / {provenance.gitCommit}
                </dd>
              </div>
            </dl>
          </article>
        </>
      ) : (
        <article className="panel">
          <h2>Detailed provenance</h2>
          <p className="muted">
            Open the canonical Result from a Leaderboard entry to inspect frozen Dataset, Strategy
            and Candidate evidence.
          </p>
        </article>
      )}
    </section>
  );
}

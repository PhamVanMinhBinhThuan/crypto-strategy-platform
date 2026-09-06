import type { StrategyDescriptor, UserStrategy } from "../model/strategy";
export function StrategyDetail({
  descriptor,
  owned,
  systemStrategies = []
}: {
  descriptor?: StrategyDescriptor;
  owned?: UserStrategy;
  systemStrategies?: readonly StrategyDescriptor[];
}) {
  if (!descriptor && !owned)
    return (
      <section className="strategy-detail">
        <p>Select a strategy to view its details.</p>
      </section>
    );
  if (owned)
    return (
      <section className="strategy-detail">
        <p className="eyebrow">Personal strategy</p>
        <h2>{owned.name}</h2>
        <p>{owned.description}</p>
        <dl>
          <dt>Status</dt>
          <dd>{owned.status}</dd>
          <dt>Version</dt>
          <dd>
            {owned.latestVersion.versionNo} · {owned.latestVersion.status}
          </dd>
          <dt>Fingerprint</dt>
          <dd>{owned.latestVersion.fingerprint}</dd>
        </dl>
        {owned.latestVersion.source.type === "COMPOSITE" &&
          (() => {
            const source = owned.latestVersion.source;
            const weighted = source.policyId === "weighted-vote";
            return (
              <section className="composite-policy-summary" aria-label="Composite policy">
                <header>
                  <div>
                    <span className="strategy-section-kicker">Combination policy</span>
                    <h3>{weighted ? "Weighted Vote" : "Majority Vote"}</h3>
                  </div>
                  <span className="policy-badge">v{source.policyVersion}</span>
                </header>
                <p>
                  {weighted
                    ? "Each signal is multiplied by its weight. The signal with the highest total wins; ties return HOLD."
                    : "Each strategy gets one vote. The signal with the most votes wins; ties return HOLD."}
                </p>
                <ul className="composite-component-summary">
                  {source.components.map((component) => {
                    const system = systemStrategies.find(
                      (item) => item.strategyVersionId === component.strategyVersionId
                    );
                    const weight = source.policyParameters[`weight.${component.strategyVersionId}`];
                    return (
                      <li key={component.strategyVersionId}>
                        <span>{system?.displayName ?? component.strategyId}</span>
                        <small>
                          v{component.version}
                          {weighted ? ` · weight ${weight ?? "?"}` : " · 1 vote"}
                        </small>
                      </li>
                    );
                  })}
                </ul>
                <div className="policy-examples" aria-label="Signal combination examples">
                  <span>
                    <code>{weighted ? "BUY 0.7 · SELL 0.3" : "BUY 2 · SELL 1"}</code>
                    <strong>→ BUY</strong>
                  </span>
                  <span>
                    <code>{weighted ? "BUY 0.5 · SELL 0.5" : "BUY 1 · SELL 1"}</code>
                    <strong>→ HOLD</strong>
                  </span>
                </div>
                <small className="illustration-note">
                  Illustrative policy examples, not current market signals.
                </small>
              </section>
            );
          })()}
        {owned.latestVersion.status === "PUBLISHED" && (
          <p role="note">Published versions are immutable. Create a new version to make changes.</p>
        )}
      </section>
    );
  return (
    <section className="strategy-detail">
      <p className="eyebrow">System strategy</p>
      <h2>{descriptor!.displayName}</h2>
      <p>{descriptor!.description}</p>
      <dl>
        <dt>Version</dt>
        <dd>{descriptor!.version}</dd>
        <dt>Lookback</dt>
        <dd>{descriptor!.requiredLookback}</dd>
        <dt>Fingerprint</dt>
        <dd>{descriptor!.descriptorFingerprint}</dd>
      </dl>
      <section className="supported-signals" aria-label="Supported signals">
        <span>Supported signals</span>
        <div>
          {descriptor!.supportedSignals.map((signal) => (
            <strong className={`signal-badge signal-${signal.toLowerCase()}`} key={signal}>
              {signal}
            </strong>
          ))}
        </div>
      </section>
    </section>
  );
}

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
        <p>Chọn một Strategy để xem chi tiết.</p>
      </section>
    );
  if (owned)
    return (
      <section className="strategy-detail">
        <p className="eyebrow">Strategy riêng</p>
        <h2>{owned.name}</h2>
        <p>{owned.description}</p>
        <dl>
          <dt>Trạng thái</dt>
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
              <section className="composite-policy-summary" aria-label="Quy tắc Composite">
                <header>
                  <div>
                    <span className="strategy-section-kicker">Combination policy</span>
                    <h3>{weighted ? "Weighted Vote" : "Majority Vote"}</h3>
                  </div>
                  <span className="policy-badge">v{source.policyVersion}</span>
                </header>
                <p>
                  {weighted
                    ? "Mỗi tín hiệu được nhân với trọng số. Tín hiệu có tổng trọng số cao nhất thắng; nếu bằng nhau thì trả HOLD."
                    : "Mỗi Strategy có một phiếu. Tín hiệu có nhiều phiếu nhất thắng; nếu bằng phiếu nhau thì trả HOLD."}
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
                          {weighted ? ` · trọng số ${weight ?? "?"}` : " · 1 phiếu"}
                        </small>
                      </li>
                    );
                  })}
                </ul>
                <div className="policy-examples" aria-label="Ví dụ kết hợp tín hiệu">
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
                  Ví dụ minh họa quy tắc, không phải tín hiệu thị trường hiện tại.
                </small>
              </section>
            );
          })()}
        {owned.latestVersion.status === "PUBLISHED" && (
          <p role="note">Version đã publish là bất biến. Hãy tạo version mới để thay đổi.</p>
        )}
      </section>
    );
  return (
    <section className="strategy-detail">
      <p className="eyebrow">Strategy hệ thống</p>
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
      <section className="supported-signals" aria-label="Tín hiệu hỗ trợ">
        <span>Tín hiệu hỗ trợ</span>
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

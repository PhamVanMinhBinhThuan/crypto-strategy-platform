import type { StrategyDescriptor, UserStrategy } from "../model/strategy";
export function StrategyDetail({
  descriptor,
  owned
}: {
  descriptor?: StrategyDescriptor;
  owned?: UserStrategy;
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
    </section>
  );
}

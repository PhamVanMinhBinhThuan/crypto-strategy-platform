import Link from "next/link";
import type { UserStrategyVersion } from "../model/strategy";

export function StrategyVersionHistory({
  versions,
  loading
}: {
  versions: readonly UserStrategyVersion[];
  loading: boolean;
}) {
  return (
    <section className="strategy-version-history" aria-label="Lịch sử version">
      <header>
        <div>
          <p className="eyebrow">Version history</p>
          <h2>Lịch sử phiên bản</h2>
        </div>
        <span>{versions.length} version</span>
      </header>
      {loading ? <p role="status">Đang tải lịch sử…</p> : null}
      {!loading && !versions.length ? <p>Chưa có dữ liệu version.</p> : null}
      <ol>
        {versions.map((version, index) => (
          <li key={version.userStrategyVersionId}>
            <span className="version-node" aria-hidden="true" />
            <div className="version-summary">
              <header>
                <strong>Version {version.versionNo}</strong>
                <span className={`version-status is-${version.status.toLowerCase()}`}>
                  {version.status === "PUBLISHED" ? "Đã publish" : "Bản nháp"}
                </span>
                {index === 0 ? <span className="version-latest">Mới nhất</span> : null}
              </header>
              <p>
                {version.source.type === "COMPOSITE"
                  ? `${version.source.components.length} thành phần · ${version.source.policyId}`
                  : `${version.source.strategy.strategyId} · v${version.source.strategy.version}`}
              </p>
              <small>
                Tạo lúc {new Date(version.createdAt).toLocaleString("vi-VN")}
                {version.publishedAt
                  ? ` · Publish lúc ${new Date(version.publishedAt).toLocaleString("vi-VN")}`
                  : ""}
              </small>
              <code title={version.fingerprint}>{version.fingerprint}</code>
              {version.status === "PUBLISHED" ? (
                <Link
                  className="version-backtest-link"
                  href={`/search?userStrategyVersionId=${encodeURIComponent(version.userStrategyVersionId)}`}
                >
                  Backtest version này →
                </Link>
              ) : null}
            </div>
          </li>
        ))}
      </ol>
    </section>
  );
}

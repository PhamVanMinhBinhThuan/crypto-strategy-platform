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
    <section className="strategy-version-history" aria-label="Version history">
      <header>
        <div>
          <p className="eyebrow">Version history</p>
          <h2>Version history</h2>
        </div>
        <span>{versions.length} version</span>
      </header>
      {loading ? <p role="status">Loading history…</p> : null}
      {!loading && !versions.length ? <p>No version history yet.</p> : null}
      <ol>
        {versions.map((version, index) => (
          <li key={version.userStrategyVersionId}>
            <span className="version-node" aria-hidden="true" />
            <div className="version-summary">
              <header>
                <strong>Version {version.versionNo}</strong>
                <span className={`version-status is-${version.status.toLowerCase()}`}>
                  {version.status === "PUBLISHED" ? "Published" : "Draft"}
                </span>
                {index === 0 ? <span className="version-latest">Latest</span> : null}
              </header>
              <p>
                {version.source.type === "COMPOSITE"
                  ? `${version.source.components.length} components · ${version.source.policyId}`
                  : `${version.source.strategy.strategyId} · v${version.source.strategy.version}`}
              </p>
              <small>
                Created {new Date(version.createdAt).toLocaleString("en-US")}
                {version.publishedAt
                  ? ` · Published ${new Date(version.publishedAt).toLocaleString("en-US")}`
                  : ""}
              </small>
              <code title={version.fingerprint}>{version.fingerprint}</code>
              {version.status === "PUBLISHED" ? (
                <Link
                  className="version-backtest-link"
                  href={`/search?userStrategyVersionId=${encodeURIComponent(version.userStrategyVersionId)}`}
                >
                  Backtest this version →
                </Link>
              ) : null}
            </div>
          </li>
        ))}
      </ol>
    </section>
  );
}

import Link from "next/link";

export function StrategyActions({
  canPublish,
  archived,
  pending,
  backtestVersionId,
  onPublish,
  onArchive,
  onNewVersion
}: {
  canPublish: boolean;
  archived: boolean;
  pending: boolean;
  backtestVersionId?: string;
  onPublish: () => void;
  onArchive: () => void;
  onNewVersion: () => void;
}) {
  return (
    <div className="strategy-actions">
      {backtestVersionId ? (
        <Link
          className="strategy-backtest-link"
          href={`/search?userStrategyVersionId=${encodeURIComponent(backtestVersionId)}`}
        >
          Backtest this strategy
        </Link>
      ) : null}
      {!archived && (
        <button disabled={pending} onClick={onNewVersion}>
          Create new version
        </button>
      )}
      {canPublish && (
        <button
          disabled={pending}
          onClick={() =>
            window.confirm("Publish this version? It will become immutable.") && onPublish()
          }
        >
          Publish version
        </button>
      )}
      {!archived && (
        <button
          disabled={pending}
          onClick={() => window.confirm("Archive this strategy?") && onArchive()}
        >
          Archive
        </button>
      )}
    </div>
  );
}

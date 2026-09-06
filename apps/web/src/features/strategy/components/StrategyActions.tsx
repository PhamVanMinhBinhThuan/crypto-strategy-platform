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
          Backtest Strategy này
        </Link>
      ) : null}
      {!archived && (
        <button disabled={pending} onClick={onNewVersion}>
          Tạo version mới
        </button>
      )}
      {canPublish && (
        <button
          disabled={pending}
          onClick={() =>
            window.confirm("Publish version này? Version sẽ trở thành bất biến.") && onPublish()
          }
        >
          Publish version
        </button>
      )}
      {!archived && (
        <button
          disabled={pending}
          onClick={() => window.confirm("Archive Strategy này?") && onArchive()}
        >
          Archive
        </button>
      )}
    </div>
  );
}

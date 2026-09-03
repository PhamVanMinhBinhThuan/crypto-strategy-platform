export function StrategyActions({
  canPublish,
  archived,
  pending,
  onPublish,
  onArchive,
  onNewVersion
}: {
  canPublish: boolean;
  archived: boolean;
  pending: boolean;
  onPublish: () => void;
  onArchive: () => void;
  onNewVersion: () => void;
}) {
  return (
    <div className="strategy-actions">
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

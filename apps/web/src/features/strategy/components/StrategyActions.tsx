export function StrategyActions({
  canPublish,
  archived,
  pending,
  onPublish,
  onArchive
}: {
  canPublish: boolean;
  archived: boolean;
  pending: boolean;
  onPublish: () => void;
  onArchive: () => void;
}) {
  return (
    <div className="strategy-actions">
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

export function EmptyState({ title = "Nothing here yet" }: { title?: string }) {
  return (
    <div className="placeholder">
      <h2>{title}</h2>
    </div>
  );
}

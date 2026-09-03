export function StatusPanel({
  title,
  message,
  kind = "info"
}: {
  title: string;
  message: string;
  kind?: "info" | "error" | "success";
}) {
  return (
    <div className={`status status-${kind}`} role={kind === "error" ? "alert" : "status"}>
      <strong>{title}</strong>
      <span>{message}</span>
    </div>
  );
}

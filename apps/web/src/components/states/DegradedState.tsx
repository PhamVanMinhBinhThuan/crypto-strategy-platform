export function DegradedState({ message }: { message: string }) {
  return (
    <div className="status">
      <strong>Limited availability</strong>
      <span>{message}</span>
    </div>
  );
}

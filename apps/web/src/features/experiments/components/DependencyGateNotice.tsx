export function DependencyGateNotice() {
  return (
    <aside className="gate-notice" role="status">
      <strong>Deterministic fixture mode</strong>
      <p>
        Start and Reproduce return finite predefined responses here. Production mode uses the
        released Search Coordinator APIs.
      </p>
    </aside>
  );
}

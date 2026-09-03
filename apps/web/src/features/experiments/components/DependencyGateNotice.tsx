export function DependencyGateNotice() {
  return (
    <aside className="gate-notice" role="status">
      <strong>Production Search Coordinator unavailable</strong>
      <p>
        Start and Reproduce can be demonstrated with finite fixtures. Production requests currently
        preserve the <code>BLOCKED_SEARCH_COORDINATOR</code> dependency response.
      </p>
    </aside>
  );
}

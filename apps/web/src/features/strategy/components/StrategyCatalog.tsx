import type { StrategyDescriptor, UserStrategySummary } from "../model/strategy";
export function StrategyCatalog({
  system,
  owned,
  loadingSystem,
  loadingOwned,
  systemError,
  ownedError,
  selectedSystemId,
  selectedOwnedId,
  onSelectSystem,
  onSelectOwned
}: {
  system: readonly StrategyDescriptor[];
  owned: readonly UserStrategySummary[];
  loadingSystem: boolean;
  loadingOwned: boolean;
  systemError?: string;
  ownedError?: string;
  selectedSystemId?: string;
  selectedOwnedId?: string;
  onSelectSystem: (v: StrategyDescriptor) => void;
  onSelectOwned: (id: string) => void;
}) {
  return (
    <aside className="strategy-library" aria-label="Strategy library">
      <section>
        <header className="strategy-library-heading">
          <div>
            <span className="strategy-section-kicker">Built-in</span>
            <h2>System strategies</h2>
          </div>
          <span>{system.length}</span>
        </header>
        {loadingSystem && <p role="status">Loading catalog…</p>}
        {systemError && <p role="alert">{systemError}</p>}
        {!loadingSystem && !systemError && !system.length && <p>No system strategies available.</p>}
        {system.map((item) => (
          <button
            className={selectedSystemId === item.strategyVersionId ? "is-selected" : undefined}
            aria-pressed={selectedSystemId === item.strategyVersionId}
            key={item.strategyVersionId}
            onClick={() => onSelectSystem(item)}
          >
            <strong>{item.displayName}</strong>
            <span>
              {item.category} · v{item.version}
            </span>
          </button>
        ))}
      </section>
      <section>
        <header className="strategy-library-heading">
          <div>
            <span className="strategy-section-kicker">Reusable</span>
            <h2>My library</h2>
          </div>
          <span>{owned.length}</span>
        </header>
        {loadingOwned && <p role="status">Loading library…</p>}
        {ownedError && <p role="alert">{ownedError}</p>}
        {!loadingOwned && !ownedError && !owned.length && <p>No personal strategies yet.</p>}
        {owned.map((item) => (
          <button
            className={selectedOwnedId === item.userStrategyId ? "is-selected" : undefined}
            aria-pressed={selectedOwnedId === item.userStrategyId}
            key={item.userStrategyId}
            onClick={() => onSelectOwned(item.userStrategyId)}
          >
            <strong>{item.name}</strong>
            <span>{item.kind}</span>
          </button>
        ))}
      </section>
    </aside>
  );
}

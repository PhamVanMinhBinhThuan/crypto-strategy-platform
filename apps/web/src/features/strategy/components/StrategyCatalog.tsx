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
    <aside className="strategy-library" aria-label="Thư viện Strategy">
      <section>
        <h2>Strategy hệ thống</h2>
        {loadingSystem && <p role="status">Đang tải catalog…</p>}
        {systemError && <p role="alert">{systemError}</p>}
        {!loadingSystem && !systemError && !system.length && <p>Chưa có Strategy hệ thống.</p>}
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
        <h2>Thư viện của tôi</h2>
        {loadingOwned && <p role="status">Đang tải thư viện…</p>}
        {ownedError && <p role="alert">{ownedError}</p>}
        {!loadingOwned && !ownedError && !owned.length && <p>Chưa có Strategy riêng.</p>}
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

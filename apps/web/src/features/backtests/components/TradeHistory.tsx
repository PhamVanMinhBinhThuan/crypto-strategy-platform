import type { TradeViewModel } from "../types/backtest-result";
const columns: [keyof TradeViewModel, string][] = [
  ["sequence", "#"],
  ["side", "Side"],
  ["entryTime", "Entry time"],
  ["entryPrice", "Entry price"],
  ["exitTime", "Exit time"],
  ["exitPrice", "Exit price"],
  ["quantity", "Quantity"],
  ["entryFee", "Entry fee"],
  ["exitFee", "Exit fee"],
  ["totalFee", "Total fee"],
  ["profitLoss", "Realized P/L"],
  ["postTradeCash", "Post-trade cash"],
  ["exitReason", "Exit reason"]
];
export function TradeHistory({ trades }: { trades: readonly TradeViewModel[] }) {
  return (
    <section className="panel">
      <h2>Trade History</h2>
      {trades.length === 0 ? (
        <p className="empty-copy">
          No trades were recorded because the strategy generated no signals.
        </p>
      ) : (
        <div
          className="table-scroll"
          tabIndex={0}
          role="region"
          aria-label="Scrollable trade history"
        >
          <table>
            <thead>
              <tr>
                {columns.map(([, label]) => (
                  <th key={label} scope="col">
                    {label}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {trades.map((t) => (
                <tr key={t.tradeId}>
                  {columns.map(([key]) => (
                    <td key={key} className="numeric" title={String(t[key])}>
                      {String(t[key])}
                    </td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}

import React from 'react';
import { Trade } from '../../types/backtest';
import { Download } from 'lucide-react';
import { cn } from '../../utils/cn';

export interface TradeTableProps {
  trades: Trade[];
  selectedTradeId: string | null;
  onSelectTrade: (tradeId: string) => void;
}

export const TradeTable: React.FC<TradeTableProps> = ({
  trades,
  selectedTradeId,
  onSelectTrade,
}) => {
  const handleExportCSV = (e: React.MouseEvent) => {
    e.stopPropagation();
    const headers = ['#', 'Entry Time', 'Entry Price', 'Exit Time', 'Exit Price', 'Side', 'Return %', 'Result'];
    const rows = trades.map((t) => [
      t.tradeNumber,
      t.entryTime,
      t.entryPrice,
      t.exitTime,
      t.exitPrice,
      t.side,
      `${t.returnPct}%`,
      t.result,
    ]);
    const csvContent = 'data:text/csv;charset=utf-8,' + [headers, ...rows].map((e) => e.join(',')).join('\n');
    const encodedUri = encodeURI(csvContent);
    const link = document.createElement('a');
    link.setAttribute('href', encodedUri);
    link.setAttribute('download', `trade_history_${Date.now()}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  return (
    <div className="bg-[#191c1f] border border-[#323538] rounded flex-1 flex flex-col overflow-hidden select-none h-full min-h-[380px] lg:min-h-0 min-w-0">
      {/* Table Header */}
      <div className="p-3 border-b border-[#323538] bg-[#0b0e11] font-sans text-xs font-semibold text-[#e1e2e7] flex justify-between items-center shrink-0">
        <span className="uppercase tracking-wider">Recent Trades</span>
        <button
          type="button"
          onClick={handleExportCSV}
          className="text-[#bbcabd] hover:text-[#e1e2e7] hover:bg-[#2B3139] p-1 rounded transition-colors cursor-pointer"
          title="Export Trades CSV"
        >
          <Download className="w-3.5 h-3.5" />
        </button>
      </div>

      {/* Scrollable Table Content */}
      <div className="overflow-y-auto flex-1">
        <table className="w-full text-left border-collapse font-mono text-xs">
          <thead className="sticky top-0 z-10">
            <tr className="text-[#bbcabd] font-sans text-[10px] font-bold uppercase tracking-wider border-b border-[#323538] bg-[#323538]">
              <th className="py-2 px-3 font-medium">#</th>
              <th className="py-2 px-3 font-medium">Entry Time</th>
              <th className="py-2 px-3 font-medium text-right">Entry Px</th>
              <th className="py-2 px-3 font-medium text-center">Side</th>
              <th className="py-2 px-3 font-medium text-right">Return</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-[#323538]/60">
            {trades.map((trade) => {
              const isSelected = selectedTradeId === trade.id;
              const isWin = trade.result === 'WIN';

              return (
                <tr
                  key={trade.id}
                  onClick={() => onSelectTrade(trade.id)}
                  className={cn(
                    'transition-colors cursor-pointer hover:bg-[#1d2023]',
                    isSelected
                      ? 'bg-[#272a2e] border-l-2 border-l-[#44e092]'
                      : 'border-l-2 border-l-transparent'
                  )}
                >
                  <td className="py-2.5 px-3 text-[#bbcabd] font-medium">
                    {trade.tradeNumber}
                  </td>
                  <td className="py-2.5 px-3 text-[#bbcabd] whitespace-nowrap">
                    {trade.entryTime}
                  </td>
                  <td className="py-2.5 px-3 text-right text-[#e1e2e7] font-medium">
                    {trade.entryPrice.toLocaleString()}
                  </td>
                  <td className="py-2.5 px-3 text-center">
                    <span
                      className={cn(
                        'px-1.5 py-0.5 rounded text-[10px] font-bold uppercase tracking-wider',
                        trade.side === 'LONG'
                          ? 'text-[#02C076] bg-[#02C076]/15 border border-[#02C076]/30'
                          : 'text-[#CF304A] bg-[#CF304A]/15 border border-[#CF304A]/30'
                      )}
                    >
                      {trade.side}
                    </span>
                  </td>
                  <td
                    className={cn(
                      'py-2.5 px-3 text-right font-bold',
                      isWin ? 'text-[#02C076]' : 'text-[#CF304A]'
                    )}
                  >
                    {trade.returnPct > 0 ? `+${trade.returnPct}%` : `${trade.returnPct}%`}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
};

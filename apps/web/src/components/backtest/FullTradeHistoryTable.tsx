import React, { useState, useMemo } from 'react';
import { Trade } from '../../types/backtest';
import { Download, ChevronLeft, ChevronRight, CheckCircle2, XCircle } from 'lucide-react';
import { cn } from '../../utils/cn';

export interface FullTradeHistoryTableProps {
  trades: Trade[];
  selectedTradeId: string | null;
  onSelectTrade: (tradeId: string) => void;
}

type TradeFilter = 'ALL' | 'WINS' | 'LOSSES' | 'LONG' | 'SHORT';

export const FullTradeHistoryTable: React.FC<FullTradeHistoryTableProps> = ({
  trades,
  selectedTradeId,
  onSelectTrade,
}) => {
  const [filter, setFilter] = useState<TradeFilter>('ALL');
  const [currentPage, setCurrentPage] = useState<number>(1);
  const pageSize = 8;

  // Filter trades
  const filteredTrades = useMemo(() => {
    return trades.filter((t) => {
      if (filter === 'WINS') return t.result === 'WIN';
      if (filter === 'LOSSES') return t.result === 'LOSS';
      if (filter === 'LONG') return t.side === 'LONG';
      if (filter === 'SHORT') return t.side === 'SHORT';
      return true;
    });
  }, [trades, filter]);

  // Pagination calculation
  const totalPages = Math.ceil(filteredTrades.length / pageSize) || 1;
  const paginatedTrades = useMemo(() => {
    const start = (currentPage - 1) * pageSize;
    return filteredTrades.slice(start, start + pageSize);
  }, [filteredTrades, currentPage, pageSize]);

  const handleFilterChange = (newFilter: TradeFilter) => {
    setFilter(newFilter);
    setCurrentPage(1);
  };

  const handleExportCSV = (e: React.MouseEvent) => {
    e.stopPropagation();
    const headers = [
      '#',
      'Entry Time',
      'Entry Price',
      'Exit Time',
      'Exit Price',
      'Side',
      'Return %',
      'Result',
    ];
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
    const csvContent =
      'data:text/csv;charset=utf-8,' +
      [headers, ...rows].map((e) => e.join(',')).join('\n');
    const encodedUri = encodeURI(csvContent);
    const link = document.createElement('a');
    link.setAttribute('href', encodedUri);
    link.setAttribute('download', `trade_history_${Date.now()}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  return (
    <div className="bg-[#191c1f] border border-[#323538] rounded flex flex-col overflow-hidden select-none w-full min-w-0">
      {/* Table Top Header Bar */}
      <div className="p-3.5 border-b border-[#323538] bg-[#0b0e11] flex flex-wrap justify-between items-center gap-3 shrink-0">
        <div className="flex items-center gap-3">
          <h2 className="font-sans text-sm font-semibold text-[#e1e2e7] tracking-tight uppercase">
            Trade History
          </h2>
          <span className="bg-[#323538] text-[#bbcabd] font-mono text-[11px] px-2 py-0.5 rounded border border-[#3c4a40]">
            {filteredTrades.length} of {trades.length} Trades
          </span>
        </div>

        {/* Filter Controls & CSV Export */}
        <div className="flex flex-wrap items-center gap-2 text-xs">
          <div className="flex items-center bg-[#191c1f] rounded border border-[#323538] p-0.5">
            <button
              type="button"
              onClick={() => handleFilterChange('ALL')}
              className={cn(
                'px-2.5 py-1 rounded text-[11px] font-sans font-medium transition-colors cursor-pointer',
                filter === 'ALL'
                  ? 'bg-[#323538] text-[#e1e2e7] shadow-xs'
                  : 'text-[#bbcabd] hover:text-[#e1e2e7]'
              )}
            >
              All
            </button>
            <button
              type="button"
              onClick={() => handleFilterChange('WINS')}
              className={cn(
                'px-2.5 py-1 rounded text-[11px] font-sans font-medium transition-colors cursor-pointer',
                filter === 'WINS'
                  ? 'bg-[#02C076]/20 text-[#02C076] font-semibold'
                  : 'text-[#bbcabd] hover:text-[#02C076]'
              )}
            >
              Wins
            </button>
            <button
              type="button"
              onClick={() => handleFilterChange('LOSSES')}
              className={cn(
                'px-2.5 py-1 rounded text-[11px] font-sans font-medium transition-colors cursor-pointer',
                filter === 'LOSSES'
                  ? 'bg-[#CF304A]/20 text-[#ff5353] font-semibold'
                  : 'text-[#bbcabd] hover:text-[#ff5353]'
              )}
            >
              Losses
            </button>
          </div>

          <button
            type="button"
            onClick={handleExportCSV}
            className="text-[#bbcabd] hover:text-[#e1e2e7] hover:bg-[#2B3139] px-2.5 py-1 border border-[#323538] rounded transition-colors text-xs font-sans flex items-center gap-1.5 cursor-pointer"
            title="Export complete trade history to CSV"
          >
            <Download className="w-3.5 h-3.5" />
            <span className="hidden sm:inline">Export CSV</span>
          </button>
        </div>
      </div>

      {/* Full-width Responsive Table */}
      <div className="overflow-x-auto">
        <table className="w-full text-left border-collapse font-mono text-xs">
          <thead>
            <tr className="text-[#bbcabd] font-sans text-[11px] font-bold uppercase tracking-wider border-b border-[#323538] bg-[#272a2e]">
              <th className="py-2.5 px-3.5 font-medium w-12 text-center">#</th>
              <th className="py-2.5 px-3.5 font-medium">Entry Time</th>
              <th className="py-2.5 px-3.5 font-medium text-right">Entry Price</th>
              <th className="py-2.5 px-3.5 font-medium">Exit Time</th>
              <th className="py-2.5 px-3.5 font-medium text-right">Exit Price</th>
              <th className="py-2.5 px-3.5 font-medium text-center">Side</th>
              <th className="py-2.5 px-3.5 font-medium text-right">Return</th>
              <th className="py-2.5 px-3.5 font-medium text-center">Result</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-[#323538]/60">
            {paginatedTrades.length === 0 ? (
              <tr>
                <td colSpan={8} className="py-8 text-center text-[#869488] font-sans text-xs">
                  No trades match the selected filter.
                </td>
              </tr>
            ) : (
              paginatedTrades.map((trade) => {
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
                    <td className="py-2.5 px-3.5 text-[#bbcabd] font-medium text-center">
                      {trade.tradeNumber}
                    </td>
                    <td className="py-2.5 px-3.5 text-[#bbcabd] whitespace-nowrap">
                      {trade.entryTime}
                    </td>
                    <td className="py-2.5 px-3.5 text-right text-[#e1e2e7] font-medium">
                      {trade.entryPrice.toLocaleString()}
                    </td>
                    <td className="py-2.5 px-3.5 text-[#bbcabd] whitespace-nowrap">
                      {trade.exitTime}
                    </td>
                    <td className="py-2.5 px-3.5 text-right text-[#e1e2e7] font-medium">
                      {trade.exitPrice.toLocaleString()}
                    </td>
                    <td className="py-2.5 px-3.5 text-center">
                      <span
                        className={cn(
                          'px-2 py-0.5 rounded text-[10px] font-bold uppercase tracking-wider',
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
                        'py-2.5 px-3.5 text-right font-bold',
                        isWin ? 'text-[#02C076]' : 'text-[#CF304A]'
                      )}
                    >
                      {trade.returnPct > 0 ? `+${trade.returnPct}%` : `${trade.returnPct}%`}
                    </td>
                    <td className="py-2.5 px-3.5 text-center">
                      <span
                        className={cn(
                          'inline-flex items-center gap-1 px-2 py-0.5 rounded text-[10px] font-bold font-mono uppercase tracking-wider',
                          isWin
                            ? 'text-[#02C076] bg-[#02C076]/15 border border-[#02C076]/30'
                            : 'text-[#CF304A] bg-[#CF304A]/15 border border-[#CF304A]/30'
                        )}
                      >
                        {isWin ? (
                          <CheckCircle2 className="w-3 h-3 text-[#02C076]" />
                        ) : (
                          <XCircle className="w-3 h-3 text-[#CF304A]" />
                        )}
                        <span>{trade.result}</span>
                      </span>
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>

      {/* Pagination Footer */}
      {totalPages > 1 && (
        <div className="p-2.5 px-3.5 border-t border-[#323538] bg-[#0b0e11] flex justify-between items-center text-xs">
          <span className="text-[#869488] font-mono text-[11px]">
            Page {currentPage} of {totalPages}
          </span>
          <div className="flex items-center gap-1.5">
            <button
              type="button"
              disabled={currentPage <= 1}
              onClick={() => setCurrentPage((p) => Math.max(1, p - 1))}
              className="p-1 rounded text-[#bbcabd] hover:text-[#e1e2e7] hover:bg-[#2B3139] disabled:opacity-30 disabled:cursor-not-allowed cursor-pointer"
              title="Previous Page"
            >
              <ChevronLeft className="w-4 h-4" />
            </button>
            <button
              type="button"
              disabled={currentPage >= totalPages}
              onClick={() => setCurrentPage((p) => Math.min(totalPages, p + 1))}
              className="p-1 rounded text-[#bbcabd] hover:text-[#e1e2e7] hover:bg-[#2B3139] disabled:opacity-30 disabled:cursor-not-allowed cursor-pointer"
              title="Next Page"
            >
              <ChevronRight className="w-4 h-4" />
            </button>
          </div>
        </div>
      )}
    </div>
  );
};

import React from 'react';
import { Trophy, ArrowDown, ArrowUp, ArrowUpDown } from 'lucide-react';
import {
  LeaderboardEntry,
  LeaderboardSortField,
  SortDirection,
  TopKSelection,
} from '../../types/search';
import { LeaderboardRow } from './LeaderboardRow';
import { cn } from '../../utils/cn';

export interface LeaderboardTableProps {
  entries: LeaderboardEntry[];
  sortField: LeaderboardSortField;
  sortDirection: SortDirection;
  onSort: (field: LeaderboardSortField) => void;
  topK: TopKSelection;
  onSelectTopK: (k: TopKSelection) => void;
  onViewBacktest: (entry: LeaderboardEntry) => void;
  onOpenInComposer: (entry: LeaderboardEntry) => void;
}

export const LeaderboardTable: React.FC<LeaderboardTableProps> = ({
  entries,
  sortField,
  sortDirection,
  onSort,
  topK,
  onSelectTopK,
  onViewBacktest,
  onOpenInComposer,
}) => {
  const renderSortIndicator = (field: LeaderboardSortField) => {
    if (sortField !== field) {
      return <ArrowUpDown className="w-3 h-3 opacity-30 group-hover:opacity-70 transition-opacity" />;
    }
    return sortDirection === 'desc' ? (
      <ArrowDown className="w-3 h-3 text-[#44e092]" />
    ) : (
      <ArrowUp className="w-3 h-3 text-[#44e092]" />
    );
  };

  return (
    <section className="bg-[#191c1f] p-4 rounded border border-[#3c4a40] flex-1 min-h-[340px] flex flex-col select-none">
      {/* Top Header: Title & Top-K Filters */}
      <div className="flex flex-wrap justify-between items-center mb-4 gap-3">
        <h3 className="font-sans text-base sm:text-lg font-bold text-[#e1e2e7] flex items-center gap-2">
          <Trophy className="w-5 h-5 text-[#f6be16]" />
          <span>Top Strategies</span>
        </h3>

        {/* Top-K Selector */}
        <div className="flex items-center gap-1.5 bg-[#0b0e11] p-0.5 rounded border border-[#3c4a40]">
          {([10, 25, 50] as TopKSelection[]).map((k) => {
            const isActive = topK === k;
            return (
              <button
                key={k}
                type="button"
                onClick={() => onSelectTopK(k)}
                className={cn(
                  'px-2.5 py-1 rounded text-xs font-sans font-medium transition-colors cursor-pointer',
                  isActive
                    ? 'bg-[#1d2023] text-[#e1e2e7] border border-[#3c4a40] shadow-sm'
                    : 'text-[#869488] hover:text-[#e1e2e7] hover:bg-[#1d2023]/60'
                )}
              >
                Top {k}
              </button>
            );
          })}
        </div>
      </div>

      {/* Table Canvas with Horizontal Overflow */}
      <div className="overflow-x-auto rounded border border-[#323538] bg-[#0b0e11]">
        <table className="w-full text-left border-collapse min-w-[780px]">
          <thead>
            <tr className="border-b border-[#323538] text-[#869488] font-sans text-[11px] font-semibold uppercase tracking-wider bg-[#0b0e11]">
              <th className="p-2.5 px-3 w-16">Rank</th>
              <th className="p-2.5 px-3">Strategy Candidate</th>
              
              <th
                onClick={() => onSort('score')}
                className="p-2.5 px-3 text-right cursor-pointer hover:text-[#e1e2e7] transition-colors group"
              >
                <div className="flex items-center justify-end gap-1">
                  <span>Score</span>
                  {renderSortIndicator('score')}
                </div>
              </th>

              <th
                onClick={() => onSort('totalReturn')}
                className="p-2.5 px-3 text-right cursor-pointer hover:text-[#e1e2e7] transition-colors group"
              >
                <div className="flex items-center justify-end gap-1">
                  <span>Return</span>
                  {renderSortIndicator('totalReturn')}
                </div>
              </th>

              <th
                onClick={() => onSort('winRate')}
                className="p-2.5 px-3 text-right cursor-pointer hover:text-[#e1e2e7] transition-colors group"
              >
                <div className="flex items-center justify-end gap-1">
                  <span>Win Rate</span>
                  {renderSortIndicator('winRate')}
                </div>
              </th>

              <th
                onClick={() => onSort('maxDrawdown')}
                className="p-2.5 px-3 text-right cursor-pointer hover:text-[#e1e2e7] transition-colors group"
              >
                <div className="flex items-center justify-end gap-1">
                  <span>Max DD</span>
                  {renderSortIndicator('maxDrawdown')}
                </div>
              </th>

              <th
                onClick={() => onSort('sharpeRatio')}
                className="p-2.5 px-3 text-right cursor-pointer hover:text-[#e1e2e7] transition-colors group"
              >
                <div className="flex items-center justify-end gap-1">
                  <span>Sharpe</span>
                  {renderSortIndicator('sharpeRatio')}
                </div>
              </th>

              <th
                onClick={() => onSort('tradesCount')}
                className="p-2.5 px-3 text-right cursor-pointer hover:text-[#e1e2e7] transition-colors group"
              >
                <div className="flex items-center justify-end gap-1">
                  <span>Trades</span>
                  {renderSortIndicator('tradesCount')}
                </div>
              </th>

              <th className="p-2.5 px-3 text-right w-24">Actions</th>
            </tr>
          </thead>

          <tbody className="divide-y divide-[#323538]/60 font-mono text-xs">
            {entries.slice(0, topK).map((entry, index) => (
              <LeaderboardRow
                key={entry.id}
                entry={entry}
                displayRank={index + 1}
                onViewBacktest={onViewBacktest}
                onOpenInComposer={onOpenInComposer}
              />
            ))}

            {entries.length === 0 && (
              <tr>
                <td colSpan={9} className="p-8 text-center text-[#869488] font-sans">
                  No strategy candidates evaluated yet. Start a search to discover alpha!
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </section>
  );
};

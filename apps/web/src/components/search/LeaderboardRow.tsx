import React from 'react';
import { LeaderboardEntry } from '../../types/search';
import { History, ExternalLink } from 'lucide-react';
import { cn } from '../../utils/cn';

export interface LeaderboardRowProps {
  entry: LeaderboardEntry;
  displayRank: number;
  onViewBacktest: (entry: LeaderboardEntry) => void;
  onOpenInComposer: (entry: LeaderboardEntry) => void;
}

export const LeaderboardRow: React.FC<LeaderboardRowProps> = ({
  entry,
  displayRank,
  onViewBacktest,
  onOpenInComposer,
}) => {
  const isTop1 = displayRank === 1;
  const isTop2 = displayRank === 2;
  const isTop3 = displayRank === 3;

  return (
    <tr
      className={cn(
        'border-b border-[#323538] hover:bg-[#1d2023] transition-colors select-none group',
        isTop1
          ? 'bg-[#0b0e11] border-l-2 border-l-[#44e092]'
          : isTop2
          ? 'bg-[#0b0e11] border-l-2 border-l-[#f6be16]'
          : isTop3
          ? 'bg-[#0b0e11] border-l-2 border-l-[#323538]'
          : 'bg-[#0b0e11]/60',
        entry.isNew && 'bg-[#02c076]/15 transition-all duration-1000'
      )}
    >
      {/* Rank */}
      <td className="p-2.5 px-3">
        <span
          className={cn(
            'font-mono text-xs font-bold',
            isTop1
              ? 'text-[#44e092]'
              : isTop2
              ? 'text-[#f6be16]'
              : isTop3
              ? 'text-[#e1e2e7]'
              : 'text-[#869488]'
          )}
        >
          #{displayRank}
        </span>
      </td>

      {/* Strategy Candidate Name & Category Tag */}
      <td className="p-2.5 px-3 min-w-[200px]">
        <div className="font-mono text-xs text-[#e1e2e7] font-medium group-hover:text-white transition-colors">
          {entry.name}
        </div>
        <div className="flex items-center gap-1.5 mt-0.5">
          {entry.identifier && (
            <span className="text-[10px] text-[#869488] font-mono">
              {entry.identifier}
            </span>
          )}
          {entry.categoryTag && (
            <span className="text-[10px] bg-[#272a2e] px-1.5 py-0.2 rounded text-[#bbcabd] font-sans font-medium tracking-tight">
              {entry.categoryTag}
            </span>
          )}
        </div>
      </td>

      {/* Score */}
      <td
        className={cn(
          'p-2.5 px-3 text-right font-mono text-xs font-semibold',
          isTop1
            ? 'text-[#44e092]'
            : isTop2
            ? 'text-[#f6be16]'
            : 'text-[#e1e2e7]'
        )}
      >
        {entry.score.toFixed(1)}
      </td>

      {/* Return */}
      <td className="p-2.5 px-3 text-right font-mono text-xs font-medium text-[#44e092]">
        +{entry.totalReturn.toFixed(1)}%
      </td>

      {/* Win Rate */}
      <td className="p-2.5 px-3 text-right font-mono text-xs text-[#e1e2e7]">
        {entry.winRate.toFixed(1)}%
      </td>

      {/* Max Drawdown */}
      <td className="p-2.5 px-3 text-right font-mono text-xs font-medium text-[#ffb4ab]">
        {entry.maxDrawdown.toFixed(1)}%
      </td>

      {/* Sharpe Ratio */}
      <td className="p-2.5 px-3 text-right font-mono text-xs text-[#e1e2e7]">
        {entry.sharpeRatio.toFixed(2)}
      </td>

      {/* Trades Count */}
      <td className="p-2.5 px-3 text-right font-mono text-xs text-[#869488]">
        {entry.tradesCount.toLocaleString()}
      </td>

      {/* Action Buttons */}
      <td className="p-2.5 px-3 text-right">
        <div className="flex items-center justify-end gap-1.5">
          <button
            type="button"
            onClick={() => onViewBacktest(entry)}
            title="View Backtest"
            className="p-1 rounded text-[#869488] hover:text-[#44e092] hover:bg-[#272a2e] transition-colors cursor-pointer"
          >
            <History className="w-3.5 h-3.5" />
          </button>

          <button
            type="button"
            onClick={() => onOpenInComposer(entry)}
            title="Open in Strategy Composer"
            className="p-1 rounded text-[#869488] hover:text-[#f6be16] hover:bg-[#272a2e] transition-colors cursor-pointer"
          >
            <ExternalLink className="w-3.5 h-3.5" />
          </button>
        </div>
      </td>
    </tr>
  );
};

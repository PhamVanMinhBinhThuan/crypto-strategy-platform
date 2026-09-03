import React from 'react';
import { NewsCoinFilter, NewsTimeRange } from '../../types/news';
import { cn } from '../../utils/cn';

export interface NewsSentimentHeaderProps {
  activeCoin: NewsCoinFilter;
  timeRange: NewsTimeRange;
  onSelectCoin: (coin: NewsCoinFilter) => void;
  onSelectTimeRange: (range: NewsTimeRange) => void;
}

const COINS: { id: NewsCoinFilter; label: string }[] = [
  { id: 'BTC', label: 'BTC' },
  { id: 'ETH', label: 'ETH' },
  { id: 'SOL', label: 'SOL' },
  { id: 'ALL', label: 'All' },
];

const TIME_RANGES: { id: NewsTimeRange; label: string }[] = [
  { id: '1H', label: '1H' },
  { id: '24H', label: '24H' },
  { id: '7D', label: '7D' },
  { id: '30D', label: '30D' },
];

export const NewsSentimentHeader: React.FC<NewsSentimentHeaderProps> = ({
  activeCoin,
  timeRange,
  onSelectCoin,
  onSelectTimeRange,
}) => {
  const getHeaderTitle = () => {
    if (activeCoin === 'ALL') return 'Crypto News Intelligence';
    return `${activeCoin} News Intelligence`;
  };

  return (
    <div className="flex flex-wrap justify-between items-end gap-3 mb-4 px-1 select-none">
      <div>
        <h2 className="font-sans text-xl font-bold text-[#e1e2e7] leading-tight mb-1">
          {getHeaderTitle()}
        </h2>
        <div className="flex items-center gap-2">
          <span className="w-1.5 h-1.5 rounded-full bg-[#02c076] animate-pulse" />
          <span className="font-mono text-xs text-[#869488]">Live feed connected</span>
        </div>
      </div>

      <div className="flex items-center gap-3">
        {/* Coin Selector */}
        <div className="flex items-center p-1 rounded-lg bg-[#191c1f] border border-[#2B3139]">
          {COINS.map((c) => {
            const isActive = activeCoin === c.id;
            return (
              <button
                key={c.id}
                type="button"
                onClick={() => onSelectCoin(c.id)}
                className={cn(
                  'px-3 py-1 text-xs font-mono rounded transition-colors cursor-pointer',
                  isActive
                    ? 'text-[#e1e2e7] bg-[#2B3139] font-medium'
                    : 'text-[#869488] hover:text-[#e1e2e7]'
                )}
              >
                {c.label}
              </button>
            );
          })}
        </div>

        {/* Time Range Selector */}
        <div className="flex items-center p-1 rounded-lg bg-[#191c1f] border border-[#2B3139]">
          {TIME_RANGES.map((t) => {
            const isActive = timeRange === t.id;
            return (
              <button
                key={t.id}
                type="button"
                onClick={() => onSelectTimeRange(t.id)}
                className={cn(
                  'px-3 py-1 text-xs font-mono rounded transition-colors cursor-pointer',
                  isActive
                    ? 'text-[#e1e2e7] bg-[#2B3139] font-medium'
                    : 'text-[#869488] hover:text-[#e1e2e7]'
                )}
              >
                {t.label}
              </button>
            );
          })}
        </div>
      </div>
    </div>
  );
};

import React from 'react';
import { Timeframe } from '../../types';
import { cn } from '../../utils/cn';

export interface TimeframeSelectorProps {
  activeTimeframe: Timeframe;
  onSelectTimeframe: (tf: Timeframe) => void;
  className?: string;
}

const ALL_TIMEFRAMES: Timeframe[] = ['1m', '5m', '15m', '30m', '1h', '2h', '4h', '1d'];

export const TimeframeSelector: React.FC<TimeframeSelectorProps> = ({
  activeTimeframe,
  onSelectTimeframe,
  className,
}) => {
  return (
    <div className={cn('flex items-center gap-1.5 font-mono text-[11px]', className)}>
      {/* Current Active Badge */}
      <span className="text-[#e1e2e7] font-bold bg-[#111417] px-1 py-0.2 rounded-[2px] border border-[#2B3139]/70 shrink-0">
        {activeTimeframe}
      </span>

      {/* Complete compact list of all 8 timeframes */}
      <div className="flex gap-0.5 text-[#bbcabd]">
        {ALL_TIMEFRAMES.map((tf) => {
          const isActive = activeTimeframe === tf;
          return (
            <button
              key={tf}
              type="button"
              onClick={() => onSelectTimeframe(tf)}
              className={cn(
                'px-1 py-0.2 rounded-[2px] transition-colors cursor-pointer select-none text-[11px] leading-tight',
                isActive
                  ? 'text-[#44e092] bg-[#111417] font-bold'
                  : 'hover:text-[#44e092] hover:bg-[#111417]'
              )}
            >
              {tf}
            </button>
          );
        })}
      </div>
    </div>
  );
};

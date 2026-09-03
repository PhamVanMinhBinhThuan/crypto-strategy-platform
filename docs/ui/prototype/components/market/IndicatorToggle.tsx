import React from 'react';
import { IndicatorState, IndicatorType } from '../../types';
import { cn } from '../../utils/cn';

export interface IndicatorToggleProps {
  indicators: IndicatorState;
  onToggleIndicator: (key: IndicatorType) => void;
  className?: string;
}

const INDICATOR_LIST: { key: IndicatorType; label: string }[] = [
  { key: 'MA', label: 'MA' },
  { key: 'BB', label: 'BB' },
  { key: 'RSI', label: 'RSI' },
  { key: 'S/R', label: 'S/R' },
];

export const IndicatorToggle: React.FC<IndicatorToggleProps> = ({
  indicators,
  onToggleIndicator,
  className,
}) => {
  return (
    <div className={cn('flex items-center gap-2.5 font-mono text-[12px] text-[#bbcabd]', className)}>
      {INDICATOR_LIST.map(({ key, label }) => {
        const isChecked = indicators[key];
        return (
          <label
            key={key}
            className="flex items-center gap-1 cursor-pointer hover:text-[#e1e2e7] select-none transition-colors"
          >
            <input
              type="checkbox"
              checked={isChecked}
              onChange={() => onToggleIndicator(key)}
              className="accent-[#44e092] bg-[#111417] border border-[#323538] rounded-[2px] h-3 w-3 cursor-pointer"
            />
            <span className={cn(isChecked ? 'text-[#e1e2e7] font-medium' : 'text-[#869488]')}>
              {label}
            </span>
          </label>
        );
      })}
    </div>
  );
};

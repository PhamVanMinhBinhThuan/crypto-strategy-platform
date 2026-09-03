import React from 'react';
import { Timeframe, IndicatorState, IndicatorType } from '../../types';
import { TimeframeSelector } from './TimeframeSelector';
import { IndicatorToggle } from './IndicatorToggle';

export interface ChartHeaderProps {
  timeframe: Timeframe;
  onSelectTimeframe: (tf: Timeframe) => void;
  indicators: IndicatorState;
  onToggleIndicator: (key: IndicatorType) => void;
}

export const ChartHeader: React.FC<ChartHeaderProps> = ({
  timeframe,
  onSelectTimeframe,
  indicators,
  onToggleIndicator,
}) => {
  return (
    <div className="h-8 border-b border-[#323538] flex items-center px-2 justify-between shrink-0 bg-[#1E2329] absolute top-0 w-full z-10 opacity-70 group-hover:opacity-100 transition-opacity">
      <TimeframeSelector
        activeTimeframe={timeframe}
        onSelectTimeframe={onSelectTimeframe}
      />
      <IndicatorToggle
        indicators={indicators}
        onToggleIndicator={onToggleIndicator}
      />
    </div>
  );
};

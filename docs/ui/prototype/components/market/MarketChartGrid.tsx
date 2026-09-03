import React from 'react';
import { MarketChart } from './MarketChart';

export interface MarketChartGridProps {
  pair?: string;
}

export const MarketChartGrid: React.FC<MarketChartGridProps> = ({
  pair = 'BTC/USDT',
}) => {
  return (
    <div className="flex-1 p-[1px] bg-[#323538] grid grid-cols-2 grid-rows-2 gap-[1px] overflow-hidden">
      {/* Chart 1: 5m (Top Left) */}
      <MarketChart
        id="chart-top-left-5m"
        initialTimeframe="5m"
        pair={pair}
      />

      {/* Chart 2: 15m (Top Right) */}
      <MarketChart
        id="chart-top-right-15m"
        initialTimeframe="15m"
        pair={pair}
      />

      {/* Chart 3: 1h (Bottom Left) */}
      <MarketChart
        id="chart-bottom-left-1h"
        initialTimeframe="1h"
        pair={pair}
      />

      {/* Chart 4: 4h (Bottom Right) */}
      <MarketChart
        id="chart-bottom-right-4h"
        initialTimeframe="4h"
        pair={pair}
      />
    </div>
  );
};

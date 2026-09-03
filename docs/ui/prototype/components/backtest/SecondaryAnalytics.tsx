import React from 'react';
import { BacktestMetrics } from '../../types/backtest';

export interface SecondaryAnalyticsProps {
  metrics: BacktestMetrics;
}

export const SecondaryAnalytics: React.FC<SecondaryAnalyticsProps> = ({ metrics }) => {
  return (
    <div className="flex flex-col justify-between h-full min-h-[240px] md:min-h-[260px] gap-2 select-none min-w-0">
      {/* Win / Loss Ratio */}
      <div className="bg-[#191c1f] border border-[#323538] p-3 rounded flex-1 flex justify-between items-center">
        <span className="font-sans text-xs text-[#bbcabd] font-medium">Win / Loss Ratio</span>
        <div className="font-mono text-sm font-semibold flex items-center gap-2">
          <span className="text-[#44e092]">{metrics.winLossRatio.wins}</span>
          <span className="text-[#3c4a40] text-xs font-normal">/</span>
          <span className="text-[#ff5353]">{metrics.winLossRatio.losses}</span>
        </div>
      </div>

      {/* Avg Win / Loss */}
      <div className="bg-[#191c1f] border border-[#323538] p-3 rounded flex-1 flex justify-between items-center">
        <span className="font-sans text-xs text-[#bbcabd] font-medium">Avg Win / Loss</span>
        <div className="font-mono text-sm font-semibold flex items-center gap-2">
          <span className="text-[#44e092]">{metrics.avgWinLoss.avgWin}</span>
          <span className="text-[#3c4a40] text-xs font-normal">/</span>
          <span className="text-[#ff5353]">{metrics.avgWinLoss.avgLoss}</span>
        </div>
      </div>

      {/* Best / Worst Trade */}
      <div className="bg-[#191c1f] border border-[#323538] p-3 rounded flex-1 flex justify-between items-center">
        <span className="font-sans text-xs text-[#bbcabd] font-medium">Best / Worst Trade</span>
        <div className="font-mono text-sm font-semibold flex items-center gap-2">
          <span className="text-[#44e092]">{metrics.bestWorstTrade.best}</span>
          <span className="text-[#3c4a40] text-xs font-normal">/</span>
          <span className="text-[#ff5353]">{metrics.bestWorstTrade.worst}</span>
        </div>
      </div>
    </div>
  );
};

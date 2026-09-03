import React from 'react';
import { BacktestMetrics } from '../../types/backtest';
import { MetricCard } from './MetricCard';

export interface BacktestMetricsGridProps {
  metrics: BacktestMetrics;
}

export const BacktestMetricsGrid: React.FC<BacktestMetricsGridProps> = ({ metrics }) => {
  return (
    <div className="grid grid-cols-2 sm:grid-cols-3 xl:grid-cols-6 gap-[1px] bg-[#323538] border border-[#323538] rounded overflow-hidden select-none w-full min-w-0">
      <MetricCard
        label="TOTAL RETURN"
        value={metrics.totalReturn}
        valueClass="text-[#44e092] font-bold"
      />
      <MetricCard
        label="WIN RATE"
        value={metrics.winRate}
        valueClass="text-[#e1e2e7]"
      />
      <MetricCard
        label="MAX DRAWDOWN"
        value={metrics.maxDrawdown}
        valueClass="text-[#ff5353] font-bold"
      />
      <MetricCard
        label="NUMBER OF TRADES"
        value={metrics.numberOfTrades}
        valueClass="text-[#e1e2e7]"
      />
      <MetricCard
        label="PROFIT FACTOR"
        value={metrics.profitFactor}
        valueClass="text-[#e1e2e7]"
      />
      <MetricCard
        label="SHARPE RATIO"
        value={metrics.sharpeRatio}
        valueClass="text-[#e1e2e7]"
      />
    </div>
  );
};

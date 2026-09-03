import React from 'react';
import { MarketHeader, MarketFooterStatusBar } from '../components/market/MarketStatus';
import { MarketChartGrid } from '../components/market/MarketChartGrid';

export interface MarketDashboardPageProps {
  activePair?: string;
}

export const MarketDashboardPage: React.FC<MarketDashboardPageProps> = ({
  activePair = 'BTC/USDT',
}) => {
  return (
    <div className="flex-1 flex flex-col h-full bg-[#0b0e11] overflow-hidden">
      {/* Context Header with BTC/USDT, Price, 24h change & connection telemetry */}
      <MarketHeader
        symbol={activePair}
        price="$64,230.15"
        change24h="+1.24% (24h)"
        statusText="Volatile"
        sourceText="Binance"
        connectionText="Synchronized"
      />

      {/* 2x2 Grid Workspace of 4 independent timeframe charts */}
      <MarketChartGrid pair={activePair} />

      {/* Realtime High-Density Footer Status Bar */}
      <MarketFooterStatusBar />
    </div>
  );
};

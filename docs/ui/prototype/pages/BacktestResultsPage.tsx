import React, { useState, useMemo } from 'react';
import { RoutePath } from '../types';
import { BacktestConfig } from '../types/backtest';
import { BacktestHeader } from '../components/backtest/BacktestHeader';
import { BacktestMetricsGrid } from '../components/backtest/BacktestMetricsGrid';
import { BacktestChart } from '../components/backtest/BacktestChart';
import { EquityCurve } from '../components/backtest/EquityCurve';
import { TradeTable } from '../components/backtest/TradeTable';
import { SecondaryAnalytics } from '../components/backtest/SecondaryAnalytics';
import { FullTradeHistoryTable } from '../components/backtest/FullTradeHistoryTable';
import { useApp, DEFAULT_BACKTEST_STRATEGY } from '../context/AppContext';

export interface BacktestResultsPageProps {
  onNavigate?: (route: RoutePath) => void;
}

export const BacktestResultsPage: React.FC<BacktestResultsPageProps> = ({
  onNavigate = (_route: RoutePath) => {},
}) => {
  const {
    selectedBacktestStrategy,
    backtestConfig,
    updateBacktestConfig,
    backtestMetrics,
    backtestTrades,
    backtestCandles,
    backtestEquityCurve,
    recomputeCurrentBacktest,
    handleWorkflowEditStrategyFromBacktest,
  } = useApp();

  const [selectedTradeId, setSelectedTradeId] = useState<string | null>('t-1');
  const [isRunning, setIsRunning] = useState<boolean>(false);

  const strategy = selectedBacktestStrategy ?? DEFAULT_BACKTEST_STRATEGY;
  const strategyDisplayName = strategy.displayName;
  const strategyVersion = strategy.version || 'v1';

  // Validate date range
  const { isValidRange, validationError } = useMemo(() => {
    if (!backtestConfig.startDate || !backtestConfig.endDate) {
      return { isValidRange: false, validationError: 'Select both start and end dates' };
    }
    const start = new Date(backtestConfig.startDate).getTime();
    const end = new Date(backtestConfig.endDate).getTime();
    if (isNaN(start) || isNaN(end)) {
      return { isValidRange: false, validationError: 'Invalid date format' };
    }
    if (start >= end) {
      return {
        isValidRange: false,
        validationError: 'Start Date must be earlier than End Date',
      };
    }
    return { isValidRange: true, validationError: null };
  }, [backtestConfig.startDate, backtestConfig.endDate]);

  const handleUpdateConfig = (updated: Partial<BacktestConfig>) => {
    updateBacktestConfig(updated);
  };

  const handleRunAgain = () => {
    if (!isValidRange) return;
    setIsRunning(true);
    setTimeout(() => {
      recomputeCurrentBacktest();
      if (backtestTrades.length > 0) {
        setSelectedTradeId(backtestTrades[0].id);
      }
      setIsRunning(false);
    }, 600);
  };

  const handleEditStrategy = () => {
    handleWorkflowEditStrategyFromBacktest(onNavigate);
  };

  const handleSelectTrade = (tradeId: string) => {
    setSelectedTradeId((prev) => (prev === tradeId ? null : tradeId));
  };

  return (
    <div className="w-full min-w-0 bg-[#111417] p-4 md:p-5 flex flex-col gap-4 select-none">
      {/* 1. Backtest Header / Configuration */}
      <BacktestHeader
        strategyName={strategyDisplayName}
        version={strategyVersion}
        config={backtestConfig}
        onUpdateConfig={handleUpdateConfig}
        onRunAgain={handleRunAgain}
        onEditStrategy={handleEditStrategy}
        isRunning={isRunning}
        isValidRange={isValidRange}
        validationError={validationError}
      />

      {/* 2. Performance Summary: 6 Reusable Metric Cards */}
      <BacktestMetricsGrid metrics={backtestMetrics} />

      {/* 3. Main Candlestick Chart + Recent Trades */}
      <div className="grid grid-cols-1 lg:grid-cols-[minmax(0,1fr)_340px] xl:grid-cols-[minmax(0,1fr)_380px] gap-4 min-w-0 items-stretch">
        <div className="min-w-0">
          <BacktestChart
            candles={backtestCandles}
            trades={backtestTrades}
            selectedTradeId={selectedTradeId}
            onSelectTrade={handleSelectTrade}
          />
        </div>

        <div className="min-w-0 flex flex-col">
          <TradeTable
            trades={backtestTrades}
            selectedTradeId={selectedTradeId}
            onSelectTrade={handleSelectTrade}
          />
        </div>
      </div>

      {/* 4. Equity Curve + Secondary Analytics */}
      <div className="grid grid-cols-1 lg:grid-cols-[minmax(0,1fr)_340px] xl:grid-cols-[minmax(0,1fr)_380px] gap-4 min-w-0 items-stretch">
        <div className="min-w-0">
          <EquityCurve data={backtestEquityCurve} />
        </div>

        <div className="min-w-0 flex flex-col justify-between">
          <SecondaryAnalytics metrics={backtestMetrics} />
        </div>
      </div>

      {/* 5. Standalone Full Trade History Table */}
      <FullTradeHistoryTable
        trades={backtestTrades}
        selectedTradeId={selectedTradeId}
        onSelectTrade={handleSelectTrade}
      />
    </div>
  );
};

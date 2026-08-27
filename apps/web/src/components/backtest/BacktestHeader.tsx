import React from 'react';
import { RefreshCw, Edit3, AlertCircle } from 'lucide-react';
import { BacktestConfig } from '../../types/backtest';

export interface BacktestHeaderProps {
  strategyName: string;
  version?: string;
  config: BacktestConfig;
  onUpdateConfig: (updated: Partial<BacktestConfig>) => void;
  onRunAgain: () => void;
  onEditStrategy: () => void;
  isRunning?: boolean;
  isValidRange?: boolean;
  validationError?: string | null;
}

export const BacktestHeader: React.FC<BacktestHeaderProps> = ({
  strategyName,
  version = 'v1',
  config,
  onUpdateConfig,
  onRunAgain,
  onEditStrategy,
  isRunning = false,
  isValidRange = true,
  validationError = null,
}) => {
  return (
    <div className="flex flex-col lg:flex-row justify-between items-start lg:items-end bg-[#191c1f] p-4 border border-[#323538] rounded select-none gap-4">
      <div className="flex-1 w-full">
        <h1 className="font-sans text-2xl lg:text-[32px] font-semibold text-[#e1e2e7] tracking-tight mb-2 leading-none">
          Backtest Results
        </h1>

        {/* Configuration Bar */}
        <div className="flex flex-wrap items-center gap-2.5 text-xs">
          {/* Strategy Name Badge */}
          <span className="bg-[#323538] text-[#e1e2e7] px-2.5 py-1 rounded font-mono text-[12px] border border-[#3c4a40] font-medium shadow-xs">
            {strategyName} ({version})
          </span>

          {/* Market Pair Dropdown */}
          <div className="flex items-center bg-[#0b0e11] rounded border border-[#3c4a40] px-2 py-0.5 hover:border-[#869488] transition-colors">
            <select
              value={config.symbol}
              onChange={(e) => onUpdateConfig({ symbol: e.target.value })}
              className="bg-transparent text-[#e1e2e7] font-mono text-[12px] font-medium outline-none cursor-pointer pr-1"
              aria-label="Market Pair"
            >
              <option value="BTC/USDT" className="bg-[#191c1f] text-[#e1e2e7]">
                BTC/USDT
              </option>
              <option value="ETH/USDT" className="bg-[#191c1f] text-[#e1e2e7]">
                ETH/USDT
              </option>
              <option value="SOL/USDT" className="bg-[#191c1f] text-[#e1e2e7]">
                SOL/USDT
              </option>
            </select>
          </div>

          <span className="text-[#3c4a40] px-0.5">|</span>

          {/* Timeframe Dropdown */}
          <div className="flex items-center bg-[#0b0e11] rounded border border-[#3c4a40] px-2 py-0.5 hover:border-[#869488] transition-colors">
            <select
              value={config.timeframe}
              onChange={(e) => onUpdateConfig({ timeframe: e.target.value })}
              className="bg-transparent text-[#e1e2e7] font-mono text-[12px] font-medium outline-none cursor-pointer pr-1"
              aria-label="Timeframe"
            >
              <option value="1m" className="bg-[#191c1f] text-[#e1e2e7]">1m</option>
              <option value="5m" className="bg-[#191c1f] text-[#e1e2e7]">5m</option>
              <option value="15m" className="bg-[#191c1f] text-[#e1e2e7]">15m</option>
              <option value="30m" className="bg-[#191c1f] text-[#e1e2e7]">30m</option>
              <option value="1h" className="bg-[#191c1f] text-[#e1e2e7]">1h</option>
              <option value="2h" className="bg-[#191c1f] text-[#e1e2e7]">2h</option>
              <option value="4h" className="bg-[#191c1f] text-[#e1e2e7]">4h</option>
              <option value="1d" className="bg-[#191c1f] text-[#e1e2e7]">1d</option>
            </select>
          </div>

          <span className="text-[#3c4a40] px-0.5">|</span>

          {/* Editable Date Range */}
          <div
            className={`flex items-center gap-1.5 bg-[#0b0e11] rounded border px-2 py-0.5 transition-colors ${
              isValidRange
                ? 'border-[#3c4a40] hover:border-[#869488]'
                : 'border-[#ff5353]/70 bg-[#ff5353]/5'
            }`}
          >
            <input
              type="date"
              value={config.startDate}
              onChange={(e) => onUpdateConfig({ startDate: e.target.value })}
              className="bg-transparent text-[#e1e2e7] font-mono text-[11px] outline-none cursor-pointer [color-scheme:dark]"
              title="Start Date"
              aria-label="Start Date"
            />
            <span className="text-[#869488] font-mono text-xs">—</span>
            <input
              type="date"
              value={config.endDate}
              onChange={(e) => onUpdateConfig({ endDate: e.target.value })}
              className="bg-transparent text-[#e1e2e7] font-mono text-[11px] outline-none cursor-pointer [color-scheme:dark]"
              title="End Date"
              aria-label="End Date"
            />
          </div>

          {/* Inline Validation Alert */}
          {!isValidRange && validationError && (
            <span className="text-[#ff5353] font-mono text-[11px] flex items-center gap-1 bg-[#ff5353]/10 border border-[#ff5353]/30 px-2 py-0.5 rounded">
              <AlertCircle className="w-3.5 h-3.5 text-[#ff5353] shrink-0" />
              <span>{validationError}</span>
            </span>
          )}
        </div>
      </div>

      {/* Header Action Buttons */}
      <div className="flex gap-2 shrink-0 self-end lg:self-auto">
        <button
          type="button"
          onClick={onRunAgain}
          disabled={isRunning || !isValidRange}
          className="px-4 py-2 text-xs md:text-sm border border-[#323538] text-[#e1e2e7] hover:bg-[#323538] transition-all rounded font-sans font-medium flex items-center gap-1.5 cursor-pointer active:scale-[0.98] disabled:opacity-40 disabled:cursor-not-allowed"
          title={!isValidRange ? 'Invalid date range' : 'Run backtest again'}
        >
          <RefreshCw
            className={`w-3.5 h-3.5 ${isRunning ? 'animate-spin text-[#44e092]' : 'text-[#bbcabd]'}`}
          />
          <span>{isRunning ? 'Running...' : 'Run Again'}</span>
        </button>

        <button
          type="button"
          onClick={onEditStrategy}
          className="px-4 py-2 text-xs md:text-sm border border-[#02c076] text-[#02c076] hover:bg-[#02c076] hover:text-[#00391f] transition-all rounded font-sans font-semibold flex items-center gap-1.5 cursor-pointer active:scale-[0.98]"
        >
          <Edit3 className="w-3.5 h-3.5" />
          <span>Edit Strategy</span>
        </button>
      </div>
    </div>
  );
};

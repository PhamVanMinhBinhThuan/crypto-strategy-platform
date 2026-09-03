import React from 'react';
import { SlidersHorizontal } from 'lucide-react';
import {
  SearchConfigurationState,
  SearchAlgorithm,
  StopConditions,
} from '../../types/search';
import { SearchControls } from './SearchControls';
import { SearchSpaceSelector } from './SearchSpaceSelector';
import { StopConditionEditor } from './StopConditionEditor';

export interface SearchConfigurationProps {
  config: SearchConfigurationState;
  searchState: 'idle' | 'running' | 'paused' | 'completed' | 'stopped';
  onUpdateMarket: (market: string) => void;
  onUpdateDatasetRange: (range: string) => void;
  onUpdateAlgorithm: (algorithm: SearchAlgorithm) => void;
  onToggleFeature: (featureId: string) => void;
  onUpdateStopConditions: (updated: Partial<StopConditions>) => void;
  onStartSearch: () => void;
  onPauseSearch: () => void;
  onResumeSearch: () => void;
  onStopSearch: () => void;
}

export const SearchConfiguration: React.FC<SearchConfigurationProps> = ({
  config,
  searchState,
  onUpdateMarket,
  onUpdateDatasetRange,
  onUpdateAlgorithm,
  onToggleFeature,
  onUpdateStopConditions,
  onStartSearch,
  onPauseSearch,
  onResumeSearch,
  onStopSearch,
}) => {
  const isRunning = searchState === 'running';

  return (
    <section className="bg-[#191c1f] p-4 rounded border border-[#3c4a40] flex-shrink-0 select-none">
      {/* Header with Title & Action Controls */}
      <div className="flex flex-wrap justify-between items-center mb-4 pb-3 border-b border-[#323538] gap-3">
        <h2 className="font-sans text-base sm:text-lg font-bold text-[#e1e2e7] flex items-center gap-2">
          <SlidersHorizontal className="w-4 h-4 text-[#44e092]" />
          <span>Search Configuration</span>
        </h2>

        <SearchControls
          searchState={searchState}
          onStart={onStartSearch}
          onPause={onPauseSearch}
          onResume={onResumeSearch}
          onStop={onStopSearch}
        />
      </div>

      {/* Main 4-Column Responsive Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 sm:gap-6">
        {/* Column 1: Parameters */}
        <div className="space-y-3.5">
          <div>
            <label className="block font-sans text-[11px] font-bold text-[#bbcabd] mb-1 uppercase tracking-wider">
              Market
            </label>
            <select
              value={config.market}
              disabled={isRunning}
              onChange={(e) => onUpdateMarket(e.target.value)}
              className="w-full bg-[#0b0e11] border border-[#3c4a40] text-[#e1e2e7] rounded p-1.5 px-2 font-mono text-xs focus:border-[#44e092] focus:outline-none focus:ring-0 disabled:opacity-50 cursor-pointer"
            >
              <option value="BTC/USDT">BTC/USDT</option>
              <option value="ETH/USDT">ETH/USDT</option>
              <option value="SOL/USDT">SOL/USDT</option>
            </select>
          </div>

          <div>
            <label className="block font-sans text-[11px] font-bold text-[#bbcabd] mb-1 uppercase tracking-wider">
              Dataset Range
            </label>
            <select
              value={config.datasetRange}
              disabled={isRunning}
              onChange={(e) => onUpdateDatasetRange(e.target.value)}
              className="w-full bg-[#0b0e11] border border-[#3c4a40] text-[#e1e2e7] rounded p-1.5 px-2 font-mono text-xs focus:border-[#44e092] focus:outline-none focus:ring-0 disabled:opacity-50 cursor-pointer"
            >
              <option value="Last 6 Months (1h)">Last 6 Months (1h)</option>
              <option value="Last 1 Year (1h)">Last 1 Year (1h)</option>
              <option value="Last 30 Days (15m)">Last 30 Days (15m)</option>
            </select>
          </div>

          <div>
            <label className="block font-sans text-[11px] font-bold text-[#bbcabd] mb-2 uppercase tracking-wider">
              Search Algorithm
            </label>
            <div className="space-y-2">
              <label className="flex items-center gap-2 cursor-pointer select-none">
                <input
                  type="radio"
                  name="algorithm"
                  value="random"
                  disabled={isRunning}
                  checked={config.algorithm === 'random'}
                  onChange={() => onUpdateAlgorithm('random')}
                  className="w-3.5 h-3.5 text-[#02c076] bg-[#0b0e11] border-[#3c4a40] focus:ring-[#02c076] focus:ring-offset-[#111417] cursor-pointer"
                />
                <span className="text-xs text-[#e1e2e7] font-sans">Random Search</span>
              </label>

              <label className="flex items-center gap-2 cursor-pointer select-none">
                <input
                  type="radio"
                  name="algorithm"
                  value="domain_guided"
                  disabled={isRunning}
                  checked={config.algorithm === 'domain_guided'}
                  onChange={() => onUpdateAlgorithm('domain_guided')}
                  className="w-3.5 h-3.5 text-[#02c076] bg-[#0b0e11] border-[#3c4a40] focus:ring-[#02c076] focus:ring-offset-[#111417] cursor-pointer"
                />
                <span className="text-xs text-[#e1e2e7] font-sans">Domain-Guided Search</span>
              </label>
            </div>
          </div>
        </div>

        {/* Column 2 & 3: Search Space (Features) - Spans 2 columns on lg */}
        <div className="md:col-span-1 lg:col-span-2">
          <SearchSpaceSelector
            features={config.features}
            onToggleFeature={onToggleFeature}
            disabled={isRunning}
          />
        </div>

        {/* Column 4: Stop Conditions */}
        <div className="md:col-span-1 lg:col-span-1">
          <StopConditionEditor
            conditions={config.stopConditions}
            onChange={onUpdateStopConditions}
            disabled={isRunning}
          />
        </div>
      </div>
    </section>
  );
};

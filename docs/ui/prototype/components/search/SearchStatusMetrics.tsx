import React from 'react';
import { SearchLiveMetrics } from '../../types/search';
import { cn } from '../../utils/cn';

export interface SearchStatusMetricsProps {
  metrics: SearchLiveMetrics;
}

export const SearchStatusMetrics: React.FC<SearchStatusMetricsProps> = ({ metrics }) => {
  const formatTime = (totalSeconds: number): string => {
    const mins = Math.floor(totalSeconds / 60);
    const secs = totalSeconds % 60;
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  };

  return (
    <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-5 gap-3 select-none">
      {/* Tested */}
      <div className="bg-[#0b0e11] p-3 rounded border border-[#3c4a40] flex flex-col justify-between">
        <div className="text-[10px] sm:text-[11px] font-sans font-semibold text-[#869488] uppercase tracking-wider mb-1">
          Tested
        </div>
        <div className="font-mono text-lg sm:text-xl font-medium text-[#e1e2e7] tracking-tight">
          {metrics.candidatesTested.toLocaleString()}
        </div>
      </div>

      {/* Remaining */}
      <div className="bg-[#0b0e11] p-3 rounded border border-[#3c4a40] flex flex-col justify-between">
        <div className="text-[10px] sm:text-[11px] font-sans font-semibold text-[#869488] uppercase tracking-wider mb-1">
          Remaining
        </div>
        <div className="font-mono text-lg sm:text-xl font-medium text-[#e1e2e7] tracking-tight">
          {metrics.candidatesRemaining.toLocaleString()}
        </div>
      </div>

      {/* Elapsed Time */}
      <div className="bg-[#0b0e11] p-3 rounded border border-[#3c4a40] flex flex-col justify-between">
        <div className="text-[10px] sm:text-[11px] font-sans font-semibold text-[#869488] uppercase tracking-wider mb-1">
          Elapsed Time
        </div>
        <div className="font-mono text-lg sm:text-xl font-medium text-[#e1e2e7] tracking-tight">
          {formatTime(metrics.elapsedSeconds)}
        </div>
      </div>

      {/* Best Score (with primary green left border) */}
      <div className="bg-[#0b0e11] p-3 rounded border border-[#3c4a40] border-l-2 border-l-[#44e092] flex flex-col justify-between">
        <div className="text-[10px] sm:text-[11px] font-sans font-semibold text-[#869488] uppercase tracking-wider mb-1">
          Best Score
        </div>
        <div className="font-mono text-lg sm:text-xl font-semibold text-[#44e092] tracking-tight">
          {metrics.bestScore.toFixed(1)}
        </div>
      </div>

      {/* Improvement */}
      <div className="bg-[#0b0e11] p-3 rounded border border-[#3c4a40] flex flex-col justify-between col-span-2 sm:col-span-1">
        <div className="text-[10px] sm:text-[11px] font-sans font-semibold text-[#869488] uppercase tracking-wider mb-1">
          Improvement
        </div>
        <div
          className={cn(
            'font-mono text-lg sm:text-xl font-semibold tracking-tight',
            metrics.scoreImprovement >= 0 ? 'text-[#44e092]' : 'text-[#ffb4ab]'
          )}
        >
          {metrics.scoreImprovement >= 0 ? `+${metrics.scoreImprovement.toFixed(1)}` : metrics.scoreImprovement.toFixed(1)}
        </div>
      </div>
    </div>
  );
};

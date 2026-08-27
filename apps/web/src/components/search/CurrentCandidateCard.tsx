import React from 'react';
import { CandidateEvaluation } from '../../types/search';
import { cn } from '../../utils/cn';

export interface CurrentCandidateCardProps {
  candidate: CandidateEvaluation;
  isSearching: boolean;
  isPaused?: boolean;
}

export const CurrentCandidateCard: React.FC<CurrentCandidateCardProps> = ({
  candidate,
  isSearching,
  isPaused = false,
}) => {
  const getStatusDisplay = () => {
    if (candidate.status === 'stopped') {
      return {
        label: 'Stopped',
        colorClass: 'text-[#869488]',
        dotClass: 'bg-[#869488]',
      };
    }
    if (candidate.status === 'idle') {
      return {
        label: 'Idle',
        colorClass: 'text-[#869488]',
        dotClass: 'bg-[#869488]',
      };
    }
    if (candidate.status === 'completed') {
      return {
        label: 'Completed',
        colorClass: 'text-[#02c076]',
        dotClass: 'bg-[#02c076]',
      };
    }
    if (isPaused || candidate.status === 'paused') {
      return {
        label: 'Paused',
        colorClass: 'text-[#f6be16]',
        dotClass: 'bg-[#f6be16]',
      };
    }
    switch (candidate.status) {
      case 'generating':
        return {
          label: 'Generate',
          colorClass: 'text-[#44e092]',
          dotClass: 'bg-[#44e092]',
        };
      case 'backtesting':
        return {
          label: 'Backtesting',
          colorClass: 'text-[#f6be16]',
          dotClass: 'bg-[#f6be16]',
        };
      case 'evaluating':
        return {
          label: 'Evaluating',
          colorClass: 'text-[#67fdac]',
          dotClass: 'bg-[#67fdac]',
        };
      case 'ranking':
      case 'ranked':
        return {
          label: 'Ranking',
          colorClass: 'text-[#02c076]',
          dotClass: 'bg-[#02c076]',
        };
      default:
        return {
          label: 'Idle',
          colorClass: 'text-[#869488]',
          dotClass: 'bg-[#869488]',
        };
    }
  };

  const status = getStatusDisplay();
  const isStopped = candidate.status === 'stopped';

  return (
    <div className="select-none">
      <h3 className="font-sans text-[11px] font-bold text-[#bbcabd] uppercase tracking-wider mb-2">
        Current Candidate
      </h3>

      <div className="bg-[#0b0e11] p-3 rounded border border-[#3c4a40] flex flex-col justify-between">
        <div className="flex justify-between items-center mb-2 gap-2">
          <span className="font-mono text-xs text-[#e1e2e7] font-medium truncate">
            {candidate.name}
          </span>
          <span
            className={cn(
              'text-xs font-sans font-semibold flex items-center gap-1.5 shrink-0',
              status.colorClass
            )}
          >
            <span
              className={cn(
                'w-1.5 h-1.5 rounded-full inline-block',
                status.dotClass,
                isSearching && !isPaused && !isStopped && 'animate-pulse'
              )}
            />
            {status.label}
          </span>
        </div>

        {/* Progress Bar */}
        <div className="w-full bg-[#1d2023] h-1.5 rounded-full overflow-hidden">
          <div
            className={cn(
              'h-full transition-all duration-300 rounded-full',
              isSearching && !isPaused && !isStopped
                ? 'bg-gradient-to-r from-[#f6be16] via-[#ffd966] to-[#f6be16] progress-bar-animated'
                : candidate.status === 'completed'
                ? 'bg-[#02c076]'
                : isStopped
                ? 'bg-[#566458]'
                : isPaused || candidate.status === 'paused'
                ? 'bg-[#f6be16]'
                : candidate.status === 'idle'
                ? 'bg-[#3c4a40]/40'
                : 'bg-[#f6be16]'
            )}
            style={{
              width: `${
                candidate.status === 'completed'
                  ? 100
                  : candidate.status === 'idle'
                  ? 0
                  : Math.max(15, candidate.progress)
              }%`,
            }}
          />
        </div>
      </div>
    </div>
  );
};

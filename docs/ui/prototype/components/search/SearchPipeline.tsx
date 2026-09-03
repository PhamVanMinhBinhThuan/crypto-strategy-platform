import React from 'react';
import { Cpu, FlaskConical, LineChart, ListOrdered, Trophy } from 'lucide-react';
import { SearchStageId, SearchStageStatus } from '../../types/search';
import { cn } from '../../utils/cn';

export interface SearchPipelineProps {
  activeStage: SearchStageId | null;
  stageStatuses: Record<SearchStageId, SearchStageStatus>;
  isSearching: boolean;
}

const STAGES: Array<{
  id: SearchStageId;
  label: string;
  icon: React.ComponentType<{ className?: string }>;
}> = [
  { id: 'generate', label: 'Generate', icon: Cpu },
  { id: 'backtest', label: 'Backtest', icon: FlaskConical },
  { id: 'evaluate', label: 'Evaluate', icon: LineChart },
  { id: 'rank', label: 'Rank', icon: ListOrdered },
  { id: 'leaderboard', label: 'Leaderboard', icon: Trophy },
];

export const SearchPipeline: React.FC<SearchPipelineProps> = ({
  activeStage,
  stageStatuses,
  isSearching,
}) => {
  return (
    <div className="flex flex-col select-none">
      <h3 className="font-sans text-[11px] font-bold text-[#bbcabd] uppercase tracking-wider mb-3">
        Pipeline Status
      </h3>

      <div className="flex items-center justify-between px-3 sm:px-8 md:px-10 py-4 bg-[#0b0e11] rounded border border-[#3c4a40] relative overflow-hidden">
        {/* Connecting Progress Line */}
        <div className="absolute top-1/2 left-8 right-8 h-[2px] bg-[#3c4a40] -translate-y-1/2 z-0" />

        {STAGES.map((stage) => {
          const Icon = stage.icon;
          const status = stageStatuses[stage.id] || 'idle';
          const isActive = status === 'active';
          const isCompleted = status === 'completed';
          const isPaused = status === 'paused';
          const isStopped = status === 'stopped';

          return (
            <div key={stage.id} className="relative z-10 flex flex-col items-center gap-1.5 sm:gap-2">
              <div
                className={cn(
                  'w-8 h-8 sm:w-9 sm:h-9 md:w-10 md:h-10 rounded-full flex items-center justify-center relative transition-all duration-300',
                  isActive
                    ? 'bg-[#1d2023] border-2 border-[#44e092] text-[#44e092] shadow-[0_0_12px_rgba(68,224,146,0.3)]'
                    : isCompleted
                    ? 'bg-[#1d2023] border-2 border-[#02c076] text-[#02c076]'
                    : isPaused
                    ? 'bg-[#1d2023] border-2 border-[#f6be16] text-[#f6be16] shadow-[0_0_8px_rgba(246,190,22,0.25)]'
                    : isStopped
                    ? 'bg-[#1d2023] border-2 border-[#869488] text-[#869488]'
                    : 'bg-[#1d2023] border-2 border-[#3c4a40] text-[#869488]'
                )}
              >
                {/* Blinking Live Indicator Dot when active only */}
                {isActive && (
                  <span className="w-2.5 h-2.5 rounded-full bg-[#44e092] absolute -top-1 -right-1 animate-ping" />
                )}
                {isActive && (
                  <span className="w-2 h-2 rounded-full bg-[#44e092] absolute -top-0.5 -right-0.5" />
                )}
                {/* Static indicator for Paused */}
                {isPaused && (
                  <span className="w-2 h-2 rounded-full bg-[#f6be16] absolute -top-0.5 -right-0.5" />
                )}

                <Icon className="w-3.5 h-3.5 sm:w-4 sm:h-4 md:w-4.5 md:h-4.5" />
              </div>

              <span
                className={cn(
                  'text-[10px] sm:text-[11px] md:text-xs font-sans tracking-wide transition-colors',
                  isActive
                    ? 'font-bold text-[#44e092]'
                    : isCompleted
                    ? 'font-semibold text-[#e1e2e7]'
                    : isPaused
                    ? 'font-bold text-[#f6be16]'
                    : isStopped
                    ? 'font-medium text-[#869488]'
                    : 'font-medium text-[#869488]'
                )}
              >
                {stage.label}
              </span>
            </div>
          );
        })}
      </div>
    </div>
  );
};

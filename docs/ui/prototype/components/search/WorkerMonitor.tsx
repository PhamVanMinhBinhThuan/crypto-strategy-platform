import React from 'react';
import { WorkerInfo } from '../../types/search';
import { RotateCw, CheckCircle2, AlertCircle, Clock, Pause } from 'lucide-react';
import { cn } from '../../utils/cn';

export interface WorkerMonitorProps {
  workers: WorkerInfo[];
  isSearching: boolean;
  searchState?: 'idle' | 'running' | 'paused' | 'completed' | 'stopped';
}

export const WorkerMonitor: React.FC<WorkerMonitorProps> = ({
  workers,
  isSearching,
  searchState = 'idle',
}) => {
  const isPaused = searchState === 'paused';

  return (
    <div className="flex-1 flex flex-col justify-between select-none">
      <h3 className="font-sans text-[11px] font-bold text-[#bbcabd] uppercase tracking-wider mb-2">
        Worker Monitor
      </h3>

      <ul className="space-y-2">
        {workers.map((worker) => {
          const isCompleted = worker.state === 'completed';
          const isWorkerPaused = isPaused || worker.state === 'paused';
          const isRunning = isSearching && !isWorkerPaused && worker.state === 'running';
          const isEvaluating = isSearching && !isWorkerPaused && worker.state === 'evaluating';

          return (
            <li
              key={worker.id}
              className={cn(
                'flex justify-between items-center bg-[#1d2023] p-2 px-3 rounded text-xs transition-opacity duration-300',
                isCompleted ? 'opacity-50' : 'opacity-100'
              )}
            >
              <span className="font-mono text-xs text-[#e1e2e7] font-medium">
                {worker.name}
              </span>

              {isWorkerPaused ? (
                <span className="text-xs font-sans text-[#f6be16] flex items-center gap-1.5 font-medium">
                  <Pause className="w-3 h-3 text-[#f6be16]" />
                  <span>Paused</span>
                </span>
              ) : isRunning ? (
                <span className="text-xs font-sans text-[#44e092] flex items-center gap-1.5 font-medium">
                  <RotateCw className="w-3 h-3 animate-spin" />
                  <span>Running</span>
                </span>
              ) : isEvaluating ? (
                <span className="text-xs font-sans text-[#67fdac] flex items-center gap-1.5 font-medium">
                  <Clock className="w-3 h-3 animate-pulse" />
                  <span>Evaluating</span>
                </span>
              ) : isCompleted ? (
                <span className="text-xs font-sans text-[#869488] flex items-center gap-1.5">
                  <CheckCircle2 className="w-3 h-3 text-[#869488]" />
                  <span>Completed</span>
                </span>
              ) : worker.state === 'error' ? (
                <span className="text-xs font-sans text-[#ffb4ab] flex items-center gap-1.5">
                  <AlertCircle className="w-3 h-3" />
                  <span>Error</span>
                </span>
              ) : (
                <span className="text-xs font-sans text-[#869488] flex items-center gap-1.5">
                  <span className="w-2 h-2 rounded-full bg-[#3c4a40]" />
                  <span>Idle</span>
                </span>
              )}
            </li>
          );
        })}
      </ul>
    </div>
  );
};

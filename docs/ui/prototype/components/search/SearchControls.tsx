import React from 'react';
import { Square, Pause, Play, RotateCcw } from 'lucide-react';
import { cn } from '../../utils/cn';

export interface SearchControlsProps {
  searchState: 'idle' | 'running' | 'paused' | 'completed' | 'stopped';
  onStart: () => void;
  onPause: () => void;
  onResume: () => void;
  onStop: () => void;
}

export const SearchControls: React.FC<SearchControlsProps> = ({
  searchState,
  onStart,
  onPause,
  onResume,
  onStop,
}) => {
  const isStopDisabled = searchState === 'idle' || searchState === 'completed' || searchState === 'stopped';
  const isCompletedOrStopped = searchState === 'completed' || searchState === 'stopped';

  return (
    <div className="flex items-center gap-2 select-none">
      {/* STOP BUTTON */}
      <button
        type="button"
        onClick={onStop}
        disabled={isStopDisabled}
        className={cn(
          'bg-[#272a2e] border border-[#3c4a40] text-[#bbcabd] px-3 py-1 rounded text-xs font-sans font-medium transition-colors flex items-center gap-1.5 cursor-pointer',
          isStopDisabled
            ? 'opacity-40 cursor-not-allowed hover:text-[#bbcabd]'
            : 'hover:text-[#e1e2e7] hover:bg-[#323538]'
        )}
      >
        <Square className="w-3.5 h-3.5 fill-current" />
        <span>STOP</span>
      </button>

      {/* PAUSE / RESUME BUTTON */}
      {searchState === 'paused' ? (
        <button
          type="button"
          onClick={onResume}
          className="bg-[#272a2e] border border-[#f6be16]/50 text-[#f6be16] hover:bg-[#f6be16]/10 px-3 py-1 rounded text-xs font-sans font-medium transition-colors flex items-center gap-1.5 cursor-pointer"
        >
          <Play className="w-3.5 h-3.5 fill-current" />
          <span>RESUME</span>
        </button>
      ) : (
        <button
          type="button"
          onClick={onPause}
          disabled={searchState !== 'running'}
          className={cn(
            'bg-[#272a2e] border border-[#3c4a40] text-[#f6be16] px-3 py-1 rounded text-xs font-sans font-medium transition-colors flex items-center gap-1.5 cursor-pointer',
            searchState !== 'running'
              ? 'opacity-40 cursor-not-allowed hover:text-[#f6be16]'
              : 'hover:text-[#ffdf99] hover:bg-[#323538]'
          )}
        >
          <Pause className="w-3.5 h-3.5" />
          <span>PAUSE</span>
        </button>
      )}

      {/* START / NEW SEARCH BUTTON */}
      {isCompletedOrStopped ? (
        <button
          type="button"
          onClick={onStart}
          className="bg-[#02c076] text-[#002110] px-4 py-1 rounded font-bold hover:brightness-110 active:scale-95 transition-all text-xs flex items-center gap-1.5 cursor-pointer shadow-sm"
        >
          <RotateCcw className="w-3.5 h-3.5" />
          <span>START NEW SEARCH</span>
        </button>
      ) : searchState === 'running' ? (
        <button
          type="button"
          disabled
          className="bg-[#02c076]/70 text-[#002110] px-4 py-1 rounded font-bold transition-all text-xs flex items-center gap-1.5 cursor-not-allowed shadow-sm"
        >
          <span className="w-2 h-2 rounded-full bg-[#002110] animate-ping" />
          <span>SEARCHING...</span>
        </button>
      ) : (
        <button
          type="button"
          onClick={onStart}
          className="bg-[#02c076] text-[#002110] px-4 py-1 rounded font-bold hover:brightness-110 active:scale-95 transition-all text-xs flex items-center gap-1.5 cursor-pointer shadow-sm"
        >
          <Play className="w-3.5 h-3.5 fill-current" />
          <span>START SEARCH</span>
        </button>
      )}
    </div>
  );
};

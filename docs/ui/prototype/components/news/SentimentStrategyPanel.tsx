import React, { useState } from 'react';
import { Network, Plus, Check } from 'lucide-react';
import { RoutePath } from '../../types';
import { useApp } from '../../context/AppContext';

export interface SentimentStrategyPanelProps {
  onNavigate?: (route: RoutePath) => void;
  avgSentimentScore: number;
}

export const SentimentStrategyPanel: React.FC<SentimentStrategyPanelProps> = ({
  onNavigate = () => {},
  avgSentimentScore,
}) => {
  const { handleWorkflowAddNewsToComposer } = useApp();
  const [isAdding, setIsAdding] = useState(false);

  const handleAddToComposer = () => {
    setIsAdding(true);
    setTimeout(() => {
      setIsAdding(false);
      handleWorkflowAddNewsToComposer(onNavigate);
    }, 300);
  };

  const getSignalDisplay = () => {
    if (avgSentimentScore > 0.7) return { signal: 'BUY', color: 'text-[#02C076]' };
    if (avgSentimentScore < -0.7) return { signal: 'SELL', color: 'text-[#f84b4b]' };
    return { signal: 'HOLD', color: 'text-[#848E9C]' };
  };

  const { signal, color } = getSignalDisplay();

  return (
    <div className="p-4 rounded-lg flex flex-col gap-3 border-l-2 border-l-[#44e092] flex-1 bg-[#191c1f] border border-[#2B3139] select-none">
      {/* Title */}
      <div className="flex items-center gap-2 mb-1">
        <Network className="w-4 h-4 text-[#44e092]" />
        <h3 className="font-sans text-[11px] font-bold tracking-wider text-[#e1e2e7] uppercase">
          USE SENTIMENT IN STRATEGY
        </h3>
      </div>

      <p className="font-sans text-xs text-[#869488] leading-relaxed mb-1">
        Connect live news sentiment scores directly into your algorithmic trading rules.
      </p>

      {/* Code Snippet Box */}
      <div className="bg-[#161a1f] p-3 rounded border border-[#2B3139] font-mono text-xs text-[#e1e2e7] leading-relaxed">
        <div className="flex items-center gap-1">
          <span className="text-[#44e092] font-semibold">if</span> (AvgSentiment &gt;{' '}
          <span className="text-[#02C076] font-semibold">0.7</span>) &#123;
        </div>
        <div className="pl-4 text-[#869488]">
          action = <span className="text-[#02C076] font-bold">BUY</span>;
        </div>
        <div>&#125;</div>

        <div className="flex items-center gap-1 mt-1.5">
          <span className="text-[#44e092] font-semibold">if</span> (AvgSentiment &lt;{' '}
          <span className="text-[#f84b4b] font-semibold">-0.7</span>) &#123;
        </div>
        <div className="pl-4 text-[#869488]">
          action = <span className="text-[#f84b4b] font-bold">SELL</span>;
        </div>
        <div>&#125;</div>
      </div>

      {/* Current Mock Evaluation */}
      <div className="flex justify-between items-center bg-[#161a1f]/70 px-3 py-2 rounded border border-[#2B3139]/60 font-mono text-xs">
        <span className="text-[#869488]">
          Avg Score:{' '}
          <span className="text-[#e1e2e7] font-semibold">
            {avgSentimentScore > 0
              ? `+${avgSentimentScore.toFixed(2)}`
              : avgSentimentScore.toFixed(2)}
          </span>
        </span>
        <span className="text-[#869488]">
          Signal: <span className={`font-bold ${color}`}>{signal}</span>
        </span>
      </div>

      {/* Action Button */}
      <button
        type="button"
        onClick={handleAddToComposer}
        className="mt-auto w-full py-2 px-3 rounded font-mono text-xs font-semibold flex items-center justify-center gap-2 border border-[#2B3139] text-[#e1e2e7] hover:bg-[#2B3139] hover:border-[#44e092] active:scale-[0.98] transition-all cursor-pointer"
      >
        {isAdding ? (
          <>
            <Check className="w-3.5 h-3.5 text-[#44e092]" />
            <span className="text-[#44e092]">Adding...</span>
          </>
        ) : (
          <>
            <Plus className="w-3.5 h-3.5 text-[#44e092]" />
            <span>Add to Strategy Composer</span>
          </>
        )}
      </button>
    </div>
  );
};

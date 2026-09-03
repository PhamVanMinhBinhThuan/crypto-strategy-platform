import React from 'react';
import { SelectedStrategy, CombinationMethod } from '../../types/strategy';
import { ArrowRight } from 'lucide-react';
import { cn } from '../../utils/cn';

export interface CombinationEngineProps {
  blocks: SelectedStrategy[];
  combinationMethod: CombinationMethod;
  onSelectMethod: (method: CombinationMethod) => void;
  onUpdateWeight: (instanceId: string, weight: number) => void;
}

export const CombinationEngine: React.FC<CombinationEngineProps> = ({
  blocks,
  combinationMethod,
  onSelectMethod,
  onUpdateWeight,
}) => {
  const isWeighted = combinationMethod === 'Weighted Combination';

  return (
    <div>
      <h3 className="font-sans text-[11px] font-bold text-[#bbcabd] mb-3 uppercase tracking-widest border-b border-[#2B3139] pb-1">
        Combination Engine
      </h3>

      <div className="bg-[#0B0E11] rounded-[2px] border border-[#2B3139] p-3 select-none">
        {/* Method Switcher Tabs */}
        <div className="flex bg-[#2B3139] p-1 rounded-[2px] mb-4 w-fit">
          <button
            type="button"
            onClick={() => onSelectMethod('Majority Vote')}
            className={cn(
              'px-4 py-1.5 rounded-[2px] font-mono text-[12px] transition-colors cursor-pointer',
              !isWeighted
                ? 'bg-[#37393d] text-[#e1e2e7] font-semibold shadow-sm border border-[#3c4a40]'
                : 'text-[#bbcabd] hover:text-[#e1e2e7]'
            )}
          >
            Majority Vote
          </button>
          <button
            type="button"
            onClick={() => onSelectMethod('Weighted Combination')}
            className={cn(
              'px-4 py-1.5 rounded-[2px] font-mono text-[12px] transition-colors cursor-pointer',
              isWeighted
                ? 'bg-[#37393d] text-[#e1e2e7] font-semibold shadow-sm border border-[#3c4a40]'
                : 'text-[#bbcabd] hover:text-[#e1e2e7]'
            )}
          >
            Weighted Combination
          </button>
        </div>

        {/* Weights Config (Active when Weighted is selected) */}
        {isWeighted && (
          <div className="space-y-3 pl-2 mb-4">
            {blocks.map((block) => (
              <div key={block.instanceId} className="grid grid-cols-12 items-center gap-3">
                <div className="col-span-4 font-mono text-[13px] text-[#e1e2e7] truncate font-medium">
                  {block.name}
                </div>
                <div className="col-span-6 flex items-center">
                  <input
                    type="range"
                    min="0"
                    max="1"
                    step="0.05"
                    value={block.weight}
                    onChange={(e) =>
                      onUpdateWeight(block.instanceId, parseFloat(e.target.value) || 0)
                    }
                    className="w-full h-1 bg-[#2B3139] accent-[#44e092] rounded-lg cursor-pointer"
                  />
                </div>
                <div className="col-span-2">
                  <input
                    type="number"
                    min="0"
                    max="1"
                    step="0.1"
                    value={block.weight}
                    onChange={(e) =>
                      onUpdateWeight(block.instanceId, parseFloat(e.target.value) || 0)
                    }
                    className="bg-[#0B0E11] border border-[#2B3139] text-[#e1e2e7] font-mono text-[13px] text-right w-full h-7 rounded-[2px] px-1.5 focus:border-[#f6be16] focus:ring-0 outline-none"
                  />
                </div>
              </div>
            ))}

            {blocks.length === 0 && (
              <div className="text-center py-4 text-[#869488] font-sans text-xs">
                No active blocks to configure weights.
              </div>
            )}
          </div>
        )}

        {!isWeighted && (
          <div className="p-3 bg-[#1E2329] rounded-[2px] border border-[#2B3139] mb-4 text-[#bbcabd] font-mono text-xs">
            Majority vote aggregates individual BUY (+1), HOLD (0), and SELL (-1) signals evenly. 
            Final decision is determined by the dominant signal count across all active components.
          </div>
        )}

        {/* Logic & Execution Thresholds Summary Box */}
        <div className="p-3 bg-[#1E2329] rounded-[2px] border border-[#2B3139]">
          <div className="flex justify-between items-center mb-2">
            <span className="font-sans text-[11px] font-bold text-[#848E9C] tracking-wide uppercase">
              Signal Encoding
            </span>
            <div className="font-mono text-[12px] space-x-3">
              <span className="text-[#02C076] font-semibold">BUY(+1)</span>
              <span className="text-[#bbcabd]">HOLD(0)</span>
              <span className="text-[#CF304A] font-semibold">SELL(-1)</span>
            </div>
          </div>

          <div className="flex justify-between items-center">
            <span className="font-sans text-[11px] font-bold text-[#848E9C] tracking-wide uppercase">
              Execution Thresholds
            </span>
            <div className="font-mono text-[12px] text-[#e1e2e7] flex items-center">
              <span className="flex items-center gap-1">
                Score &gt; 0.3
                <ArrowRight className="w-3 h-3 text-[#02C076] inline" />
                <span className="text-[#02C076] font-bold">BUY</span>
              </span>
              <span className="mx-2 text-[#2B3139]">|</span>
              <span className="flex items-center gap-1">
                Score &lt; -0.3
                <ArrowRight className="w-3 h-3 text-[#CF304A] inline" />
                <span className="text-[#CF304A] font-bold">SELL</span>
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

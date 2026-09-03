import React from 'react';
import { SelectedStrategy } from '../../types/strategy';
import { GripVertical, X, Sliders } from 'lucide-react';
import { cn } from '../../utils/cn';

export interface ActiveStrategyBlockProps {
  block: SelectedStrategy;
  onUpdateParam: (instanceId: string, paramKey: string, value: number) => void;
  onRemove: (instanceId: string) => void;
}

export const ActiveStrategyBlock: React.FC<ActiveStrategyBlockProps> = ({
  block,
  onUpdateParam,
  onRemove,
}) => {
  return (
    <div
      className={cn(
        'bg-[#0B0E11] rounded-[2px] border border-[#2B3139] p-3 border-l-4 flex flex-col gap-3 transition-colors select-none',
        block.borderAccentClass
      )}
    >
      {/* Top Header of Block */}
      <div className="flex justify-between items-center">
        <div className="flex items-center gap-2">
          <GripVertical className="w-4 h-4 text-[#869488] cursor-grab hover:text-[#e1e2e7]" />
          <h4 className="font-mono text-[14px] text-[#e1e2e7] font-bold">
            {block.name}
          </h4>
        </div>
        <div className="flex items-center gap-1 text-[#bbcabd]">
          <button
            type="button"
            className="w-6 h-6 flex items-center justify-center hover:bg-[#2B3139] rounded-[2px] transition-colors cursor-pointer text-[#869488] hover:text-[#e1e2e7]"
            title="Module Settings"
          >
            <Sliders className="w-3.5 h-3.5" />
          </button>
          <button
            type="button"
            onClick={() => onRemove(block.instanceId)}
            className="w-6 h-6 flex items-center justify-center hover:bg-[#2B3139] rounded-[2px] transition-colors cursor-pointer text-[#869488] hover:text-[#ff5353]"
            title={`Remove ${block.name}`}
          >
            <X className="w-4 h-4" />
          </button>
        </div>
      </div>

      {/* Parameter Fields based on Block Type */}
      <div className="bg-[#1E2329] p-2.5 rounded-[2px] border border-[#2B3139]">
        {/* Moving Average */}
        {block.definitionId === 'moving-average' && (
          <div className="flex items-center gap-4">
            <div className="flex items-center gap-2 flex-1">
              <label className="font-mono text-[12px] text-[#848E9C] w-24 shrink-0">
                Fast Period
              </label>
              <input
                type="number"
                min="1"
                max="200"
                value={block.params.fastPeriod ?? 20}
                onChange={(e) =>
                  onUpdateParam(block.instanceId, 'fastPeriod', parseFloat(e.target.value) || 0)
                }
                className="bg-[#0B0E11] border border-[#2B3139] text-[#e1e2e7] font-mono text-[14px] text-right w-16 h-7 rounded-[2px] px-1.5 focus:border-[#f6be16] focus:ring-0 outline-none"
              />
            </div>
            <div className="flex items-center gap-2 flex-1">
              <label className="font-mono text-[12px] text-[#848E9C] w-24 shrink-0">
                Slow Period
              </label>
              <input
                type="number"
                min="2"
                max="500"
                value={block.params.slowPeriod ?? 50}
                onChange={(e) =>
                  onUpdateParam(block.instanceId, 'slowPeriod', parseFloat(e.target.value) || 0)
                }
                className="bg-[#0B0E11] border border-[#2B3139] text-[#e1e2e7] font-mono text-[14px] text-right w-16 h-7 rounded-[2px] px-1.5 focus:border-[#f6be16] focus:ring-0 outline-none"
              />
            </div>
          </div>
        )}

        {/* RSI */}
        {block.definitionId === 'rsi' && (
          <div className="flex items-center gap-4">
            <div className="flex items-center gap-2">
              <label className="font-mono text-[12px] text-[#848E9C]">Period</label>
              <input
                type="number"
                min="2"
                max="100"
                value={block.params.period ?? 14}
                onChange={(e) =>
                  onUpdateParam(block.instanceId, 'period', parseFloat(e.target.value) || 0)
                }
                className="bg-[#0B0E11] border border-[#2B3139] text-[#e1e2e7] font-mono text-[14px] text-right w-12 h-7 rounded-[2px] px-1.5 focus:border-[#f6be16] focus:ring-0 outline-none"
              />
            </div>
            <div className="flex items-center gap-2">
              <label className="font-mono text-[12px] text-[#848E9C]">Buy &lt;</label>
              <input
                type="number"
                min="1"
                max="50"
                value={block.params.buyThreshold ?? 30}
                onChange={(e) =>
                  onUpdateParam(block.instanceId, 'buyThreshold', parseFloat(e.target.value) || 0)
                }
                className="bg-[#0B0E11] border border-[#2B3139] text-[#02C076] font-mono font-bold text-[14px] text-right w-12 h-7 rounded-[2px] px-1.5 focus:border-[#f6be16] focus:ring-0 outline-none"
              />
            </div>
            <div className="flex items-center gap-2">
              <label className="font-mono text-[12px] text-[#848E9C]">Sell &gt;</label>
              <input
                type="number"
                min="50"
                max="99"
                value={block.params.sellThreshold ?? 70}
                onChange={(e) =>
                  onUpdateParam(block.instanceId, 'sellThreshold', parseFloat(e.target.value) || 0)
                }
                className="bg-[#0B0E11] border border-[#2B3139] text-[#CF304A] font-mono font-bold text-[14px] text-right w-12 h-7 rounded-[2px] px-1.5 focus:border-[#f6be16] focus:ring-0 outline-none"
              />
            </div>
          </div>
        )}

        {/* Support / Resistance */}
        {block.definitionId === 'support-resistance' && (
          <div className="flex flex-col gap-3">
            <div className="flex items-center gap-2 w-full justify-between">
              <label className="font-mono text-[12px] text-[#848E9C] w-24 shrink-0">
                Sensitivity
              </label>
              <input
                type="range"
                min="1"
                max="10"
                step="1"
                value={block.params.sensitivity ?? 5}
                onChange={(e) =>
                  onUpdateParam(block.instanceId, 'sensitivity', parseInt(e.target.value, 10) || 1)
                }
                className="flex-1 mx-2 h-1 bg-[#2B3139] accent-[#44e092] rounded-lg cursor-pointer"
              />
              <span className="font-mono text-[14px] text-[#e1e2e7] w-5 text-right font-medium">
                {block.params.sensitivity ?? 5}
              </span>
            </div>
            <div className="flex items-center gap-2 w-full">
              <label className="font-mono text-[12px] text-[#848E9C] w-24 shrink-0">
                Lookback
              </label>
              <input
                type="number"
                min="10"
                max="500"
                value={block.params.lookback ?? 100}
                onChange={(e) =>
                  onUpdateParam(block.instanceId, 'lookback', parseFloat(e.target.value) || 0)
                }
                className="bg-[#0B0E11] border border-[#2B3139] text-[#e1e2e7] font-mono text-[14px] text-right w-16 h-7 rounded-[2px] px-1.5 focus:border-[#f6be16] focus:ring-0 outline-none"
              />
            </div>
          </div>
        )}

        {/* Bollinger Bands */}
        {block.definitionId === 'bollinger-bands' && (
          <div className="flex items-center gap-4">
            <div className="flex items-center gap-2 flex-1">
              <label className="font-mono text-[12px] text-[#848E9C] w-20 shrink-0">Period</label>
              <input
                type="number"
                min="5"
                max="100"
                value={block.params.period ?? 20}
                onChange={(e) =>
                  onUpdateParam(block.instanceId, 'period', parseFloat(e.target.value) || 0)
                }
                className="bg-[#0B0E11] border border-[#2B3139] text-[#e1e2e7] font-mono text-[14px] text-right w-16 h-7 rounded-[2px] px-1.5 focus:border-[#f6be16] focus:ring-0 outline-none"
              />
            </div>
            <div className="flex items-center gap-2 flex-1">
              <label className="font-mono text-[12px] text-[#848E9C] w-20 shrink-0">Std Dev</label>
              <input
                type="number"
                min="1"
                max="5"
                step="0.1"
                value={block.params.stdDev ?? 2}
                onChange={(e) =>
                  onUpdateParam(block.instanceId, 'stdDev', parseFloat(e.target.value) || 0)
                }
                className="bg-[#0B0E11] border border-[#2B3139] text-[#e1e2e7] font-mono text-[14px] text-right w-16 h-7 rounded-[2px] px-1.5 focus:border-[#f6be16] focus:ring-0 outline-none"
              />
            </div>
          </div>
        )}

        {/* News Sentiment */}
        {block.definitionId === 'news-sentiment' && (
          <div className="flex items-center gap-4">
            <div className="flex items-center gap-2 flex-1">
              <label className="font-mono text-[12px] text-[#848E9C] w-24 shrink-0">Min Score</label>
              <input
                type="number"
                min="0"
                max="100"
                value={block.params.minScore ?? 60}
                onChange={(e) =>
                  onUpdateParam(block.instanceId, 'minScore', parseFloat(e.target.value) || 0)
                }
                className="bg-[#0B0E11] border border-[#2B3139] text-[#e1e2e7] font-mono text-[14px] text-right w-16 h-7 rounded-[2px] px-1.5 focus:border-[#f6be16] focus:ring-0 outline-none"
              />
            </div>
            <div className="flex items-center gap-2 flex-1">
              <label className="font-mono text-[12px] text-[#848E9C] w-24 shrink-0">Lookback (h)</label>
              <input
                type="number"
                min="1"
                max="168"
                value={block.params.lookbackHours ?? 24}
                onChange={(e) =>
                  onUpdateParam(block.instanceId, 'lookbackHours', parseFloat(e.target.value) || 0)
                }
                className="bg-[#0B0E11] border border-[#2B3139] text-[#e1e2e7] font-mono text-[14px] text-right w-16 h-7 rounded-[2px] px-1.5 focus:border-[#f6be16] focus:ring-0 outline-none"
              />
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

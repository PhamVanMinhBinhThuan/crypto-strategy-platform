import React from 'react';
import { SelectedStrategy, CombinationMethod, StrategySignal } from '../../types/strategy';
import { SignalBadge } from './SignalBadge';
import { Play, Save, FolderPlus, Check } from 'lucide-react';
import { cn } from '../../utils/cn';

export interface LiveStrategySummaryProps {
  strategyName: string;
  onUpdateStrategyName: (name: string) => void;
  blocks: SelectedStrategy[];
  combinationMethod: CombinationMethod;
  compositeScore: number;
  finalSignal: StrategySignal;
  onRunBacktest: () => void;
  onSaveStrategy: () => void;
  onAddToSearchSpace: () => void;
  isSaved?: boolean;
}

export const LiveStrategySummary: React.FC<LiveStrategySummaryProps> = ({
  strategyName,
  onUpdateStrategyName,
  blocks,
  combinationMethod,
  compositeScore,
  finalSignal,
  onRunBacktest,
  onSaveStrategy,
  onAddToSearchSpace,
  isSaved = false,
}) => {
  const isWeighted = combinationMethod === 'Weighted Combination';

  // Build formula preview string
  const formulaString = React.useMemo(() => {
    if (blocks.length === 0) return 'No active modules';
    if (!isWeighted) {
      return `Vote(${blocks.map((b) => b.abbreviation).join(', ')})`;
    }
    return blocks
      .map((b) => `${b.weight.toFixed(1)}(${b.abbreviation})`)
      .join(' + ');
  }, [blocks, isWeighted]);

  const formattedScore = React.useMemo(() => {
    const prefix = compositeScore > 0 ? '+' : '';
    return `${prefix}${compositeScore.toFixed(2)}`;
  }, [compositeScore]);

  return (
    <section className="w-80 bg-[#1E2329] flex flex-col rounded-[2px] border border-[#2B3139] shrink-0 h-full overflow-hidden select-none">
      {/* Header with Strategy Name */}
      <div className="p-3 border-b border-[#2B3139] bg-[#1d2023] shrink-0">
        <h2 className="font-sans text-[11px] font-bold text-[#bbcabd] mb-1 uppercase tracking-widest">
          Live Output
        </h2>
        <input
          type="text"
          value={strategyName}
          onChange={(e) => onUpdateStrategyName(e.target.value)}
          className="bg-transparent border-none p-0 outline-none font-mono text-[16px] text-[#e1e2e7] font-bold w-full focus:ring-0 focus:text-[#44e092] transition-colors"
          placeholder="Strategy Name..."
        />
      </div>

      <div className="flex-1 flex flex-col p-4 gap-3.5 min-h-0 overflow-hidden">
        {/* Stats Grid: Components & Method */}
        <div className="grid grid-cols-2 gap-2 shrink-0">
          <div className="bg-[#0B0E11] p-2.5 rounded-[2px] border border-[#2B3139]">
            <div className="font-mono text-[11px] text-[#848E9C] mb-0.5">Components</div>
            <div className="font-mono text-[18px] font-bold text-[#e1e2e7]">
              {blocks.length}
            </div>
          </div>
          <div className="bg-[#0B0E11] p-2.5 rounded-[2px] border border-[#2B3139]">
            <div className="font-mono text-[11px] text-[#848E9C] mb-0.5">Method</div>
            <div className="font-mono text-[16px] font-medium text-[#e1e2e7] truncate">
              {isWeighted ? 'Weighted' : 'Majority'}
            </div>
          </div>
        </div>

        {/* Formula Preview Box */}
        <div className="bg-[#0B0E11] p-3 rounded-[2px] border border-[#2B3139] shrink-0 flex flex-col">
          <div className="font-sans text-[10px] font-bold text-[#bbcabd] mb-1.5 uppercase tracking-wider">
            Formula Preview
          </div>
          <div className="font-mono text-[13px] text-[#848E9C] break-words leading-snug max-h-20 overflow-y-auto pr-1">
            {blocks.map((b, i) => (
              <span key={b.instanceId}>
                {i > 0 && ' + '}
                {isWeighted && (
                  <span className="text-[#e1e2e7] font-semibold">{b.weight.toFixed(1)}</span>
                )}
                <span className="text-[#bbcabd]">({b.abbreviation})</span>
              </span>
            ))}
            {blocks.length === 0 && <span className="text-[#848E9C]">None</span>}
          </div>
        </div>

        {/* Real-time Component Signals Breakdown Table */}
        <div className="flex-1 min-h-0 flex flex-col overflow-hidden">
          <div className="font-sans text-[10px] font-bold text-[#bbcabd] mb-2 border-b border-[#2B3139] pb-1 uppercase tracking-wider shrink-0">
            Real-Time Signals
          </div>
          <div className="flex-1 overflow-y-auto min-h-0 pr-1">
            <table className="w-full text-left font-mono text-[13px]">
              <tbody>
                {blocks.map((block) => {
                  const isBuy = block.signal === 'BUY';
                  const isSell = block.signal === 'SELL';
                  const sign = block.signalValue > 0 ? `+${block.signalValue}` : `${block.signalValue}`;

                  return (
                    <tr key={block.instanceId} className="border-b border-[#2B3139]/80">
                      <td className="py-2 text-[#e1e2e7] font-medium truncate max-w-[110px]">
                        {block.name}
                      </td>
                      <td
                        className={cn(
                          'py-2 text-right font-bold pr-3',
                          isBuy && 'text-[#02C076]',
                          isSell && 'text-[#CF304A]',
                          !isBuy && !isSell && 'text-[#bbcabd]'
                        )}
                      >
                        {sign}
                      </td>
                      <td className="py-2 text-right">
                        <SignalBadge signal={block.signal} />
                      </td>
                    </tr>
                  );
                })}

                {blocks.length === 0 && (
                  <tr>
                    <td colSpan={3} className="py-3 text-center text-[#869488] font-sans text-xs">
                      No components added yet.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>

        {/* Composite Result Card with subtle glow */}
        <div className="shrink-0 min-h-[148px] bg-[#0B0E11] p-4 rounded-[2px] border border-[#2B3139] flex flex-col items-center justify-center gap-1.5 relative overflow-hidden">
          {/* Subtle Ambient Radial Glow */}
          <div
            className={cn(
              'absolute inset-0 pointer-events-none opacity-10',
              finalSignal === 'BUY' && 'bg-[#02C076]',
              finalSignal === 'SELL' && 'bg-[#CF304A]',
              finalSignal === 'HOLD' && 'bg-[#f6be16]'
            )}
          />

          <div className="font-sans text-[10px] font-bold text-[#bbcabd] z-10 uppercase tracking-wider">
            Composite Score
          </div>

          <div
            className={cn(
              'font-mono text-3xl font-bold z-10 tracking-tight',
              finalSignal === 'BUY' && 'text-[#02C076]',
              finalSignal === 'SELL' && 'text-[#CF304A]',
              finalSignal === 'HOLD' && 'text-[#f6be16]'
            )}
          >
            {formattedScore}
          </div>

          <div
            className={cn(
              'mt-1 px-5 py-1.5 rounded-full font-sans text-[13px] font-bold z-10 uppercase tracking-wide',
              finalSignal === 'BUY' &&
                'bg-[#02C076] text-[#00391f] shadow-[0_0_15px_rgba(2,192,118,0.35)]',
              finalSignal === 'SELL' &&
                'bg-[#CF304A] text-[#ffffff] shadow-[0_0_15px_rgba(207,48,74,0.35)]',
              finalSignal === 'HOLD' &&
                'bg-[#f6be16] text-[#251a00] shadow-[0_0_15px_rgba(246,190,22,0.35)]'
            )}
          >
            {finalSignal} SIGNAL
          </div>
        </div>

        {/* Strategy Action Buttons */}
        <div className="flex flex-col gap-2 shrink-0">
          {/* Run Backtest (Primary Action) */}
          <button
            type="button"
            onClick={onRunBacktest}
            className="w-full py-2.5 px-3 rounded-[2px] bg-[#02c076] text-[#004728] font-mono text-[13px] font-bold hover:opacity-90 active:scale-[0.99] transition-all flex items-center justify-center gap-2 cursor-pointer shadow-sm"
          >
            <Play className="w-4 h-4 fill-current" />
            <span>Run Backtest</span>
          </button>

          {/* Add to Search Space & Save Strategy */}
          <div className="flex gap-2">
            <button
              type="button"
              onClick={onAddToSearchSpace}
              className="flex-1 py-2 px-2 rounded-[2px] bg-[#2B3139] border border-[#3c4a40] text-[#e1e2e7] font-mono text-[11px] font-semibold hover:bg-[#323538] active:scale-[0.99] transition-all flex items-center justify-center gap-1.5 cursor-pointer whitespace-nowrap"
              title="Add to Search Space"
            >
              <FolderPlus className="w-3.5 h-3.5 text-[#bbcabd] shrink-0" />
              <span>Add to Search Space</span>
            </button>

            <button
              type="button"
              onClick={onSaveStrategy}
              className="flex-1 py-2 px-2 rounded-[2px] bg-[#2B3139] border border-[#3c4a40] text-[#e1e2e7] font-mono text-[11px] font-semibold hover:bg-[#323538] active:scale-[0.99] transition-all flex items-center justify-center gap-1.5 cursor-pointer whitespace-nowrap"
            >
              {isSaved ? (
                <>
                  <Check className="w-3.5 h-3.5 text-[#02C076] shrink-0" />
                  <span className="text-[#02C076]">Saved</span>
                </>
              ) : (
                <>
                  <Save className="w-3.5 h-3.5 text-[#bbcabd] shrink-0" />
                  <span>Save Strategy</span>
                </>
              )}
            </button>
          </div>
        </div>
      </div>
    </section>
  );
};

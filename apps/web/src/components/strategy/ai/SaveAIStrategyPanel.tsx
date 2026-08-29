import React, { useState, useEffect } from 'react';
import { AIStrategyJSON, ValidationSummary } from '../../../types/aiStrategy';
import { Save, Check, Play, FolderPlus, Eye, Plus, X, Tag, Sparkles, Globe } from 'lucide-react';
import { cn } from '../../../utils/cn';

export interface SaveAIStrategyPanelProps {
  strategyJson: AIStrategyJSON | null;
  validation: ValidationSummary | null;
  onSave: (finalStrategy: AIStrategyJSON) => void;
  isSaved: boolean;
  onViewInComposer: () => void;
  onRunBacktest: () => void;
  onAddToSearchSpace: () => void;
}

export const SaveAIStrategyPanel: React.FC<SaveAIStrategyPanelProps> = ({
  strategyJson,
  validation,
  onSave,
  isSaved,
  onViewInComposer,
  onRunBacktest,
  onAddToSearchSpace,
}) => {
  const [name, setName] = useState<string>('');
  const [version, setVersion] = useState<string>('1.0.0');
  const [tags, setTags] = useState<string[]>([]);
  const [newTagInput, setNewTagInput] = useState<string>('');
  const [timeframe, setTimeframe] = useState<string>('1h');
  const [market, setMarket] = useState<string>('spot');

  // Sync state when strategyJson changes
  useEffect(() => {
    if (strategyJson) {
      setName(strategyJson.name || 'Custom_Strategy');
      setVersion(strategyJson.version || '1.0.0');
      setTags(strategyJson.tags || ['AI Generated']);
      setTimeframe(strategyJson.timeframe || '1h');
      setMarket(strategyJson.market || 'spot');
    }
  }, [strategyJson]);

  const handleAddTag = () => {
    const t = newTagInput.trim();
    if (t && !tags.includes(t)) {
      setTags([...tags, t]);
      setNewTagInput('');
    }
  };

  const handleRemoveTag = (tagToRemove: string) => {
    setTags(tags.filter((t) => t !== tagToRemove));
  };

  const handleSaveClick = () => {
    if (!strategyJson || !validation?.canSave) return;

    const finalStrat: AIStrategyJSON = {
      ...strategyJson,
      name: name.trim() || strategyJson.name,
      version: version.trim() || strategyJson.version,
      tags,
      timeframe,
      market,
    };

    onSave(finalStrat);
  };

  const canSave = Boolean(strategyJson && validation?.canSave);
  const isUrl = strategyJson?.source === 'URL_IMPORT';

  return (
    <section className="bg-[#1E2329] rounded-[2px] border border-[#2B3139] flex flex-col h-full overflow-hidden select-none">
      {/* Header */}
      <div className="p-3 border-b border-[#2B3139] bg-[#1d2023] flex items-center justify-between shrink-0">
        <h2 className="font-sans text-[11px] font-bold text-[#bbcabd] uppercase tracking-widest flex items-center gap-1.5">
          <Save className="w-3.5 h-3.5 text-[#02C076]" />
          <span>Save Strategy</span>
        </h2>
        {strategyJson && (
          <span
            className={cn(
              'text-[10px] font-mono px-1.5 py-0.5 rounded border flex items-center gap-1',
              isUrl
                ? 'bg-[#f6be16]/10 text-[#f6be16] border-[#f6be16]/30'
                : 'bg-[#02C076]/10 text-[#02C076] border-[#02C076]/30'
            )}
          >
            {isUrl ? <Globe className="w-3 h-3" /> : <Sparkles className="w-3 h-3" />}
            <span>{strategyJson.source || 'AI_PROMPT'}</span>
          </span>
        )}
      </div>

      {/* Body */}
      <div className="flex-1 overflow-y-auto p-3.5 flex flex-col justify-between gap-3">
        {!strategyJson ? (
          <div className="flex-1 flex flex-col items-center justify-center text-center p-6 text-[#848E9C]">
            <Save className="w-8 h-8 text-[#2B3139] mb-2" />
            <div className="font-mono text-[13px] text-[#bbcabd] mb-1">
              Save parameters
            </div>
            <p className="text-[11px] font-sans max-w-[200px] leading-relaxed">
              Naming, tags, and library metadata will unlock once a strategy is parsed.
            </p>
          </div>
        ) : (
          <>
            {/* Form Fields */}
            <div className="space-y-3">
              {/* Name Field */}
              <div className="flex flex-col gap-1">
                <label className="font-sans text-[10px] font-bold text-[#bbcabd] uppercase tracking-wider">
                  Strategy Name
                </label>
                <input
                  type="text"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  placeholder="e.g. RSI_BB_LONG_SL2_TP4"
                  className="w-full bg-[#0B0E11] rounded-[2px] border border-[#2B3139] px-2.5 py-1.5 text-[12px] font-mono text-[#e1e2e7] focus:outline-none focus:border-[#02C076] transition-colors"
                />
              </div>

              {/* Version & Timeframe Grid */}
              <div className="grid grid-cols-2 gap-2">
                <div className="flex flex-col gap-1">
                  <label className="font-sans text-[10px] font-bold text-[#bbcabd] uppercase tracking-wider">
                    Version
                  </label>
                  <input
                    type="text"
                    value={version}
                    onChange={(e) => setVersion(e.target.value)}
                    placeholder="1.0.0"
                    className="w-full bg-[#0B0E11] rounded-[2px] border border-[#2B3139] px-2.5 py-1.5 text-[12px] font-mono text-[#e1e2e7] focus:outline-none focus:border-[#02C076] transition-colors"
                  />
                </div>

                <div className="flex flex-col gap-1">
                  <label className="font-sans text-[10px] font-bold text-[#bbcabd] uppercase tracking-wider">
                    Timeframe
                  </label>
                  <select
                    value={timeframe}
                    onChange={(e) => setTimeframe(e.target.value)}
                    className="w-full bg-[#0B0E11] rounded-[2px] border border-[#2B3139] px-2 py-1.5 text-[12px] font-mono text-[#e1e2e7] focus:outline-none focus:border-[#02C076] transition-colors"
                  >
                    <option value="1m">1m</option>
                    <option value="5m">5m</option>
                    <option value="15m">15m</option>
                    <option value="1h">1h</option>
                    <option value="4h">4h</option>
                    <option value="1d">1d</option>
                  </select>
                </div>
              </div>

              {/* Tags Section */}
              <div className="flex flex-col gap-1.5">
                <label className="font-sans text-[10px] font-bold text-[#bbcabd] uppercase tracking-wider flex items-center gap-1">
                  <Tag className="w-3 h-3 text-[#f6be16]" />
                  <span>Tags</span>
                </label>

                {/* Tag Chips */}
                <div className="flex flex-wrap gap-1 min-h-[28px] p-1 bg-[#0B0E11] rounded-[2px] border border-[#2B3139]">
                  {tags.map((t) => (
                    <span
                      key={t}
                      className="px-1.5 py-0.5 rounded-[2px] bg-[#2B3139] text-[#e1e2e7] font-mono text-[10px] flex items-center gap-1 border border-[#3c4a40]"
                    >
                      <span>{t}</span>
                      <button
                        type="button"
                        onClick={() => handleRemoveTag(t)}
                        className="text-[#848E9C] hover:text-[#CF304A] transition-colors cursor-pointer"
                        title={`Remove tag ${t}`}
                      >
                        <X className="w-2.5 h-2.5" />
                      </button>
                    </span>
                  ))}
                  {tags.length === 0 && (
                    <span className="text-[10px] font-mono text-[#848E9C] p-0.5">
                      No tags added
                    </span>
                  )}
                </div>

                {/* Add Tag Input */}
                <div className="flex items-center gap-1 mt-0.5">
                  <input
                    type="text"
                    value={newTagInput}
                    onChange={(e) => setNewTagInput(e.target.value)}
                    onKeyDown={(e) => {
                      if (e.key === 'Enter') {
                        e.preventDefault();
                        handleAddTag();
                      }
                    }}
                    placeholder="Add tag (e.g. Mean Reversion)..."
                    className="flex-1 bg-[#0B0E11] rounded-[2px] border border-[#2B3139] px-2 py-1 text-[11px] font-mono text-[#e1e2e7] focus:outline-none focus:border-[#02C076] transition-colors"
                  />
                  <button
                    type="button"
                    onClick={handleAddTag}
                    className="px-2 py-1 bg-[#2B3139] border border-[#3c4a40] text-[#e1e2e7] text-[11px] font-mono rounded-[2px] hover:bg-[#323538] transition-colors flex items-center gap-1 cursor-pointer"
                  >
                    <Plus className="w-3 h-3 text-[#02C076]" />
                    <span>Add</span>
                  </button>
                </div>
              </div>
            </div>

            {/* Actions Area */}
            <div className="flex flex-col gap-2 pt-2 border-t border-[#2B3139]/80 shrink-0">
              {/* Primary Save Button */}
              <button
                type="button"
                disabled={!canSave}
                onClick={handleSaveClick}
                className={cn(
                  'w-full py-2.5 px-3 rounded-[2px] font-mono text-[13px] font-bold flex items-center justify-center gap-2 transition-all cursor-pointer shadow-sm',
                  !canSave
                    ? 'bg-[#2B3139] text-[#869488] cursor-not-allowed border border-[#3c4a40]'
                    : isSaved
                    ? 'bg-[#02C076]/20 text-[#02C076] border border-[#02C076]/40 hover:bg-[#02C076]/30'
                    : 'bg-[#02C076] text-[#00391f] hover:bg-[#02C076]/90 active:scale-[0.99]'
                )}
              >
                {isSaved ? (
                  <>
                    <Check className="w-4 h-4 text-[#02C076]" />
                    <span>Strategy Saved to Library</span>
                  </>
                ) : (
                  <>
                    <Save className="w-4 h-4" />
                    <span>Save Strategy</span>
                  </>
                )}
              </button>

              {/* Instant Next Action Buttons */}
              <div className="flex gap-2">
                <button
                  type="button"
                  disabled={!strategyJson}
                  onClick={onViewInComposer}
                  className="flex-1 py-1.5 px-2 rounded-[2px] bg-[#2B3139] border border-[#3c4a40] text-[#e1e2e7] font-mono text-[11px] font-semibold hover:bg-[#323538] active:scale-[0.99] transition-all flex items-center justify-center gap-1.5 cursor-pointer disabled:opacity-40 disabled:cursor-not-allowed whitespace-nowrap"
                  title="Open in Visual Composer"
                >
                  <Eye className="w-3.5 h-3.5 text-[#02C076] shrink-0" />
                  <span>View in Composer</span>
                </button>

                <button
                  type="button"
                  disabled={!strategyJson}
                  onClick={onRunBacktest}
                  className="flex-1 py-1.5 px-2 rounded-[2px] bg-[#2B3139] border border-[#3c4a40] text-[#e1e2e7] font-mono text-[11px] font-semibold hover:bg-[#323538] active:scale-[0.99] transition-all flex items-center justify-center gap-1.5 cursor-pointer disabled:opacity-40 disabled:cursor-not-allowed whitespace-nowrap"
                  title="Run Backtest on Strategy"
                >
                  <Play className="w-3.5 h-3.5 text-[#f6be16] fill-current shrink-0" />
                  <span>Run Backtest</span>
                </button>

                <button
                  type="button"
                  disabled={!strategyJson}
                  onClick={onAddToSearchSpace}
                  className="flex-1 py-1.5 px-2 rounded-[2px] bg-[#2B3139] border border-[#3c4a40] text-[#e1e2e7] font-mono text-[11px] font-semibold hover:bg-[#323538] active:scale-[0.99] transition-all flex items-center justify-center gap-1.5 cursor-pointer disabled:opacity-40 disabled:cursor-not-allowed whitespace-nowrap"
                  title="Add to Search Space"
                >
                  <FolderPlus className="w-3.5 h-3.5 text-[#44e092] shrink-0" />
                  <span>Search</span>
                </button>
              </div>
            </div>
          </>
        )}
      </div>
    </section>
  );
};

import React from 'react';
import { RecentAIStrategy } from '../../../types/aiStrategy';
import { History, Eye, Play, FolderPlus, Trash2, Globe, Sparkles, CheckCircle2, AlertTriangle } from 'lucide-react';
import { cn } from '../../../utils/cn';

export interface RecentAIStrategiesTableProps {
  strategies: RecentAIStrategy[];
  onViewStrategy: (strategy: RecentAIStrategy) => void;
  onBacktestStrategy: (strategy: RecentAIStrategy) => void;
  onAddToSearchStrategy: (strategy: RecentAIStrategy) => void;
  onDeleteStrategy: (id: string) => void;
}

export const RecentAIStrategiesTable: React.FC<RecentAIStrategiesTableProps> = ({
  strategies,
  onViewStrategy,
  onBacktestStrategy,
  onAddToSearchStrategy,
  onDeleteStrategy,
}) => {
  return (
    <section className="bg-[#1E2329] rounded-[2px] border border-[#2B3139] flex flex-col overflow-hidden select-none">
      {/* Header */}
      <div className="p-3 border-b border-[#2B3139] bg-[#1d2023] flex items-center justify-between shrink-0">
        <div className="flex items-center gap-2">
          <History className="w-4 h-4 text-[#02C076]" />
          <h2 className="font-sans text-[12px] font-bold text-[#e1e2e7] tracking-wider uppercase">
            Recently AI-Created Strategies
          </h2>
        </div>
        <span className="text-[11px] font-mono text-[#848E9C] bg-[#0B0E11] px-2 py-0.5 rounded border border-[#2B3139]">
          {strategies.length} {strategies.length === 1 ? 'strategy' : 'strategies'}
        </span>
      </div>

      {/* Table Content */}
      <div className="overflow-x-auto">
        <table className="w-full text-left font-mono text-[12px] border-collapse">
          <thead>
            <tr className="border-b border-[#2B3139] bg-[#0B0E11] text-[#848E9C] uppercase text-[10px] tracking-wider font-sans">
              <th className="py-2.5 px-3">Strategy</th>
              <th className="py-2.5 px-3">Source</th>
              <th className="py-2.5 px-3">Created</th>
              <th className="py-2.5 px-3">Version</th>
              <th className="py-2.5 px-3">Tags</th>
              <th className="py-2.5 px-3">Status</th>
              <th className="py-2.5 px-3 text-right">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-[#2B3139]/60">
            {strategies.length === 0 ? (
              <tr>
                <td colSpan={7} className="py-8 text-center text-[#848E9C] font-sans text-xs">
                  No AI strategies created yet. Describe a strategy above to get started.
                </td>
              </tr>
            ) : (
              strategies.map((strat) => {
                const isUrl = strat.source === 'URL_IMPORT';
                const isValid = strat.status === 'Valid';

                return (
                  <tr
                    key={strat.id}
                    className="hover:bg-[#2B3139]/40 transition-colors group"
                  >
                    {/* Strategy Name & Description */}
                    <td className="py-3 px-3 max-w-[240px]">
                      <div className="font-mono text-[13px] font-bold text-[#e1e2e7] group-hover:text-[#02C076] transition-colors truncate">
                        {strat.name}
                      </div>
                      <div className="font-sans text-[11px] text-[#848E9C] truncate leading-tight mt-0.5" title={strat.description}>
                        {strat.description}
                      </div>
                    </td>

                    {/* Source */}
                    <td className="py-3 px-3 whitespace-nowrap">
                      <span
                        className={cn(
                          'px-1.5 py-0.5 rounded-[2px] border text-[10px] font-mono inline-flex items-center gap-1 font-semibold',
                          isUrl
                            ? 'bg-[#f6be16]/10 text-[#f6be16] border-[#f6be16]/30'
                            : 'bg-[#02C076]/10 text-[#02C076] border-[#02C076]/30'
                        )}
                      >
                        {isUrl ? <Globe className="w-3 h-3" /> : <Sparkles className="w-3 h-3" />}
                        <span>{strat.source}</span>
                      </span>
                    </td>

                    {/* Created */}
                    <td className="py-3 px-3 text-[#848E9C] font-sans text-[11px] whitespace-nowrap">
                      {strat.createdAt}
                    </td>

                    {/* Version */}
                    <td className="py-3 px-3 text-[#bbcabd] font-mono text-[11px] whitespace-nowrap">
                      v{strat.version}
                    </td>

                    {/* Tags */}
                    <td className="py-3 px-3">
                      <div className="flex flex-wrap gap-1 max-w-[200px]">
                        {strat.tags.slice(0, 3).map((tag) => (
                          <span
                            key={tag}
                            className="px-1.5 py-0.5 bg-[#0B0E11] text-[#848E9C] rounded-[2px] border border-[#2B3139] text-[10px] font-mono whitespace-nowrap"
                          >
                            {tag}
                          </span>
                        ))}
                        {strat.tags.length > 3 && (
                          <span className="text-[10px] font-mono text-[#848E9C] self-center">
                            +{strat.tags.length - 3}
                          </span>
                        )}
                      </div>
                    </td>

                    {/* Status */}
                    <td className="py-3 px-3 whitespace-nowrap">
                      <span
                        className={cn(
                          'inline-flex items-center gap-1 px-1.5 py-0.5 rounded-[2px] text-[10px] font-mono font-bold',
                          isValid
                            ? 'bg-[#02C076]/10 text-[#02C076]'
                            : 'bg-[#f6be16]/10 text-[#f6be16]'
                        )}
                      >
                        {isValid ? (
                          <CheckCircle2 className="w-3 h-3" />
                        ) : (
                          <AlertTriangle className="w-3 h-3" />
                        )}
                        <span>{strat.status}</span>
                      </span>
                    </td>

                    {/* Actions */}
                    <td className="py-3 px-3 text-right whitespace-nowrap">
                      <div className="flex items-center justify-end gap-1.5">
                        <button
                          type="button"
                          onClick={() => onViewStrategy(strat)}
                          className="px-2 py-1 bg-[#2B3139] hover:bg-[#323538] text-[#e1e2e7] hover:text-[#02C076] border border-[#3c4a40] rounded-[2px] font-mono text-[10px] font-semibold flex items-center gap-1 transition-colors cursor-pointer"
                          title="View and Edit in Visual Composer"
                        >
                          <Eye className="w-3 h-3 text-[#02C076]" />
                          <span>View</span>
                        </button>

                        <button
                          type="button"
                          onClick={() => onBacktestStrategy(strat)}
                          className="px-2 py-1 bg-[#2B3139] hover:bg-[#323538] text-[#e1e2e7] hover:text-[#f6be16] border border-[#3c4a40] rounded-[2px] font-mono text-[10px] font-semibold flex items-center gap-1 transition-colors cursor-pointer"
                          title="Run Backtest"
                        >
                          <Play className="w-3 h-3 text-[#f6be16] fill-current" />
                          <span>Backtest</span>
                        </button>

                        <button
                          type="button"
                          onClick={() => onAddToSearchStrategy(strat)}
                          className="px-2 py-1 bg-[#2B3139] hover:bg-[#323538] text-[#e1e2e7] hover:text-[#44e092] border border-[#3c4a40] rounded-[2px] font-mono text-[10px] font-semibold flex items-center gap-1 transition-colors cursor-pointer"
                          title="Add to Search Space"
                        >
                          <FolderPlus className="w-3 h-3 text-[#44e092]" />
                          <span>Search</span>
                        </button>

                        <button
                          type="button"
                          onClick={() => onDeleteStrategy(strat.id)}
                          className="p-1 text-[#848E9C] hover:text-[#CF304A] hover:bg-[#CF304A]/10 rounded-[2px] transition-colors cursor-pointer"
                          title="Delete from list"
                        >
                          <Trash2 className="w-3.5 h-3.5" />
                        </button>
                      </div>
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>
    </section>
  );
};

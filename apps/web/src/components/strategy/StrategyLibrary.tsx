import React, { useState, useMemo } from 'react';
import { StrategyDefinition } from '../../types/strategy';
import { STRATEGY_CATEGORIES, STRATEGY_LIBRARY } from '../../data/strategyLibraryData';
import { StrategyLibraryItem } from './StrategyLibraryItem';
import { Search } from 'lucide-react';
import { useApp } from '../../context/AppContext';

export interface StrategyLibraryProps {
  onAddStrategy: (strategy: StrategyDefinition) => void;
  activeDefinitionIds: string[];
}

export const StrategyLibrary: React.FC<StrategyLibraryProps> = ({
  onAddStrategy,
  activeDefinitionIds,
}) => {
  const [searchQuery, setSearchQuery] = useState('');
  const { strategyLibrary = STRATEGY_LIBRARY } = useApp();

  const filteredLibrary = useMemo(() => {
    const libraryToFilter = strategyLibrary || STRATEGY_LIBRARY;
    if (!searchQuery.trim()) return libraryToFilter;
    const q = searchQuery.toLowerCase();
    return libraryToFilter.filter(
      (s) =>
        s.name.toLowerCase().includes(q) ||
        s.category.toLowerCase().includes(q) ||
        s.description.toLowerCase().includes(q) ||
        s.tags?.some((t) => t.toLowerCase().includes(q))
    );
  }, [searchQuery, strategyLibrary]);

  return (
    <section className="w-80 bg-[#1E2329] flex flex-col rounded-[2px] border border-[#2B3139] shrink-0 overflow-hidden select-none">
      {/* Header & Search Bar */}
      <div className="p-3 border-b border-[#2B3139] bg-[#1d2023]">
        <h2 className="font-sans text-[11px] font-bold text-[#bbcabd] mb-2 uppercase tracking-widest">
          Library
        </h2>
        <div className="flex items-center bg-[#0b0e11] rounded-[2px] border border-[#2B3139] focus-within:border-[#f6be16] px-2 h-8 transition-colors">
          <Search className="w-4 h-4 text-[#869488] mr-2 shrink-0" />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Search modules..."
            className="bg-transparent border-none outline-none text-[12px] font-sans w-full focus:ring-0 text-[#e1e2e7] placeholder-[#869488]"
          />
        </div>
      </div>

      {/* Categories and Strategy Cards */}
      <div className="flex-1 overflow-y-auto p-3 space-y-4">
        {STRATEGY_CATEGORIES.map((cat) => {
          const strategiesInCat = filteredLibrary.filter((s) => s.category === cat.id);
          if (strategiesInCat.length === 0) return null;

          return (
            <div key={cat.id}>
              {/* Category Header with Colored Left Accent */}
              <div
                className={`font-sans text-[11px] font-bold text-[#bbcabd] mb-2 pl-1.5 border-l-2 ${cat.borderClass} uppercase tracking-wider`}
              >
                {cat.label}
              </div>

              {/* Module Cards in this Category */}
              <div className="space-y-2">
                {strategiesInCat.map((strategy) => (
                  <StrategyLibraryItem
                    key={strategy.id}
                    strategy={strategy}
                    onAdd={onAddStrategy}
                    isAlreadyAdded={activeDefinitionIds.includes(strategy.id)}
                  />
                ))}
              </div>
            </div>
          );
        })}

        {filteredLibrary.length === 0 && (
          <div className="text-center py-8 text-[#869488] font-sans text-xs">
            No strategy modules found for "{searchQuery}"
          </div>
        )}
      </div>
    </section>
  );
};

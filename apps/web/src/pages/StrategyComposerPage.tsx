import React, { useState, useMemo, useCallback } from 'react';
import { RoutePath } from '../types';
import { 
  StrategyDefinition, 
  CombinationMethod, 
} from '../types/strategy';
import { StrategyLibrary } from '../components/strategy/StrategyLibrary';
import { ActiveStrategyBlock } from '../components/strategy/ActiveStrategyBlock';
import { CombinationEngine } from '../components/strategy/CombinationEngine';
import { LiveStrategySummary } from '../components/strategy/LiveStrategySummary';
import { Network } from 'lucide-react';
import { useApp } from '../context/AppContext';

export interface StrategyComposerPageProps {
  onNavigate?: (route: RoutePath) => void;
}

export const StrategyComposerPage: React.FC<StrategyComposerPageProps> = ({
  onNavigate = (_route: RoutePath) => {},
}) => {
  const {
    composerState,
    setComposerStrategyName,
    setComposerBlocks,
    setComposerCombinationMethod,
    addStrategyToComposer,
    handleWorkflowRunBacktestFromComposer,
    handleWorkflowAddToSearchSpaceFromComposer,
  } = useApp();

  const { strategyName, blocks, combinationMethod, compositeScore, finalSignal } = composerState;
  const [isSaved, setIsSaved] = useState<boolean>(false);

  // Add a strategy module from library to composite builder
  const handleAddStrategy = useCallback((def: StrategyDefinition) => {
    addStrategyToComposer(def);
    setIsSaved(false);
  }, [addStrategyToComposer]);

  // Remove active block
  const handleRemoveBlock = useCallback((instanceId: string) => {
    setComposerBlocks((prev) => prev.filter((b) => b.instanceId !== instanceId));
    setIsSaved(false);
  }, [setComposerBlocks]);

  // Clear all blocks
  const handleClearAll = useCallback(() => {
    setComposerBlocks([]);
    setIsSaved(false);
  }, [setComposerBlocks]);

  // Update specific parameter in a block
  const handleUpdateParam = useCallback(
    (instanceId: string, paramKey: string, value: number) => {
      setComposerBlocks((prev) =>
        prev.map((b) => {
          if (b.instanceId !== instanceId) return b;
          return {
            ...b,
            params: {
              ...b.params,
              [paramKey]: value,
            },
          };
        })
      );
      setIsSaved(false);
    },
    [setComposerBlocks]
  );

  // Update block weight
  const handleUpdateWeight = useCallback((instanceId: string, weight: number) => {
    setComposerBlocks((prev) =>
      prev.map((b) => {
        if (b.instanceId !== instanceId) return b;
        return { ...b, weight: Math.max(0, Math.min(1, Math.round(weight * 100) / 100)) };
      })
    );
    setIsSaved(false);
  }, [setComposerBlocks]);

  // Active definition IDs in builder for library indicator
  const activeDefinitionIds = useMemo(() => {
    return blocks.map((b) => b.definitionId);
  }, [blocks]);

  // Action handlers
  const handleRunBacktest = useCallback(() => {
    handleWorkflowRunBacktestFromComposer(onNavigate);
  }, [handleWorkflowRunBacktestFromComposer, onNavigate]);

  const handleSaveStrategy = useCallback(() => {
    setIsSaved(true);
    setTimeout(() => {
      setIsSaved(false);
    }, 2500);
  }, []);

  const handleAddToSearchSpace = useCallback(() => {
    handleWorkflowAddToSearchSpaceFromComposer(onNavigate);
  }, [handleWorkflowAddToSearchSpaceFromComposer, onNavigate]);

  return (
    <div className="flex-1 flex gap-2 overflow-hidden bg-[#0b0e11] p-2 h-full select-none">
      {/* 1. LEFT COLUMN: STRATEGY LIBRARY */}
      <StrategyLibrary
        onAddStrategy={handleAddStrategy}
        activeDefinitionIds={activeDefinitionIds}
      />

      {/* 2. CENTER COLUMN: COMPOSITE STRATEGY BUILDER */}
      <section className="flex-1 bg-[#1E2329] flex flex-col rounded-[2px] border border-[#2B3139] overflow-hidden min-w-[500px]">
        {/* Center Column Header */}
        <div className="p-3 border-b border-[#2B3139] bg-[#1d2023] flex justify-between items-center shrink-0">
          <h2 className="font-sans text-[18px] font-bold text-[#e1e2e7] flex items-center gap-2">
            <Network className="w-5 h-5 text-[#44e092]" />
            <span>Composite Strategy</span>
          </h2>
          <div className="flex gap-2">
            <button
              type="button"
              onClick={handleClearAll}
              className="px-3 py-1 rounded-[2px] bg-[#2B3139] text-[#bbcabd] font-mono text-[12px] hover:bg-[#323538] hover:text-[#e1e2e7] transition-colors border border-[#3c4a40] cursor-pointer"
            >
              Clear All
            </button>
          </div>
        </div>

        {/* Center Column Scrollable Body */}
        <div className="flex-1 overflow-y-auto p-4 flex flex-col gap-6">
          {/* Active Blocks List */}
          <div>
            <h3 className="font-sans text-[11px] font-bold text-[#bbcabd] mb-3 uppercase tracking-widest border-b border-[#2B3139] pb-1">
              Active Blocks
            </h3>
            <div className="space-y-3">
              {blocks.map((block) => (
                <ActiveStrategyBlock
                  key={block.instanceId}
                  block={block}
                  onUpdateParam={handleUpdateParam}
                  onRemove={handleRemoveBlock}
                />
              ))}

              {blocks.length === 0 && (
                <div className="p-8 text-center bg-[#0B0E11] rounded-[2px] border border-dashed border-[#2B3139] text-[#869488]">
                  <div className="font-mono text-sm mb-1 text-[#bbcabd]">
                    No strategy modules selected
                  </div>
                  <div className="text-xs">
                    Click &quot;+&quot; on any module in the Strategy Library on the left to add it.
                  </div>
                </div>
              )}
            </div>
          </div>

          {/* Combination Engine */}
          <CombinationEngine
            blocks={blocks}
            combinationMethod={combinationMethod}
            onSelectMethod={setComposerCombinationMethod}
            onUpdateWeight={handleUpdateWeight}
          />
        </div>
      </section>

      {/* 3. RIGHT COLUMN: LIVE OUTPUT & ACTIONS */}
      <LiveStrategySummary
        strategyName={strategyName}
        onUpdateStrategyName={setComposerStrategyName}
        blocks={blocks}
        combinationMethod={combinationMethod}
        compositeScore={compositeScore}
        finalSignal={finalSignal}
        onRunBacktest={handleRunBacktest}
        onSaveStrategy={handleSaveStrategy}
        onAddToSearchSpace={handleAddToSearchSpace}
        isSaved={isSaved}
      />
    </div>
  );
};

import React, { useState, useMemo, useCallback } from 'react';
import { RoutePath } from '../types';
import { 
  StrategyDefinition, 
  CombinationMethod, 
} from '../types/strategy';
import { AIStrategyJSON } from '../types/aiStrategy';
import { StrategyLibrary } from '../components/strategy/StrategyLibrary';
import { ActiveStrategyBlock } from '../components/strategy/ActiveStrategyBlock';
import { CombinationEngine } from '../components/strategy/CombinationEngine';
import { LiveStrategySummary } from '../components/strategy/LiveStrategySummary';
import { CreateWithAIWorkspace } from '../components/strategy/ai/CreateWithAIWorkspace';
import { Network, LayoutGrid, Sparkles } from 'lucide-react';
import { useApp } from '../context/AppContext';
import { cn } from '../utils/cn';

export type ComposerTabMode = 'visual' | 'ai';

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
    saveAIStrategyToLibrary,
    loadAIStrategyIntoComposer,
    recentAIStrategies,
    deleteRecentAIStrategy,
  } = useApp();

  const { strategyName, blocks, combinationMethod, compositeScore, finalSignal } = composerState;
  const [activeTab, setActiveTab] = useState<ComposerTabMode>('visual');
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

  // Action handlers in Visual Composer
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

  // Handlers for "Create with AI" Tab
  const handleAISaveToLibrary = useCallback((aiJson: AIStrategyJSON) => {
    saveAIStrategyToLibrary(aiJson);
  }, [saveAIStrategyToLibrary]);

  const handleAIViewInVisualComposer = useCallback((aiJson: AIStrategyJSON) => {
    loadAIStrategyIntoComposer(aiJson);
    setActiveTab('visual');
  }, [loadAIStrategyIntoComposer]);

  const handleAIRunBacktest = useCallback((aiJson: AIStrategyJSON) => {
    loadAIStrategyIntoComposer(aiJson);
    handleWorkflowRunBacktestFromComposer(onNavigate);
  }, [loadAIStrategyIntoComposer, handleWorkflowRunBacktestFromComposer, onNavigate]);

  const handleAIAddToSearchSpace = useCallback((aiJson: AIStrategyJSON) => {
    loadAIStrategyIntoComposer(aiJson);
    handleWorkflowAddToSearchSpaceFromComposer(onNavigate);
  }, [loadAIStrategyIntoComposer, handleWorkflowAddToSearchSpaceFromComposer, onNavigate]);

  return (
    <div className="flex-1 flex flex-col min-h-0 bg-[#0b0e11] select-none">
      {/* Top Header with Title and Mode Switcher Tabs */}
      <div className="px-4 py-2.5 bg-[#14181d] border-b border-[#2B3139] flex flex-col sm:flex-row sm:items-center justify-between gap-2.5 shrink-0">
        <div>
          <h1 className="font-sans text-[15px] font-bold text-[#e1e2e7] tracking-wide flex items-center gap-2">
            <span>Strategy Composer</span>
          </h1>
          <p className="font-sans text-[11px] text-[#848E9C]">
            {activeTab === 'visual'
              ? 'Build, combine, test and create quantitative trading strategies.'
              : 'Describe a trading strategy in natural language or import one from a public URL.'}
          </p>
        </div>

        {/* Mode Switcher Tabs */}
        <div className="flex items-center bg-[#0B0E11] p-0.5 rounded-[2px] border border-[#2B3139] self-start sm:self-auto">
          <button
            type="button"
            onClick={() => setActiveTab('visual')}
            className={cn(
              'px-3 py-1 rounded-[2px] font-mono text-[11px] font-semibold flex items-center gap-1.5 transition-all cursor-pointer',
              activeTab === 'visual'
                ? 'bg-[#1E2329] text-[#e1e2e7] shadow-sm border border-[#2B3139]'
                : 'text-[#848E9C] hover:text-[#e1e2e7]'
            )}
          >
            <LayoutGrid className="w-3.5 h-3.5 text-[#44e092]" />
            <span>Visual Composer</span>
          </button>

          <button
            type="button"
            onClick={() => setActiveTab('ai')}
            className={cn(
              'px-3 py-1 rounded-[2px] font-mono text-[11px] font-semibold flex items-center gap-1.5 transition-all cursor-pointer',
              activeTab === 'ai'
                ? 'bg-[#1E2329] text-[#02C076] shadow-sm border border-[#02C076]/30'
                : 'text-[#848E9C] hover:text-[#e1e2e7]'
            )}
          >
            <Sparkles className="w-3.5 h-3.5 text-[#02C076]" />
            <span>Create with AI</span>
          </button>
        </div>
      </div>

      {/* Main Tab Content Viewport */}
      <div className="flex-1 p-2 min-h-0 overflow-y-auto overflow-x-hidden flex flex-col">
        {activeTab === 'visual' ? (
          <div className="flex-1 flex gap-2 overflow-hidden h-full min-h-[580px]">
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
        ) : (
          <div className="flex-1 flex flex-col pb-4">
            <CreateWithAIWorkspace
              onSaveToLibrary={handleAISaveToLibrary}
              onViewInVisualComposer={handleAIViewInVisualComposer}
              onRunBacktest={handleAIRunBacktest}
              onAddToSearchSpace={handleAIAddToSearchSpace}
              recentStrategies={recentAIStrategies}
              onDeleteRecentStrategy={deleteRecentAIStrategy}
            />
          </div>
        )}
      </div>
    </div>
  );
};

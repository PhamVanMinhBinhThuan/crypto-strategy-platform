import React, { useState } from 'react';
import {
  AIStrategyJSON,
  ParsedStrategyData,
  ValidationSummary,
  RecentAIStrategy,
} from '../../../types/aiStrategy';
import {
  parseStrategyPrompt,
  validateStrategyDefinition,
} from '../../../utils/aiStrategyParser';
import { StrategyAIInput } from './StrategyAIInput';
import { ParsedStrategyPanel } from './ParsedStrategyPanel';
import { StrategyDefinitionPanel } from './StrategyDefinitionPanel';
import { StrategyValidationPanel } from './StrategyValidationPanel';
import { SaveAIStrategyPanel } from './SaveAIStrategyPanel';
import { RecentAIStrategiesTable } from './RecentAIStrategiesTable';
import { CheckCircle2, X } from 'lucide-react';

export interface CreateWithAIWorkspaceProps {
  onSaveToLibrary: (aiJson: AIStrategyJSON) => void;
  onViewInVisualComposer: (aiJson: AIStrategyJSON) => void;
  onRunBacktest: (aiJson: AIStrategyJSON) => void;
  onAddToSearchSpace: (aiJson: AIStrategyJSON) => void;
  recentStrategies: RecentAIStrategy[];
  onDeleteRecentStrategy: (id: string) => void;
}

export const CreateWithAIWorkspace: React.FC<CreateWithAIWorkspaceProps> = ({
  onSaveToLibrary,
  onViewInVisualComposer,
  onRunBacktest,
  onAddToSearchSpace,
  recentStrategies,
  onDeleteRecentStrategy,
}) => {
  const [isAnalyzing, setIsAnalyzing] = useState<boolean>(false);
  const [strategyJson, setStrategyJson] = useState<AIStrategyJSON | null>(null);
  const [parsedData, setParsedData] = useState<ParsedStrategyData | null>(null);
  const [validation, setValidation] = useState<ValidationSummary | null>(null);
  const [isSaved, setIsSaved] = useState<boolean>(false);
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  // Auto-dismiss toast
  const triggerToast = (msg: string) => {
    setToastMessage(msg);
    setTimeout(() => {
      setToastMessage(null);
    }, 4000);
  };

  // 1. Analyze from Natural Language Prompt
  const handleAnalyzePrompt = (prompt: string) => {
    setIsAnalyzing(true);
    setIsSaved(false);

    // Realistic brief analysis delay for smooth UX
    setTimeout(() => {
      try {
        const { strategyJson: generatedJson, parsedData: parsed } = parseStrategyPrompt(prompt, 'AI_PROMPT');
        const validSummary = validateStrategyDefinition(generatedJson);

        setStrategyJson(generatedJson);
        setParsedData(parsed);
        setValidation(validSummary);
        triggerToast('Strategy analyzed and verified by Quant Engine');
      } finally {
        setIsAnalyzing(false);
      }
    }, 400);
  };

  // 2. Analyze from Extracted URL Logic
  const handleAnalyzeUrl = (prompt: string, url: string) => {
    setIsAnalyzing(true);
    setIsSaved(false);

    setTimeout(() => {
      try {
        const { strategyJson: generatedJson, parsedData: parsed } = parseStrategyPrompt(prompt, 'URL_IMPORT', url);
        const validSummary = validateStrategyDefinition(generatedJson);

        setStrategyJson(generatedJson);
        setParsedData(parsed);
        setValidation(validSummary);
        triggerToast('Strategy logic extracted and validated from public URL');
      } finally {
        setIsAnalyzing(false);
      }
    }, 400);
  };

  // 3. User manually edits JSON definition in panel
  const handleUpdateStrategyJson = (updated: AIStrategyJSON) => {
    setStrategyJson(updated);
    setIsSaved(false);
    const validSummary = validateStrategyDefinition(updated);
    setValidation(validSummary);

    // Sync parsed summary card view
    const longTexts = (updated.conditions?.long || []).map(
      (c) => c.text || `${c.indicator} ${c.operator} ${c.value ?? c.indicatorRef ?? ''}`
    );
    const shortTexts = (updated.conditions?.short || []).map(
      (c) => c.text || `${c.indicator} ${c.operator} ${c.value ?? c.indicatorRef ?? ''}`
    );

    setParsedData({
      longConditions: longTexts,
      shortConditions: shortTexts,
      hasShortConditions: shortTexts.length > 0,
      stopLoss: updated.riskManagement?.stopLoss?.value ? `${updated.riskManagement.stopLoss.value}%` : 'Not specified',
      hasStopLoss: Boolean(updated.riskManagement?.stopLoss?.value),
      takeProfit: updated.riskManagement?.takeProfit?.value ? `${updated.riskManagement.takeProfit.value}%` : 'Not specified',
      hasTakeProfit: Boolean(updated.riskManagement?.takeProfit?.value),
      timeframe: updated.timeframe || '1h',
      isTimeframeDefault: !updated.timeframe,
      market: updated.pairs ? `${updated.pairs} · ${updated.market?.toUpperCase() || 'SPOT'}` : 'USDT pairs · SPOT',
      isMarketDefault: !updated.pairs,
      indicators: (updated.indicators || []).map((i) => i.name),
    });
  };

  // 4. Save to Strategy Library
  const handleSaveStrategy = (finalStrategy: AIStrategyJSON) => {
    onSaveToLibrary(finalStrategy);
    setIsSaved(true);
    triggerToast('Strategy saved to Strategy Library');
  };

  // 5. Workflows
  const handleViewInComposer = () => {
    if (strategyJson) {
      onViewInVisualComposer(strategyJson);
    }
  };

  const handleRunBacktest = () => {
    if (strategyJson) {
      onRunBacktest(strategyJson);
    }
  };

  const handleAddToSearch = () => {
    if (strategyJson) {
      onAddToSearchSpace(strategyJson);
    }
  };

  // Table row actions
  const handleTableView = (recent: RecentAIStrategy) => {
    onViewInVisualComposer(recent.strategyJson);
  };

  const handleTableBacktest = (recent: RecentAIStrategy) => {
    onRunBacktest(recent.strategyJson);
  };

  const handleTableAddToSearch = (recent: RecentAIStrategy) => {
    onAddToSearchSpace(recent.strategyJson);
  };

  return (
    <div className="flex-1 flex flex-col gap-4 min-w-0">
      {/* Toast Notification Banner */}
      {toastMessage && (
        <div className="bg-[#02C076] text-[#00391f] px-3.5 py-2 rounded-[2px] font-mono text-[12px] font-bold flex items-center justify-between shadow-lg animate-in fade-in slide-in-from-top-2 duration-200">
          <div className="flex items-center gap-2">
            <CheckCircle2 className="w-4 h-4 text-[#00391f]" />
            <span>{toastMessage}</span>
          </div>
          <button
            type="button"
            onClick={() => setToastMessage(null)}
            className="text-[#00391f] hover:opacity-80 transition-opacity p-0.5 cursor-pointer"
          >
            <X className="w-3.5 h-3.5" />
          </button>
        </div>
      )}

      {/* 4-Column Workspace Grid on desktop, stackable on tablet/mobile */}
      <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-3.5 min-h-[500px]">
        {/* Column 1: Input Panel */}
        <div className="h-full min-h-[460px]">
          <StrategyAIInput
            onAnalyzePrompt={handleAnalyzePrompt}
            onAnalyzeUrl={handleAnalyzeUrl}
            isAnalyzing={isAnalyzing}
          />
        </div>

        {/* Column 2: Parsed Strategy Panel */}
        <div className="h-full min-h-[460px]">
          <ParsedStrategyPanel
            parsedData={parsedData}
            isAnalyzing={isAnalyzing}
          />
        </div>

        {/* Column 3: Strategy Definition Panel */}
        <div className="h-full min-h-[460px]">
          <StrategyDefinitionPanel
            strategyJson={strategyJson}
            onUpdateStrategyJson={handleUpdateStrategyJson}
          />
        </div>

        {/* Column 4: Validation & Save Strategy Panels */}
        <div className="flex flex-col gap-3.5 h-full min-h-[460px]">
          <div className="flex-1 min-h-[220px]">
            <StrategyValidationPanel
              validation={validation}
              hasStrategy={Boolean(strategyJson)}
            />
          </div>
          <div className="flex-1 min-h-[220px]">
            <SaveAIStrategyPanel
              strategyJson={strategyJson}
              validation={validation}
              onSave={handleSaveStrategy}
              isSaved={isSaved}
              onViewInComposer={handleViewInComposer}
              onRunBacktest={handleRunBacktest}
              onAddToSearchSpace={handleAddToSearch}
            />
          </div>
        </div>
      </div>

      {/* Section Below: Recently AI-Created Strategies */}
      <div className="mt-2">
        <RecentAIStrategiesTable
          strategies={recentStrategies}
          onViewStrategy={handleTableView}
          onBacktestStrategy={handleTableBacktest}
          onAddToSearchStrategy={handleTableAddToSearch}
          onDeleteStrategy={onDeleteRecentStrategy}
        />
      </div>
    </div>
  );
};

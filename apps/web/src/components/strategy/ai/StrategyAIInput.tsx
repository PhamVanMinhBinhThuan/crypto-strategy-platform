import React, { useState } from 'react';
import { Sparkles, Trash2, Globe, ArrowRight, Loader2, AlertCircle, HelpCircle } from 'lucide-react';
import { cn } from '../../../utils/cn';
import { extractStrategyFromUrl } from '../../../utils/aiStrategyParser';

export interface StrategyAIInputProps {
  onAnalyzePrompt: (prompt: string) => void;
  onAnalyzeUrl: (extractedPrompt: string, url: string) => void;
  isAnalyzing: boolean;
}

export const StrategyAIInput: React.FC<StrategyAIInputProps> = ({
  onAnalyzePrompt,
  onAnalyzeUrl,
  isAnalyzing,
}) => {
  const [promptText, setPromptText] = useState<string>(
    'When RSI is below 30 and price closes below the Bollinger Lower Band, open LONG. Stop loss 2%, take profit 4%.'
  );
  const [urlInput, setUrlInput] = useState<string>('');
  const [promptError, setPromptError] = useState<string | null>(null);
  const [urlError, setUrlError] = useState<string | null>(null);
  const [isExtractingUrl, setIsExtractingUrl] = useState<boolean>(false);

  const handlePromptChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    const val = e.target.value;
    if (val.length <= 1000) {
      setPromptText(val);
      if (promptError && val.trim().length > 0) {
        setPromptError(null);
      }
    }
  };

  const handleClear = () => {
    setPromptText('');
    setPromptError(null);
  };

  const handleAnalyzeClick = () => {
    if (!promptText.trim()) {
      setPromptError('Describe a strategy before analyzing.');
      return;
    }
    setPromptError(null);
    onAnalyzePrompt(promptText.trim());
  };

  const handleExtractUrlClick = async () => {
    if (!urlInput.trim()) {
      setUrlError('Enter a valid URL.');
      return;
    }
    setUrlError(null);
    setIsExtractingUrl(true);

    try {
      const res = await extractStrategyFromUrl(urlInput.trim());
      if (res.success && res.prompt) {
        setPromptText(res.prompt);
        onAnalyzeUrl(res.prompt, urlInput.trim());
      } else {
        setUrlError(res.error || 'Unable to extract a strategy from this URL. Try another public page or paste the strategy description manually.');
      }
    } catch {
      setUrlError('Unable to extract a strategy from this URL. Try another public page or paste the strategy description manually.');
    } finally {
      setIsExtractingUrl(false);
    }
  };

  return (
    <section className="bg-[#1E2329] rounded-[2px] border border-[#2B3139] flex flex-col h-full overflow-hidden select-none">
      {/* Panel Header */}
      <div className="p-3 border-b border-[#2B3139] bg-[#1d2023] flex items-center justify-between shrink-0">
        <h2 className="font-sans text-[11px] font-bold text-[#bbcabd] uppercase tracking-widest flex items-center gap-1.5">
          <Sparkles className="w-3.5 h-3.5 text-[#02C076]" />
          <span>Create Strategy</span>
        </h2>
        <span className="text-[10px] font-mono text-[#869488] bg-[#0B0E11] px-1.5 py-0.5 rounded border border-[#2B3139]">
          Prompt / URL
        </span>
      </div>

      {/* Panel Body */}
      <div className="flex-1 overflow-y-auto p-3.5 flex flex-col gap-4">
        {/* Method A: Natural Language Prompt */}
        <div className="flex flex-col gap-1.5">
          <div className="flex justify-between items-center">
            <label className="font-sans text-[11px] font-bold text-[#e1e2e7] flex items-center gap-1">
              <span>Describe your strategy</span>
              <span title="Enter natural language rules: e.g. indicators, entry conditions, exit targets, stop loss and timeframes.">
                <HelpCircle className="w-3 h-3 text-[#848E9C] cursor-help" />
              </span>
            </label>
            <span className="font-mono text-[10px] text-[#848E9C]">
              {promptText.length} / 1000
            </span>
          </div>

          <textarea
            value={promptText}
            onChange={handlePromptChange}
            placeholder="When RSI is below 30 and price closes below the lower Bollinger Band, open LONG. Stop loss 2%, take profit 4%."
            rows={5}
            className="w-full bg-[#0B0E11] rounded-[2px] border border-[#2B3139] p-2.5 text-[12px] font-sans text-[#e1e2e7] placeholder-[#848E9C] focus:outline-none focus:border-[#02C076] transition-colors resize-none leading-relaxed"
          />

          {promptError && (
            <div className="flex items-center gap-1 text-[#CF304A] text-[11px] font-sans">
              <AlertCircle className="w-3 h-3 shrink-0" />
              <span>{promptError}</span>
            </div>
          )}

          <div className="flex items-center gap-2 mt-1">
            <button
              type="button"
              disabled={isAnalyzing || isExtractingUrl}
              onClick={handleAnalyzeClick}
              className={cn(
                'flex-1 py-2 px-3 rounded-[2px] font-mono text-[12px] font-bold flex items-center justify-center gap-2 transition-all cursor-pointer',
                isAnalyzing
                  ? 'bg-[#2B3139] text-[#869488] cursor-not-allowed'
                  : 'bg-[#02C076] text-[#00391f] hover:bg-[#02c076]/90 active:scale-[0.99] shadow-sm'
              )}
            >
              {isAnalyzing ? (
                <>
                  <Loader2 className="w-3.5 h-3.5 animate-spin" />
                  <span>Analyzing strategy...</span>
                </>
              ) : (
                <>
                  <Sparkles className="w-3.5 h-3.5" />
                  <span>Analyze with AI</span>
                </>
              )}
            </button>

            <button
              type="button"
              onClick={handleClear}
              disabled={isAnalyzing || promptText.length === 0}
              className="py-2 px-2.5 rounded-[2px] bg-[#2B3139] border border-[#3c4a40] text-[#bbcabd] hover:text-[#e1e2e7] hover:bg-[#323538] text-[12px] font-mono transition-colors flex items-center justify-center gap-1 cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
              title="Clear Prompt"
            >
              <Trash2 className="w-3.5 h-3.5 text-[#869488]" />
              <span>Clear</span>
            </button>
          </div>
        </div>

        {/* Divider with OR tag */}
        <div className="relative flex items-center justify-center my-0.5">
          <div className="border-t border-[#2B3139] w-full" />
          <span className="bg-[#1E2329] px-2 font-mono text-[10px] text-[#848E9C] uppercase absolute">
            or import
          </span>
        </div>

        {/* Method B: Public URL Extraction */}
        <div className="flex flex-col gap-1.5">
          <label className="font-sans text-[11px] font-bold text-[#e1e2e7] flex items-center gap-1">
            <Globe className="w-3 h-3 text-[#f6be16]" />
            <span>Import from URL</span>
          </label>

          <input
            type="text"
            value={urlInput}
            onChange={(e) => {
              setUrlInput(e.target.value);
              if (urlError) setUrlError(null);
            }}
            placeholder="https://www.tradingview.com/script/..."
            className="w-full bg-[#0B0E11] rounded-[2px] border border-[#2B3139] px-2.5 py-1.5 text-[12px] font-mono text-[#e1e2e7] placeholder-[#848E9C] focus:outline-none focus:border-[#f6be16] transition-colors"
          />

          <p className="text-[11px] font-sans text-[#848E9C] leading-snug">
            Import strategy logic from TradingView, GitHub, Medium, blogs or public documentation.
          </p>

          {urlError && (
            <div className="flex items-start gap-1 text-[#CF304A] text-[11px] font-sans">
              <AlertCircle className="w-3 h-3 shrink-0 mt-0.5" />
              <span>{urlError}</span>
            </div>
          )}

          <button
            type="button"
            disabled={isExtractingUrl || isAnalyzing}
            onClick={handleExtractUrlClick}
            className={cn(
              'mt-1 py-1.5 px-3 rounded-[2px] font-mono text-[11px] font-semibold flex items-center justify-center gap-1.5 transition-all border cursor-pointer',
              isExtractingUrl
                ? 'bg-[#2B3139] border-[#3c4a40] text-[#869488] cursor-not-allowed'
                : 'bg-[#2B3139] border-[#3c4a40] text-[#e1e2e7] hover:bg-[#323538] hover:border-[#f6be16] active:scale-[0.99]'
            )}
          >
            {isExtractingUrl ? (
              <>
                <Loader2 className="w-3 h-3 animate-spin text-[#f6be16]" />
                <span>Extracting Strategy...</span>
              </>
            ) : (
              <>
                <Globe className="w-3.5 h-3.5 text-[#f6be16]" />
                <span>Extract Strategy</span>
                <ArrowRight className="w-3 h-3 ml-auto text-[#848E9C]" />
              </>
            )}
          </button>
        </div>
      </div>
    </section>
  );
};

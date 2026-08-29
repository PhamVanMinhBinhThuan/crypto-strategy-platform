import React, { useState, useEffect } from 'react';
import { AIStrategyJSON } from '../../../types/aiStrategy';
import { Code, Copy, Check, Edit3, Eye, AlertCircle, FileCode } from 'lucide-react';
import { cn } from '../../../utils/cn';

export interface StrategyDefinitionPanelProps {
  strategyJson: AIStrategyJSON | null;
  onUpdateStrategyJson?: (updated: AIStrategyJSON) => void;
}

export const StrategyDefinitionPanel: React.FC<StrategyDefinitionPanelProps> = ({
  strategyJson,
  onUpdateStrategyJson,
}) => {
  const [copied, setCopied] = useState<boolean>(false);
  const [isEditing, setIsEditing] = useState<boolean>(false);
  const [rawText, setRawText] = useState<string>('');
  const [jsonSyntaxError, setJsonSyntaxError] = useState<string | null>(null);

  // Sync JSON text when strategyJson changes from outside
  useEffect(() => {
    if (strategyJson) {
      setRawText(JSON.stringify(strategyJson, null, 2));
      setJsonSyntaxError(null);
    } else {
      setRawText('');
      setJsonSyntaxError(null);
    }
  }, [strategyJson]);

  const handleCopy = () => {
    if (!rawText) return;
    navigator.clipboard.writeText(rawText);
    setCopied(true);
    setTimeout(() => {
      setCopied(false);
    }, 2000);
  };

  const handleTextChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    const val = e.target.value;
    setRawText(val);
    try {
      const parsed = JSON.parse(val) as AIStrategyJSON;
      setJsonSyntaxError(null);
      if (onUpdateStrategyJson) {
        onUpdateStrategyJson(parsed);
      }
    } catch (err: any) {
      setJsonSyntaxError('Strategy Definition contains invalid JSON: ' + (err.message || 'Syntax error'));
    }
  };

  return (
    <section className="bg-[#1E2329] rounded-[2px] border border-[#2B3139] flex flex-col h-full overflow-hidden select-none">
      {/* Header */}
      <div className="p-3 border-b border-[#2B3139] bg-[#1d2023] flex items-center justify-between shrink-0">
        <h2 className="font-sans text-[11px] font-bold text-[#bbcabd] uppercase tracking-widest flex items-center gap-1.5">
          <Code className="w-3.5 h-3.5 text-[#44e092]" />
          <span>Strategy Definition (JSON)</span>
        </h2>

        {strategyJson && (
          <div className="flex items-center gap-1.5">
            <button
              type="button"
              onClick={() => setIsEditing(!isEditing)}
              className={cn(
                'px-2 py-0.5 rounded-[2px] font-mono text-[10px] flex items-center gap-1 transition-colors border cursor-pointer',
                isEditing
                  ? 'bg-[#f6be16]/20 text-[#f6be16] border-[#f6be16]/40'
                  : 'bg-[#0B0E11] text-[#bbcabd] border-[#2B3139] hover:text-[#e1e2e7]'
              )}
              title={isEditing ? 'View Formatted JSON' : 'Edit JSON Definition'}
            >
              {isEditing ? (
                <>
                  <Eye className="w-3 h-3" />
                  <span>Preview</span>
                </>
              ) : (
                <>
                  <Edit3 className="w-3 h-3" />
                  <span>Edit</span>
                </>
              )}
            </button>

            <button
              type="button"
              onClick={handleCopy}
              className="px-2 py-0.5 rounded-[2px] bg-[#0B0E11] text-[#bbcabd] hover:text-[#e1e2e7] border border-[#2B3139] font-mono text-[10px] flex items-center gap-1 transition-colors cursor-pointer"
              title="Copy JSON Definition"
            >
              {copied ? (
                <>
                  <Check className="w-3 h-3 text-[#02C076]" />
                  <span className="text-[#02C076]">Copied!</span>
                </>
              ) : (
                <>
                  <Copy className="w-3 h-3 text-[#848E9C]" />
                  <span>Copy</span>
                </>
              )}
            </button>
          </div>
        )}
      </div>

      {/* Body / Code Viewport */}
      <div className="flex-1 min-h-0 bg-[#0B0E11] relative overflow-hidden flex flex-col">
        {!strategyJson && !rawText ? (
          <div className="flex-1 flex flex-col items-center justify-center text-center p-6 text-[#848E9C]">
            <FileCode className="w-8 h-8 text-[#2B3139] mb-2" />
            <div className="font-mono text-[13px] text-[#bbcabd] mb-1">
              Strategy definition will appear after analysis
            </div>
            <p className="text-[11px] font-sans max-w-[200px] leading-relaxed">
              The normalized AST/JSON representation will be compiled and shown here.
            </p>
          </div>
        ) : isEditing ? (
          <div className="flex-1 flex flex-col p-2 min-h-0 overflow-hidden">
            {jsonSyntaxError && (
              <div className="mb-2 p-2 bg-[#CF304A]/10 border border-[#CF304A]/30 rounded-[2px] flex items-center gap-1.5 text-[#CF304A] text-[11px] font-mono shrink-0">
                <AlertCircle className="w-3.5 h-3.5 shrink-0" />
                <span className="truncate">{jsonSyntaxError}</span>
              </div>
            )}
            <textarea
              value={rawText}
              onChange={handleTextChange}
              spellCheck={false}
              className="flex-1 w-full bg-[#080a0c] text-[#e1e2e7] font-mono text-[11px] p-2.5 rounded-[2px] border border-[#2B3139] focus:outline-none focus:border-[#44e092] leading-relaxed resize-none overflow-y-auto"
            />
          </div>
        ) : (
          <div className="flex-1 overflow-y-auto p-3 text-[11px] font-mono leading-relaxed text-[#bbcabd] select-text">
            <pre className="whitespace-pre-wrap break-all font-mono">
              <code>{rawText}</code>
            </pre>
          </div>
        )}
      </div>
    </section>
  );
};

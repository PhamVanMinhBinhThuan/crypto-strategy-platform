import React from 'react';
import { ParsedStrategyData } from '../../../types/aiStrategy';
import { ArrowUpRight, ArrowDownRight, ShieldCheck, Clock, Layers, HelpCircle } from 'lucide-react';
import { cn } from '../../../utils/cn';

export interface ParsedStrategyPanelProps {
  parsedData: ParsedStrategyData | null;
  isAnalyzing?: boolean;
}

export const ParsedStrategyPanel: React.FC<ParsedStrategyPanelProps> = ({
  parsedData,
  isAnalyzing = false,
}) => {
  return (
    <section className="bg-[#1E2329] rounded-[2px] border border-[#2B3139] flex flex-col h-full overflow-hidden select-none">
      {/* Header */}
      <div className="p-3 border-b border-[#2B3139] bg-[#1d2023] flex items-center justify-between shrink-0">
        <h2 className="font-sans text-[11px] font-bold text-[#bbcabd] uppercase tracking-widest flex items-center gap-1.5">
          <Layers className="w-3.5 h-3.5 text-[#f6be16]" />
          <span>Parsed Strategy</span>
        </h2>
        {parsedData && (
          <span className="text-[10px] font-mono text-[#02C076] bg-[#0B0E11] px-1.5 py-0.5 rounded border border-[#02C076]/30">
            {parsedData.longConditions.length + parsedData.shortConditions.length} Conditions
          </span>
        )}
      </div>

      {/* Body */}
      <div className="flex-1 overflow-y-auto p-3.5 flex flex-col gap-3">
        {!parsedData ? (
          <div className="flex-1 flex flex-col items-center justify-center text-center p-6 text-[#848E9C]">
            <HelpCircle className="w-8 h-8 text-[#2B3139] mb-2" />
            <div className="font-mono text-[13px] text-[#bbcabd] mb-1">
              No strategy analyzed yet
            </div>
            <p className="text-[11px] font-sans max-w-[200px] leading-relaxed">
              Enter a prompt or import a strategy URL on the left and click "Analyze with AI".
            </p>
          </div>
        ) : (
          <>
            {/* 1. LONG Conditions Card */}
            <div className="bg-[#0B0E11] rounded-[2px] border border-[#2B3139] p-3 border-l-2 border-l-[#02C076] flex flex-col gap-1.5">
              <div className="flex items-center justify-between">
                <span className="font-sans text-[11px] font-bold text-[#02C076] flex items-center gap-1 uppercase tracking-wider">
                  <ArrowUpRight className="w-3.5 h-3.5" />
                  <span>LONG Conditions</span>
                </span>
                <span className="text-[10px] font-mono text-[#848E9C]">
                  {parsedData.longConditions.length > 0 ? `${parsedData.longConditions.length} rule(s)` : 'None'}
                </span>
              </div>

              {parsedData.longConditions.length > 0 ? (
                <ul className="space-y-1 mt-0.5">
                  {parsedData.longConditions.map((cond, idx) => (
                    <li key={idx} className="font-mono text-[12px] text-[#e1e2e7] flex items-start gap-1.5 leading-snug">
                      <span className="text-[#02C076] font-bold">•</span>
                      <span>{cond}</span>
                    </li>
                  ))}
                </ul>
              ) : (
                <div className="text-[11px] font-sans text-[#848E9C] italic">
                  No LONG conditions defined
                </div>
              )}
            </div>

            {/* 2. SHORT Conditions Card */}
            <div className="bg-[#0B0E11] rounded-[2px] border border-[#2B3139] p-3 border-l-2 border-l-[#CF304A] flex flex-col gap-1.5">
              <div className="flex items-center justify-between">
                <span className="font-sans text-[11px] font-bold text-[#CF304A] flex items-center gap-1 uppercase tracking-wider">
                  <ArrowDownRight className="w-3.5 h-3.5" />
                  <span>SHORT Conditions</span>
                </span>
                <span className="text-[10px] font-mono text-[#848E9C]">
                  {parsedData.hasShortConditions ? `${parsedData.shortConditions.length} rule(s)` : '0 rules'}
                </span>
              </div>

              {parsedData.hasShortConditions ? (
                <ul className="space-y-1 mt-0.5">
                  {parsedData.shortConditions.map((cond, idx) => (
                    <li key={idx} className="font-mono text-[12px] text-[#e1e2e7] flex items-start gap-1.5 leading-snug">
                      <span className="text-[#CF304A] font-bold">•</span>
                      <span>{cond}</span>
                    </li>
                  ))}
                </ul>
              ) : (
                <div className="text-[11px] font-sans text-[#848E9C] italic">
                  No SHORT conditions defined
                </div>
              )}
            </div>

            {/* 3. Risk Management Card */}
            <div className="bg-[#0B0E11] rounded-[2px] border border-[#2B3139] p-3 border-l-2 border-l-[#f6be16] flex flex-col gap-1.5">
              <div className="flex items-center justify-between">
                <span className="font-sans text-[11px] font-bold text-[#f6be16] flex items-center gap-1 uppercase tracking-wider">
                  <ShieldCheck className="w-3.5 h-3.5" />
                  <span>Risk Management</span>
                </span>
              </div>

              <div className="space-y-1 mt-0.5 font-mono text-[12px]">
                <div className="flex items-center gap-1.5">
                  <span className="text-[#848E9C]">• Stop Loss:</span>
                  <span
                    className={cn(
                      'font-semibold',
                      parsedData.hasStopLoss ? 'text-[#02C076]' : 'text-[#848E9C] italic'
                    )}
                  >
                    {parsedData.stopLoss}
                  </span>
                </div>
                <div className="flex items-center gap-1.5">
                  <span className="text-[#848E9C]">• Take Profit:</span>
                  <span
                    className={cn(
                      'font-semibold',
                      parsedData.hasTakeProfit ? 'text-[#02C076]' : 'text-[#848E9C] italic'
                    )}
                  >
                    {parsedData.takeProfit}
                  </span>
                </div>
              </div>
            </div>

            {/* 4. Timeframe & Market Applicability Grid */}
            <div className="grid grid-cols-2 gap-2">
              {/* Timeframe */}
              <div className="bg-[#0B0E11] rounded-[2px] border border-[#2B3139] p-2.5 flex flex-col gap-1">
                <span className="font-sans text-[10px] font-bold text-[#848E9C] flex items-center gap-1 uppercase tracking-wider">
                  <Clock className="w-3 h-3 text-[#44e092]" />
                  <span>Timeframe</span>
                </span>
                <div className="font-mono text-[12px] text-[#e1e2e7] font-semibold flex items-center gap-1">
                  <span>• {parsedData.timeframe}</span>
                  {parsedData.isTimeframeDefault && (
                    <span className="text-[10px] text-[#848E9C] font-normal font-sans">(default)</span>
                  )}
                </div>
              </div>

              {/* Market */}
              <div className="bg-[#0B0E11] rounded-[2px] border border-[#2B3139] p-2.5 flex flex-col gap-1">
                <span className="font-sans text-[10px] font-bold text-[#848E9C] flex items-center gap-1 uppercase tracking-wider">
                  <Layers className="w-3 h-3 text-[#67fdac]" />
                  <span>Market</span>
                </span>
                <div className="font-mono text-[12px] text-[#e1e2e7] font-semibold flex items-center gap-1">
                  <span>• {parsedData.market}</span>
                  {parsedData.isMarketDefault && (
                    <span className="text-[10px] text-[#848E9C] font-normal font-sans"></span>
                  )}
                </div>
              </div>
            </div>
          </>
        )}
      </div>
    </section>
  );
};

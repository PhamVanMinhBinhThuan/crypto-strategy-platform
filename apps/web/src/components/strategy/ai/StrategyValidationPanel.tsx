import React from 'react';
import { ValidationSummary } from '../../../types/aiStrategy';
import { CheckCircle2, AlertTriangle, XCircle, Shield, Check, X, Clock } from 'lucide-react';
import { cn } from '../../../utils/cn';

export interface StrategyValidationPanelProps {
  validation: ValidationSummary | null;
  hasStrategy: boolean;
}

export const StrategyValidationPanel: React.FC<StrategyValidationPanelProps> = ({
  validation,
  hasStrategy,
}) => {
  return (
    <section className="bg-[#1E2329] rounded-[2px] border border-[#2B3139] flex flex-col h-full overflow-hidden select-none">
      {/* Header */}
      <div className="p-3 border-b border-[#2B3139] bg-[#1d2023] flex items-center justify-between shrink-0">
        <h2 className="font-sans text-[11px] font-bold text-[#bbcabd] uppercase tracking-widest flex items-center gap-1.5">
          <Shield className="w-3.5 h-3.5 text-[#02C076]" />
          <span>Validation</span>
        </h2>
        {hasStrategy && validation && (
          <span
            className={cn(
              'text-[10px] font-mono px-1.5 py-0.5 rounded border flex items-center gap-1 font-bold',
              validation.isValid
                ? 'bg-[#02C076]/10 text-[#02C076] border-[#02C076]/30'
                : 'bg-[#CF304A]/10 text-[#CF304A] border-[#CF304A]/30'
            )}
          >
            {validation.isValid ? (
              <>
                <Check className="w-3 h-3" />
                <span>Valid</span>
              </>
            ) : (
              <>
                <X className="w-3 h-3" />
                <span>Errors</span>
              </>
            )}
          </span>
        )}
      </div>

      {/* Body */}
      <div className="flex-1 overflow-y-auto p-3.5 flex flex-col justify-between gap-3">
        {!hasStrategy || !validation ? (
          <div className="flex-1 flex flex-col items-center justify-center text-center p-6 text-[#848E9C]">
            <Clock className="w-8 h-8 text-[#2B3139] mb-2" />
            <div className="font-mono text-[13px] text-[#bbcabd] mb-1">
              Waiting for strategy
            </div>
            <p className="text-[11px] font-sans max-w-[200px] leading-relaxed">
              Validation rules and engine verification will execute automatically.
            </p>
          </div>
        ) : (
          <>
            {/* Validation Rows */}
            <div className="space-y-2.5">
              {validation.items.map((item) => {
                const isSuccess = item.status === 'success';
                const isWarning = item.status === 'warning';
                const isError = item.status === 'error';

                return (
                  <div
                    key={item.id}
                    className={cn(
                      'p-2.5 rounded-[2px] border flex items-start gap-2.5 transition-colors',
                      isSuccess && 'bg-[#0B0E11] border-[#2B3139]',
                      isWarning && 'bg-[#f6be16]/5 border-[#f6be16]/30',
                      isError && 'bg-[#CF304A]/5 border-[#CF304A]/30'
                    )}
                  >
                    {/* Status Icon */}
                    <div className="mt-0.5 shrink-0">
                      {isSuccess && <CheckCircle2 className="w-4 h-4 text-[#02C076]" />}
                      {isWarning && <AlertTriangle className="w-4 h-4 text-[#f6be16]" />}
                      {isError && <XCircle className="w-4 h-4 text-[#CF304A]" />}
                    </div>

                    {/* Content */}
                    <div className="flex-1 min-w-0">
                      <div className="font-sans text-[11px] font-bold text-[#e1e2e7] flex items-center justify-between">
                        <span>{item.label}</span>
                        <span
                          className={cn(
                            'font-mono text-[9px] uppercase tracking-wider px-1 py-0.2 rounded',
                            isSuccess && 'text-[#02C076] bg-[#02C076]/10',
                            isWarning && 'text-[#f6be16] bg-[#f6be16]/10',
                            isError && 'text-[#CF304A] bg-[#CF304A]/10'
                          )}
                        >
                          {item.status}
                        </span>
                      </div>
                      <p
                        className={cn(
                          'text-[11px] font-mono mt-0.5 leading-snug',
                          isSuccess && 'text-[#848E9C]',
                          isWarning && 'text-[#f6be16]',
                          isError && 'text-[#ffb3b6]'
                        )}
                      >
                        {item.message}
                      </p>
                    </div>
                  </div>
                );
              })}
            </div>

            {/* Bottom Readiness Banner */}
            <div
              className={cn(
                'p-3 rounded-[2px] border flex items-center gap-2.5 mt-auto shrink-0',
                validation.isValid
                  ? 'bg-[#02C076]/10 border-[#02C076]/40 text-[#02C076]'
                  : 'bg-[#CF304A]/10 border-[#CF304A]/40 text-[#CF304A]'
              )}
            >
              {validation.isValid ? (
                <CheckCircle2 className="w-5 h-5 text-[#02C076] shrink-0" />
              ) : (
                <XCircle className="w-5 h-5 text-[#CF304A] shrink-0" />
              )}
              <div className="min-w-0">
                <div className="font-mono text-[12px] font-bold uppercase tracking-wider">
                  {validation.overallStatus}
                </div>
                <div className="text-[11px] font-sans text-[#e1e2e7] leading-tight mt-0.5">
                  {validation.overallMessage}
                </div>
              </div>
            </div>
          </>
        )}
      </div>
    </section>
  );
};

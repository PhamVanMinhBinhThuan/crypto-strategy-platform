import React from 'react';
import { cn, formatPercent } from '../../utils/cn';

export interface MetricBoxProps {
  label: string;
  value: string | number;
  delta?: number;
  deltaSuffix?: string;
  subtext?: string;
  icon?: React.ReactNode;
  variant?: 'default' | 'cyan' | 'emerald' | 'rose';
  className?: string;
}

export const MetricBox: React.FC<MetricBoxProps> = ({
  label,
  value,
  delta,
  deltaSuffix = '',
  subtext,
  icon,
  variant = 'default',
  className,
}) => {
  const isPositive = delta !== undefined && delta > 0;
  const isNegative = delta !== undefined && delta < 0;

  return (
    <div
      className={cn(
        'p-3.5 rounded-lg border border-slate-800 bg-slate-900/60 flex flex-col justify-between',
        className
      )}
    >
      <div className="flex items-center justify-between gap-2 text-xs font-medium text-slate-400">
        <span>{label}</span>
        {icon && <span className="text-slate-500">{icon}</span>}
      </div>

      <div className="my-1.5 flex items-baseline justify-between gap-2">
        <span className="text-xl font-bold font-mono tracking-tight text-slate-100 tabular-nums">
          {value}
        </span>
        {delta !== undefined && (
          <span
            className={cn(
              'text-xs font-mono font-semibold px-1.5 py-0.5 rounded',
              isPositive && 'text-emerald-400 bg-emerald-950/60 border border-emerald-800/40',
              isNegative && 'text-rose-400 bg-rose-950/60 border border-rose-800/40',
              !isPositive && !isNegative && 'text-slate-400 bg-slate-800'
            )}
          >
            {formatPercent(delta)}
            {deltaSuffix}
          </span>
        )}
      </div>

      {subtext && <div className="text-[11px] text-slate-500 truncate">{subtext}</div>}
    </div>
  );
};

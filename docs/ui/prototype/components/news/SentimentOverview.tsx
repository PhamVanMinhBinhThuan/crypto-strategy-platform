import React from 'react';
import { TrendingUp, TrendingDown, Minus } from 'lucide-react';
import { cn } from '../../utils/cn';

export interface SentimentOverviewProps {
  score: number;
  label: string;
  positivePct: number;
  neutralPct: number;
  negativePct: number;
}

export const SentimentOverview: React.FC<SentimentOverviewProps> = ({
  score,
  label,
  positivePct,
  neutralPct,
  negativePct,
}) => {
  const formattedScore = score > 0 ? `+${score.toFixed(2)}` : score.toFixed(2);
  const isPositive = score > 0.15;
  const isNegative = score < -0.15;

  return (
    <div className="p-4 rounded-lg flex flex-col sm:flex-row gap-4 sm:gap-6 bg-[#191c1f] border border-[#2B3139] select-none">
      {/* Score / Gauge Summary */}
      <div className="sm:w-48 flex flex-col items-center justify-center sm:border-r border-[#2B3139] sm:pr-6 py-1">
        <div className="font-sans text-[11px] font-bold tracking-wider text-[#869488] mb-1.5 uppercase">
          MARKET SENTIMENT
        </div>
        <div
          className={cn(
            'text-3xl font-mono font-bold tracking-tight',
            isPositive
              ? 'text-[#02c076]'
              : isNegative
              ? 'text-[#f84b4b]'
              : 'text-[#848E9C]'
          )}
        >
          {formattedScore}
        </div>
        <div
          className={cn(
            'font-mono text-xs font-semibold mt-1 tracking-wide',
            isPositive
              ? 'text-[#02c076]'
              : isNegative
              ? 'text-[#f84b4b]'
              : 'text-[#848E9C]'
          )}
        >
          {label}
        </div>
      </div>

      {/* Breakdown Cards */}
      <div className="flex-1 grid grid-cols-3 gap-3 sm:gap-4">
        {/* Positive Card */}
        <div className="bg-[#161a1f] border border-[#2B3139] p-3 rounded flex flex-col justify-between">
          <div className="flex justify-between items-center mb-2">
            <span className="font-sans text-xs text-[#869488]">Positive</span>
            <TrendingUp className="w-4 h-4 text-[#02c076]" />
          </div>
          <div className="font-mono text-lg font-semibold text-[#e1e2e7]">
            {positivePct}%
          </div>
        </div>

        {/* Neutral Card */}
        <div className="bg-[#161a1f] border border-[#2B3139] p-3 rounded flex flex-col justify-between">
          <div className="flex justify-between items-center mb-2">
            <span className="font-sans text-xs text-[#869488]">Neutral</span>
            <Minus className="w-4 h-4 text-[#848E9C]" />
          </div>
          <div className="font-mono text-lg font-semibold text-[#e1e2e7]">
            {neutralPct}%
          </div>
        </div>

        {/* Negative Card */}
        <div className="bg-[#161a1f] border border-[#2B3139] p-3 rounded flex flex-col justify-between">
          <div className="flex justify-between items-center mb-2">
            <span className="font-sans text-xs text-[#869488]">Negative</span>
            <TrendingDown className="w-4 h-4 text-[#f84b4b]" />
          </div>
          <div className="font-mono text-lg font-semibold text-[#e1e2e7]">
            {negativePct}%
          </div>
        </div>
      </div>
    </div>
  );
};

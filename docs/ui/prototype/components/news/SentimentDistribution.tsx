import React from 'react';

export interface SentimentDistributionProps {
  positivePct: number;
  neutralPct: number;
  negativePct: number;
}

export const SentimentDistribution: React.FC<SentimentDistributionProps> = ({
  positivePct,
  neutralPct,
  negativePct,
}) => {
  return (
    <div className="p-3 rounded-lg flex items-center gap-3 bg-[#191c1f] border border-[#2B3139] select-none">
      <span className="font-sans text-[11px] font-bold tracking-wider text-[#869488] w-12 sm:w-16 uppercase">
        DIST
      </span>
      <div className="flex-1 h-2 rounded-full overflow-hidden flex bg-[#161a1f] border border-[#2B3139]/40">
        <div
          className="bg-[#02c076] h-full transition-all duration-500"
          style={{ width: `${positivePct}%` }}
          title={`Positive: ${positivePct}%`}
        />
        <div
          className="bg-[#4a5568] h-full transition-all duration-500"
          style={{ width: `${neutralPct}%` }}
          title={`Neutral: ${neutralPct}%`}
        />
        <div
          className="bg-[#f84b4b] h-full transition-all duration-500"
          style={{ width: `${negativePct}%` }}
          title={`Negative: ${negativePct}%`}
        />
      </div>
    </div>
  );
};

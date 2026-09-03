import React from 'react';
import { NewsTopic, NewsTimeRange } from '../../types/news';

export interface NewsAnalyticsProps {
  timeRange: NewsTimeRange;
  analyzedCount: number;
  positiveCount: number;
  neutralCount: number;
  negativeCount: number;
  topics: NewsTopic[];
}

export const NewsAnalytics: React.FC<NewsAnalyticsProps> = ({
  timeRange,
  analyzedCount,
  positiveCount,
  neutralCount,
  negativeCount,
  topics,
}) => {
  return (
    <div className="p-4 rounded-lg flex flex-col gap-4 bg-[#191c1f] border border-[#2B3139] select-none">
      <h3 className="font-sans text-[11px] font-bold tracking-wider text-[#869488] border-b border-[#2B3139] pb-2 uppercase">
        {timeRange} STATISTICS
      </h3>

      {/* Grid of Stats Cards */}
      <div className="grid grid-cols-2 gap-3">
        <div className="bg-[#161a1f] p-2.5 rounded border border-[#2B3139]">
          <div className="font-mono text-xs text-[#869488] mb-1">Analyzed</div>
          <div className="font-mono text-sm font-semibold text-[#e1e2e7]">
            {analyzedCount}
          </div>
        </div>

        <div className="bg-[#161a1f] p-2.5 rounded border border-[#2B3139] border-t-2 border-t-[#02C076]">
          <div className="font-mono text-xs text-[#869488] mb-1">Positive</div>
          <div className="font-mono text-sm font-semibold text-[#02C076]">
            {positiveCount}
          </div>
        </div>

        <div className="bg-[#161a1f] p-2.5 rounded border border-[#2B3139] border-t-2 border-t-[#848E9C]">
          <div className="font-mono text-xs text-[#869488] mb-1">Neutral</div>
          <div className="font-mono text-sm font-semibold text-[#848E9C]">
            {neutralCount}
          </div>
        </div>

        <div className="bg-[#161a1f] p-2.5 rounded border border-[#2B3139] border-t-2 border-t-[#f84b4b]">
          <div className="font-mono text-xs text-[#869488] mb-1">Negative</div>
          <div className="font-mono text-sm font-semibold text-[#f84b4b]">
            {negativeCount}
          </div>
        </div>
      </div>

      {/* Topics Detected */}
      <h3 className="font-sans text-[11px] font-bold tracking-wider text-[#869488] border-b border-[#2B3139] pb-2 mt-1 uppercase">
        TOPICS DETECTED
      </h3>

      <ul className="space-y-2">
        {topics.map((topic) => (
          <li key={topic.name} className="flex justify-between items-center text-xs">
            <span className="font-sans text-[#e1e2e7] font-medium truncate max-w-[180px]">
              {topic.name}
            </span>
            <span className="font-mono text-xs text-[#869488] bg-[#2B3139] px-1.5 py-0.5 rounded">
              {topic.count}
            </span>
          </li>
        ))}
      </ul>
    </div>
  );
};

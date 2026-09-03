import React from 'react';
import { NewsItem, NewsSentimentFilter } from '../../types/news';
import { cn } from '../../utils/cn';

export interface NewsFeedTableProps {
  articles: NewsItem[];
  sentimentFilter: NewsSentimentFilter;
  onSelectSentimentFilter: (filter: NewsSentimentFilter) => void;
}

const FILTER_BUTTONS: { id: NewsSentimentFilter; label: string }[] = [
  { id: 'ALL', label: 'ALL' },
  { id: 'POSITIVE', label: 'POS' },
  { id: 'NEUTRAL', label: 'NEU' },
  { id: 'NEGATIVE', label: 'NEG' },
];

export const NewsFeedTable: React.FC<NewsFeedTableProps> = ({
  articles,
  sentimentFilter,
  onSelectSentimentFilter,
}) => {
  return (
    <div className="flex-1 rounded-lg flex flex-col min-h-[320px] bg-[#191c1f] border border-[#2B3139] overflow-hidden select-none">
      {/* Table Header Controls */}
      <div className="p-3 border-b border-[#2B3139] flex justify-between items-center bg-[#1E2329]">
        <h3 className="font-sans text-[11px] font-bold tracking-wider text-[#869488] uppercase">
          LATEST ARTICLES
        </h3>
        <div className="flex gap-1.5">
          {FILTER_BUTTONS.map((btn) => {
            const isActive = sentimentFilter === btn.id;
            return (
              <button
                key={btn.id}
                type="button"
                onClick={() => onSelectSentimentFilter(btn.id)}
                className={cn(
                  'px-2.5 py-0.5 text-xs font-mono rounded border transition-colors cursor-pointer',
                  isActive
                    ? 'border-[#02C076] text-[#02C076] bg-[#02C076]/10 font-bold'
                    : 'border-[#2B3139] text-[#e1e2e7] bg-transparent hover:bg-[#2B3139]'
                )}
              >
                {btn.label}
              </button>
            );
          })}
        </div>
      </div>

      {/* Table Body */}
      <div className="flex-1 overflow-x-auto overflow-y-auto max-h-[360px]">
        <table className="w-full text-left border-collapse">
          <thead className="bg-[#161a1f] sticky top-0 z-10 border-b border-[#2B3139]">
            <tr>
              <th className="py-2 px-3 font-sans text-[11px] font-medium text-[#869488] tracking-wider uppercase w-20">
                TIME
              </th>
              <th className="py-2 px-3 font-sans text-[11px] font-medium text-[#869488] tracking-wider uppercase">
                HEADLINE
              </th>
              <th className="py-2 px-3 font-sans text-[11px] font-medium text-[#869488] tracking-wider uppercase w-28">
                SOURCE
              </th>
              <th className="py-2 px-3 font-sans text-[11px] font-medium text-[#869488] tracking-wider uppercase text-center w-28">
                SENTIMENT
              </th>
              <th className="py-2 px-3 font-sans text-[11px] font-medium text-[#869488] tracking-wider uppercase text-right w-20">
                CONF.
              </th>
            </tr>
          </thead>
          <tbody className="divide-y divide-[#2B3139]">
            {articles.length === 0 ? (
              <tr>
                <td
                  colSpan={5}
                  className="py-8 text-center font-mono text-xs text-[#869488]"
                >
                  No articles found matching the current filters.
                </td>
              </tr>
            ) : (
              articles.map((item) => {
                const isPos = item.sentiment === 'POSITIVE';
                const isNeg = item.sentiment === 'NEGATIVE';

                return (
                  <tr
                    key={item.id}
                    className="h-10 hover:bg-[#2B3139]/40 transition-colors group"
                  >
                    {/* TIME */}
                    <td className="py-1 px-3 font-mono text-xs text-[#869488] whitespace-nowrap">
                      {item.time}
                    </td>

                    {/* HEADLINE */}
                    <td className="py-1 px-3 font-sans text-xs text-[#e1e2e7] group-hover:text-white transition-colors">
                      <div className="line-clamp-1 max-w-xl font-medium">
                        {item.title}
                      </div>
                    </td>

                    {/* SOURCE */}
                    <td className="py-1 px-3 font-mono text-xs text-[#869488] whitespace-nowrap">
                      {item.source}
                    </td>

                    {/* SENTIMENT BADGE */}
                    <td className="py-1 px-3 text-center whitespace-nowrap">
                      {isPos ? (
                        <span className="inline-block px-2 py-0.5 bg-[#02C076]/10 text-[#02C076] border border-[#02C076]/30 rounded text-[10px] font-bold font-mono tracking-wider">
                          POSITIVE
                        </span>
                      ) : isNeg ? (
                        <span className="inline-block px-2 py-0.5 bg-[#f84b4b]/10 text-[#f84b4b] border border-[#f84b4b]/30 rounded text-[10px] font-bold font-mono tracking-wider">
                          NEGATIVE
                        </span>
                      ) : (
                        <span className="inline-block px-2 py-0.5 bg-[#848E9C]/10 text-[#848E9C] border border-[#848E9C]/30 rounded text-[10px] font-bold font-mono tracking-wider">
                          NEUTRAL
                        </span>
                      )}
                    </td>

                    {/* CONFIDENCE */}
                    <td className="py-1 px-3 font-mono text-xs text-[#e1e2e7] text-right whitespace-nowrap">
                      {item.confidence.toFixed(2)}
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
};

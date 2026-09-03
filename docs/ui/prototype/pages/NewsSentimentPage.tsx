import React, { useMemo } from 'react';
import { RoutePath } from '../types';
import { MOCK_NEWS_ITEMS, MOCK_STATS_BY_COIN } from '../data/mockNewsData';
import { NewsSentimentHeader } from '../components/news/NewsSentimentHeader';
import { SentimentOverview } from '../components/news/SentimentOverview';
import { SentimentDistribution } from '../components/news/SentimentDistribution';
import { SentimentTrendChart } from '../components/news/SentimentTrendChart';
import { NewsFeedTable } from '../components/news/NewsFeedTable';
import { NewsAnalytics } from '../components/news/NewsAnalytics';
import { SentimentStrategyPanel } from '../components/news/SentimentStrategyPanel';
import { useApp } from '../context/AppContext';

export interface NewsSentimentPageProps {
  onNavigate?: (route: RoutePath) => void;
}

export const NewsSentimentPage: React.FC<NewsSentimentPageProps> = ({
  onNavigate = () => {},
}) => {
  const { newsState, setNewsSelectedCoin, setNewsTimeRange, setNewsSentimentFilter } = useApp();
  const { selectedCoin, timeRange, sentimentFilter } = newsState;

  // Stats data according to selected coin
  const stats = useMemo(() => {
    return MOCK_STATS_BY_COIN[selectedCoin] || MOCK_STATS_BY_COIN.BTC;
  }, [selectedCoin]);

  // Filtered articles
  const filteredArticles = useMemo(() => {
    return MOCK_NEWS_ITEMS.filter((item) => {
      // Coin filter
      if (selectedCoin !== 'ALL' && item.coin !== 'ALL' && item.coin !== selectedCoin) {
        return false;
      }
      // Sentiment filter
      if (sentimentFilter !== 'ALL' && item.sentiment !== sentimentFilter) {
        return false;
      }
      return true;
    });
  }, [selectedCoin, sentimentFilter]);

  return (
    <div className="flex-1 flex flex-col h-full bg-[#0b0e11] overflow-y-auto p-3 sm:p-4 select-none">
      {/* 1. Page Header */}
      <NewsSentimentHeader
        activeCoin={selectedCoin}
        timeRange={timeRange}
        onSelectCoin={setNewsSelectedCoin}
        onSelectTimeRange={setNewsTimeRange}
      />

      {/* 2. Main Grid Layout */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-3 flex-1 pb-4">
        {/* Left / Center Column (Main Data - 9 cols) */}
        <div className="lg:col-span-9 flex flex-col gap-3">
          {/* Sentiment Overview Panel */}
          <SentimentOverview
            score={stats.score}
            label={stats.label}
            positivePct={stats.positivePct}
            neutralPct={stats.neutralPct}
            negativePct={stats.negativePct}
          />

          {/* Sentiment Distribution Bar */}
          <SentimentDistribution
            positivePct={stats.positivePct}
            neutralPct={stats.neutralPct}
            negativePct={stats.negativePct}
          />

          {/* Trend Chart */}
          <SentimentTrendChart
            trendPoints={stats.trendPoints}
            timeRange={timeRange}
          />

          {/* News Feed Table */}
          <NewsFeedTable
            articles={filteredArticles}
            sentimentFilter={sentimentFilter}
            onSelectSentimentFilter={setNewsSentimentFilter}
          />
        </div>

        {/* Right Column (Analytics & Strategy - 3 cols) */}
        <div className="lg:col-span-3 flex flex-col gap-3">
          {/* 24H / TimeRange Statistics & Topics */}
          <NewsAnalytics
            timeRange={timeRange}
            analyzedCount={stats.analyzedCount}
            positiveCount={stats.positiveCount}
            neutralCount={stats.neutralCount}
            negativeCount={stats.negativeCount}
            topics={stats.topics}
          />

          {/* Strategy Integration Card */}
          <SentimentStrategyPanel
            onNavigate={onNavigate}
            avgSentimentScore={stats.score}
          />
        </div>
      </div>
    </div>
  );
};

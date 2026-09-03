export type SentimentType = 'POSITIVE' | 'NEUTRAL' | 'NEGATIVE';

export type NewsCoinFilter = 'ALL' | 'BTC' | 'ETH' | 'SOL';
export type NewsTimeRange = '1H' | '24H' | '7D' | '30D';
export type NewsSentimentFilter = 'ALL' | 'POSITIVE' | 'NEUTRAL' | 'NEGATIVE';

export interface NewsItem {
  id: string;
  time: string; // e.g. '08:15'
  title: string;
  source: string;
  sentiment: SentimentType;
  confidence: number; // e.g. 0.82
  coin: 'BTC' | 'ETH' | 'SOL' | 'ALL';
  url?: string;
  summary?: string;
}

export interface SentimentTrendPoint {
  timeLabel: string;
  score: number; // -1.0 to +1.0
  eventMarker?: {
    title: string;
    sentiment: SentimentType;
    type: string;
  };
}

export interface NewsTopic {
  name: string;
  count: number;
}

export interface CoinSentimentStats {
  score: number;
  label: string;
  positivePct: number;
  neutralPct: number;
  negativePct: number;
  analyzedCount: number;
  positiveCount: number;
  neutralCount: number;
  negativeCount: number;
  topics: NewsTopic[];
  trendPoints: SentimentTrendPoint[];
}

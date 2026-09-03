export type RoutePath = '/market' | '/strategy' | '/backtest' | '/search' | '/news';

export type ScreenId = 
  | 'market-dashboard'
  | 'strategy-composer'
  | 'backtest-results'
  | 'search-leaderboard'
  | 'news-sentiment';

export type Timeframe = '1m' | '5m' | '15m' | '30m' | '1h' | '2h' | '4h' | '1d';

export type IndicatorType = 'MA' | 'BB' | 'RSI' | 'S/R';

export interface IndicatorState {
  MA: boolean;
  BB: boolean;
  RSI: boolean;
  'S/R': boolean;
}

export interface Candle {
  time: string;
  timestamp: number;
  open: number;
  high: number;
  low: number;
  close: number;
  volume: number;
}

export type CandleData = Candle;

export interface TradeMarker {
  type: 'BUY' | 'SELL';
  price: number;
  time: string;
  index: number;
}

export interface MarketChartConfig {
  id: string;
  symbol: string;
  timeframe: Timeframe;
  indicators: IndicatorState;
}

export interface CryptoAsset {
  symbol: string;
  name: string;
  price: number;
  change24h: number;
  high24h: number;
  low24h: number;
  volume24h: number;
  marketCap: number;
  sparkline: number[];
}

export interface IndicatorConfig {
  id: string;
  name: string;
  type: 'RSI' | 'MACD' | 'EMA' | 'SMA' | 'BollingerBands' | 'ATR' | 'Supertrend' | 'SentimentSignal';
  params: Record<string, number | string>;
  timeframe: Timeframe;
  active: boolean;
}

export interface StrategyRule {
  id: string;
  type: 'entry' | 'exit' | 'stop_loss' | 'take_profit';
  indicatorId: string;
  operator: '>' | '<' | '>=' | '<=' | 'crosses_above' | 'crosses_below' | 'equals';
  threshold: number | string;
  action: 'BUY' | 'SELL' | 'CLOSE' | 'REDUCE';
}

export interface Strategy {
  id: string;
  name: string;
  description: string;
  author: string;
  version: string;
  targetPair: string;
  baseTimeframe: Timeframe;
  timeframes: Timeframe[];
  indicators: IndicatorConfig[];
  rules: StrategyRule[];
  tags: string[];
  createdAt: string;
  updatedAt: string;
}

export interface BacktestTrade {
  id: string;
  timestamp: string;
  pair: string;
  side: 'BUY' | 'SELL';
  entryPrice: number;
  exitPrice: number;
  pnl: number;
  pnlPercent: number;
  duration: string;
  reason: string;
}

export interface BacktestResultMetrics {
  totalReturn: number;
  annualizedReturn: number;
  sharpeRatio: number;
  sortinoRatio: number;
  maxDrawdown: number;
  winRate: number;
  profitFactor: number;
  totalTrades: number;
  winningTrades: number;
  losingTrades: number;
  averageTradePnl: number;
  calmarRatio: number;
  benchmarkReturn: number;
}

export interface BacktestReport {
  id: string;
  strategyId: string;
  strategyName: string;
  pair: string;
  timeframe: Timeframe;
  startDate: string;
  endDate: string;
  initialCapital: number;
  finalEquity: number;
  metrics: BacktestResultMetrics;
  equityCurve: { date: string; equity: number; benchmark: number; drawdown: number }[];
  trades: BacktestTrade[];
}

export interface LeaderboardEntry {
  rank: number;
  id: string;
  name: string;
  author: string;
  pair: string;
  timeframes: Timeframe[];
  sharpeRatio: number;
  totalReturn: number;
  maxDrawdown: number;
  winRate: number;
  tradesCount: number;
  rating: number;
  isFavorited?: boolean;
}

export interface NewsItem {
  id: string;
  title: string;
  source: string;
  url: string;
  publishedAt: string;
  relatedCoins: string[];
  sentiment: 'BULLISH' | 'BEARISH' | 'NEUTRAL';
  sentimentScore: number;
  impact: 'HIGH' | 'MEDIUM' | 'LOW';
  summary: string;
}

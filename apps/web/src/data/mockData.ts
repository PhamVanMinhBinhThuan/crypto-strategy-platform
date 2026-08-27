import { CryptoAsset, Strategy, BacktestReport, LeaderboardEntry, NewsItem, CandleData } from '../types';

export const MOCK_ASSETS: CryptoAsset[] = [
  {
    symbol: 'BTC/USDT',
    name: 'Bitcoin',
    price: 68420.50,
    change24h: 3.42,
    high24h: 69150.00,
    low24h: 66010.20,
    volume24h: 28450120900,
    marketCap: 1345980000000,
    sparkline: [66200, 66450, 65900, 66800, 67200, 67900, 68420.5],
  },
  {
    symbol: 'ETH/USDT',
    name: 'Ethereum',
    price: 3512.80,
    change24h: 4.18,
    high24h: 3580.00,
    low24h: 3340.50,
    volume24h: 15210400500,
    marketCap: 422150000000,
    sparkline: [3360, 3390, 3420, 3380, 3450, 3490, 3512.8],
  },
  {
    symbol: 'SOL/USDT',
    name: 'Solana',
    price: 184.65,
    change24h: -1.25,
    high24h: 192.40,
    low24h: 181.10,
    volume24h: 5620800100,
    marketCap: 85200000000,
    sparkline: [188, 189, 186, 187, 184, 182, 184.65],
  },
  {
    symbol: 'BNB/USDT',
    name: 'BNB',
    price: 588.20,
    change24h: 0.85,
    high24h: 594.00,
    low24h: 579.50,
    volume24h: 1420500800,
    marketCap: 87400000000,
    sparkline: [582, 584, 581, 585, 586, 587, 588.2],
  },
  {
    symbol: 'AVAX/USDT',
    name: 'Avalanche',
    price: 32.45,
    change24h: 5.62,
    high24h: 33.10,
    low24h: 30.50,
    volume24h: 680400200,
    marketCap: 12900000000,
    sparkline: [30.8, 31.0, 30.6, 31.4, 31.8, 32.1, 32.45],
  },
];

export const MOCK_STRATEGIES: Strategy[] = [
  {
    id: 'strat-alpha-momentum',
    name: 'Multi-TF Momentum Breakout (Alpha)',
    description: 'Combines 4H Supertrend alignment with 15m RSI pullback triggers and dynamic ATR trailing stops.',
    author: 'QuantLab Core',
    version: '2.4.0',
    targetPair: 'BTC/USDT',
    baseTimeframe: '15m',
    timeframes: ['15m', '1h', '4h', '1d'],
    indicators: [
      { id: 'ind-1', name: 'RSI (14)', type: 'RSI', params: { period: 14, overbought: 70, oversold: 30 }, timeframe: '15m', active: true },
      { id: 'ind-2', name: 'EMA Ribbon (20/50/200)', type: 'EMA', params: { fast: 20, slow: 50, trend: 200 }, timeframe: '1h', active: true },
      { id: 'ind-3', name: 'Supertrend (10, 3.0)', type: 'Supertrend', params: { period: 10, multiplier: 3.0 }, timeframe: '4h', active: true },
      { id: 'ind-4', name: 'News Sentiment Filter', type: 'SentimentSignal', params: { minScore: 0.25 }, timeframe: '1d', active: true },
    ],
    rules: [
      { id: 'rule-1', type: 'entry', indicatorId: 'ind-1', operator: '<', threshold: 35, action: 'BUY' },
      { id: 'rule-2', type: 'entry', indicatorId: 'ind-3', operator: 'equals', threshold: 'BULLISH', action: 'BUY' },
      { id: 'rule-3', type: 'exit', indicatorId: 'ind-1', operator: '>', threshold: 75, action: 'SELL' },
      { id: 'rule-4', type: 'stop_loss', indicatorId: 'ind-3', operator: 'crosses_below', threshold: 'BAND_LOWER', action: 'CLOSE' },
    ],
    tags: ['Momentum', 'Multi-TF', 'Trend-Following', 'Sentiment-Boosted'],
    createdAt: '2026-06-12',
    updatedAt: '2026-08-15',
  },
  {
    id: 'strat-mean-reversion-sol',
    name: 'Bollinger Mean Reversion & Liquidity Sweep',
    description: 'Exploits high-volatility deviations on SOL with rapid 5m scalping and 1h macro regime filtering.',
    author: 'K-Labs',
    version: '1.8.2',
    targetPair: 'SOL/USDT',
    baseTimeframe: '5m',
    timeframes: ['5m', '15m', '1h'],
    indicators: [
      { id: 'ind-5', name: 'Bollinger Bands (20, 2.0)', type: 'BollingerBands', params: { period: 20, stdDev: 2.0 }, timeframe: '5m', active: true },
      { id: 'ind-6', name: 'ATR Volatility (14)', type: 'ATR', params: { period: 14 }, timeframe: '15m', active: true },
    ],
    rules: [
      { id: 'rule-5', type: 'entry', indicatorId: 'ind-5', operator: '<=', threshold: 'LOWER_BAND', action: 'BUY' },
      { id: 'rule-6', type: 'exit', indicatorId: 'ind-5', operator: '>=', threshold: 'MIDDLE_BAND', action: 'SELL' },
    ],
    tags: ['Mean Reversion', 'Scalp', 'High Frequency'],
    createdAt: '2026-07-01',
    updatedAt: '2026-08-10',
  }
];

export const MOCK_BACKTEST_REPORT: BacktestReport = {
  id: 'bt-report-2026-8821',
  strategyId: 'strat-alpha-momentum',
  strategyName: 'Multi-TF Momentum Breakout (Alpha)',
  pair: 'BTC/USDT',
  timeframe: '15m',
  startDate: '2025-08-01',
  endDate: '2026-08-15',
  initialCapital: 100000,
  finalEquity: 248650.80,
  metrics: {
    totalReturn: 148.65,
    annualizedReturn: 139.20,
    sharpeRatio: 2.84,
    sortinoRatio: 3.92,
    maxDrawdown: -9.45,
    winRate: 64.2,
    profitFactor: 2.45,
    totalTrades: 328,
    winningTrades: 211,
    losingTrades: 117,
    averageTradePnl: 453.20,
    calmarRatio: 14.73,
    benchmarkReturn: 54.10,
  },
  equityCurve: [
    { date: '2025-08', equity: 100000, benchmark: 100000, drawdown: 0 },
    { date: '2025-09', equity: 112400, benchmark: 104200, drawdown: -1.2 },
    { date: '2025-10', equity: 128900, benchmark: 109100, drawdown: -2.4 },
    { date: '2025-11', equity: 124500, benchmark: 105600, drawdown: -5.8 },
    { date: '2025-12', equity: 145000, benchmark: 118400, drawdown: -1.1 },
    { date: '2026-01', equity: 168200, benchmark: 125000, drawdown: -2.0 },
    { date: '2026-02', equity: 182400, benchmark: 131200, drawdown: -3.1 },
    { date: '2026-03', equity: 176500, benchmark: 127800, drawdown: -6.4 },
    { date: '2026-04', equity: 198000, benchmark: 139500, drawdown: -2.2 },
    { date: '2026-05', equity: 214500, benchmark: 144100, drawdown: -1.5 },
    { date: '2026-06', equity: 226800, benchmark: 147800, drawdown: -3.8 },
    { date: '2026-07', equity: 239100, benchmark: 151200, drawdown: -2.0 },
    { date: '2026-08', equity: 248650, benchmark: 154100, drawdown: -1.2 },
  ],
  trades: [
    { id: 'tr-1', timestamp: '2026-08-14 14:30', pair: 'BTC/USDT', side: 'BUY', entryPrice: 67120, exitPrice: 68540, pnl: 4260, pnlPercent: 2.11, duration: '4h 15m', reason: 'TP hit at Upper Bollinger' },
    { id: 'tr-2', timestamp: '2026-08-12 09:15', pair: 'BTC/USDT', side: 'BUY', entryPrice: 65900, exitPrice: 66890, pnl: 2970, pnlPercent: 1.50, duration: '2h 45m', reason: 'RSI overbought exit' },
    { id: 'tr-3', timestamp: '2026-08-10 21:00', pair: 'BTC/USDT', side: 'BUY', entryPrice: 66400, exitPrice: 65880, pnl: -1560, pnlPercent: -0.78, duration: '1h 10m', reason: 'ATR Stop Loss triggered' },
    { id: 'tr-4', timestamp: '2026-08-08 11:45', pair: 'BTC/USDT', side: 'BUY', entryPrice: 64200, exitPrice: 66100, pnl: 5700, pnlPercent: 2.95, duration: '8h 20m', reason: 'Supertrend Trend exhaustion' },
  ],
};

export const MOCK_LEADERBOARD: LeaderboardEntry[] = [
  {
    rank: 1,
    id: 'lb-1',
    name: 'Multi-TF Momentum Breakout (Alpha)',
    author: 'QuantLab Core',
    pair: 'BTC/USDT',
    timeframes: ['15m', '1h', '4h'],
    sharpeRatio: 2.84,
    totalReturn: 148.65,
    maxDrawdown: -9.45,
    winRate: 64.2,
    tradesCount: 328,
    rating: 4.9,
    isFavorited: true,
  },
  {
    rank: 2,
    id: 'lb-2',
    name: 'ETH Sentiment-Volume Divergence',
    author: 'NeuroQuant',
    pair: 'ETH/USDT',
    timeframes: ['1h', '4h'],
    sharpeRatio: 2.61,
    totalReturn: 132.40,
    maxDrawdown: -11.20,
    winRate: 61.8,
    tradesCount: 245,
    rating: 4.8,
    isFavorited: true,
  },
  {
    rank: 3,
    id: 'lb-3',
    name: 'SOL Dynamic ATR Volatility Break',
    author: 'HyperLiquid Alpha',
    pair: 'SOL/USDT',
    timeframes: ['5m', '15m'],
    sharpeRatio: 2.45,
    totalReturn: 119.80,
    maxDrawdown: -14.60,
    winRate: 58.4,
    tradesCount: 512,
    rating: 4.7,
  },
  {
    rank: 4,
    id: 'lb-4',
    name: 'Macro Liquidity Reversal Model',
    author: 'DeepCurve',
    pair: 'BTC/USDT',
    timeframes: ['4h', '1d'],
    sharpeRatio: 2.38,
    totalReturn: 104.50,
    maxDrawdown: -8.10,
    winRate: 67.5,
    tradesCount: 94,
    rating: 4.8,
  },
  {
    rank: 5,
    id: 'lb-5',
    name: 'Cross-Exchange Arbitrage & Mean Reversion',
    author: 'Apex Trading',
    pair: 'BNB/USDT',
    timeframes: ['1m', '5m'],
    sharpeRatio: 2.22,
    totalReturn: 92.10,
    maxDrawdown: -6.40,
    winRate: 72.1,
    tradesCount: 840,
    rating: 4.6,
  },
];

export const MOCK_NEWS: NewsItem[] = [
  {
    id: 'news-1',
    title: 'Institutional Inflows to Spot Bitcoin ETFs Surge Past $1.2B in Weekly Record',
    source: 'Bloomberg Crypto',
    url: '#',
    publishedAt: '18m ago',
    relatedCoins: ['BTC', 'ETH'],
    sentiment: 'BULLISH',
    sentimentScore: 0.88,
    impact: 'HIGH',
    summary: 'Sustained institutional allocation drives open interest and funding rates into positive regime while exchange balances hit 5-year lows.',
  },
  {
    id: 'news-2',
    title: 'Layer 2 Gas Optimization Upgrade Deployed Successfully Across Major Rollups',
    source: 'CoinDesk',
    url: '#',
    publishedAt: '1h ago',
    relatedCoins: ['ETH', 'ARB', 'OP'],
    sentiment: 'BULLISH',
    sentimentScore: 0.65,
    impact: 'MEDIUM',
    summary: 'Transaction fees decreased by 40% while throughput increased, driving on-chain DEX velocity.',
  },
  {
    id: 'news-3',
    title: 'Macro Regulatory Review Committee Announces Working Session on Liquidity Providers',
    source: 'Reuters Financial',
    url: '#',
    publishedAt: '3h ago',
    relatedCoins: ['BTC', 'SOL', 'USDT'],
    sentiment: 'NEUTRAL',
    sentimentScore: 0.05,
    impact: 'MEDIUM',
    summary: 'Standard quarterly consultation with key automated market makers; minimal direct market impact anticipated.',
  },
  {
    id: 'news-4',
    title: 'Derivatives Whale Liquidations Cascade Causes Brief Dip to Support Levels',
    source: 'The Block',
    url: '#',
    publishedAt: '5h ago',
    relatedCoins: ['SOL', 'AVAX'],
    sentiment: 'BEARISH',
    sentimentScore: -0.52,
    impact: 'HIGH',
    summary: 'Over $120M in leveraged longs liquidated in under 30 minutes following rapid basis contraction.',
  },
];

export function generateSampleCandles(count: number = 40, basePrice: number = 68000): CandleData[] {
  const candles: CandleData[] = [];
  let current = basePrice;
  const now = Date.now();
  const step = 15 * 60 * 1000; // 15 mins

  for (let i = count; i >= 0; i--) {
    const timestamp = now - i * step;
    const delta = (Math.random() - 0.48) * (basePrice * 0.008);
    const open = current;
    const close = open + delta;
    const high = Math.max(open, close) + Math.random() * (basePrice * 0.004);
    const low = Math.min(open, close) - Math.random() * (basePrice * 0.004);
    const volume = Math.floor(50 + Math.random() * 400);

    const date = new Date(timestamp);
    const time = `${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`;

    candles.push({ time, timestamp, open, high, low, close, volume });
    current = close;
  }
  return candles;
}

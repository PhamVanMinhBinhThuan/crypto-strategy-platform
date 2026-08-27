import {
  SearchSpaceFeature,
  StopConditions,
  LeaderboardEntry,
  WorkerInfo,
  CandidateEvaluation,
} from '../types/search';
import { CompositeStrategy, SelectedStrategy, calculateCompositeSignal } from '../types/strategy';
import { STRATEGY_LIBRARY } from './strategyLibraryData';

export const INITIAL_SEARCH_FEATURES: SearchSpaceFeature[] = [
  {
    id: 'movingAverage',
    name: 'Moving Average',
    label: 'Moving Average (MA)',
    category: 'Trend',
    enabled: false,
    opacity: 0.7,
  },
  {
    id: 'rsi',
    name: 'RSI',
    label: 'RSI',
    category: 'Momentum',
    enabled: false,
    opacity: 0.7,
  },
  {
    id: 'bollingerBands',
    name: 'Bollinger Bands',
    label: 'Bollinger Bands',
    category: 'Volatility',
    enabled: true,
    opacity: 1,
  },
  {
    id: 'supportResistance',
    name: 'Support / Resistance',
    label: 'Support / Resistance',
    category: 'Price Action',
    enabled: true,
    opacity: 1,
  },
  {
    id: 'newsSentiment',
    name: 'News Sentiment',
    label: 'News Sentiment',
    category: 'NLP Alpha',
    enabled: false,
    opacity: 0.7,
  },
];

export const INITIAL_STOP_CONDITIONS: StopConditions = {
  maxCandidates: 1000,
  noImprovementIters: 50,
  timeLimitHours: 12,
};

export const INITIAL_WORKERS_IDLE: WorkerInfo[] = [
  { id: 'w-1', name: 'Worker_01', state: 'idle' },
  { id: 'w-2', name: 'Worker_02', state: 'idle' },
  { id: 'w-3', name: 'Worker_03', state: 'idle' },
  { id: 'w-4', name: 'Worker_04', state: 'idle' },
];

export const INITIAL_CANDIDATE_IDLE: CandidateEvaluation = {
  id: 'cand-none',
  candidateNumber: 0,
  name: 'None / Waiting',
  categoryTag: 'Pending',
  status: 'idle',
  progress: 0,
  currentScore: 0,
};

export function createLeaderboardStrategy(
  strategyId: string,
  version: string,
  displayName: string,
  moduleSpecs: Array<{ defId: string; params: Record<string, number>; weight?: number }>
): CompositeStrategy {
  const blocks: SelectedStrategy[] = moduleSpecs.map((spec, idx) => {
    const def = STRATEGY_LIBRARY.find((d) => d.id === spec.defId) || STRATEGY_LIBRARY[0];
    return {
      instanceId: `${strategyId.toLowerCase()}-${spec.defId}-${idx}`,
      definitionId: def.id,
      name: def.name,
      category: def.category,
      abbreviation: def.abbreviation,
      params: { ...def.defaultParams, ...spec.params },
      weight: spec.weight ?? 1 / moduleSpecs.length,
      signal: def.defaultSignal,
      signalValue: def.defaultSignalValue,
      accentColor: def.accentColor,
      borderAccentClass: def.borderAccentClass,
    };
  });

  const weights: Record<string, number> = {};
  const parameters: Record<string, Record<string, number>> = {};
  blocks.forEach((b) => {
    weights[b.instanceId] = b.weight;
    parameters[b.instanceId] = { ...b.params };
  });

  const { compositeScore, finalSignal } = calculateCompositeSignal(blocks, 'Weighted Combination');

  return {
    id: strategyId,
    version,
    displayName,
    isCustomNamed: true,
    blocks,
    combinationMethod: 'Weighted Combination',
    weights,
    parameters,
    compositeScore,
    finalSignal,
  };
}

export const INITIAL_LEADERBOARD: LeaderboardEntry[] = [
  {
    id: 'STR-184',
    strategyId: 'STR-184',
    rank: 1,
    name: 'MA20 + RSI14 + Support/Resistance',
    identifier: 'STR-184 · v3',
    version: 'v3',
    categoryTag: 'Momentum',
    score: 84.1,
    totalReturn: 142.5,
    winRate: 58.2,
    maxDrawdown: -12.4,
    sharpeRatio: 2.14,
    profitFactor: 2.18,
    tradesCount: 1245,
    strategy: createLeaderboardStrategy('STR-184', 'v3', 'MA20 + RSI14 + Support/Resistance', [
      { defId: 'moving-average', params: { fastPeriod: 20, slowPeriod: 50 }, weight: 0.35 },
      { defId: 'rsi', params: { period: 14, buyThreshold: 30, sellThreshold: 70 }, weight: 0.35 },
      { defId: 'support-resistance', params: { lookback: 50, sensitivity: 5 }, weight: 0.30 },
    ]),
    strategyConfig: {
      blocks: [
        { type: 'MA_CROSS', label: 'EMA Trend Filter', parameters: { fast: 20, slow: 50 } },
        { type: 'RSI_OSC', label: 'RSI Filter', parameters: { period: 14, overbought: 70, oversold: 30 } },
        { type: 'SUP_RES', label: 'Key Support / Resistance', parameters: { lookback: 50, touchCount: 3 } },
      ],
    },
  },
  {
    id: 'STR-102',
    strategyId: 'STR-102',
    rank: 2,
    name: 'MA50 + Bollinger Bands',
    identifier: 'STR-102 · v2',
    version: 'v2',
    categoryTag: 'Mean Reversion',
    score: 81.7,
    totalReturn: 118.2,
    winRate: 62.1,
    maxDrawdown: -15.8,
    sharpeRatio: 1.95,
    profitFactor: 1.92,
    tradesCount: 892,
    strategy: createLeaderboardStrategy('STR-102', 'v2', 'MA50 + Bollinger Bands', [
      { defId: 'moving-average', params: { fastPeriod: 50, slowPeriod: 200 }, weight: 0.5 },
      { defId: 'bollinger-bands', params: { period: 20, stdDev: 2.0 }, weight: 0.5 },
    ]),
    strategyConfig: {
      blocks: [
        { type: 'BB_BANDS', label: 'Bollinger Reversal', parameters: { period: 20, stdDev: 2.0 } },
        { type: 'MA_CROSS', label: 'MA50 Trend Line', parameters: { period: 50 } },
      ],
    },
  },
  {
    id: 'STR-103',
    strategyId: 'STR-103',
    rank: 3,
    name: 'RSI14 + Support/Resistance',
    identifier: 'STR-103 · v1',
    version: 'v1',
    categoryTag: 'Breakout',
    score: 79.2,
    totalReturn: 155.0,
    winRate: 45.8,
    maxDrawdown: -22.1,
    sharpeRatio: 1.72,
    profitFactor: 1.85,
    tradesCount: 2105,
    strategy: createLeaderboardStrategy('STR-103', 'v1', 'RSI14 + Support/Resistance', [
      { defId: 'rsi', params: { period: 14, buyThreshold: 30, sellThreshold: 70 }, weight: 0.5 },
      { defId: 'support-resistance', params: { lookback: 50, sensitivity: 3 }, weight: 0.5 },
    ]),
    strategyConfig: {
      blocks: [
        { type: 'SUP_RES', label: 'Support Breakout', parameters: { lookback: 50, touchCount: 3 } },
        { type: 'RSI_OSC', label: 'RSI Threshold', parameters: { period: 14, overbought: 70, oversold: 30 } },
      ],
    },
  },
  {
    id: 'STR-104',
    strategyId: 'STR-104',
    rank: 4,
    name: 'MA20 + Bollinger + S/R',
    identifier: 'STR-104 · v3',
    version: 'v3',
    categoryTag: 'Trend Following',
    score: 75.5,
    totalReturn: 95.4,
    winRate: 51.2,
    maxDrawdown: -14.2,
    sharpeRatio: 1.55,
    profitFactor: 1.62,
    tradesCount: 1100,
    strategy: createLeaderboardStrategy('STR-104', 'v3', 'MA20 + Bollinger + S/R', [
      { defId: 'moving-average', params: { fastPeriod: 20, slowPeriod: 50 } },
      { defId: 'bollinger-bands', params: { period: 20, stdDev: 2.0 } },
      { defId: 'support-resistance', params: { lookback: 50, sensitivity: 5 } },
    ]),
  },
  {
    id: 'STR-105',
    strategyId: 'STR-105',
    rank: 5,
    name: 'RSI Divergence + Moving Average',
    identifier: 'STR-105 · v2',
    version: 'v2',
    categoryTag: 'Reversal',
    score: 72.1,
    totalReturn: 82.1,
    winRate: 54.5,
    maxDrawdown: -18.5,
    sharpeRatio: 1.41,
    profitFactor: 1.51,
    tradesCount: 654,
    strategy: createLeaderboardStrategy('STR-105', 'v2', 'RSI Divergence + Moving Average', [
      { defId: 'rsi', params: { period: 14, buyThreshold: 30, sellThreshold: 70 } },
      { defId: 'moving-average', params: { fastPeriod: 50, slowPeriod: 100 } },
    ]),
  },
  {
    id: 'STR-106',
    strategyId: 'STR-106',
    rank: 6,
    name: 'Bollinger Volatility Breakout',
    identifier: 'STR-106 · v1',
    version: 'v1',
    categoryTag: 'Volatility',
    score: 70.8,
    totalReturn: 76.8,
    winRate: 50.4,
    maxDrawdown: -16.2,
    sharpeRatio: 1.38,
    profitFactor: 1.45,
    tradesCount: 940,
    strategy: createLeaderboardStrategy('STR-106', 'v1', 'Bollinger Volatility Breakout', [
      { defId: 'bollinger-bands', params: { period: 20, stdDev: 2.5 } },
      { defId: 'support-resistance', params: { lookback: 30, sensitivity: 6 } },
    ]),
  },
  {
    id: 'STR-107',
    strategyId: 'STR-107',
    rank: 7,
    name: 'EMA Ribbon + Momentum Filter',
    identifier: 'STR-107 · v3',
    version: 'v3',
    categoryTag: 'Trend Following',
    score: 69.4,
    totalReturn: 68.2,
    winRate: 49.0,
    maxDrawdown: -13.9,
    sharpeRatio: 1.32,
    profitFactor: 1.40,
    tradesCount: 1420,
    strategy: createLeaderboardStrategy('STR-107', 'v3', 'EMA Ribbon + Momentum Filter', [
      { defId: 'moving-average', params: { fastPeriod: 9, slowPeriod: 21 } },
      { defId: 'rsi', params: { period: 14, buyThreshold: 40, sellThreshold: 60 } },
    ]),
  },
  {
    id: 'STR-108',
    strategyId: 'STR-108',
    rank: 8,
    name: 'Support / Resistance Dynamic Bounce',
    identifier: 'STR-108 · v2',
    version: 'v2',
    categoryTag: 'Price Action',
    score: 67.9,
    totalReturn: 61.5,
    winRate: 56.8,
    maxDrawdown: -11.5,
    sharpeRatio: 1.28,
    profitFactor: 1.38,
    tradesCount: 520,
    strategy: createLeaderboardStrategy('STR-108', 'v2', 'Support / Resistance Dynamic Bounce', [
      { defId: 'support-resistance', params: { lookback: 80, sensitivity: 7 } },
    ]),
  },
  {
    id: 'STR-109',
    strategyId: 'STR-109',
    rank: 9,
    name: 'RSI + Stochastic Confluence',
    identifier: 'STR-109 · v2',
    version: 'v2',
    categoryTag: 'Momentum',
    score: 65.2,
    totalReturn: 54.0,
    winRate: 53.1,
    maxDrawdown: -19.4,
    sharpeRatio: 1.22,
    profitFactor: 1.32,
    tradesCount: 780,
    strategy: createLeaderboardStrategy('STR-109', 'v2', 'RSI + Stochastic Confluence', [
      { defId: 'rsi', params: { period: 14, buyThreshold: 25, sellThreshold: 75 } },
      { defId: 'bollinger-bands', params: { period: 14, stdDev: 1.8 } },
    ]),
  },
  {
    id: 'STR-110',
    strategyId: 'STR-110',
    rank: 10,
    name: 'Multi-MA Confluence + S/R',
    identifier: 'STR-110 · v1',
    version: 'v1',
    categoryTag: 'Trend Following',
    score: 63.8,
    totalReturn: 48.9,
    winRate: 47.6,
    maxDrawdown: -15.1,
    sharpeRatio: 1.15,
    profitFactor: 1.25,
    tradesCount: 1650,
    strategy: createLeaderboardStrategy('STR-110', 'v1', 'Multi-MA Confluence + S/R', [
      { defId: 'moving-average', params: { fastPeriod: 20, slowPeriod: 100 } },
      { defId: 'support-resistance', params: { lookback: 60, sensitivity: 4 } },
    ]),
  },
  {
    id: 'STR-111',
    strategyId: 'STR-111',
    rank: 11,
    name: 'Bollinger Squeeze + RSI',
    identifier: 'STR-111 · v2',
    version: 'v2',
    categoryTag: 'Volatility',
    score: 62.4,
    totalReturn: 44.2,
    winRate: 52.0,
    maxDrawdown: -14.8,
    sharpeRatio: 1.12,
    profitFactor: 1.22,
    tradesCount: 810,
    strategy: createLeaderboardStrategy('STR-111', 'v2', 'Bollinger Squeeze + RSI', [
      { defId: 'bollinger-bands', params: { period: 20, stdDev: 1.5 } },
      { defId: 'rsi', params: { period: 14, buyThreshold: 30, sellThreshold: 70 } },
    ]),
  },
  {
    id: 'STR-112',
    strategyId: 'STR-112',
    rank: 12,
    name: 'MACD Zero-Lag + Moving Average',
    identifier: 'STR-112 · v3',
    version: 'v3',
    categoryTag: 'Momentum',
    score: 61.0,
    totalReturn: 41.5,
    winRate: 48.5,
    maxDrawdown: -17.2,
    sharpeRatio: 1.08,
    profitFactor: 1.18,
    tradesCount: 1190,
    strategy: createLeaderboardStrategy('STR-112', 'v3', 'MACD Zero-Lag + Moving Average', [
      { defId: 'moving-average', params: { fastPeriod: 12, slowPeriod: 26 } },
      { defId: 'rsi', params: { period: 9, buyThreshold: 35, sellThreshold: 65 } },
    ]),
  },
];

// Pool of candidate names to generate during live simulation
export const MOCK_CANDIDATE_POOL = [
  { name: 'MA20 + RSI14 + Support/Resistance', category: 'Momentum' },
  { name: 'MA50 + Bollinger Bands', category: 'Volatility' },
  { name: 'RSI14 + Support/Resistance', category: 'Price Action' },
  { name: 'MA20 + Bollinger + S/R', category: 'Trend Following' },
  { name: 'EMA20 + EMA50 + Bollinger Bands', category: 'Mean Reversion' },
  { name: 'RSI21 + Support / Resistance', category: 'Breakout' },
  { name: 'Triple MA (9/21/55) + RSI', category: 'Trend Following' },
  { name: 'Bollinger Bands + RSI Oversold', category: 'Price Action' },
  { name: 'Support Rejection + MA Trend', category: 'Momentum' },
  { name: 'MA100 + RSI Pullback + S/R', category: 'Volatility' },
];

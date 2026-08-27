import { BacktestMetrics, Trade, EquityPoint, BacktestCandle, BacktestConfig } from '../types/backtest';
import { LeaderboardEntry } from '../types/search';
import { CompositeStrategy } from '../types/strategy';

export const MOCK_BACKTEST_METRICS: BacktestMetrics = {
  totalReturn: '+18.2%',
  winRate: '61%',
  maxDrawdown: '-6.1%',
  numberOfTrades: 81,
  profitFactor: '1.74',
  sharpeRatio: '1.42',
  winLossRatio: { wins: 49, losses: 32 },
  avgWinLoss: { avgWin: '+2.4%', avgLoss: '-1.1%' },
  bestWorstTrade: { best: '+8.2%', worst: '-3.5%' },
};

export const MOCK_TRADES: Trade[] = [
  {
    id: 't-1',
    tradeNumber: 1,
    entryTime: '01 Jul 2026 08:00',
    entryPrice: 108000,
    exitTime: '01 Jul 2026 15:00',
    exitPrice: 110000,
    side: 'LONG',
    returnPct: 1.85,
    result: 'WIN',
    entryCandleIndex: 2,
    exitCandleIndex: 4,
    chartEntryX: 160,
    chartEntryY: 108000,
    chartExitX: 280,
    chartExitY: 110000,
  },
  {
    id: 't-2',
    tradeNumber: 2,
    entryTime: '02 Jul 2026 10:00',
    entryPrice: 111000,
    exitTime: '02 Jul 2026 18:00',
    exitPrice: 110000,
    side: 'LONG',
    returnPct: -0.90,
    result: 'LOSS',
    entryCandleIndex: 8,
    exitCandleIndex: 10,
    chartEntryX: 340,
    chartEntryY: 111000,
    chartExitX: 420,
    chartExitY: 110000,
  },
  {
    id: 't-3',
    tradeNumber: 3,
    entryTime: '04 Jul 2026 07:00',
    entryPrice: 109000,
    exitTime: '05 Jul 2026 12:00',
    exitPrice: 114000,
    side: 'LONG',
    returnPct: 4.58,
    result: 'WIN',
    entryCandleIndex: 13,
    exitCandleIndex: 18,
    chartEntryX: 520,
    chartEntryY: 109000,
    chartExitX: 680,
    chartExitY: 113800,
  },
  {
    id: 't-4',
    tradeNumber: 4,
    entryTime: '06 Jul 2026 14:15',
    entryPrice: 115200,
    exitTime: '06 Jul 2026 19:30',
    exitPrice: 112781,
    side: 'SHORT',
    returnPct: 2.10,
    result: 'WIN',
    entryCandleIndex: 21,
    exitCandleIndex: 23,
    chartEntryX: 740,
    chartEntryY: 114600,
    chartExitX: 840,
    chartExitY: 112200,
  },
  {
    id: 't-5',
    tradeNumber: 5,
    entryTime: '08 Jul 2026 09:30',
    entryPrice: 112500,
    exitTime: '08 Jul 2026 17:45',
    exitPrice: 111206,
    side: 'LONG',
    returnPct: -1.15,
    result: 'LOSS',
    entryCandleIndex: 26,
    exitCandleIndex: 28,
    chartEntryX: 880,
    chartEntryY: 112500,
    chartExitX: 950,
    chartExitY: 111200,
  },
  {
    id: 't-6',
    tradeNumber: 6,
    entryTime: '10 Jul 2026 03:00',
    entryPrice: 108400,
    exitTime: '11 Jul 2026 10:15',
    exitPrice: 112000,
    side: 'LONG',
    returnPct: 3.32,
    result: 'WIN',
    entryCandleIndex: 3,
    exitCandleIndex: 6,
    chartEntryX: 180,
    chartEntryY: 108400,
    chartExitX: 300,
    chartExitY: 112000,
  },
  {
    id: 't-7',
    tradeNumber: 7,
    entryTime: '13 Jul 2026 11:30',
    entryPrice: 113100,
    exitTime: '13 Jul 2026 18:30',
    exitPrice: 111449,
    side: 'SHORT',
    returnPct: 1.46,
    result: 'WIN',
    entryCandleIndex: 19,
    exitCandleIndex: 22,
    chartEntryX: 710,
    chartEntryY: 113100,
    chartExitX: 790,
    chartExitY: 111450,
  },
  {
    id: 't-8',
    tradeNumber: 8,
    entryTime: '16 Jul 2026 19:45',
    entryPrice: 109800,
    exitTime: '17 Jul 2026 04:15',
    exitPrice: 108746,
    side: 'LONG',
    returnPct: -0.96,
    result: 'LOSS',
    entryCandleIndex: 12,
    exitCandleIndex: 14,
    chartEntryX: 480,
    chartEntryY: 109800,
    chartExitX: 540,
    chartExitY: 108750,
  },
  {
    id: 't-9',
    tradeNumber: 9,
    entryTime: '19 Jul 2026 08:30',
    entryPrice: 107600,
    exitTime: '20 Jul 2026 14:00',
    exitPrice: 111300,
    side: 'LONG',
    returnPct: 3.44,
    result: 'WIN',
    entryCandleIndex: 4,
    exitCandleIndex: 7,
    chartEntryX: 220,
    chartEntryY: 107600,
    chartExitX: 360,
    chartExitY: 111300,
  },
  {
    id: 't-10',
    tradeNumber: 10,
    entryTime: '22 Jul 2026 16:20',
    entryPrice: 114300,
    exitTime: '23 Jul 2026 02:45',
    exitPrice: 112800,
    side: 'SHORT',
    returnPct: 1.31,
    result: 'WIN',
    entryCandleIndex: 20,
    exitCandleIndex: 23,
    chartEntryX: 760,
    chartEntryY: 114300,
    chartExitX: 830,
    chartExitY: 112800,
  },
  {
    id: 't-11',
    tradeNumber: 11,
    entryTime: '24 Jul 2026 10:15',
    entryPrice: 111900,
    exitTime: '25 Jul 2026 01:00',
    exitPrice: 110750,
    side: 'LONG',
    returnPct: -1.03,
    result: 'LOSS',
    entryCandleIndex: 15,
    exitCandleIndex: 17,
    chartEntryX: 580,
    chartEntryY: 111900,
    chartExitX: 650,
    chartExitY: 110750,
  },
  {
    id: 't-12',
    tradeNumber: 12,
    entryTime: '27 Jul 2026 06:00',
    entryPrice: 108900,
    exitTime: '28 Jul 2026 18:30',
    exitPrice: 113500,
    side: 'LONG',
    returnPct: 4.22,
    result: 'WIN',
    entryCandleIndex: 5,
    exitCandleIndex: 10,
    chartEntryX: 290,
    chartEntryY: 108900,
    chartExitX: 470,
    chartExitY: 113500,
  },
];

export const MOCK_EQUITY_CURVE: EquityPoint[] = [
  { date: '01 Jan', equity: 10000, drawdown: 0 },
  { date: '15 Jan', equity: 10250, drawdown: 0 },
  { date: '01 Feb', equity: 10680, drawdown: 0 },
  { date: '15 Feb', equity: 10420, drawdown: -2.4 },
  { date: '01 Mar', equity: 10910, drawdown: 0 },
  { date: '15 Mar', equity: 11150, drawdown: 0 },
  { date: '01 Apr', equity: 11340, drawdown: 0 },
  { date: '15 Apr', equity: 11520, drawdown: 0 },
  { date: '01 May', equity: 11180, drawdown: -2.9 },
  { date: '15 May', equity: 10820, drawdown: -6.1 },
  { date: '01 Jun', equity: 11450, drawdown: -0.6 },
  { date: '15 Jun', equity: 11720, drawdown: 0 },
  { date: '01 Jul', equity: 11820, drawdown: 0 },
];

export const MOCK_CANDLES: BacktestCandle[] = [
  { time: '01/07 00:00', open: 107800, high: 108200, low: 107400, close: 108000, volume: 142 },
  { time: '01/07 04:00', open: 108000, high: 108600, low: 107900, close: 108400, volume: 185 },
  { time: '01/07 08:00', open: 108400, high: 109200, low: 108100, close: 109050, volume: 240 },
  { time: '01/07 12:00', open: 109050, high: 109800, low: 108800, close: 109500, volume: 310 },
  { time: '01/07 16:00', open: 109500, high: 110200, low: 109300, close: 110000, volume: 275 },
  { time: '01/07 20:00', open: 110000, high: 110600, low: 109700, close: 110400, volume: 190 },
  { time: '02/07 00:00', open: 110400, high: 111200, low: 110100, close: 110900, volume: 215 },
  { time: '02/07 04:00', open: 110900, high: 111400, low: 110500, close: 111100, volume: 160 },
  { time: '02/07 08:00', open: 111100, high: 111300, low: 110200, close: 110400, volume: 225 },
  { time: '02/07 12:00', open: 110400, high: 110700, low: 109800, close: 110000, volume: 280 },
  { time: '02/07 16:00', open: 110000, high: 110200, low: 109100, close: 109300, volume: 310 },
  { time: '03/07 00:00', open: 109300, high: 109800, low: 108900, close: 109100, volume: 175 },
  { time: '03/07 08:00', open: 109100, high: 109500, low: 108600, close: 108900, volume: 205 },
  { time: '03/07 16:00', open: 108900, high: 109600, low: 108800, close: 109400, volume: 230 },
  { time: '04/07 00:00', open: 109400, high: 110200, low: 109200, close: 109900, volume: 290 },
  { time: '04/07 08:00', open: 109900, high: 111200, low: 109800, close: 111000, volume: 380 },
  { time: '04/07 16:00', open: 111000, high: 112400, low: 110800, close: 112100, volume: 420 },
  { time: '05/07 00:00', open: 112100, high: 113500, low: 111900, close: 113200, volume: 490 },
  { time: '05/07 08:00', open: 113200, high: 114200, low: 112900, close: 113800, volume: 530 },
  { time: '05/07 16:00', open: 113800, high: 114500, low: 113400, close: 114100, volume: 380 },
  { time: '06/07 00:00', open: 114100, high: 114800, low: 113700, close: 114600, volume: 310 },
  { time: '06/07 08:00', open: 114600, high: 115200, low: 114200, close: 114900, volume: 350 },
  { time: '06/07 16:00', open: 114900, high: 115000, low: 113800, close: 114100, volume: 410 },
  { time: '07/07 00:00', open: 114100, high: 114200, low: 113000, close: 113200, volume: 340 },
  { time: '07/07 08:00', open: 113200, high: 113400, low: 112200, close: 112600, volume: 390 },
  { time: '07/07 16:00', open: 112600, high: 112900, low: 111800, close: 112200, volume: 290 },
  { time: '08/07 00:00', open: 112200, high: 112800, low: 111900, close: 112500, volume: 210 },
  { time: '08/07 08:00', open: 112500, high: 112700, low: 111400, close: 111600, volume: 270 },
  { time: '08/07 16:00', open: 111600, high: 111900, low: 110800, close: 111200, volume: 320 },
];

export function resolveBacktestResult(
  strategy: CompositeStrategy | null | undefined,
  config: BacktestConfig
): { metrics: BacktestMetrics; candles: BacktestCandle[]; trades: Trade[]; equityCurve: EquityPoint[] } {
  const baseMultiplier =
    config.symbol === 'ETH/USDT' ? 0.03 : config.symbol === 'SOL/USDT' ? 0.00165 : 1;

  let rawReturn = 18.2;
  let rawWinRate = 61.0;
  let rawDrawdown = -6.1;
  let rawSharpe = 1.42;
  let rawTrades = 81;
  let profitFactorVal = '1.74';

  const blocks = strategy?.blocks || [];
  const stratNameLower = (strategy?.displayName || strategy?.id || '').toLowerCase();

  // Known leaderboard strategies grounding
  if (stratNameLower.includes('str-184') || (stratNameLower.includes('ma20') && stratNameLower.includes('rsi14'))) {
    rawReturn = 142.5;
    rawWinRate = 58.2;
    rawDrawdown = -12.4;
    rawSharpe = 2.14;
    profitFactorVal = '2.18';
    rawTrades = 1245;
  } else if (stratNameLower.includes('str-102') || (stratNameLower.includes('ma50') && stratNameLower.includes('bollinger'))) {
    rawReturn = 118.2;
    rawWinRate = 62.1;
    rawDrawdown = -15.8;
    rawSharpe = 1.95;
    profitFactorVal = '1.92';
    rawTrades = 892;
  } else if (stratNameLower.includes('str-103') || (stratNameLower.includes('rsi14') && stratNameLower.includes('support'))) {
    rawReturn = 155.0;
    rawWinRate = 45.8;
    rawDrawdown = -22.1;
    rawSharpe = 1.72;
    profitFactorVal = '1.85';
    rawTrades = 2105;
  } else if (stratNameLower.includes('str-149') || (stratNameLower.includes('news') && stratNameLower.includes('moving'))) {
    rawReturn = 104.8;
    rawWinRate = 54.0;
    rawDrawdown = -18.5;
    rawSharpe = 1.68;
    profitFactorVal = '1.78';
    rawTrades = 740;
  } else if (blocks.length > 0) {
    // Dynamic derivation based on active modules in Strategy Composer
    let baseRet = 14.0;
    let baseWin = 52.0;
    let baseDd = -11.0;
    let baseTrades = 70;

    blocks.forEach((b) => {
      const id = (b.definitionId || b.name || '').toLowerCase();
      if (id.includes('mov') || id.includes('ma')) {
        baseRet += 8.5;
        baseWin += 2.8;
        baseDd += 1.5;
        baseTrades += 25;
      } else if (id.includes('rsi')) {
        baseRet += 12.8;
        baseWin += 4.2;
        baseDd += 2.0;
        baseTrades += 30;
      } else if (id.includes('boll') || id.includes('bb')) {
        baseRet += 10.4;
        baseWin += 5.1;
        baseDd += 3.2;
        baseTrades += 20;
      } else if (id.includes('supp') || id.includes('sr') || id.includes('res')) {
        baseRet += 15.2;
        baseWin += 5.8;
        baseDd += 2.2;
        baseTrades += 24;
      } else if (id.includes('news')) {
        baseRet += 18.5;
        baseWin += 3.8;
        baseDd -= 1.8;
        baseTrades += 35;
      }
    });

    if (config.symbol === 'ETH/USDT') {
      baseRet *= 1.15;
      baseTrades += 15;
    } else if (config.symbol === 'SOL/USDT') {
      baseRet *= 1.35;
      baseTrades += 30;
      baseDd -= 2.5;
    }

    rawReturn = Math.round(baseRet * 10) / 10;
    rawWinRate = Math.min(78, Math.max(45, Math.round(baseWin * 10) / 10));
    rawDrawdown = -Math.min(28, Math.max(4.5, Math.round(Math.abs(baseDd) * 10) / 10));
    rawTrades = baseTrades;
    rawSharpe = Math.round((1.15 + (rawReturn / 100) * 0.85 + (rawWinRate - 50) * 0.02) * 100) / 100;
    profitFactorVal = (1.35 + (rawReturn / 100) * 0.65).toFixed(2);
  }

  const totalReturn = `+${rawReturn.toFixed(1)}%`;
  const winRate = `${rawWinRate.toFixed(1)}%`;
  const maxDrawdown = `${rawDrawdown.toFixed(1)}%`;
  const numberOfTrades = rawTrades;
  const sharpeRatio = rawSharpe.toFixed(2);
  const profitFactor = profitFactorVal;

  const wins = Math.round((numberOfTrades * rawWinRate) / 100);
  const losses = numberOfTrades - wins;

  const metrics: BacktestMetrics = {
    totalReturn,
    winRate,
    maxDrawdown,
    numberOfTrades,
    profitFactor,
    sharpeRatio,
    winLossRatio: { wins, losses },
    avgWinLoss: {
      avgWin: `+${((rawReturn / (wins || 1)) * 2.5).toFixed(1)}%`,
      avgLoss: `-${Math.abs(rawDrawdown / 8).toFixed(1)}%`,
    },
    bestWorstTrade: {
      best: `+${(rawReturn * 0.11).toFixed(1)}%`,
      worst: `-${(Math.abs(rawDrawdown) * 0.42).toFixed(1)}%`,
    },
  };

  // Scale candles
  const candles: BacktestCandle[] = MOCK_CANDLES.map((c) => ({
    time: c.time,
    open: Math.round(c.open * baseMultiplier * 100) / 100,
    high: Math.round(c.high * baseMultiplier * 100) / 100,
    low: Math.round(c.low * baseMultiplier * 100) / 100,
    close: Math.round(c.close * baseMultiplier * 100) / 100,
    volume: c.volume,
  }));

  // Scale trades
  const trades: Trade[] = MOCK_TRADES.map((t, idx) => {
    const isWin = idx % 10 < Math.round(rawWinRate / 10);
    const returnPct = isWin
      ? Math.round((1.5 + (idx % 4) * 1.2) * 100) / 100
      : -Math.round((0.8 + (idx % 3) * 0.5) * 100) / 100;
    return {
      ...t,
      returnPct,
      result: isWin ? 'WIN' : 'LOSS',
      entryPrice: Math.round(t.entryPrice * baseMultiplier * 100) / 100,
      exitPrice: Math.round(t.exitPrice * baseMultiplier * 100) / 100,
      chartEntryY: Math.round(t.chartEntryY * baseMultiplier * 100) / 100,
      chartExitY: Math.round(t.chartExitY * baseMultiplier * 100) / 100,
    };
  });

  // Scale equity curve
  const growthFactor = 1 + rawReturn / 100;
  const equityCurve: EquityPoint[] = MOCK_EQUITY_CURVE.map((p, idx) => {
    const progress = idx / (MOCK_EQUITY_CURVE.length - 1);
    const targetEq = 10000 * (1 + (growthFactor - 1) * Math.pow(progress, 0.95));
    const scaledDrawdown =
      p.drawdown !== 0 ? Math.round((p.drawdown / 6.1) * Math.abs(rawDrawdown) * 10) / 10 : 0;
    return {
      date: p.date,
      equity: Math.round(targetEq),
      drawdown: -scaledDrawdown,
    };
  });

  return { metrics, candles, trades, equityCurve };
}

export function generateMockBacktestData(
  config: BacktestConfig,
  strategy?: CompositeStrategy | null,
  entry?: LeaderboardEntry | null
) {
  if (entry) {
    const entryStrategy = entry.strategy || {
      id: entry.strategyId || entry.id,
      version: entry.version || 'v1',
      displayName: entry.name,
      blocks: [],
      combinationMethod: 'Weighted Combination',
      weights: {},
      parameters: {},
      compositeScore: 0,
      finalSignal: 'BUY',
    };
    return resolveBacktestResult(entryStrategy, config);
  }
  return resolveBacktestResult(strategy, config);
}

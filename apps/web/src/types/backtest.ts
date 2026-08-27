export type TradeSide = 'LONG' | 'SHORT';
export type TradeResult = 'WIN' | 'LOSS';

export interface BacktestConfig {
  symbol: string;
  timeframe: string;
  startDate: string; // YYYY-MM-DD
  endDate: string;   // YYYY-MM-DD
}

export interface Trade {
  id: string;
  tradeNumber: number;
  entryTime: string;
  entryPrice: number;
  exitTime: string;
  exitPrice: number;
  side: TradeSide;
  returnPct: number;
  result: TradeResult;
  entryCandleIndex?: number;
  exitCandleIndex?: number;
  // Chart visual coordinates normalized (0-1000 X, price Y)
  chartEntryX: number;
  chartEntryY: number;
  chartExitX: number;
  chartExitY: number;
}

export interface BacktestMetrics {
  totalReturn: string;
  winRate: string;
  maxDrawdown: string;
  numberOfTrades: number;
  profitFactor: string;
  sharpeRatio: string;
  winLossRatio: { wins: number; losses: number };
  avgWinLoss: { avgWin: string; avgLoss: string };
  bestWorstTrade: { best: string; worst: string };
}

export interface EquityPoint {
  date: string;
  equity: number;
  drawdown: number;
}

export interface BacktestCandle {
  time: string;
  open: number;
  high: number;
  low: number;
  close: number;
  volume: number;
}

import { CompositeStrategy } from './strategy';

export type SearchAlgorithm = 'random' | 'domain_guided';

export type SearchStageId = 'generate' | 'backtest' | 'evaluate' | 'rank' | 'leaderboard';
export type SearchStageStatus = 'idle' | 'active' | 'completed' | 'waiting' | 'paused' | 'stopped';

export type WorkerState = 'idle' | 'running' | 'completed' | 'evaluating' | 'paused' | 'error';

export interface WorkerInfo {
  id: string;
  name: string;
  state: WorkerState;
  currentTask?: string;
}

export interface SearchSpaceFeature {
  id: string;
  name: string;
  label: string;
  category: string;
  enabled: boolean;
  opacity?: number;
}

export interface StopConditions {
  maxCandidates: number;
  noImprovementIters: number;
  timeLimitHours: number;
}

export interface SearchConfigurationState {
  market: string;
  datasetRange: string;
  algorithm: SearchAlgorithm;
  features: SearchSpaceFeature[];
  stopConditions: StopConditions;
}

export type CandidateStatus =
  | 'idle'
  | 'generating'
  | 'backtesting'
  | 'evaluating'
  | 'ranking'
  | 'ranked'
  | 'completed'
  | 'paused'
  | 'stopped';

export interface CandidateEvaluation {
  id: string;
  candidateNumber: number;
  name: string;
  categoryTag: string;
  status: CandidateStatus;
  progress: number;
  currentScore?: number;
  returnPct?: number;
  winRate?: number;
  maxDrawdown?: number;
  sharpe?: number;
  trades?: number;
}

export interface SearchLiveMetrics {
  candidatesTested: number;
  candidatesRemaining: number;
  elapsedSeconds: number;
  bestScore: number;
  scoreImprovement: number;
}

export interface LeaderboardEntry {
  id: string;
  strategyId?: string;
  rank: number;
  name: string;
  identifier?: string;
  version?: string;
  categoryTag: string;
  score: number;
  totalReturn: number; // e.g. 142.5 for +142.5%
  winRate: number;     // e.g. 58.2 for 58.2%
  maxDrawdown: number; // e.g. -12.4 for -12.4%
  sharpeRatio: number; // e.g. 2.14
  tradesCount: number; // e.g. 1245
  profitFactor?: number;
  isNew?: boolean;     // for subtle live highlight
  strategy?: CompositeStrategy;
  strategyConfig?: {
    blocks: Array<{
      type: string;
      label: string;
      parameters: Record<string, number | string>;
    }>;
  };
}

export type LeaderboardSortField = 'score' | 'totalReturn' | 'winRate' | 'maxDrawdown' | 'sharpeRatio' | 'tradesCount';
export type SortDirection = 'asc' | 'desc';
export type TopKSelection = 10 | 25 | 50;

import { CompositeStrategy, StrategyDefinition } from './strategy';

export type AIStrategySource = 'AI_PROMPT' | 'URL_IMPORT';

export interface AIStrategyCondition {
  indicator: string;
  operator: string;
  value?: number;
  indicatorRef?: string;
  position?: '<' | '>';
  text?: string;
}

export interface AIStrategyIndicator {
  name: string;
  period?: number;
  stdDev?: number;
  fastPeriod?: number;
  slowPeriod?: number;
  overbought?: number;
  oversold?: number;
  lookback?: number;
  sensitivity?: number;
  parameters?: Record<string, number>;
}

export interface AIStrategyRiskItem {
  type: string;
  value: number;
}

export interface AIStrategyRiskManagement {
  stopLoss?: AIStrategyRiskItem;
  takeProfit?: AIStrategyRiskItem;
}

export interface AIStrategyJSON {
  name: string;
  version: string;
  description: string;
  source?: AIStrategySource;
  sourceUrl?: string;
  indicators: AIStrategyIndicator[];
  conditions: {
    long: AIStrategyCondition[];
    short: AIStrategyCondition[];
  };
  riskManagement: AIStrategyRiskManagement;
  timeframe: string;
  isTimeframeDefault?: boolean;
  market: string;
  pairs: string;
  isMarketDefault?: boolean;
  tags: string[];
}

export interface ParsedStrategyData {
  longConditions: string[];
  shortConditions: string[];
  hasShortConditions: boolean;
  stopLoss: string;
  hasStopLoss: boolean;
  takeProfit: string;
  hasTakeProfit: boolean;
  timeframe: string;
  isTimeframeDefault: boolean;
  market: string;
  isMarketDefault: boolean;
  indicators: string[];
}

export type ValidationStatus = 'success' | 'warning' | 'error';

export type ValidationCategory =
  | 'Required Fields'
  | 'Strategy Logic'
  | 'Supported Indicators'
  | 'Risk Management'
  | 'Strategy Definition';

export interface ValidationItem {
  id: string;
  category: ValidationCategory;
  label: string;
  message: string;
  status: ValidationStatus;
}

export interface ValidationSummary {
  isValid: boolean;
  canSave: boolean;
  items: ValidationItem[];
  overallStatus: 'Ready to Save' | 'Requires Changes';
  overallMessage: string;
}

export interface RecentAIStrategy {
  id: string;
  name: string;
  description: string;
  source: AIStrategySource;
  sourceUrl?: string;
  createdAt: string;
  version: string;
  tags: string[];
  status: 'Valid' | 'Warning' | 'Error';
  strategyJson: AIStrategyJSON;
  definition?: StrategyDefinition;
  compositeStrategy?: CompositeStrategy;
}

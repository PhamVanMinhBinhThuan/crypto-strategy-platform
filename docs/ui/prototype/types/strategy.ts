export type StrategyCategory = 'Trend' | 'Momentum' | 'Volatility' | 'Structure' | 'Information';

export type StrategySignal = 'BUY' | 'HOLD' | 'SELL';

export type CombinationMethod = 'Majority Vote' | 'Weighted Combination';

export interface StrategyParamDef {
  id: string;
  label: string;
  type: 'number' | 'range';
  min?: number;
  max?: number;
  step?: number;
  defaultValue: number;
  colorType?: 'normal' | 'buy' | 'sell';
  prefix?: string;
  suffix?: string;
  width?: string;
}

export interface StrategyDefinition {
  id: string;
  name: string;
  category: StrategyCategory;
  description: string;
  tags?: string[];
  paramDefs: StrategyParamDef[];
  defaultParams: Record<string, number>;
  defaultSignal: StrategySignal;
  defaultSignalValue: number; // +1, 0, -1
  accentColor: string;
  borderAccentClass: string;
  categoryBorderClass: string;
  abbreviation: string;
}

export interface SelectedStrategy {
  instanceId: string;
  definitionId: string;
  name: string;
  category: StrategyCategory;
  abbreviation: string;
  params: Record<string, number>;
  weight: number;
  signal: StrategySignal;
  signalValue: number;
  accentColor: string;
  borderAccentClass: string;
}

export interface CompositeStrategy {
  id: string;
  version: string;
  displayName: string;
  isCustomNamed?: boolean;
  blocks: SelectedStrategy[];
  combinationMethod: CombinationMethod;
  weights: Record<string, number>;
  parameters: Record<string, Record<string, number>>;
  compositeScore: number;
  finalSignal: StrategySignal;
}

export interface CompositeStrategyState {
  name: string;
  blocks: SelectedStrategy[];
  combinationMethod: CombinationMethod;
  compositeScore: number;
  finalSignal: StrategySignal;
}

export function deriveStrategyDisplayName(blocks: SelectedStrategy[]): string {
  if (!blocks || blocks.length === 0) {
    return 'Empty Strategy';
  }
  return blocks
    .map((b) => {
      if (b.name === 'Support / Resistance') {
        return 'Support/Resistance';
      }
      return b.name;
    })
    .join(' + ');
}

export function calculateCompositeSignal(
  blocks: SelectedStrategy[],
  combinationMethod: CombinationMethod
): { compositeScore: number; finalSignal: StrategySignal } {
  if (!blocks || blocks.length === 0) {
    return { compositeScore: 0, finalSignal: 'HOLD' };
  }

  if (combinationMethod === 'Weighted Combination') {
    const totalWeight = blocks.reduce((sum, b) => sum + b.weight, 0);
    let weightedSum = 0;
    blocks.forEach((b) => {
      const normalizedWeight = totalWeight > 0 ? b.weight / totalWeight : 1 / blocks.length;
      weightedSum += b.signalValue * normalizedWeight;
    });

    let signal: StrategySignal = 'HOLD';
    if (weightedSum > 0.3) {
      signal = 'BUY';
    } else if (weightedSum < -0.3) {
      signal = 'SELL';
    } else {
      signal = 'HOLD';
    }

    return { compositeScore: Math.round(weightedSum * 100) / 100, finalSignal: signal };
  } else {
    let voteSum = 0;
    blocks.forEach((b) => {
      voteSum += b.signalValue;
    });
    const avgScore = voteSum / blocks.length;

    let signal: StrategySignal = 'HOLD';
    if (voteSum > 0) {
      signal = 'BUY';
    } else if (voteSum < 0) {
      signal = 'SELL';
    } else {
      signal = 'HOLD';
    }

    return { compositeScore: Math.round(avgScore * 100) / 100, finalSignal: signal };
  }
}

export type CanonicalStrategyModuleId =
  | 'movingAverage'
  | 'rsi'
  | 'bollingerBands'
  | 'supportResistance'
  | 'newsSentiment';

export function toCanonicalModuleId(idOrName: string | undefined | null): CanonicalStrategyModuleId | null {
  if (!idOrName) return null;
  const norm = idOrName.toLowerCase().replace(/[-_/\s]/g, '');
  if (norm === 'movingaverage' || norm === 'ma') return 'movingAverage';
  if (norm === 'rsi') return 'rsi';
  if (norm === 'bollingerbands' || norm === 'bb') return 'bollingerBands';
  if (norm === 'supportresistance' || norm === 'sr' || norm === 'suppres') return 'supportResistance';
  if (norm === 'newssentiment' || norm === 'news' || norm === 'sentiment') return 'newsSentiment';
  return null;
}

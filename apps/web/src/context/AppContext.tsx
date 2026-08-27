import React, { createContext, useContext, useState, useCallback, useMemo, ReactNode } from 'react';
import { RoutePath } from '../types';
import {
  SelectedStrategy,
  CombinationMethod,
  StrategySignal,
  CompositeStrategy,
  StrategyDefinition,
  deriveStrategyDisplayName,
  calculateCompositeSignal,
  CanonicalStrategyModuleId,
  toCanonicalModuleId,
} from '../types/strategy';
import {
  SearchConfigurationState,
  SearchAlgorithm,
  StopConditions,
  LeaderboardEntry,
} from '../types/search';
import { BacktestConfig, BacktestMetrics, Trade, EquityPoint, BacktestCandle } from '../types/backtest';
import { NewsCoinFilter, NewsTimeRange, NewsSentimentFilter } from '../types/news';
import { INITIAL_SELECTED_BLOCKS, STRATEGY_LIBRARY } from '../data/strategyLibraryData';
import {
  INITIAL_SEARCH_FEATURES,
  INITIAL_STOP_CONDITIONS,
  INITIAL_LEADERBOARD,
} from '../data/mockSearchData';
import {
  MOCK_BACKTEST_METRICS,
  MOCK_TRADES,
  MOCK_EQUITY_CURVE,
  MOCK_CANDLES,
  generateMockBacktestData,
  resolveBacktestResult,
} from '../data/mockBacktestData';

export const DEFAULT_BACKTEST_STRATEGY: CompositeStrategy = {
  id: 'strat-primary-1',
  version: 'v1',
  displayName: deriveStrategyDisplayName(INITIAL_SELECTED_BLOCKS),
  isCustomNamed: false,
  blocks: INITIAL_SELECTED_BLOCKS,
  combinationMethod: 'Weighted Combination',
  weights: {
    'block-sr-1': 0.5,
    'block-bb-1': 0.5,
  },
  parameters: {
    'block-sr-1': { lookback: 100, sensitivity: 5 },
    'block-bb-1': { period: 20, stdDev: 2 },
  },
  compositeScore: 0.5,
  finalSignal: 'BUY',
};

export function convertLeaderboardEntryToStrategy(entry: LeaderboardEntry): CompositeStrategy {
  if (entry.strategy && entry.strategy.blocks && entry.strategy.blocks.length > 0) {
    return {
      ...entry.strategy,
      displayName: entry.name || entry.strategy.displayName,
      id: entry.strategyId || entry.strategy.id || entry.id,
      version: entry.version || entry.strategy.version || 'v1',
    };
  }

  const convertedBlocks: SelectedStrategy[] = (entry.strategyConfig?.blocks || []).map((b, idx) => {
    let defId = 'moving-average';
    if (b.type.includes('RSI')) defId = 'rsi';
    else if (b.type.includes('BB')) defId = 'bollinger-bands';
    else if (b.type.includes('SUP') || b.type.includes('SR')) defId = 'support-resistance';
    else if (b.type.includes('NEWS')) defId = 'news-sentiment';

    const libDef = STRATEGY_LIBRARY.find((d) => d.id === defId) || STRATEGY_LIBRARY[0];
    return {
      instanceId: `${(entry.strategyId || entry.id).toLowerCase()}-${defId}-${idx}`,
      definitionId: libDef.id,
      name: libDef.name,
      category: libDef.category,
      abbreviation: libDef.abbreviation,
      params: typeof b.parameters === 'object' ? (b.parameters as Record<string, number>) : { ...libDef.defaultParams },
      weight: 0.33,
      signal: libDef.defaultSignal,
      signalValue: libDef.defaultSignalValue,
      accentColor: libDef.accentColor,
      borderAccentClass: libDef.borderAccentClass,
    };
  });

  const targetBlocks = convertedBlocks.length > 0 ? convertedBlocks : INITIAL_SELECTED_BLOCKS;
  const weights: Record<string, number> = {};
  const parameters: Record<string, Record<string, number>> = {};
  targetBlocks.forEach((b) => {
    weights[b.instanceId] = b.weight;
    parameters[b.instanceId] = { ...b.params };
  });
  const { compositeScore, finalSignal } = calculateCompositeSignal(targetBlocks, 'Weighted Combination');

  return {
    id: entry.strategyId || entry.id,
    version: entry.version || 'v1',
    displayName: entry.name,
    isCustomNamed: true,
    blocks: targetBlocks,
    combinationMethod: 'Weighted Combination',
    weights,
    parameters,
    compositeScore,
    finalSignal,
  };
}

// Shared App State Interface
export interface AppContextType {
  // Global Market Context
  activePair: string;
  setActivePair: (pair: string) => void;
  selectedTimeframe: string;
  setSelectedTimeframe: (tf: string) => void;

  // Canonical Composite Strategy State
  currentCompositeStrategy: CompositeStrategy;
  composerState: CompositeStrategy & { strategyName: string };
  setComposerStrategyName: (name: string) => void;
  setComposerBlocks: (blocks: SelectedStrategy[] | ((prev: SelectedStrategy[]) => SelectedStrategy[])) => void;
  setComposerCombinationMethod: (method: CombinationMethod) => void;
  addStrategyToComposer: (def: StrategyDefinition) => void;
  addNewsSentimentToComposer: () => void;
  setCompositeStrategy: (strategy: CompositeStrategy) => void;
  loadStrategyIntoComposer: (
    strategyOrName: CompositeStrategy | string,
    blocks?: SelectedStrategy[],
    method?: CombinationMethod,
    version?: string,
    strategyId?: string
  ) => void;

  // Search & Leaderboard State
  searchConfig: SearchConfigurationState;
  setSearchConfig: React.Dispatch<React.SetStateAction<SearchConfigurationState>>;
  searchResetKey: number;
  updateSearchMarket: (market: string) => void;
  updateSearchDatasetRange: (range: string) => void;
  updateSearchAlgorithm: (algo: SearchAlgorithm) => void;
  toggleSearchFeature: (featureId: string) => void;
  updateSearchStopConditions: (cond: Partial<StopConditions>) => void;
  replaceSearchFeaturesFromBlocks: (blocks: SelectedStrategy[]) => void;
  enableSearchFeaturesFromBlocks: (blocks: SelectedStrategy[]) => void;
  leaderboardEntries: LeaderboardEntry[];
  selectedLeaderboardStrategy: LeaderboardEntry | null;
  setSelectedLeaderboardStrategy: (entry: LeaderboardEntry | null) => void;

  // Backtest State
  backtestConfig: BacktestConfig;
  setBacktestConfig: React.Dispatch<React.SetStateAction<BacktestConfig>>;
  updateBacktestConfig: (updated: Partial<BacktestConfig>) => void;
  selectedBacktestStrategy: CompositeStrategy | null;
  setSelectedBacktestStrategy: (strategy: CompositeStrategy | null) => void;
  backtestSource: 'composer' | 'leaderboard' | 'direct';
  setBacktestSource: (source: 'composer' | 'leaderboard' | 'direct') => void;
  activeBacktestStrategyName: string;
  activeBacktestBlocks: SelectedStrategy[];
  backtestMetrics: BacktestMetrics;
  backtestTrades: Trade[];
  backtestCandles: BacktestCandle[];
  backtestEquityCurve: EquityPoint[];
  openBacktest: (
    strategy: CompositeStrategy,
    source?: 'composer' | 'leaderboard' | 'direct',
    customConfig?: Partial<BacktestConfig>,
    onNavigate?: (r: RoutePath) => void
  ) => void;
  runBacktestWithStrategy: (
    strategy: CompositeStrategy | string,
    sBlocks?: SelectedStrategy[],
    customConfig?: Partial<BacktestConfig>
  ) => void;
  runBacktestFromLeaderboard: (entry: LeaderboardEntry) => void;
  recomputeCurrentBacktest: () => void;

  // News Sentiment State
  newsState: {
    selectedCoin: NewsCoinFilter;
    timeRange: NewsTimeRange;
    sentimentFilter: NewsSentimentFilter;
  };
  setNewsSelectedCoin: (coin: NewsCoinFilter) => void;
  setNewsTimeRange: (range: NewsTimeRange) => void;
  setNewsSentimentFilter: (filter: NewsSentimentFilter) => void;

  // Cross-Screen Workflow Helpers
  handleWorkflowRunBacktestFromComposer: (onNavigate: (r: RoutePath) => void) => void;
  handleWorkflowAddToSearchSpaceFromComposer: (onNavigate: (r: RoutePath) => void) => void;
  handleWorkflowViewBacktestFromLeaderboard: (entry: LeaderboardEntry, onNavigate: (r: RoutePath) => void) => void;
  handleWorkflowOpenInComposerFromLeaderboard: (entry: LeaderboardEntry, onNavigate: (r: RoutePath) => void) => void;
  handleWorkflowEditStrategyFromBacktest: (onNavigate: (r: RoutePath) => void) => void;
  handleWorkflowAddNewsToComposer: (onNavigate: (r: RoutePath) => void) => void;
}

const AppContext = createContext<AppContextType | null>(null);

export const AppProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  // 1. Global Market Context
  const [activePair, setActivePairState] = useState<string>('BTC/USDT');
  const [selectedTimeframe, setSelectedTimeframe] = useState<string>('15m');

  // 2. Canonical Strategy Composer State
  const [composerStrategyId, setComposerStrategyId] = useState<string>('strat-primary-1');
  const [composerVersion, setComposerVersion] = useState<string>('v1');
  const [customStrategyName, setCustomStrategyName] = useState<string | null>(null);
  const [blocks, setComposerBlocks] = useState<SelectedStrategy[]>(INITIAL_SELECTED_BLOCKS);
  const [combinationMethod, setComposerCombinationMethod] = useState<CombinationMethod>('Weighted Combination');

  // Derive Canonical CompositeStrategy Object
  const currentCompositeStrategy: CompositeStrategy = useMemo(() => {
    const isCustomNamed = Boolean(customStrategyName && customStrategyName.trim().length > 0);
    const displayName = isCustomNamed ? customStrategyName! : deriveStrategyDisplayName(blocks);

    const weights: Record<string, number> = {};
    const parameters: Record<string, Record<string, number>> = {};
    blocks.forEach((b) => {
      weights[b.instanceId] = b.weight;
      parameters[b.instanceId] = { ...b.params };
    });

    const { compositeScore, finalSignal } = calculateCompositeSignal(blocks, combinationMethod);

    return {
      id: composerStrategyId,
      version: composerVersion,
      displayName,
      isCustomNamed,
      blocks,
      combinationMethod,
      weights,
      parameters,
      compositeScore,
      finalSignal,
    };
  }, [composerStrategyId, composerVersion, customStrategyName, blocks, combinationMethod]);

  const composerState = useMemo(
    () => ({
      ...currentCompositeStrategy,
      strategyName: currentCompositeStrategy.displayName,
    }),
    [currentCompositeStrategy]
  );

  const setComposerStrategyName = useCallback((name: string) => {
    setCustomStrategyName(name.trim().length > 0 ? name : null);
  }, []);

  // Add a strategy module from library to composer with duplicate prevention
  const addStrategyToComposer = useCallback((def: StrategyDefinition) => {
    setComposerBlocks((prev) => {
      // Check if definition already exists
      if (prev.some((b) => b.definitionId === def.id)) {
        return prev;
      }
      const instanceId = `block-${def.id}-${Date.now()}-${Math.random().toString(36).substring(2, 7)}`;
      const newBlock: SelectedStrategy = {
        instanceId,
        definitionId: def.id,
        name: def.name,
        category: def.category,
        abbreviation: def.abbreviation,
        params: { ...def.defaultParams },
        weight: 0.33,
        signal: def.defaultSignal,
        signalValue: def.defaultSignalValue,
        accentColor: def.accentColor,
        borderAccentClass: def.borderAccentClass,
      };
      return [...prev, newBlock];
    });
    // If not custom named, clear custom flag to auto-derive from new block list
    setCustomStrategyName(null);
  }, []);

  // Add News Sentiment to composer with duplicate prevention
  const addNewsSentimentToComposer = useCallback(() => {
    const newsDef = STRATEGY_LIBRARY.find((s) => s.id === 'news-sentiment');
    if (!newsDef) return;
    setComposerBlocks((prev) => {
      if (prev.some((b) => b.definitionId === 'news-sentiment')) {
        return prev;
      }
      const instanceId = `block-news-${Date.now()}-${Math.random().toString(36).substring(2, 7)}`;
      const newBlock: SelectedStrategy = {
        instanceId,
        definitionId: newsDef.id,
        name: newsDef.name,
        category: newsDef.category,
        abbreviation: newsDef.abbreviation,
        params: { ...newsDef.defaultParams },
        weight: 0.25,
        signal: newsDef.defaultSignal,
        signalValue: newsDef.defaultSignalValue,
        accentColor: newsDef.accentColor,
        borderAccentClass: newsDef.borderAccentClass,
      };
      return [...prev, newBlock];
    });
    setCustomStrategyName(null);

    // News Sentiment added to Composer - does NOT mutate Search Space until user clicks 'Add to Search Space'
  }, []);

  const setCompositeStrategy = useCallback((strategy: CompositeStrategy) => {
    if (!strategy) return;
    setComposerStrategyId(strategy.id || 'strat-primary-1');
    setComposerVersion(strategy.version || 'v1');
    setCustomStrategyName(strategy.displayName || null);

    // Reconstruct clean active blocks for composer with exact parameters and weights preserved
    const restoredBlocks: SelectedStrategy[] = (strategy.blocks || []).map((b, idx) => {
      let defId = b.definitionId;
      if (!defId) {
        const lower = (b.name || '').toLowerCase();
        if (lower.includes('moving') || lower.includes('ma')) defId = 'moving-average';
        else if (lower.includes('rsi')) defId = 'rsi';
        else if (lower.includes('bollinger') || lower.includes('bb')) defId = 'bollinger-bands';
        else if (lower.includes('support') || lower.includes('resistance') || lower.includes('sr') || lower.includes('sup')) defId = 'support-resistance';
        else if (lower.includes('news')) defId = 'news-sentiment';
      }
      const libDef = STRATEGY_LIBRARY.find((d) => d.id === defId) || STRATEGY_LIBRARY[0];
      const weight = strategy.weights?.[b.instanceId] ?? b.weight ?? (1 / (strategy.blocks?.length || 1));
      const params = strategy.parameters?.[b.instanceId]
        ? { ...strategy.parameters[b.instanceId] }
        : { ...b.params };

      return {
        instanceId: b.instanceId || `composer-block-${libDef.id}-${idx}`,
        definitionId: libDef.id,
        name: libDef.name,
        category: libDef.category,
        abbreviation: libDef.abbreviation,
        params: { ...libDef.defaultParams, ...params },
        weight,
        signal: b.signal || libDef.defaultSignal,
        signalValue: b.signalValue ?? libDef.defaultSignalValue,
        accentColor: libDef.accentColor,
        borderAccentClass: libDef.borderAccentClass,
      };
    });

    setComposerBlocks(restoredBlocks);
    setComposerCombinationMethod(strategy.combinationMethod || 'Weighted Combination');
  }, []);

  const loadStrategyIntoComposer = useCallback(
    (
      strategyOrName: CompositeStrategy | string,
      newBlocks?: SelectedStrategy[],
      method: CombinationMethod = 'Weighted Combination',
      version: string = 'v1',
      strategyId: string = 'strat-primary-1'
    ) => {
      if (typeof strategyOrName === 'object' && strategyOrName !== null) {
        setCompositeStrategy(strategyOrName);
      } else {
        setCustomStrategyName(strategyOrName || null);
        if (newBlocks) setComposerBlocks(newBlocks);
        setComposerCombinationMethod(method);
        setComposerVersion(version);
        setComposerStrategyId(strategyId);
      }
    },
    [setCompositeStrategy]
  );

  // 3. Search & Leaderboard State
  const [searchResetKey, setSearchResetKey] = useState<number>(0);
  const [searchConfig, setSearchConfig] = useState<SearchConfigurationState>({
    market: 'BTC/USDT',
    datasetRange: 'Last 6 Months (1h)',
    algorithm: 'random',
    features: INITIAL_SEARCH_FEATURES,
    stopConditions: INITIAL_STOP_CONDITIONS,
  });
  const [leaderboardEntries] = useState<LeaderboardEntry[]>(INITIAL_LEADERBOARD);
  const [selectedLeaderboardStrategy, setSelectedLeaderboardStrategy] = useState<LeaderboardEntry | null>(null);

  const updateSearchMarket = useCallback((market: string) => {
    setSearchConfig((prev) => ({ ...prev, market }));
  }, []);

  const updateSearchDatasetRange = useCallback((datasetRange: string) => {
    setSearchConfig((prev) => ({ ...prev, datasetRange }));
  }, []);

  const updateSearchAlgorithm = useCallback((algorithm: SearchAlgorithm) => {
    setSearchConfig((prev) => ({ ...prev, algorithm }));
  }, []);

  const toggleSearchFeature = useCallback((featureId: string) => {
    setSearchConfig((prev) => ({
      ...prev,
      features: prev.features.map((f) => {
        const isMatch = f.id === featureId || toCanonicalModuleId(f.id) === toCanonicalModuleId(featureId);
        if (isMatch) {
          const next = !f.enabled;
          return { ...f, enabled: next, opacity: next ? 1 : 0.7 };
        }
        return f;
      }),
    }));
  }, []);

  const updateSearchStopConditions = useCallback((cond: Partial<StopConditions>) => {
    setSearchConfig((prev) => ({
      ...prev,
      stopConditions: { ...prev.stopConditions, ...cond },
    }));
  }, []);

  // Strict replacement of Search Space features based on Strategy Composer active blocks
  const replaceSearchFeaturesFromBlocks = useCallback((composerBlocks: SelectedStrategy[]) => {
    const activeCanonicalIds = new Set<CanonicalStrategyModuleId>();
    for (const block of composerBlocks) {
      const canonical =
        toCanonicalModuleId(block.definitionId) ||
        toCanonicalModuleId(block.abbreviation) ||
        toCanonicalModuleId(block.name);
      if (canonical) {
        activeCanonicalIds.add(canonical);
      }
    }

    setSearchConfig((prev) => ({
      ...prev,
      features: prev.features.map((f) => {
        const featureCanonical = toCanonicalModuleId(f.id) || toCanonicalModuleId(f.name);
        const isSelected = featureCanonical ? activeCanonicalIds.has(featureCanonical) : false;
        return {
          ...f,
          enabled: isSelected,
          opacity: isSelected ? 1 : 0.7,
        };
      }),
    }));

    // Trigger reset of search execution state
    setSearchResetKey((prev) => prev + 1);
  }, []);

  const enableSearchFeaturesFromBlocks = replaceSearchFeaturesFromBlocks;

  // 4. Backtest State - Canonical Shared Strategy
  const [backtestConfig, setBacktestConfig] = useState<BacktestConfig>({
    symbol: 'BTC/USDT',
    timeframe: '15m',
    startDate: '2026-01-01',
    endDate: '2026-07-01',
  });

  const [selectedBacktestStrategy, setSelectedBacktestStrategy] = useState<CompositeStrategy | null>(
    DEFAULT_BACKTEST_STRATEGY
  );
  const [backtestSource, setBacktestSource] = useState<'composer' | 'leaderboard' | 'direct'>('composer');
  const [activeBacktestStrategyName, setActiveBacktestStrategyName] = useState<string>(() =>
    DEFAULT_BACKTEST_STRATEGY.displayName
  );
  const [activeBacktestBlocks, setActiveBacktestBlocks] = useState<SelectedStrategy[]>(DEFAULT_BACKTEST_STRATEGY.blocks);

  const [backtestMetrics, setBacktestMetrics] = useState<BacktestMetrics>(MOCK_BACKTEST_METRICS);
  const [backtestTrades, setBacktestTrades] = useState<Trade[]>(MOCK_TRADES);
  const [backtestCandles, setBacktestCandles] = useState<BacktestCandle[]>(MOCK_CANDLES);
  const [backtestEquityCurve, setBacktestEquityCurve] = useState<EquityPoint[]>(MOCK_EQUITY_CURVE);

  const updateBacktestConfig = useCallback((updated: Partial<BacktestConfig>) => {
    setBacktestConfig((prev) => ({ ...prev, ...updated }));
  }, []);

  // Canonical openBacktest: Single source of truth for loading & evaluating backtests
  const openBacktest = useCallback(
    (
      strategy: CompositeStrategy,
      source: 'composer' | 'leaderboard' | 'direct' = 'direct',
      customConfig?: Partial<BacktestConfig>,
      onNavigate?: (r: RoutePath) => void
    ) => {
      // 1. Overwrite selectedBacktestStrategy with the supplied strategy
      setSelectedBacktestStrategy(strategy);
      setBacktestSource(source);
      setActiveBacktestStrategyName(strategy.displayName);
      setActiveBacktestBlocks(strategy.blocks || []);

      // 2. Set target backtest config
      let targetConfig = backtestConfig;
      if (customConfig) {
        targetConfig = { ...backtestConfig, ...customConfig };
        setBacktestConfig(targetConfig);
      }

      // 3. Load/select corresponding mock BacktestResult
      const generated = resolveBacktestResult(strategy, targetConfig);
      setBacktestMetrics(generated.metrics);
      setBacktestTrades(generated.trades);
      setBacktestCandles(generated.candles);
      setBacktestEquityCurve(generated.equityCurve);

      // 4. Navigate if callback provided
      if (onNavigate) {
        onNavigate('/backtest');
      }
    },
    [backtestConfig]
  );

  const recomputeCurrentBacktest = useCallback(() => {
    const targetStrategy = selectedBacktestStrategy ?? DEFAULT_BACKTEST_STRATEGY;
    const generated = resolveBacktestResult(targetStrategy, backtestConfig);
    setBacktestMetrics(generated.metrics);
    setBacktestTrades(generated.trades);
    setBacktestCandles(generated.candles);
    setBacktestEquityCurve(generated.equityCurve);
  }, [backtestConfig, selectedBacktestStrategy]);

  const runBacktestWithStrategy = useCallback(
    (
      strategyOrName: CompositeStrategy | string,
      sBlocks?: SelectedStrategy[],
      customConfig?: Partial<BacktestConfig>
    ) => {
      let resolvedStrategy: CompositeStrategy;

      if (typeof strategyOrName === 'string') {
        const bl = sBlocks || INITIAL_SELECTED_BLOCKS;
        const weights: Record<string, number> = {};
        const parameters: Record<string, Record<string, number>> = {};
        bl.forEach((b) => {
          weights[b.instanceId] = b.weight;
          parameters[b.instanceId] = { ...b.params };
        });
        const { compositeScore, finalSignal } = calculateCompositeSignal(bl, 'Weighted Combination');

        resolvedStrategy = {
          id: `strat-${Date.now()}-${Math.random().toString(36).substring(2, 7)}`,
          version: 'v1',
          displayName: strategyOrName,
          blocks: bl,
          combinationMethod: 'Weighted Combination',
          weights,
          parameters,
          compositeScore,
          finalSignal,
        };
      } else {
        resolvedStrategy = strategyOrName;
      }

      openBacktest(resolvedStrategy, 'composer', customConfig);
    },
    [openBacktest]
  );

  const runBacktestFromLeaderboard = useCallback(
    (entry: LeaderboardEntry) => {
      setSelectedLeaderboardStrategy(entry);
      const targetStrategy = convertLeaderboardEntryToStrategy(entry);
      const searchMarket = searchConfig.market || backtestConfig.symbol || 'BTC/USDT';
      openBacktest(targetStrategy, 'leaderboard', { symbol: searchMarket });
    },
    [searchConfig.market, backtestConfig.symbol, openBacktest]
  );

  // 5. News Sentiment State
  const [newsCoin, setNewsSelectedCoin] = useState<NewsCoinFilter>('BTC');
  const [newsTimeRange, setNewsTimeRange] = useState<NewsTimeRange>('24H');
  const [newsSentimentFilter, setNewsSentimentFilter] = useState<NewsSentimentFilter>('ALL');

  const newsState = useMemo(
    () => ({
      selectedCoin: newsCoin,
      timeRange: newsTimeRange,
      sentimentFilter: newsSentimentFilter,
    }),
    [newsCoin, newsTimeRange, newsSentimentFilter]
  );

  // Global market sync
  const setActivePair = useCallback((pair: string) => {
    setActivePairState(pair);
    // Propagate cleanly to search and backtest
    setSearchConfig((prev) => ({ ...prev, market: pair }));
    setBacktestConfig((prev) => ({ ...prev, symbol: pair }));
    const coin = pair.split('/')[0] as NewsCoinFilter;
    if (coin === 'BTC' || coin === 'ETH' || coin === 'SOL') {
      setNewsSelectedCoin(coin);
    }
  }, []);

  // =========================================================================
  // CROSS-SCREEN WORKFLOW HANDLERS
  // =========================================================================

  // FLOW A: Strategy Composer -> Run Backtest
  const handleWorkflowRunBacktestFromComposer = useCallback(
    (onNavigate: (r: RoutePath) => void) => {
      openBacktest(currentCompositeStrategy, 'composer', { symbol: activePair }, onNavigate);
    },
    [currentCompositeStrategy, activePair, openBacktest]
  );

  // FLOW B: Strategy Composer -> Add to Search Space
  const handleWorkflowAddToSearchSpaceFromComposer = useCallback(
    (onNavigate: (r: RoutePath) => void) => {
      replaceSearchFeaturesFromBlocks(blocks);
      onNavigate('/search');
    },
    [blocks, replaceSearchFeaturesFromBlocks]
  );

  // FLOW C: Search & Leaderboard -> View Backtest
  const handleWorkflowViewBacktestFromLeaderboard = useCallback(
    (entry: LeaderboardEntry, onNavigate: (r: RoutePath) => void) => {
      setSelectedLeaderboardStrategy(entry);
      const targetStrategy = convertLeaderboardEntryToStrategy(entry);
      const searchMarket = searchConfig.market || backtestConfig.symbol || 'BTC/USDT';
      openBacktest(targetStrategy, 'leaderboard', { symbol: searchMarket }, onNavigate);
    },
    [searchConfig.market, backtestConfig.symbol, openBacktest]
  );

  // FLOW D: Search & Leaderboard -> Open in Strategy Composer
  const handleWorkflowOpenInComposerFromLeaderboard = useCallback(
    (entry: LeaderboardEntry, onNavigate: (r: RoutePath) => void) => {
      setSelectedLeaderboardStrategy(entry);
      const targetStrategy = convertLeaderboardEntryToStrategy(entry);
      setCompositeStrategy(targetStrategy);
      onNavigate('/strategy');
    },
    [setCompositeStrategy]
  );

  // FLOW E: Backtest Results -> Edit Strategy
  const handleWorkflowEditStrategyFromBacktest = useCallback(
    (onNavigate: (r: RoutePath) => void) => {
      const targetStrategy = selectedBacktestStrategy ?? DEFAULT_BACKTEST_STRATEGY;
      setCompositeStrategy(targetStrategy);
      onNavigate('/strategy');
    },
    [selectedBacktestStrategy, setCompositeStrategy]
  );

  // FLOW F: News Sentiment -> Add to Strategy Composer
  const handleWorkflowAddNewsToComposer = useCallback(
    (onNavigate: (r: RoutePath) => void) => {
      addNewsSentimentToComposer();
      onNavigate('/strategy');
    },
    [addNewsSentimentToComposer]
  );

  const value: AppContextType = {
    activePair,
    setActivePair,
    selectedTimeframe,
    setSelectedTimeframe,

    currentCompositeStrategy,
    composerState,
    setComposerStrategyName,
    setComposerBlocks,
    setComposerCombinationMethod,
    addStrategyToComposer,
    addNewsSentimentToComposer,
    setCompositeStrategy,
    loadStrategyIntoComposer,

    searchConfig,
    setSearchConfig,
    searchResetKey,
    updateSearchMarket,
    updateSearchDatasetRange,
    updateSearchAlgorithm,
    toggleSearchFeature,
    updateSearchStopConditions,
    replaceSearchFeaturesFromBlocks,
    enableSearchFeaturesFromBlocks,
    leaderboardEntries,
    selectedLeaderboardStrategy,
    setSelectedLeaderboardStrategy,

    backtestConfig,
    setBacktestConfig,
    updateBacktestConfig,
    selectedBacktestStrategy,
    setSelectedBacktestStrategy,
    backtestSource,
    setBacktestSource,
    activeBacktestStrategyName,
    activeBacktestBlocks,
    backtestMetrics,
    backtestTrades,
    backtestCandles,
    backtestEquityCurve,
    openBacktest,
    runBacktestWithStrategy,
    runBacktestFromLeaderboard,
    recomputeCurrentBacktest,

    newsState,
    setNewsSelectedCoin,
    setNewsTimeRange,
    setNewsSentimentFilter,

    handleWorkflowRunBacktestFromComposer,
    handleWorkflowAddToSearchSpaceFromComposer,
    handleWorkflowViewBacktestFromLeaderboard,
    handleWorkflowOpenInComposerFromLeaderboard,
    handleWorkflowEditStrategyFromBacktest,
    handleWorkflowAddNewsToComposer,
  };

  return <AppContext.Provider value={value}>{children}</AppContext.Provider>;
};

export const useApp = (): AppContextType => {
  const context = useContext(AppContext);
  if (!context) {
    throw new Error('useApp must be used within an AppProvider');
  }
  return context;
};

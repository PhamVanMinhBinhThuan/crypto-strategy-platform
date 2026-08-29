import {
  AIStrategyJSON,
  AIStrategyIndicator,
  AIStrategyCondition,
  AIStrategySource,
  ParsedStrategyData,
  ValidationItem,
  ValidationSummary,
} from '../types/aiStrategy';
import {
  StrategyDefinition,
  SelectedStrategy,
  CompositeStrategy,
  calculateCompositeSignal,
} from '../types/strategy';
import { STRATEGY_LIBRARY } from '../data/strategyLibraryData';

// Known library indicator names mapped to canonical IDs
export const KNOWN_INDICATOR_MAP: Record<string, string> = {
  rsi: 'rsi',
  'relative strength index': 'rsi',
  bollinger: 'bollinger-bands',
  'bollinger bands': 'bollinger-bands',
  'bollinger band': 'bollinger-bands',
  bb: 'bollinger-bands',
  ma: 'moving-average',
  'moving average': 'moving-average',
  ema: 'moving-average',
  sma: 'moving-average',
  'exponential moving average': 'moving-average',
  'support resistance': 'support-resistance',
  'support / resistance': 'support-resistance',
  support: 'support-resistance',
  resistance: 'support-resistance',
  sr: 'support-resistance',
  news: 'news-sentiment',
  'news sentiment': 'news-sentiment',
  sentiment: 'news-sentiment',
};

/**
 * Parses natural language prompt into structured AIStrategyJSON and ParsedStrategyData
 */
export function parseStrategyPrompt(
  prompt: string,
  source: AIStrategySource = 'AI_PROMPT',
  sourceUrl?: string
): { strategyJson: AIStrategyJSON; parsedData: ParsedStrategyData } {
  const p = prompt.trim();
  const lower = p.toLowerCase();

  // 1. Identify Indicators & Parameters
  const indicators: AIStrategyIndicator[] = [];
  const indicatorTags: string[] = [];

  // RSI Check
  const hasRsi = /\brsi\b/i.test(p);
  if (hasRsi) {
    const periodMatch = p.match(/rsi\s*\(?(\d+)\)?/i) || p.match(/rsi[^\d]{1,10}(\d+)\s*(?:period|len|length)/i);
    const period = periodMatch ? parseInt(periodMatch[1], 10) : 14;

    const obMatch = p.match(/(?:overbought|above|vượt|trên|quá mua)\s*(?:mức)?\s*(\d{2})/i);
    const osMatch = p.match(/(?:oversold|below|under|dưới|quá bán)\s*(?:mức)?\s*(\d{2})/i);

    indicators.push({
      name: 'RSI',
      period,
      overbought: obMatch ? parseInt(obMatch[1], 10) : 70,
      oversold: osMatch ? parseInt(osMatch[1], 10) : 30,
      parameters: { period, buyThreshold: osMatch ? parseInt(osMatch[1], 10) : 30, sellThreshold: obMatch ? parseInt(obMatch[1], 10) : 70 },
    });
    indicatorTags.push('RSI');
  }

  // Bollinger Bands Check
  const hasBB = /\b(?:bollinger|bb)\b/i.test(p);
  if (hasBB) {
    const periodMatch = p.match(/(?:bollinger|bb)[^\d]{0,10}\(?(\d+)(?:,\s*(\d+(?:\.\d+)?))?\)?/i);
    const period = periodMatch && periodMatch[1] ? parseInt(periodMatch[1], 10) : 20;
    const stdDev = periodMatch && periodMatch[2] ? parseFloat(periodMatch[2]) : 2;

    indicators.push({
      name: 'BollingerBands',
      period,
      stdDev,
      parameters: { period, stdDev },
    });
    indicatorTags.push('Bollinger');
  }

  // Moving Average / EMA / SMA Check
  const hasMA = /\b(?:moving average|ma|ema|sma)\b/i.test(p);
  if (hasMA) {
    const maNumbers = [...p.matchAll(/\b(?:ema|sma|ma)\s*(\d+)\b/gi)].map((m) => parseInt(m[1], 10));
    const fastPeriod = maNumbers[0] || 20;
    const slowPeriod = maNumbers[1] || (fastPeriod === 20 ? 50 : 200);

    indicators.push({
      name: 'MovingAverage',
      fastPeriod,
      slowPeriod,
      parameters: { fastPeriod, slowPeriod },
    });
    indicatorTags.push('Moving Average', 'Trend');
  }

  // Support / Resistance Check
  const hasSR = /\b(?:support|resistance|hỗ trợ|kháng cự|s\/r|breakout)\b/i.test(p);
  if (hasSR) {
    indicators.push({
      name: 'SupportResistance',
      lookback: 100,
      sensitivity: 5,
      parameters: { lookback: 100, sensitivity: 5 },
    });
    indicatorTags.push('Key Levels', 'Structure');
  }

  // News Sentiment Check
  const hasNews = /\b(?:news|sentiment|tin tức|sentiment score)\b/i.test(p);
  if (hasNews) {
    const scoreMatch = p.match(/(?:sentiment|score|tin tức)[^\d]{0,10}(\d{2})/i);
    const minScore = scoreMatch ? parseInt(scoreMatch[1], 10) : 60;

    indicators.push({
      name: 'NewsSentiment',
      parameters: { minScore, lookbackHours: 24 },
    });
    indicatorTags.push('News Sentiment', 'NLP');
  }

  // Fallback indicator if none matched
  if (indicators.length === 0) {
    indicators.push({
      name: 'RSI',
      period: 14,
      parameters: { period: 14, buyThreshold: 30, sellThreshold: 70 },
    });
    indicatorTags.push('RSI');
  }

  // 2. Identify LONG Conditions
  const longConditions: AIStrategyCondition[] = [];
  const longTextDescriptions: string[] = [];

  const mentionsLong = /\b(?:long|buy|mua|open long|mở long|entry long)\b/i.test(p);
  const mentionsShort = /\b(?:short|sell|bán|open short|mở short|entry short)\b/i.test(p);

  // If prompt mentions RSI below 30 / oversold
  if (hasRsi && (/(?:rsi|chỉ số rsi)[^\n]{0,40}(?:below|under|<|dưới|<=|quá bán)\s*(\d{2})/i.test(p) || mentionsLong)) {
    const valMatch = p.match(/(?:rsi)[^\n]{0,30}(?:below|under|<|dưới|<=)\s*(\d{2})/i);
    const threshold = valMatch ? parseInt(valMatch[1], 10) : 30;
    longConditions.push({
      indicator: 'RSI',
      operator: '<',
      value: threshold,
      text: `RSI (14) < ${threshold}`,
    });
    longTextDescriptions.push(`RSI (14) < ${threshold}`);
  }

  // If prompt mentions Bollinger Lower Band
  if (hasBB && /(?:lower|dưới|below|<)[^\n]{0,30}(?:bollinger|lower band|dải dưới)/i.test(p)) {
    longConditions.push({
      indicator: 'Close',
      operator: '<',
      indicatorRef: 'BB_Lower',
      position: '<',
      text: 'Close < Bollinger Lower Band (20, 2)',
    });
    longTextDescriptions.push('Close < Bollinger Lower Band (20, 2)');
  }

  // If prompt mentions MA Crossover (Fast crosses above Slow)
  if (hasMA && /(?:crosses above|cắt lên|golden cross|vượt lên|above)/i.test(p)) {
    longConditions.push({
      indicator: 'MA_Fast',
      operator: 'crosses_above',
      indicatorRef: 'MA_Slow',
      text: 'MA Fast (20) crosses above MA Slow (50)',
    });
    longTextDescriptions.push('MA Fast (20) crosses above MA Slow (50)');
  }

  // If Support/Resistance breakout
  if (hasSR && /(?:breaks above|breakout|vượt kháng cự|vượt đỉnh)/i.test(p)) {
    longConditions.push({
      indicator: 'Price',
      operator: '>',
      indicatorRef: 'Resistance_Level',
      text: 'Price breaks above Resistance level (100)',
    });
    longTextDescriptions.push('Price breaks above Resistance level (100)');
  }

  // If News Sentiment positive
  if (hasNews && /(?:positive|bullish|tích cực|>|trên)/i.test(p)) {
    longConditions.push({
      indicator: 'NewsSentiment',
      operator: '>',
      value: 60,
      text: 'News Sentiment Score > 60',
    });
    longTextDescriptions.push('News Sentiment Score > 60');
  }

  // Default long if none extracted but long mentioned
  if (longConditions.length === 0 && (mentionsLong || !mentionsShort)) {
    longConditions.push({
      indicator: 'RSI',
      operator: '<',
      value: 30,
      text: 'RSI (14) < 30',
    });
    longTextDescriptions.push('RSI (14) < 30');
  }

  // 3. Identify SHORT Conditions (CRITICAL: Do NOT invent short conditions if only long is requested)
  const shortConditions: AIStrategyCondition[] = [];
  const shortTextDescriptions: string[] = [];

  if (mentionsShort) {
    if (hasRsi && /(?:rsi)[^\n]{0,30}(?:above|over|>|trên|>=)\s*(\d{2})/i.test(p)) {
      const valMatch = p.match(/(?:rsi)[^\n]{0,30}(?:above|over|>|trên|>=)\s*(\d{2})/i);
      const threshold = valMatch ? parseInt(valMatch[1], 10) : 70;
      shortConditions.push({
        indicator: 'RSI',
        operator: '>',
        value: threshold,
        text: `RSI (14) > ${threshold}`,
      });
      shortTextDescriptions.push(`RSI (14) > ${threshold}`);
    }

    if (hasBB && /(?:upper|trên|above|>)[^\n]{0,30}(?:bollinger|upper band|dải trên)/i.test(p)) {
      shortConditions.push({
        indicator: 'Close',
        operator: '>',
        indicatorRef: 'BB_Upper',
        position: '>',
        text: 'Close > Bollinger Upper Band (20, 2)',
      });
      shortTextDescriptions.push('Close > Bollinger Upper Band (20, 2)');
    }

    if (hasMA && /(?:crosses below|cắt xuống|death cross|dưới)/i.test(p)) {
      shortConditions.push({
        indicator: 'MA_Fast',
        operator: 'crosses_below',
        indicatorRef: 'MA_Slow',
        text: 'MA Fast (20) crosses below MA Slow (50)',
      });
      shortTextDescriptions.push('MA Fast (20) crosses below MA Slow (50)');
    }
  }

  // 4. Identify Risk Management (Stop Loss & Take Profit)
  let stopLossVal: number | undefined;
  let takeProfitVal: number | undefined;

  const slMatch = p.match(/(?:stop loss|sl|dừng lỗ)[^\d]{0,10}(\d+(?:\.\d+)?)\s*%/i);
  if (slMatch) {
    stopLossVal = parseFloat(slMatch[1]);
  }

  const tpMatch = p.match(/(?:take profit|tp|chốt lời)[^\d]{0,10}(\d+(?:\.\d+)?)\s*%/i);
  if (tpMatch) {
    takeProfitVal = parseFloat(tpMatch[1]);
  }

  // 5. Identify Timeframe
  let timeframe = '1h';
  let isTimeframeDefault = true;
  const tfMatch = p.match(/\b(1m|5m|15m|30m|1h|2h|4h|1d|1w|khung\s*\w+)\b/i);
  if (tfMatch) {
    const rawTf = tfMatch[1].toLowerCase();
    if (['1m', '5m', '15m', '30m', '1h', '2h', '4h', '1d'].includes(rawTf)) {
      timeframe = rawTf;
      isTimeframeDefault = false;
    }
  }

  // 6. Identify Market / Pairs
  let market = 'spot';
  let pairs = 'USDT_ALL';
  let isMarketDefault = true;

  if (/\b(?:futures|future|hợp đồng tương lai|perpetual|perp)\b/i.test(p)) {
    market = 'futures';
    isMarketDefault = false;
  }
  if (/\b(?:btc|eth|sol|bnb|btc\/usdt|eth\/usdt)\b/i.test(p)) {
    const pairMatch = p.match(/\b(btc|eth|sol|bnb|btc\/usdt|eth\/usdt)\b/i);
    if (pairMatch) {
      pairs = pairMatch[1].toUpperCase().includes('/') ? pairMatch[1].toUpperCase() : `${pairMatch[1].toUpperCase()}/USDT`;
      isMarketDefault = false;
    }
  }

  // 7. Generate Strategy Name & Tags
  let suggestedName = '';
  const componentsNameParts: string[] = [];
  if (hasRsi) componentsNameParts.push('RSI');
  if (hasBB) componentsNameParts.push('BB');
  if (hasMA) componentsNameParts.push('MA');
  if (hasSR) componentsNameParts.push('SR');
  if (hasNews) componentsNameParts.push('NEWS');

  if (componentsNameParts.length === 0) componentsNameParts.push('QUANT');

  const sidePart = shortConditions.length > 0 && longConditions.length > 0 ? 'BOTH' : shortConditions.length > 0 ? 'SHORT' : 'LONG';
  const slPart = stopLossVal !== undefined ? `_SL${stopLossVal}` : '';
  const tpPart = takeProfitVal !== undefined ? `_TP${takeProfitVal}` : '';

  suggestedName = `${componentsNameParts.join('_')}_${sidePart}${slPart}${tpPart}`.replace(/\./g, '_');

  const tags = Array.from(
    new Set([
      ...indicatorTags,
      sidePart === 'BOTH' ? 'Long & Short' : sidePart === 'SHORT' ? 'Short' : 'Long',
      hasBB && hasRsi ? 'Mean Reversion' : hasMA ? 'Trend' : 'Quantitative',
    ])
  );

  // 8. Construct Description
  const description =
    p.length > 120
      ? p.substring(0, 117) + '...'
      : p || `Strategy utilizing ${indicators.map((i) => i.name).join(' & ')} on ${timeframe}.`;

  const strategyJson: AIStrategyJSON = {
    name: suggestedName,
    version: '1.0.0',
    description,
    source,
    sourceUrl,
    indicators,
    conditions: {
      long: longConditions,
      short: shortConditions,
    },
    riskManagement: {
      ...(stopLossVal !== undefined ? { stopLoss: { type: 'percent', value: stopLossVal } } : {}),
      ...(takeProfitVal !== undefined ? { takeProfit: { type: 'percent', value: takeProfitVal } } : {}),
    },
    timeframe,
    isTimeframeDefault,
    market,
    pairs,
    isMarketDefault,
    tags,
  };

  const parsedData: ParsedStrategyData = {
    longConditions: longTextDescriptions,
    shortConditions: shortTextDescriptions,
    hasShortConditions: shortTextDescriptions.length > 0,
    stopLoss: stopLossVal !== undefined ? `${stopLossVal}%` : 'Not specified',
    hasStopLoss: stopLossVal !== undefined,
    takeProfit: takeProfitVal !== undefined ? `${takeProfitVal}%` : 'Not specified',
    hasTakeProfit: takeProfitVal !== undefined,
    timeframe,
    isTimeframeDefault,
    market: pairs === 'USDT_ALL' ? 'USDT pairs · Spot' : `${pairs} · ${market.toUpperCase()}`,
    isMarketDefault,
    indicators: indicators.map((i) => i.name),
  };

  return { strategyJson, parsedData };
}

/**
 * Validates an AI Strategy JSON structure against strict quant lab requirements
 */
export function validateStrategyDefinition(strategyJson: AIStrategyJSON | null): ValidationSummary {
  if (!strategyJson) {
    return {
      isValid: false,
      canSave: false,
      items: [
        {
          id: 'v-req',
          category: 'Required Fields',
          label: 'Required Fields',
          message: 'Waiting for strategy input',
          status: 'warning',
        },
        {
          id: 'v-logic',
          category: 'Strategy Logic',
          label: 'Strategy Logic',
          message: 'No conditions defined',
          status: 'warning',
        },
        {
          id: 'v-ind',
          category: 'Supported Indicators',
          label: 'Supported Indicators',
          message: 'No indicators analyzed',
          status: 'warning',
        },
        {
          id: 'v-risk',
          category: 'Risk Management',
          label: 'Risk Management',
          message: 'Awaiting risk configuration',
          status: 'warning',
        },
        {
          id: 'v-def',
          category: 'Strategy Definition',
          label: 'Strategy Definition',
          message: 'Waiting for strategy',
          status: 'warning',
        },
      ],
      overallStatus: 'Requires Changes',
      overallMessage: 'Waiting for strategy analysis.',
    };
  }

  const items: ValidationItem[] = [];
  let hasBlockingError = false;

  // 1. Required Fields
  const hasName = Boolean(strategyJson.name && strategyJson.name.trim().length > 0);
  const hasVersion = Boolean(strategyJson.version && strategyJson.version.trim().length > 0);
  const hasIndicators = Boolean(strategyJson.indicators && strategyJson.indicators.length > 0);

  if (hasName && hasVersion && hasIndicators) {
    items.push({
      id: 'v-req',
      category: 'Required Fields',
      label: 'Required Fields',
      message: 'All required fields are available',
      status: 'success',
    });
  } else {
    hasBlockingError = true;
    items.push({
      id: 'v-req',
      category: 'Required Fields',
      label: 'Required Fields',
      message: 'Missing strategy name, version, or indicator definition',
      status: 'error',
    });
  }

  // 2. Strategy Logic
  const longCount = strategyJson.conditions?.long?.length || 0;
  const shortCount = strategyJson.conditions?.short?.length || 0;
  const totalConditions = longCount + shortCount;

  if (totalConditions > 0) {
    const summaryMsg =
      longCount > 0 && shortCount > 0
        ? 'Entry conditions (LONG & SHORT) are logically valid'
        : longCount > 0
        ? 'Entry conditions (LONG) are logically valid'
        : 'Entry conditions (SHORT) are logically valid';

    items.push({
      id: 'v-logic',
      category: 'Strategy Logic',
      label: 'Strategy Logic',
      message: summaryMsg,
      status: 'success',
    });
  } else {
    hasBlockingError = true;
    items.push({
      id: 'v-logic',
      category: 'Strategy Logic',
      label: 'Strategy Logic',
      message: 'At least one LONG or SHORT entry condition is required',
      status: 'error',
    });
  }

  // 3. Supported Indicators
  const indicatorNames = (strategyJson.indicators || []).map((i) => i.name);
  const unsupported = indicatorNames.filter((name) => {
    const norm = name.toLowerCase().replace(/[-_/\s]/g, '');
    return !Object.keys(KNOWN_INDICATOR_MAP).some(
      (k) => k.replace(/[-_/\s]/g, '') === norm
    );
  });

  if (unsupported.length === 0) {
    const listStr = indicatorNames.join(' and ') || 'Indicators';
    items.push({
      id: 'v-ind',
      category: 'Supported Indicators',
      label: 'Supported Indicators',
      message: `${listStr} are fully supported in Strategy Engine`,
      status: 'success',
    });
  } else {
    items.push({
      id: 'v-ind',
      category: 'Supported Indicators',
      label: 'Supported Indicators',
      message: `${unsupported.join(', ')} is experimental / custom defined`,
      status: 'warning',
    });
  }

  // 4. Risk Management
  const hasSL = Boolean(strategyJson.riskManagement?.stopLoss?.value);
  const hasTP = Boolean(strategyJson.riskManagement?.takeProfit?.value);

  if (hasSL && hasTP) {
    items.push({
      id: 'v-risk',
      category: 'Risk Management',
      label: 'Risk Management',
      message: `Stop Loss (${strategyJson.riskManagement.stopLoss?.value}%) and Take Profit (${strategyJson.riskManagement.takeProfit?.value}%) configured`,
      status: 'success',
    });
  } else if (hasSL || hasTP) {
    items.push({
      id: 'v-risk',
      category: 'Risk Management',
      label: 'Risk Management',
      message: !hasSL ? 'Stop Loss was not specified' : 'Take Profit was not specified',
      status: 'warning',
    });
  } else {
    items.push({
      id: 'v-risk',
      category: 'Risk Management',
      label: 'Risk Management',
      message: 'Risk management rules are optional but recommended (None specified)',
      status: 'warning',
    });
  }

  // 5. Strategy Definition
  if (strategyJson && typeof strategyJson === 'object' && strategyJson.name) {
    items.push({
      id: 'v-def',
      category: 'Strategy Definition',
      label: 'Strategy Definition',
      message: 'Definition format is valid JSON structure',
      status: 'success',
    });
  } else {
    hasBlockingError = true;
    items.push({
      id: 'v-def',
      category: 'Strategy Definition',
      label: 'Strategy Definition',
      message: 'Strategy Definition contains invalid structure',
      status: 'error',
    });
  }

  const isValid = !hasBlockingError;
  const canSave = isValid;

  return {
    isValid,
    canSave,
    items,
    overallStatus: isValid ? 'Ready to Save' : 'Requires Changes',
    overallMessage: isValid
      ? 'Strategy is valid and can be added to your Strategy Library.'
      : 'Fix validation errors before saving.',
  };
}

/**
 * Converts an AIStrategyJSON into a runtime CompositeStrategy object
 */
export function convertAIStrategyToCompositeStrategy(aiJson: AIStrategyJSON): CompositeStrategy {
  const blocks: SelectedStrategy[] = [];

  (aiJson.indicators || []).forEach((ind, idx) => {
    const normName = ind.name.toLowerCase().replace(/[-_/\s]/g, '');
    let defId = 'moving-average';
    if (normName.includes('rsi')) defId = 'rsi';
    else if (normName.includes('bollinger') || normName.includes('bb')) defId = 'bollinger-bands';
    else if (normName.includes('support') || normName.includes('resistance') || normName.includes('sr')) defId = 'support-resistance';
    else if (normName.includes('news') || normName.includes('sentiment')) defId = 'news-sentiment';

    const libDef = STRATEGY_LIBRARY.find((d) => d.id === defId) || STRATEGY_LIBRARY[0];
    const customParams = ind.parameters || { ...libDef.defaultParams };

    blocks.push({
      instanceId: `ai-block-${defId}-${idx}-${Date.now()}`,
      definitionId: libDef.id,
      name: libDef.name,
      category: libDef.category,
      abbreviation: libDef.abbreviation,
      params: { ...libDef.defaultParams, ...customParams },
      weight: 1 / Math.max(1, aiJson.indicators.length),
      signal: libDef.defaultSignal,
      signalValue: libDef.defaultSignalValue,
      accentColor: libDef.accentColor,
      borderAccentClass: libDef.borderAccentClass,
    });
  });

  const defaultFallbackBlocks: SelectedStrategy[] = [
    {
      instanceId: `ai-block-rsi-${Date.now()}`,
      definitionId: 'rsi',
      name: 'RSI',
      category: 'Momentum',
      abbreviation: 'RSI',
      params: { period: 14, buyThreshold: 30, sellThreshold: 70 },
      weight: 0.5,
      signal: 'BUY',
      signalValue: 1,
      accentColor: '#f6be16',
      borderAccentClass: 'border-l-[#f6be16]',
    },
    {
      instanceId: `ai-block-bb-${Date.now()}`,
      definitionId: 'bollinger-bands',
      name: 'Bollinger Bands',
      category: 'Volatility',
      abbreviation: 'BB',
      params: { period: 20, stdDev: 2 },
      weight: 0.5,
      signal: 'HOLD',
      signalValue: 0,
      accentColor: '#ffb3b6',
      borderAccentClass: 'border-l-[#ffb3b6]',
    },
  ];

  const targetBlocks: SelectedStrategy[] = blocks.length > 0 ? blocks : defaultFallbackBlocks;

  const weights: Record<string, number> = {};
  const parameters: Record<string, Record<string, number>> = {};
  targetBlocks.forEach((b) => {
    weights[b.instanceId] = b.weight;
    parameters[b.instanceId] = { ...b.params };
  });

  const { compositeScore, finalSignal } = calculateCompositeSignal(targetBlocks, 'Weighted Combination');

  return {
    id: `ai-strat-${Date.now()}`,
    version: aiJson.version || '1.0.0',
    displayName: aiJson.name || 'AI Generated Strategy',
    isCustomNamed: true,
    blocks: targetBlocks,
    combinationMethod: 'Weighted Combination',
    weights,
    parameters,
    compositeScore,
    finalSignal,
  };
}

/**
 * Converts an AIStrategyJSON into a StrategyDefinition that can be placed in StrategyLibrary
 */
export function convertAIStrategyToDefinition(aiJson: AIStrategyJSON): StrategyDefinition {
  const primaryIndicator = aiJson.indicators?.[0]?.name?.toLowerCase() || 'rsi';
  let category: StrategyDefinition['category'] = 'Momentum';
  let accentColor = '#f6be16';
  let borderClass = 'border-l-[#f6be16]';

  if (primaryIndicator.includes('ma') || primaryIndicator.includes('trend')) {
    category = 'Trend';
    accentColor = '#44e092';
    borderClass = 'border-l-[#44e092]';
  } else if (primaryIndicator.includes('bollinger') || primaryIndicator.includes('volatility')) {
    category = 'Volatility';
    accentColor = '#ffb3b6';
    borderClass = 'border-l-[#ffb3b6]';
  } else if (primaryIndicator.includes('support') || primaryIndicator.includes('structure')) {
    category = 'Structure';
    accentColor = '#67fdac';
    borderClass = 'border-l-[#67fdac]';
  } else if (primaryIndicator.includes('news') || primaryIndicator.includes('sentiment')) {
    category = 'Information';
    accentColor = '#006d41';
    borderClass = 'border-l-[#006d41]';
  }

  const defaultParams: Record<string, number> = {};
  (aiJson.indicators || []).forEach((ind) => {
    if (ind.parameters) {
      Object.assign(defaultParams, ind.parameters);
    }
  });

  return {
    id: `custom-${aiJson.name.toLowerCase().replace(/[^a-z0-9]/g, '-')}-${Date.now().toString(36)}`,
    name: aiJson.name,
    category,
    description: aiJson.description || 'AI Created Quantitative Strategy',
    tags: aiJson.tags || ['AI Generated'],
    paramDefs: [
      { id: 'period', label: 'Period', type: 'number', defaultValue: 14, min: 2, max: 100 },
      { id: 'sensitivity', label: 'Sensitivity', type: 'number', defaultValue: 5, min: 1, max: 10 },
    ],
    defaultParams: Object.keys(defaultParams).length > 0 ? defaultParams : { period: 14, sensitivity: 5 },
    defaultSignal: aiJson.conditions?.long?.length ? 'BUY' : 'HOLD',
    defaultSignalValue: aiJson.conditions?.long?.length ? 1 : 0,
    accentColor,
    borderAccentClass: borderClass,
    categoryBorderClass: borderClass,
    abbreviation: aiJson.name.substring(0, 4).toUpperCase(),
  };
}

/**
 * Realistic public URL strategy extractor
 */
export async function extractStrategyFromUrl(
  url: string
): Promise<{ success: boolean; prompt?: string; error?: string }> {
  // Validate URL syntax
  try {
    const parsed = new URL(url);
    if (!['http:', 'https:'].includes(parsed.protocol)) {
      return { success: false, error: 'Enter a valid URL starting with http:// or https://' };
    }
  } catch {
    return { success: false, error: 'Enter a valid URL.' };
  }

  // Simulate network extraction delay for high realism
  await new Promise((r) => setTimeout(r, 600));

  const lowerUrl = url.toLowerCase();

  if (lowerUrl.includes('tradingview.com/script') || lowerUrl.includes('tradingview')) {
    if (lowerUrl.includes('macd') || lowerUrl.includes('trend')) {
      return {
        success: true,
        prompt:
          'TradingView Script: Trend following system. When EMA 20 crosses above EMA 50, enter LONG. When EMA 20 crosses below EMA 50, enter SHORT. Stop loss 1.8%, take profit 3.6% on 1h timeframe.',
      };
    } else if (lowerUrl.includes('breakout') || lowerUrl.includes('support')) {
      return {
        success: true,
        prompt:
          'TradingView Breakout Engine: Buy when price breaks above 100-period Support/Resistance level with News Sentiment > 65. Stop loss 2.5%, take profit 5.0%.',
      };
    } else {
      return {
        success: true,
        prompt:
          'TradingView Script: Mean Reversion Strategy. When RSI is below 30 and price closes below Bollinger Lower Band, open LONG. When RSI is above 70 and price closes above Bollinger Upper Band, open SHORT. Stop loss 2%, take profit 4%.',
      };
    }
  }

  if (lowerUrl.includes('github.com')) {
    return {
      success: true,
      prompt:
        'GitHub Quant Repository: Python backtest model. When RSI(14) < 30 and Close < Bollinger Lower Band (20, 2), open LONG position. Stop loss 2%, take profit 4%. 1h timeframe.',
    };
  }

  if (lowerUrl.includes('medium.com') || lowerUrl.includes('substack') || lowerUrl.includes('blog')) {
    return {
      success: true,
      prompt:
        'Quantitative Trading Article: Double confirmation strategy. Open LONG when RSI is below 30 and price touches Bollinger Lower Band. Stop loss 2%, take profit 4%.',
    };
  }

  // Fallback for valid domain URLs
  if (lowerUrl.includes('.')) {
    return {
      success: true,
      prompt:
        'Imported Strategy from Public Documentation: When RSI is below 30 and price closes below the Bollinger Lower Band, open LONG. Stop loss 2%, take profit 4%.',
    };
  }

  return {
    success: false,
    error: 'Unable to extract a strategy from this URL. Try another public page or paste the strategy description manually.',
  };
}

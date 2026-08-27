import { Candle, Timeframe, TradeMarker } from '../types';

export interface TimeframeChartData {
  timeframe: Timeframe;
  candles: Candle[];
  maSlowPath: string;
  maFastPath: string;
  upperBandPath: string;
  lowerBandPath: string;
  supportY: number;
  supportHeight: number;
  resistanceY: number;
  resistanceHeight: number;
  buyMarkers: TradeMarker[];
  sellMarkers: TradeMarker[];
  priceLabels: string[];
}

/**
 * Generate distinct realistic candle series and SVG indicator curves for each timeframe
 */
export function getChartDataForTimeframe(timeframe: Timeframe, basePrice: number = 64230.15): TimeframeChartData {
  // Different characteristics per timeframe
  switch (timeframe) {
    case '1m':
      return generateDataset({
        timeframe: '1m',
        stepMinutes: 1,
        candlesCount: 16,
        basePrice,
        priceLabels: ['64.35k', '64.28k', '64.23k', '64.18k', 'Vol'],
        slowWave: { amplitude: 35, freq: 3.2, offset: 230 },
        fastWave: { amplitude: 45, freq: 4.5, offset: 215 },
        supportY: 290,
        supportHeight: 18,
        resistanceY: 85,
        resistanceHeight: 18,
        buyMarkers: [{ type: 'BUY', price: 64190, time: '14:02', index: 320 }],
        sellMarkers: [{ type: 'SELL', price: 64310, time: '14:08', index: 780 }],
        pattern: [0.2, 0.6, -0.4, 0.9, -0.2, -0.8, 0.5, 1.1, -0.3, 0.4, 0.8, -0.6, 0.2, 0.7, -0.1, 0.5]
      });

    case '5m':
      // Top Left primary detailed chart
      return generateDataset({
        timeframe: '5m',
        stepMinutes: 5,
        candlesCount: 18,
        basePrice,
        priceLabels: ['64.5k', '64.2k', '64.0k', '63.8k', 'Vol'],
        slowWave: { amplitude: 60, freq: 2.1, offset: 220 },
        fastWave: { amplitude: 75, freq: 2.8, offset: 200 },
        supportY: 300,
        supportHeight: 20,
        resistanceY: 80,
        resistanceHeight: 20,
        buyMarkers: [{ type: 'BUY', price: 63950, time: '13:45', index: 400 }],
        sellMarkers: [{ type: 'SELL', price: 64230, time: '14:00', index: 800 }],
        pattern: [-0.5, 0.8, -0.6, 0.3, 1.4, -0.2, -0.9, 0.4, 1.2, 1.8, -0.3, -0.7, 0.6, 1.3, 0.2, -0.4, 0.9, 0.5]
      });

    case '15m':
      // Top Right chart
      return generateDataset({
        timeframe: '15m',
        stepMinutes: 15,
        candlesCount: 16,
        basePrice: basePrice + 120,
        priceLabels: ['64.8k', '64.5k', '64.2k', '63.9k', 'Vol'],
        slowWave: { amplitude: 50, freq: 1.6, offset: 240 },
        fastWave: { amplitude: 65, freq: 2.2, offset: 210 },
        supportY: 310,
        supportHeight: 22,
        resistanceY: 75,
        resistanceHeight: 18,
        buyMarkers: [{ type: 'BUY', price: 64050, time: '12:30', index: 280 }],
        sellMarkers: [{ type: 'SELL', price: 64650, time: '13:45', index: 850 }],
        pattern: [1.1, 0.5, -0.3, -0.8, -1.2, -0.4, 0.6, 1.5, 0.8, 0.2, -0.5, -0.9, 0.4, 1.1, 1.6, 0.7]
      });

    case '30m':
      return generateDataset({
        timeframe: '30m',
        stepMinutes: 30,
        candlesCount: 15,
        basePrice: basePrice - 80,
        priceLabels: ['65.0k', '64.5k', '64.0k', '63.5k', 'Vol'],
        slowWave: { amplitude: 70, freq: 1.4, offset: 230 },
        fastWave: { amplitude: 80, freq: 1.9, offset: 220 },
        supportY: 315,
        supportHeight: 20,
        resistanceY: 70,
        resistanceHeight: 20,
        buyMarkers: [{ type: 'BUY', price: 63800, time: '10:00', index: 350 }],
        sellMarkers: [{ type: 'SELL', price: 64700, time: '12:30', index: 720 }],
        pattern: [-0.8, -1.2, -0.4, 0.5, 1.2, 0.9, 0.1, -0.6, -1.0, 0.3, 0.8, 1.4, 1.1, 0.6, 0.9]
      });

    case '1h':
      // Bottom Left chart
      return generateDataset({
        timeframe: '1h',
        stepMinutes: 60,
        candlesCount: 16,
        basePrice: basePrice - 240,
        priceLabels: ['65.2k', '64.6k', '64.0k', '63.4k', 'Vol'],
        slowWave: { amplitude: 85, freq: 1.2, offset: 250 },
        fastWave: { amplitude: 95, freq: 1.5, offset: 225 },
        supportY: 320,
        supportHeight: 22,
        resistanceY: 65,
        resistanceHeight: 22,
        buyMarkers: [{ type: 'BUY', price: 63600, time: '08:00', index: 220 }],
        sellMarkers: [{ type: 'SELL', price: 64800, time: '12:00', index: 680 }],
        pattern: [0.6, 1.2, 1.8, 0.9, -0.2, -1.1, -1.5, -0.7, 0.2, 0.9, 1.5, 2.0, 1.3, 0.4, -0.3, 0.8]
      });

    case '2h':
      return generateDataset({
        timeframe: '2h',
        stepMinutes: 120,
        candlesCount: 14,
        basePrice: basePrice + 350,
        priceLabels: ['65.8k', '65.0k', '64.2k', '63.4k', 'Vol'],
        slowWave: { amplitude: 90, freq: 1.0, offset: 230 },
        fastWave: { amplitude: 100, freq: 1.3, offset: 210 },
        supportY: 325,
        supportHeight: 20,
        resistanceY: 60,
        resistanceHeight: 20,
        buyMarkers: [{ type: 'BUY', price: 63900, time: '04:00', index: 300 }],
        sellMarkers: [{ type: 'SELL', price: 65400, time: '10:00', index: 760 }],
        pattern: [-1.2, -0.6, 0.4, 1.1, 1.7, 1.2, 0.3, -0.5, -1.1, -0.2, 0.7, 1.5, 1.8, 1.0]
      });

    case '4h':
      // Bottom Right chart
      return generateDataset({
        timeframe: '4h',
        stepMinutes: 240,
        candlesCount: 15,
        basePrice: basePrice + 500,
        priceLabels: ['66.2k', '65.2k', '64.2k', '63.2k', 'Vol'],
        slowWave: { amplitude: 95, freq: 0.9, offset: 220 },
        fastWave: { amplitude: 110, freq: 1.1, offset: 200 },
        supportY: 330,
        supportHeight: 24,
        resistanceY: 55,
        resistanceHeight: 22,
        buyMarkers: [{ type: 'BUY', price: 63400, time: 'Aug 16', index: 180 }],
        sellMarkers: [{ type: 'SELL', price: 65800, time: 'Aug 17', index: 820 }],
        pattern: [1.4, 0.8, -0.3, -1.2, -1.8, -1.0, 0.2, 1.1, 2.2, 1.6, 0.7, -0.4, 0.8, 1.7, 2.1]
      });

    case '1d':
      return generateDataset({
        timeframe: '1d',
        stepMinutes: 1440,
        candlesCount: 14,
        basePrice: basePrice - 1100,
        priceLabels: ['68.0k', '65.5k', '63.0k', '60.5k', 'Vol'],
        slowWave: { amplitude: 110, freq: 0.7, offset: 240 },
        fastWave: { amplitude: 125, freq: 0.9, offset: 215 },
        supportY: 335,
        supportHeight: 25,
        resistanceY: 50,
        resistanceHeight: 24,
        buyMarkers: [{ type: 'BUY', price: 61200, time: 'Aug 12', index: 240 }],
        sellMarkers: [{ type: 'SELL', price: 66800, time: 'Aug 16', index: 750 }],
        pattern: [-1.8, -1.2, -0.4, 0.7, 1.6, 2.4, 1.7, 0.8, -0.2, -0.9, 0.5, 1.4, 2.1, 2.8]
      });

    default:
      return getChartDataForTimeframe('5m', basePrice);
  }
}

interface GeneratorParams {
  timeframe: Timeframe;
  stepMinutes: number;
  candlesCount: number;
  basePrice: number;
  priceLabels: string[];
  slowWave: { amplitude: number; freq: number; offset: number };
  fastWave: { amplitude: number; freq: number; offset: number };
  supportY: number;
  supportHeight: number;
  resistanceY: number;
  resistanceHeight: number;
  buyMarkers: TradeMarker[];
  sellMarkers: TradeMarker[];
  pattern: number[];
}

function generateDataset(params: GeneratorParams): TimeframeChartData {
  const candles: Candle[] = [];
  const now = Date.now();
  let currentClose = params.basePrice - 140;

  for (let i = 0; i < params.candlesCount; i++) {
    const timeOffset = (params.candlesCount - i) * params.stepMinutes * 60 * 1000;
    const date = new Date(now - timeOffset);
    const timeStr = `${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`;
    
    const factor = params.pattern[i % params.pattern.length] || 0.5;
    const open = currentClose;
    const change = factor * 45;
    const close = open + change;
    const high = Math.max(open, close) + Math.abs(factor) * 20 + 15;
    const low = Math.min(open, close) - Math.abs(factor) * 18 - 15;
    const volume = Math.floor(120 + Math.abs(change) * 5 + (i % 3) * 40);

    candles.push({
      time: timeStr,
      timestamp: date.getTime(),
      open: Math.round(open * 100) / 100,
      high: Math.round(high * 100) / 100,
      low: Math.round(low * 100) / 100,
      close: Math.round(close * 100) / 100,
      volume,
    });

    currentClose = close;
  }

  // Generate SVG spline paths
  const maSlowPoints: string[] = [];
  const maFastPoints: string[] = [];
  const upperBandPoints: string[] = [];
  const lowerBandPoints: string[] = [];

  const resolution = 24;
  for (let i = 0; i <= resolution; i++) {
    const progress = i / resolution;
    const x = Math.round(progress * 1000);

    const ySlow = Math.round(
      params.slowWave.offset - Math.sin(progress * Math.PI * params.slowWave.freq) * params.slowWave.amplitude
    );
    const yFast = Math.round(
      params.fastWave.offset - Math.sin((progress + 0.15) * Math.PI * params.fastWave.freq) * params.fastWave.amplitude
    );

    const yUpper = Math.max(30, ySlow - 40 - Math.sin(progress * Math.PI * 2) * 10);
    const yLower = Math.min(370, ySlow + 50 + Math.sin(progress * Math.PI * 2) * 10);

    maSlowPoints.push(`${i === 0 ? 'M' : 'L'} ${x} ${ySlow}`);
    maFastPoints.push(`${i === 0 ? 'M' : 'L'} ${x} ${yFast}`);
    upperBandPoints.push(`${i === 0 ? 'M' : 'L'} ${x} ${yUpper}`);
    lowerBandPoints.push(`${i === 0 ? 'M' : 'L'} ${x} ${yLower}`);
  }

  return {
    timeframe: params.timeframe,
    candles,
    maSlowPath: maSlowPoints.join(' '),
    maFastPath: maFastPoints.join(' '),
    upperBandPath: upperBandPoints.join(' '),
    lowerBandPath: lowerBandPoints.join(' '),
    supportY: params.supportY,
    supportHeight: params.supportHeight,
    resistanceY: params.resistanceY,
    resistanceHeight: params.resistanceHeight,
    buyMarkers: params.buyMarkers,
    sellMarkers: params.sellMarkers,
    priceLabels: params.priceLabels,
  };
}

export const ALL_SUPPORTED_TIMEFRAMES: Timeframe[] = ['1m', '5m', '15m', '30m', '1h', '2h', '4h', '1d'];

export const DEFAULT_CHART_INDICATORS = {
  MA: true,
  BB: true,
  RSI: false,
  'S/R': true,
};

import React from 'react';
import { MOCK_ASSETS } from '../../data/mockData';
import { formatCurrency, formatPercent, cn } from '../../utils/cn';
import { TrendingUp, TrendingDown, Activity } from 'lucide-react';

export const TickerBar: React.FC = () => {
  return (
    <div className="h-9 bg-slate-950 border-b border-slate-800/80 px-4 flex items-center gap-6 overflow-x-auto whitespace-nowrap text-xs select-none">
      <div className="flex items-center gap-1.5 text-cyan-400 font-semibold uppercase tracking-wider text-[10px] shrink-0">
        <Activity className="w-3.5 h-3.5 animate-pulse text-cyan-400" />
        <span>MARKETS</span>
      </div>

      <div className="flex items-center gap-6 divide-x divide-slate-800/60 font-mono">
        {MOCK_ASSETS.map((asset) => {
          const isUp = asset.change24h >= 0;
          return (
            <div key={asset.symbol} className="flex items-center gap-2 pl-4 first:pl-0 shrink-0">
              <span className="font-semibold text-slate-300">{asset.symbol.split('/')[0]}</span>
              <span className="text-slate-100 tabular-nums">{formatCurrency(asset.price, asset.price < 100 ? 2 : 1)}</span>
              <span
                className={cn(
                  'flex items-center text-[11px] font-semibold tabular-nums',
                  isUp ? 'text-emerald-400' : 'text-rose-400'
                )}
              >
                {isUp ? <TrendingUp className="w-3 h-3 inline mr-0.5" /> : <TrendingDown className="w-3 h-3 inline mr-0.5" />}
                {formatPercent(asset.change24h)}
              </span>
            </div>
          );
        })}
      </div>

      <div className="ml-auto shrink-0 flex items-center gap-4 text-[11px] text-slate-400 font-mono">
        <span className="flex items-center gap-1.5">
          <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-ping"></span>
          <span className="text-emerald-400 font-medium">STREAM READY</span>
        </span>
        <span className="text-slate-600">|</span>
        <span>GLOBAL 24H VOL: <strong className="text-slate-200">$51.4B</strong></span>
      </div>
    </div>
  );
};

import React from 'react';
import { ScreenId } from '../types';
import { 
  BarChart3, 
  GitBranch, 
  LineChart, 
  Trophy, 
  Newspaper, 
  FlaskConical, 
  Layers, 
  SlidersHorizontal,
  ChevronDown
} from 'lucide-react';
import { cn } from '../utils/cn';

interface HeaderProps {
  currentScreen: ScreenId;
  onNavigate: (screen: ScreenId) => void;
  activePair: string;
  onSelectPair: (pair: string) => void;
}

const SCREENS: { id: ScreenId; label: string; icon: React.ComponentType<{ className?: string }>; description: string }[] = [
  { id: 'market-dashboard', label: 'Market Dashboard', icon: BarChart3, description: 'Realtime multi-timeframe feeds' },
  { id: 'strategy-composer', label: 'Strategy Composer', icon: GitBranch, description: 'Visual multi-rule builder' },
  { id: 'backtest-results', label: 'Backtest Results', icon: LineChart, description: 'Historical tear-sheets & trades' },
  { id: 'search-leaderboard', label: 'Search & Leaderboard', icon: Trophy, description: 'Alpha strategies ranking' },
  { id: 'news-sentiment', label: 'News Sentiment', icon: Newspaper, description: 'NLP signals & macro feeds' },
];

export const Header: React.FC<HeaderProps> = ({
  currentScreen,
  onNavigate,
  activePair,
  onSelectPair,
}) => {
  return (
    <header className="bg-slate-900 border-b border-slate-800 select-none">
      {/* Top Branding & Main Controls */}
      <div className="h-14 px-4 flex items-center justify-between gap-4">
        {/* Brand */}
        <div className="flex items-center gap-3">
          <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-cyan-500 to-blue-600 flex items-center justify-center shadow-lg shadow-cyan-500/20">
            <FlaskConical className="w-5 h-5 text-slate-950 stroke-[2.5]" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <span className="font-extrabold text-sm tracking-tight text-slate-100 uppercase">
                Crypto Strategy Lab
              </span>
              <span className="text-[10px] font-mono font-semibold px-1.5 py-0.5 rounded bg-cyan-950 text-cyan-400 border border-cyan-800/60">
                PRO QUANT
              </span>
            </div>
            <p className="text-[11px] text-slate-400 font-medium">Quantitative Research & Multi-Timeframe Engine</p>
          </div>
        </div>

        {/* Global Active Pair Selector & Timeframe quick pill */}
        <div className="hidden md:flex items-center gap-3">
          <div className="flex items-center gap-2 bg-slate-950 px-3 py-1.5 rounded-lg border border-slate-800 text-xs font-mono">
            <span className="text-slate-500">PAIR:</span>
            <select
              value={activePair}
              onChange={(e) => onSelectPair(e.target.value)}
              aria-label="Target crypto asset pair"
              className="bg-transparent text-cyan-400 font-bold outline-none cursor-pointer"
            >
              <option value="BTC/USDT" className="bg-slate-900 text-slate-100">BTC/USDT (Bitcoin)</option>
              <option value="ETH/USDT" className="bg-slate-900 text-slate-100">ETH/USDT (Ethereum)</option>
              <option value="SOL/USDT" className="bg-slate-900 text-slate-100">SOL/USDT (Solana)</option>
              <option value="BNB/USDT" className="bg-slate-900 text-slate-100">BNB/USDT (BNB)</option>
              <option value="AVAX/USDT" className="bg-slate-900 text-slate-100">AVAX/USDT (Avalanche)</option>
            </select>
            <ChevronDown className="w-3.5 h-3.5 text-slate-500 pointer-events-none" />
          </div>

          <div className="flex items-center gap-1 bg-slate-950 px-2 py-1 rounded-lg border border-slate-800 text-[11px] font-mono text-slate-400">
            <Layers className="w-3.5 h-3.5 text-slate-400 mr-1" />
            <span className="text-slate-200 font-semibold">4 TF</span>
            <span className="text-slate-600">|</span>
            <span className="text-cyan-400">15m</span>
            <span>1h</span>
            <span>4h</span>
            <span>1d</span>
          </div>
        </div>

        {/* System / Environment badges */}
        <div className="flex items-center gap-2.5">
          <div className="hidden lg:flex items-center gap-2 text-xs font-mono bg-slate-950 px-2.5 py-1 rounded border border-slate-800">
            <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span>
            <span className="text-slate-300">ENGINE: <strong className="text-emerald-400">ONLINE</strong></span>
          </div>
          <div className="text-[11px] font-mono text-slate-400 bg-slate-800/80 px-2 py-1 rounded border border-slate-700">
            DESKTOP UI
          </div>
        </div>
      </div>

      {/* Screen Navigation Tabs (5 Main Screens) */}
      <nav className="px-4 flex items-center gap-1 bg-slate-950/90 border-t border-slate-800/80 overflow-x-auto">
        {SCREENS.map((screen) => {
          const Icon = screen.icon;
          const isActive = currentScreen === screen.id;
          return (
            <button
              key={screen.id}
              onClick={() => onNavigate(screen.id)}
              className={cn(
                'group relative flex items-center gap-2.5 px-4 py-2.5 text-xs font-medium transition-all select-none border-b-2 cursor-pointer shrink-0',
                isActive
                  ? 'border-cyan-400 text-cyan-300 bg-slate-900/90 font-semibold shadow-inner'
                  : 'border-transparent text-slate-400 hover:text-slate-200 hover:bg-slate-900/40'
              )}
            >
              <Icon className={cn('w-4 h-4 transition-transform group-hover:scale-110', isActive ? 'text-cyan-400' : 'text-slate-500')} />
              <span>{screen.label}</span>
              {isActive && (
                <span className="w-1.5 h-1.5 rounded-full bg-cyan-400 animate-pulse ml-0.5"></span>
              )}
            </button>
          );
        })}
      </nav>
    </header>
  );
};

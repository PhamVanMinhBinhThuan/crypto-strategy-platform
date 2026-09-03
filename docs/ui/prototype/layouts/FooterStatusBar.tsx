import React, { useState, useEffect } from 'react';
import { ScreenId } from '../types';
import { Wifi, Cpu, Database, Clock, HardDrive, Terminal } from 'lucide-react';

interface FooterStatusBarProps {
  currentScreen: ScreenId;
  activePair: string;
}

export const FooterStatusBar: React.FC<FooterStatusBarProps> = ({
  currentScreen,
  activePair,
}) => {
  const [utcTime, setUtcTime] = useState<string>('');

  useEffect(() => {
    const updateTime = () => {
      const now = new Date();
      setUtcTime(now.toUTCString().slice(17, 25) + ' UTC');
    };
    updateTime();
    const interval = setInterval(updateTime, 1000);
    return () => clearInterval(interval);
  }, []);

  const screenNames: Record<ScreenId, string> = {
    'market-dashboard': '01_MARKET_DASHBOARD',
    'strategy-composer': '02_STRATEGY_COMPOSER',
    'backtest-results': '03_BACKTEST_RESULTS',
    'search-leaderboard': '04_SEARCH_LEADERBOARD',
    'news-sentiment': '05_NEWS_SENTIMENT',
  };

  return (
    <footer className="h-7 bg-slate-950 border-t border-slate-800 px-3 flex items-center justify-between text-[11px] font-mono text-slate-400 select-none">
      {/* Left section: active view & pair context */}
      <div className="flex items-center gap-3">
        <div className="flex items-center gap-1.5 text-cyan-400 font-semibold">
          <Terminal className="w-3.5 h-3.5" />
          <span>{screenNames[currentScreen]}</span>
        </div>
        <span className="text-slate-700">|</span>
        <span className="text-slate-300">ACTIVE: <strong className="text-slate-100">{activePair}</strong></span>
        <span className="text-slate-700">|</span>
        <span className="text-slate-400">DATA FEED: <strong className="text-slate-300">MOCK_STREAM (SYNTH)</strong></span>
      </div>

      {/* Right section: System telemetry & latency */}
      <div className="flex items-center gap-4">
        <div className="flex items-center gap-1.5">
          <Database className="w-3 h-3 text-slate-500" />
          <span>CACHE: <strong className="text-slate-300">4 TF BUFFERS</strong></span>
        </div>
        <span className="text-slate-700">|</span>
        <div className="flex items-center gap-1.5">
          <Cpu className="w-3 h-3 text-emerald-400" />
          <span>WORKER: <strong className="text-emerald-400">IDLE</strong></span>
        </div>
        <span className="text-slate-700">|</span>
        <div className="flex items-center gap-1.5">
          <Wifi className="w-3 h-3 text-emerald-400" />
          <span>PING: <strong className="text-emerald-400">14ms</strong></span>
        </div>
        <span className="text-slate-700">|</span>
        <div className="flex items-center gap-1.5 text-slate-300">
          <Clock className="w-3 h-3 text-slate-500" />
          <span>{utcTime || '00:00:00 UTC'}</span>
        </div>
      </div>
    </footer>
  );
};

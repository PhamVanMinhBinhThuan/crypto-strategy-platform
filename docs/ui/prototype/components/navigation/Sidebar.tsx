import React from 'react';
import { RoutePath } from '../../types';
import { SidebarItem } from './SidebarItem';
import { 
  TrendingUp, 
  LayoutDashboard, 
  SlidersHorizontal, 
  LineChart, 
  Trophy, 
  Flame, 
  Settings, 
  HelpCircle 
} from 'lucide-react';

export interface SidebarProps {
  currentRoute: RoutePath;
  onNavigate: (route: RoutePath) => void;
}

interface NavItem {
  label: string;
  path: RoutePath;
  icon: React.ComponentType<{ className?: string }>;
}

const MAIN_NAV_ITEMS: NavItem[] = [
  { label: 'Market Dashboard', path: '/market', icon: LayoutDashboard },
  { label: 'Strategy Composer', path: '/strategy', icon: SlidersHorizontal },
  { label: 'Backtest Results', path: '/backtest', icon: LineChart },
  { label: 'Search & Leaderboard', path: '/search', icon: Trophy },
  { label: 'News Sentiment', path: '/news', icon: Flame },
];

export const Sidebar: React.FC<SidebarProps> = ({
  currentRoute,
  onNavigate,
}) => {
  return (
    <nav className="w-64 h-full fixed left-0 top-0 border-r border-[#3c4a40] bg-[#0b0e11] flex flex-col p-2 z-50 select-none">
      {/* Product Branding: Crypto Strategy Lab */}
      <div className="px-3 py-4 mb-4 border-b border-[#323538] flex items-center gap-3">
        <div 
          aria-label="Quant Icon" 
          className="w-8 h-8 rounded bg-[#02c076] flex items-center justify-center text-[#004728] font-bold shrink-0"
        >
          <TrendingUp className="w-5 h-5 stroke-[2.5]" />
        </div>
        <div className="min-w-0">
          <div className="font-bold text-[16px] text-[#44e092] leading-tight tracking-tight truncate">
            Crypto Strategy Lab
          </div>
          <div className="text-[10px] text-[#bbcabd] uppercase tracking-wider font-bold truncate">
            Institutional Grade
          </div>
        </div>
      </div>

      {/* Main 5 Primary Navigation Items */}
      <ul className="flex-1 space-y-1">
        {MAIN_NAV_ITEMS.map((item) => (
          <SidebarItem
            key={item.path}
            label={item.label}
            icon={item.icon}
            path={item.path}
            isActive={currentRoute === item.path}
            onClick={() => onNavigate(item.path)}
          />
        ))}
      </ul>

      {/* Footer Utilities */}
      <ul className="mt-auto border-t border-[#323538] pt-2 space-y-1">
        <li>
          <button
            type="button"
            className="w-full flex items-center gap-3 px-3 py-2 text-[#bbcabd] hover:bg-[#1d2023] hover:text-[#e1e2e7] transition-colors text-sm rounded-[2px] cursor-pointer active:scale-95 duration-100"
          >
            <Settings className="w-4 h-4 shrink-0" />
            <span>Settings</span>
          </button>
        </li>
        <li>
          <button
            type="button"
            className="w-full flex items-center gap-3 px-3 py-2 text-[#bbcabd] hover:bg-[#1d2023] hover:text-[#e1e2e7] transition-colors text-sm rounded-[2px] cursor-pointer active:scale-95 duration-100"
          >
            <HelpCircle className="w-4 h-4 shrink-0" />
            <span>Support</span>
          </button>
        </li>
      </ul>
    </nav>
  );
};

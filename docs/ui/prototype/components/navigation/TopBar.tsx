import React, { useState, useRef, useEffect } from 'react';
import { Bell, User, CheckCircle2 } from 'lucide-react';
import { MarketSelector } from '../common/MarketSelector';
import { StatusBadge } from '../common/StatusBadge';
import { cn } from '../../utils/cn';

export interface TopBarProps {
  activePair?: string;
  onSelectPair?: (pair: string) => void;
  price?: string;
  change24h?: string;
  isPositive?: boolean;
}

export const TopBar: React.FC<TopBarProps> = ({
  activePair = 'BTC/USDT',
  onSelectPair = () => {},
  price = '$64,230.15',
  change24h = '+1.24%',
  isPositive = true,
}) => {
  const [showNotifications, setShowNotifications] = useState(false);
  const [showProfileMenu, setShowProfileMenu] = useState(false);
  const [notificationCount, setNotificationCount] = useState(2);

  const notificationsRef = useRef<HTMLDivElement>(null);
  const profileRef = useRef<HTMLDivElement>(null);

  // Close popovers on click outside
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (
        notificationsRef.current &&
        !notificationsRef.current.contains(event.target as Node)
      ) {
        setShowNotifications(false);
      }
      if (
        profileRef.current &&
        !profileRef.current.contains(event.target as Node)
      ) {
        setShowProfileMenu(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  return (
    <header className="fixed top-0 right-0 left-64 h-12 border-b border-[#3c4a40] bg-[#1d2023] flex justify-between items-center px-4 z-40 select-none">
      {/* Left: Market selector & Live status */}
      <div className="flex items-center gap-4">
        <nav className="flex items-center gap-4 font-mono text-[14px]">
          <MarketSelector selectedPair={activePair} onSelectPair={onSelectPair} />
          <span className="text-[#3c4a40] select-none">|</span>
          <StatusBadge status="live" label="Live Status" />
        </nav>
      </div>

      {/* Right: Price, 24h change, Notifications & Profile controls (No Execute button) */}
      <div className="flex items-center gap-6">
        <div className="flex gap-4 text-[#bbcabd] font-mono text-xs">
          <span>
            Price: <span className="text-[#e1e2e7] font-semibold">{price}</span>
          </span>
          <span className={cn(isPositive ? 'text-[#44e092]' : 'text-[#ff5353]', 'font-semibold')}>
            24h: {change24h}
          </span>
        </div>

        <div className="flex items-center gap-2 text-[#bbcabd]">
          {/* Notifications Dropdown */}
          <div className="relative" ref={notificationsRef}>
            <button
              type="button"
              onClick={() => {
                setShowNotifications(!showNotifications);
                setShowProfileMenu(false);
              }}
              className="hover:text-[#e1e2e7] transition-colors w-8 h-8 flex items-center justify-center rounded hover:bg-[#323538] cursor-pointer relative"
              title="Notifications"
            >
              <Bell className="w-4 h-4" />
              {notificationCount > 0 && (
                <span className="absolute top-1.5 right-1.5 w-1.5 h-1.5 rounded-full bg-[#44e092]" />
              )}
            </button>

            {showNotifications && (
              <div className="absolute right-0 top-full mt-2 w-72 bg-[#1E2329] border border-[#2B3139] rounded-[2px] shadow-2xl z-50 p-3 font-sans text-xs">
                <div className="flex items-center justify-between pb-2 border-b border-[#2B3139] text-[#e1e2e7] font-semibold">
                  <span>System Alerts</span>
                  {notificationCount > 0 && (
                    <button
                      type="button"
                      onClick={() => setNotificationCount(0)}
                      className="text-[11px] text-[#44e092] hover:underline cursor-pointer"
                    >
                      Mark all read
                    </button>
                  )}
                </div>
                <div className="mt-2 space-y-2 text-[#bbcabd]">
                  <div className="p-2 rounded bg-[#191c1f] border border-[#2B3139]">
                    <div className="text-[#e1e2e7] font-medium flex items-center gap-1.5">
                      <CheckCircle2 className="w-3.5 h-3.5 text-[#44e092]" />
                      <span>Data Stream Connected</span>
                    </div>
                    <div className="text-[11px] text-[#869488] mt-0.5">
                      Binance WebSocket feed connected with 12ms latency.
                    </div>
                  </div>
                  <div className="p-2 rounded bg-[#191c1f] border border-[#2B3139]">
                    <div className="text-[#e1e2e7] font-medium">Market Volatility Alert</div>
                    <div className="text-[11px] text-[#869488] mt-0.5">
                      BTC 5m Bollinger Bands expanding rapidly.
                    </div>
                  </div>
                </div>
              </div>
            )}
          </div>

          {/* Profile Dropdown */}
          <div className="relative" ref={profileRef}>
            <button
              type="button"
              onClick={() => {
                setShowProfileMenu(!showProfileMenu);
                setShowNotifications(false);
              }}
              className="hover:text-[#e1e2e7] transition-colors w-8 h-8 flex items-center justify-center rounded hover:bg-[#323538] cursor-pointer"
              title="Account"
            >
              <User className="w-4 h-4" />
            </button>

            {showProfileMenu && (
              <div className="absolute right-0 top-full mt-2 w-48 bg-[#1E2329] border border-[#2B3139] rounded-[2px] shadow-2xl z-50 p-2 font-sans text-xs">
                <div className="px-2 py-1.5 border-b border-[#2B3139] text-[#e1e2e7] font-bold">
                  <div>Quant Operator</div>
                  <div className="text-[10px] text-[#869488] font-normal">quant@strategylab.io</div>
                </div>
                <div className="py-1 space-y-0.5">
                  <button
                    type="button"
                    className="w-full text-left px-2 py-1.5 rounded hover:bg-[#272a2e] text-[#bbcabd] hover:text-[#e1e2e7] cursor-pointer"
                  >
                    Account Settings
                  </button>
                  <button
                    type="button"
                    className="w-full text-left px-2 py-1.5 rounded hover:bg-[#272a2e] text-[#bbcabd] hover:text-[#e1e2e7] cursor-pointer"
                  >
                    API Keys
                  </button>
                  <button
                    type="button"
                    className="w-full text-left px-2 py-1.5 rounded hover:bg-[#272a2e] text-[#ff5353] cursor-pointer"
                  >
                    Sign Out
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </header>
  );
};

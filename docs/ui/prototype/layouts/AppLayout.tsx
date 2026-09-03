import React from 'react';
import { RoutePath } from '../types';
import { Sidebar } from '../components/navigation/Sidebar';
import { TopBar } from '../components/navigation/TopBar';

export interface AppLayoutProps {
  currentRoute: RoutePath;
  onNavigate: (route: RoutePath) => void;
  activePair: string;
  onSelectPair: (pair: string) => void;
  children: React.ReactNode;
}

export const AppLayout: React.FC<AppLayoutProps> = ({
  currentRoute,
  onNavigate,
  activePair,
  onSelectPair,
  children,
}) => {
  return (
    <div className="bg-[#0B0E11] text-[#e1e2e7] antialiased overflow-hidden h-screen flex select-none font-sans">
      {/* Reusable Persistent Left Sidebar */}
      <Sidebar
        currentRoute={currentRoute}
        onNavigate={onNavigate}
      />

      {/* Main Content Viewport */}
      <div className="ml-64 flex-1 flex flex-col h-full bg-[#0b0e11] relative overflow-hidden min-w-0 min-h-0">
        {/* Reusable Top Navigation Bar */}
        <TopBar
          activePair={activePair}
          onSelectPair={onSelectPair}
        />

        {/* Page Content Workspace (under 48px / 3rem topbar) */}
        <main className="mt-12 flex-1 flex flex-col min-w-0 min-h-0 overflow-y-auto overflow-x-hidden">
          {children}
        </main>
      </div>
    </div>
  );
};

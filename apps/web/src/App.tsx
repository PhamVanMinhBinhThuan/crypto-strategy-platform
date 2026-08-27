/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useState, useEffect, useCallback } from 'react';
import { RoutePath } from './types';
import { AppProvider, useApp } from './context/AppContext';
import { AppLayout } from './layouts/AppLayout';
import { MarketDashboardPage } from './pages/MarketDashboardPage';
import { StrategyComposerPage } from './pages/StrategyComposerPage';
import { BacktestResultsPage } from './pages/BacktestResultsPage';
import { SearchLeaderboardPage } from './pages/SearchLeaderboardPage';
import { NewsSentimentPage } from './pages/NewsSentimentPage';

const VALID_ROUTES: RoutePath[] = ['/market', '/strategy', '/backtest', '/search', '/news'];

function parseCurrentRoute(): RoutePath {
  // Support both hash routing (#/market or #market) and pathname routing (/market)
  const hash = window.location.hash.replace(/^#\/?/, '/');
  if (VALID_ROUTES.includes(hash as RoutePath)) {
    return hash as RoutePath;
  }
  const pathname = window.location.pathname;
  if (VALID_ROUTES.includes(pathname as RoutePath)) {
    return pathname as RoutePath;
  }
  return '/market';
}

function MainAppShell() {
  const [currentRoute, setCurrentRoute] = useState<RoutePath>(parseCurrentRoute);
  const { activePair, setActivePair } = useApp();

  const handleNavigate = useCallback((route: RoutePath) => {
    setCurrentRoute(route);
    window.location.hash = `#${route}`;
  }, []);

  useEffect(() => {
    const handleHashChange = () => {
      setCurrentRoute(parseCurrentRoute());
    };
    window.addEventListener('hashchange', handleHashChange);
    window.addEventListener('popstate', handleHashChange);
    return () => {
      window.removeEventListener('hashchange', handleHashChange);
      window.removeEventListener('popstate', handleHashChange);
    };
  }, []);

  const renderRouteContent = () => {
    switch (currentRoute) {
      case '/market':
        return <MarketDashboardPage activePair={activePair} />;
      case '/strategy':
        return <StrategyComposerPage onNavigate={handleNavigate} />;
      case '/backtest':
        return <BacktestResultsPage onNavigate={handleNavigate} />;
      case '/search':
        return <SearchLeaderboardPage onNavigate={handleNavigate} activePair={activePair} />;
      case '/news':
        return <NewsSentimentPage onNavigate={handleNavigate} />;
      default:
        return <MarketDashboardPage activePair={activePair} />;
    }
  };

  return (
    <AppLayout
      currentRoute={currentRoute}
      onNavigate={handleNavigate}
      activePair={activePair}
      onSelectPair={setActivePair}
    >
      {renderRouteContent()}
    </AppLayout>
  );
}

export default function App() {
  return (
    <AppProvider>
      <MainAppShell />
    </AppProvider>
  );
}

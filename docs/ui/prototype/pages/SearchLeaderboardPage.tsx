import React, { useState, useEffect, useRef, useMemo, useCallback } from 'react';
import { RoutePath } from '../types';
import {
  SearchStageId,
  SearchStageStatus,
  SearchLiveMetrics,
  CandidateEvaluation,
  CandidateStatus,
  WorkerInfo,
  LeaderboardEntry,
  LeaderboardSortField,
  SortDirection,
  TopKSelection,
} from '../types/search';
import {
  INITIAL_WORKERS_IDLE,
  INITIAL_CANDIDATE_IDLE,
  MOCK_CANDIDATE_POOL,
} from '../data/mockSearchData';
import { SearchConfiguration } from '../components/search/SearchConfiguration';
import { SearchPipeline } from '../components/search/SearchPipeline';
import { SearchStatusMetrics } from '../components/search/SearchStatusMetrics';
import { CurrentCandidateCard } from '../components/search/CurrentCandidateCard';
import { WorkerMonitor } from '../components/search/WorkerMonitor';
import { LeaderboardTable } from '../components/search/LeaderboardTable';
import { useApp } from '../context/AppContext';

export interface SearchLeaderboardPageProps {
  onNavigate: (route: RoutePath) => void;
  activePair?: string;
}

export const SearchLeaderboardPage: React.FC<SearchLeaderboardPageProps> = ({
  onNavigate,
}) => {
  const {
    searchConfig,
    updateSearchMarket,
    updateSearchDatasetRange,
    updateSearchAlgorithm,
    toggleSearchFeature,
    updateSearchStopConditions,
    leaderboardEntries,
    handleWorkflowViewBacktestFromLeaderboard,
    handleWorkflowOpenInComposerFromLeaderboard,
    searchResetKey,
  } = useApp();

  // 2. Execution / Simulation State
  const [searchState, setSearchState] = useState<'idle' | 'running' | 'paused' | 'completed' | 'stopped'>('idle');
  const [activeStage, setActiveStage] = useState<SearchStageId | null>(null);
  const [stageStatuses, setStageStatuses] = useState<Record<SearchStageId, SearchStageStatus>>({
    generate: 'idle',
    backtest: 'idle',
    evaluate: 'idle',
    rank: 'idle',
    leaderboard: 'idle',
  });

  // 3. Live Metrics
  const [metrics, setMetrics] = useState<SearchLiveMetrics>({
    candidatesTested: 0,
    candidatesRemaining: searchConfig.stopConditions.maxCandidates,
    elapsedSeconds: 0,
    bestScore: 0.0,
    scoreImprovement: 0.0,
  });

  // 4. Current Candidate & Workers
  const [currentCandidate, setCurrentCandidate] = useState<CandidateEvaluation>(INITIAL_CANDIDATE_IDLE);
  const [workers, setWorkers] = useState<WorkerInfo[]>(INITIAL_WORKERS_IDLE);

  // 5. Leaderboard State & Sorting
  const [leaderboard, setLeaderboard] = useState<LeaderboardEntry[]>(leaderboardEntries);
  const [sortField, setSortField] = useState<LeaderboardSortField>('score');
  const [sortDirection, setSortDirection] = useState<SortDirection>('desc');
  const [topK, setTopK] = useState<TopKSelection>(10);

  // Simulation timer & step counter references
  const timerRef = useRef<NodeJS.Timeout | null>(null);
  const simStepRef = useRef<NodeJS.Timeout | null>(null);
  const candidateIndexRef = useRef<number>(0);
  const candidateNumberRef = useRef<number>(1);
  const dynamicEntryCountRef = useRef<number>(0);
  const currentCandidateRef = useRef<CandidateEvaluation>(currentCandidate);

  useEffect(() => {
    currentCandidateRef.current = currentCandidate;
  }, [currentCandidate]);

  // Controls Handlers
  const handleStartSearch = useCallback(() => {
    const firstPool = MOCK_CANDIDATE_POOL[0];
    candidateIndexRef.current = 0;
    candidateNumberRef.current = 1;

    setMetrics({
      candidatesTested: 1,
      candidatesRemaining: Math.max(0, searchConfig.stopConditions.maxCandidates - 1),
      elapsedSeconds: 0,
      bestScore: 84.1,
      scoreImprovement: 0.0,
    });

    setCurrentCandidate({
      id: 'cand-1',
      candidateNumber: 1,
      name: firstPool.name,
      categoryTag: firstPool.category,
      status: 'generating',
      progress: 20,
      currentScore: 78.5,
    });

    setWorkers([
      { id: 'w-1', name: 'Worker_01', state: 'running' },
      { id: 'w-2', name: 'Worker_02', state: 'running' },
      { id: 'w-3', name: 'Worker_03', state: 'running' },
      { id: 'w-4', name: 'Worker_04', state: 'evaluating' },
    ]);

    setActiveStage('generate');
    setStageStatuses({
      generate: 'active',
      backtest: 'waiting',
      evaluate: 'waiting',
      rank: 'waiting',
      leaderboard: 'waiting',
    });

    setSearchState('running');
  }, [searchConfig.stopConditions.maxCandidates]);

  const handlePauseSearch = useCallback(() => {
    setSearchState('paused');
    setStageStatuses((prev) => {
      const next = { ...prev };
      (Object.keys(next) as SearchStageId[]).forEach((key) => {
        if (next[key] === 'active') {
          next[key] = 'paused';
        }
      });
      return next;
    });
    setWorkers((prev) =>
      prev.map((w) => (w.state === 'running' || w.state === 'evaluating' ? { ...w, state: 'paused' } : w))
    );
    setCurrentCandidate((prev) => ({
      ...prev,
      status: 'paused',
    }));
  }, []);

  const handleResumeSearch = useCallback(() => {
    setSearchState('running');
    setStageStatuses((prev) => {
      const next = { ...prev };
      (Object.keys(next) as SearchStageId[]).forEach((key) => {
        if (next[key] === 'paused') {
          next[key] = 'active';
        }
      });
      return next;
    });
    setWorkers([
      { id: 'w-1', name: 'Worker_01', state: 'running' },
      { id: 'w-2', name: 'Worker_02', state: 'running' },
      { id: 'w-3', name: 'Worker_03', state: 'running' },
      { id: 'w-4', name: 'Worker_04', state: 'evaluating' },
    ]);
    setCurrentCandidate((prev) => {
      let nextStatus: CandidateStatus = 'backtesting';
      if (prev.progress <= 25) nextStatus = 'generating';
      else if (prev.progress <= 55) nextStatus = 'backtesting';
      else if (prev.progress <= 75) nextStatus = 'evaluating';
      else nextStatus = 'ranking';
      return { ...prev, status: nextStatus };
    });
  }, []);

  const handleStopSearch = useCallback(() => {
    setSearchState('stopped');
    setActiveStage(null);
    setStageStatuses((prev) => {
      const next = { ...prev };
      (Object.keys(next) as SearchStageId[]).forEach((key) => {
        if (next[key] === 'active' || next[key] === 'paused') {
          next[key] = 'stopped';
        }
      });
      return next;
    });
    setWorkers(INITIAL_WORKERS_IDLE);
    setCurrentCandidate((prev) => ({
      ...prev,
      status: 'stopped',
    }));
  }, []);

  const handleCompleteSearch = useCallback(() => {
    setSearchState('completed');
    setActiveStage('leaderboard');
    setStageStatuses({
      generate: 'completed',
      backtest: 'completed',
      evaluate: 'completed',
      rank: 'completed',
      leaderboard: 'completed',
    });
    setWorkers(INITIAL_WORKERS_IDLE);
    setCurrentCandidate((prev) => ({
      ...prev,
      status: 'completed',
      progress: 100,
    }));
  }, []);

  // Reset execution state when a new strategy is sent to search space
  useEffect(() => {
    if (searchResetKey > 0) {
      if (timerRef.current) clearInterval(timerRef.current);
      if (simStepRef.current) clearInterval(simStepRef.current);
      setSearchState('idle');
      setActiveStage(null);
      setStageStatuses({
        generate: 'idle',
        backtest: 'idle',
        evaluate: 'idle',
        rank: 'idle',
        leaderboard: 'idle',
      });
      setMetrics({
        candidatesTested: 0,
        candidatesRemaining: searchConfig.stopConditions.maxCandidates,
        elapsedSeconds: 0,
        bestScore: 0.0,
        scoreImprovement: 0.0,
      });
      setCurrentCandidate(INITIAL_CANDIDATE_IDLE);
      setWorkers(INITIAL_WORKERS_IDLE);
    }
  }, [searchResetKey, searchConfig.stopConditions.maxCandidates]);

  // 1-second interval timer for elapsed seconds
  useEffect(() => {
    if (searchState === 'running') {
      timerRef.current = setInterval(() => {
        setMetrics((prev) => ({
          ...prev,
          elapsedSeconds: prev.elapsedSeconds + 1,
        }));
      }, 1000);
    } else {
      if (timerRef.current) clearInterval(timerRef.current);
    }
    return () => {
      if (timerRef.current) clearInterval(timerRef.current);
    };
  }, [searchState]);

  // Simulation tick loop (~2.2s per stage progression)
  useEffect(() => {
    if (searchState !== 'running') {
      if (simStepRef.current) clearInterval(simStepRef.current);
      return;
    }

    simStepRef.current = setInterval(() => {
      const prev = currentCandidateRef.current;
      const p = prev.progress;

      if (p < 30) {
        setActiveStage('generate');
        setStageStatuses({
          generate: 'active',
          backtest: 'waiting',
          evaluate: 'waiting',
          rank: 'waiting',
          leaderboard: 'waiting',
        });
        setCurrentCandidate({ ...prev, progress: 45, status: 'backtesting' });
      } else if (p < 60) {
        setActiveStage('backtest');
        setStageStatuses({
          generate: 'completed',
          backtest: 'active',
          evaluate: 'waiting',
          rank: 'waiting',
          leaderboard: 'waiting',
        });
        setCurrentCandidate({ ...prev, progress: 75, status: 'evaluating' });
      } else if (p < 90) {
        setActiveStage('evaluate');
        setStageStatuses({
          generate: 'completed',
          backtest: 'completed',
          evaluate: 'active',
          rank: 'waiting',
          leaderboard: 'waiting',
        });
        setCurrentCandidate({ ...prev, progress: 95, status: 'ranking' });
      } else {
        // Completed candidate cycle, check if qualifies for leaderboard
        setActiveStage('rank');
        setStageStatuses({
          generate: 'completed',
          backtest: 'completed',
          evaluate: 'completed',
          rank: 'active',
          leaderboard: 'waiting',
        });

        const isHighPerformer = prev.currentScore && prev.currentScore > 80;
        if (isHighPerformer) {
          dynamicEntryCountRef.current += 1;
          const newEntryId = `lead-dyn-${Date.now()}-${dynamicEntryCountRef.current}-${Math.random().toString(36).substring(2, 7)}`;
          const newEntry: LeaderboardEntry = {
            id: newEntryId,
            rank: 1,
            name: prev.name,
            identifier: `STR-AUTO · v${Math.floor(Math.random() * 4 + 1)}`,
            categoryTag: prev.categoryTag,
            score: prev.currentScore || 82.5,
            totalReturn: Math.round((prev.currentScore || 80) * 1.6 * 10) / 10,
            winRate: Math.round(((prev.currentScore || 80) * 0.72) * 10) / 10,
            maxDrawdown: -Math.round(((100 - (prev.currentScore || 80)) * 0.6) * 10) / 10,
            sharpeRatio: Math.round(((prev.currentScore || 80) / 40) * 100) / 100,
            tradesCount: Math.floor(Math.random() * 800 + 400),
            isNew: true,
            strategyConfig: {
              blocks: [
                { type: 'MA_CROSS', label: 'Dynamic MA Cross', parameters: { fast: 20, slow: 50 } },
                { type: 'RSI_OSC', label: 'RSI Filter', parameters: { period: 14, overbought: 70, oversold: 30 } },
              ],
            },
          };

          setLeaderboard((prevLb) => {
            const seenIds = new Set<string>([newEntryId]);
            const filteredPrev = prevLb.filter((e) => {
              if (seenIds.has(e.id)) return false;
              seenIds.add(e.id);
              return true;
            });
            const updated = [newEntry, ...filteredPrev.map((e) => ({ ...e, isNew: false }))];
            return updated.map((item, idx) => ({ ...item, rank: idx + 1 }));
          });

          setMetrics((m) => ({
            ...m,
            bestScore: Math.max(m.bestScore, prev.currentScore || 0),
            scoreImprovement: Math.round((Math.max(m.bestScore, prev.currentScore || 0) - 84.1) * 10) / 10,
          }));
        }

        // Advance to next candidate in pool
        candidateIndexRef.current = (candidateIndexRef.current + 1) % MOCK_CANDIDATE_POOL.length;
        candidateNumberRef.current += 1;
        const nextPool = MOCK_CANDIDATE_POOL[candidateIndexRef.current];

        setMetrics((m) => {
          const nextTested = m.candidatesTested + 1;
          const nextRemaining = Math.max(0, searchConfig.stopConditions.maxCandidates - nextTested);
          if (nextRemaining === 0) {
            handleCompleteSearch();
          }
          return {
            ...m,
            candidatesTested: nextTested,
            candidatesRemaining: nextRemaining,
          };
        });

        setCurrentCandidate({
          id: `cand-${candidateNumberRef.current}`,
          candidateNumber: candidateNumberRef.current,
          name: nextPool.name,
          categoryTag: nextPool.category,
          status: 'generating',
          progress: 15,
          currentScore: Math.round((70 + Math.random() * 20) * 10) / 10,
        });
      }

      // Randomize worker states subtly for high realism
      setWorkers((prevWorkers) =>
        prevWorkers.map((w) => {
          const rand = Math.random();
          if (rand > 0.8) {
            return { ...w, state: 'evaluating' };
          } else {
            return { ...w, state: 'running' };
          }
        })
      );
    }, 2200);

    return () => {
      if (timerRef.current) clearInterval(timerRef.current);
      if (simStepRef.current) clearInterval(simStepRef.current);
    };
  }, [searchState, searchConfig.stopConditions.maxCandidates, handleCompleteSearch]);

  // Sorting Logic for Leaderboard Table
  const handleSort = (field: LeaderboardSortField) => {
    if (sortField === field) {
      setSortDirection((prev) => (prev === 'asc' ? 'desc' : 'asc'));
    } else {
      setSortField(field);
      setSortDirection(field === 'maxDrawdown' ? 'asc' : 'desc');
    }
  };

  const sortedLeaderboard = useMemo(() => {
    const list = [...leaderboard];
    list.sort((a, b) => {
      const valA = a[sortField];
      const valB = b[sortField];

      if (valA === undefined) return 1;
      if (valB === undefined) return -1;

      if (sortDirection === 'asc') {
        return valA > valB ? 1 : -1;
      } else {
        return valA < valB ? 1 : -1;
      }
    });
    return list;
  }, [leaderboard, sortField, sortDirection]);

  // Leaderboard Row Action: View Backtest
  const handleViewBacktest = (entry: LeaderboardEntry) => {
    handleWorkflowViewBacktestFromLeaderboard(entry, onNavigate);
  };

  // Leaderboard Row Action: Open in Composer
  const handleOpenInComposer = (entry: LeaderboardEntry) => {
    handleWorkflowOpenInComposerFromLeaderboard(entry, onNavigate);
  };

  return (
    <div className="w-full min-w-0 bg-[#0b0e11] p-2 sm:p-3 md:p-4 flex flex-col gap-3 sm:gap-4 select-none">
      {/* 1. TOP SECTION: SEARCH CONFIGURATION */}
      <SearchConfiguration
        config={searchConfig}
        searchState={searchState}
        onUpdateMarket={updateSearchMarket}
        onUpdateDatasetRange={updateSearchDatasetRange}
        onUpdateAlgorithm={updateSearchAlgorithm}
        onToggleFeature={toggleSearchFeature}
        onUpdateStopConditions={updateSearchStopConditions}
        onStartSearch={handleStartSearch}
        onPauseSearch={handlePauseSearch}
        onResumeSearch={handleResumeSearch}
        onStopSearch={handleStopSearch}
      />

      {/* 2. MIDDLE SECTION: PIPELINE & LIVE STATUS */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-3 sm:gap-4 flex-shrink-0">
        {/* Left 2 Cols: Pipeline Vis + Metrics */}
        <div className="bg-[#191c1f] p-4 rounded border border-[#3c4a40] lg:col-span-2 flex flex-col justify-between gap-4">
          <SearchPipeline
            activeStage={activeStage}
            stageStatuses={stageStatuses}
            isSearching={searchState === 'running'}
          />

          <SearchStatusMetrics metrics={metrics} />
        </div>

        {/* Right 1 Col: Live Status & Worker Monitor */}
        <div className="bg-[#191c1f] p-4 rounded border border-[#3c4a40] flex flex-col justify-between gap-4">
          <CurrentCandidateCard
            candidate={currentCandidate}
            isSearching={searchState === 'running'}
            isPaused={searchState === 'paused'}
          />

          <WorkerMonitor
            workers={workers}
            isSearching={searchState === 'running'}
            searchState={searchState}
          />
        </div>
      </div>

      {/* 3. BOTTOM SECTION: LEADERBOARD TABLE */}
      <LeaderboardTable
        entries={sortedLeaderboard}
        sortField={sortField}
        sortDirection={sortDirection}
        onSort={handleSort}
        topK={topK}
        onSelectTopK={setTopK}
        onViewBacktest={handleViewBacktest}
        onOpenInComposer={handleOpenInComposer}
      />
    </div>
  );
};

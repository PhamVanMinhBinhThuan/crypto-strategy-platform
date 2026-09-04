"use client";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import type { RealtimeStatus } from "@/src/foundation/realtime/contracts";
import { useClients } from "@/src/foundation/composition/client-provider";
import { parseMarketSelection, marketSelectionQuery } from "../model/market-selection";
import { marketRangeEndingAt } from "../model/market-range";
import type { MarketPair, MarketTimeframe } from "../model/market-catalog";
import type { Candle } from "../model/candle";
import { listCandles } from "../api/market-api";
import { emptyCandleState, mergeCandles, type CandleState } from "../state/candle-reducer";
import { observeMarket, type ProviderStatus } from "../state/market-realtime-controller";
import { LatestRequest } from "../../shared/latest-request";
import { AsyncStatus } from "../../shared/AsyncStatus";
import { MarketControls } from "./MarketControls";
import { CandleChart } from "./CandleChart";
import { MarketConnectionStatus } from "./MarketConnectionStatus";

export function MarketDashboard() {
  const params = useSearchParams(),
    router = useRouter(),
    { api, realtime } = useClients();
  const selection = useMemo(
    () => parseMarketSelection(new URLSearchParams(params.toString())),
    [params]
  );
  const [panels, setPanels] = useState<Record<string, CandleState>>({});
  const [loading, setLoading] = useState(true),
    [error, setError] = useState<string>();
  const [transport, setTransport] = useState<RealtimeStatus>(realtime.status());
  const [provider, setProvider] = useState<ProviderStatus>("DISCONNECTED");
  const lastDataAt = useMemo(() => {
    const timestamps = Object.values(panels).flatMap((panel) => [
      ...Object.values(panel.eventTimes),
      ...panel.items.map((item) => item.closeTime)
    ]);
    return timestamps.sort().at(-1);
  }, [panels]);
  const requests = useRef(new LatestRequest());
  const load = useCallback(async () => {
    const request = requests.current.next();
    setLoading(true);
    setError(undefined);
    const now = new Date();
    const results = await Promise.all(
      selection.panels.map(async (panel) => {
        const range = marketRangeEndingAt(now, panel.timeframe);
        return {
          panel,
          result: await listCandles(api, {
            pair: selection.pair,
            timeframe: panel.timeframe,
            ...range
          })
        };
      })
    );
    if (!requests.current.isLatest(request.generation)) return;
    const next: Record<string, CandleState> = {};
    let firstError: string | undefined;
    results.forEach(({ panel, result }) => {
      if (result.ok) next[panel.id] = mergeCandles(emptyCandleState, result.data.items);
      else
        firstError ??= result.error.retryable
          ? "Market đang tạm gián đoạn. Vui lòng thử lại."
          : "Không thể tải dữ liệu Market cho lựa chọn hiện tại.";
    });
    setPanels((current) => {
      for (const panel of selection.panels) {
        const matchingCurrent = current[panel.id]?.items.filter(
          (item) => item.pair === selection.pair && item.timeframe === panel.timeframe
        );
        if (next[panel.id] && matchingCurrent?.length)
          next[panel.id] = mergeCandles(next[panel.id], matchingCurrent);
      }
      return next;
    });
    setError(firstError);
    setLoading(false);
  }, [api, selection]);
  useEffect(() => {
    const activeRequests = requests.current;
    const timer = window.setTimeout(() => void load(), 0);
    return () => {
      window.clearTimeout(timer);
      activeRequests.cancel();
    };
  }, [load]);
  useEffect(
    () =>
      observeMarket(realtime, selection, {
        onCandle: (id, value, at) =>
          setPanels((current) => ({
            ...current,
            [id]: mergeCandles(current[id] ?? emptyCandleState, [value as Candle], at)
          })),
        onTransport: setTransport,
        onProvider: setProvider,
        onRecovery: () => void load()
      }),
    [realtime, selection, load]
  );
  const navigate = (pair: MarketPair, frames: readonly MarketTimeframe[]) =>
    router.replace(
      `/market?${marketSelectionQuery({ pair, panels: frames.map((timeframe, index) => ({ id: `panel-${index + 1}`, timeframe })) })}`
    );
  const frames = selection.panels.map((p) => p.timeframe);
  return (
    <main className="market-workspace">
      <AsyncStatus
        message={
          loading
            ? "Đang tải dữ liệu Market"
            : error
              ? "Market đang gián đoạn"
              : "Market đã sẵn sàng"
        }
        urgent={Boolean(error)}
      />
      <header className="market-heading">
        <div>
          <p className="eyebrow">F-012 · Market</p>
          <h1>Market Dashboard</h1>
          <p>{selection.pair} · bốn góc nhìn thời gian, một nguồn dữ liệu authoritative.</p>
        </div>
        <MarketConnectionStatus transport={transport} provider={provider} lastDataAt={lastDataAt} />
      </header>
      <MarketControls
        pair={selection.pair}
        timeframes={frames}
        onPair={(pair) => navigate(pair, frames)}
        onTimeframe={(index, value) =>
          navigate(
            selection.pair,
            frames.map((v, i) => (i === index ? value : v))
          )
        }
      />
      {error && (
        <div className="market-error" role="alert">
          {error}
          <button onClick={() => void load()}>Thử lại</button>
        </div>
      )}
      <section className="market-grid" aria-busy={loading}>
        {selection.panels.map((panel) => (
          <article className="market-panel" key={panel.id}>
            <header>
              <strong>{selection.pair}</strong>
              <span>{panel.timeframe}</span>
            </header>
            {loading && !panels[panel.id] ? (
              <div className="market-empty" role="status">
                Đang tải…
              </div>
            ) : (
              <CandleChart
                candles={panels[panel.id]?.items ?? []}
                label={`${selection.pair} ${panel.timeframe}`}
              />
            )}
          </article>
        ))}
      </section>
    </main>
  );
}

"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import type { ApiClient } from "@/src/foundation/http/contracts";

type CandidateDetail = Readonly<{
  candidateId: string;
  generationIndex: number;
  definition: Readonly<Record<string, unknown>>;
  generatorState: Readonly<Record<string, unknown>>;
  candidateFingerprint: string;
  dataset: Readonly<{
    datasetId: string;
    checksum: string;
    provider: string;
    pair: string;
    timeframe: string;
    startTime: string;
    endTime: string;
    candleCount: number;
  }>;
  backtestResultId: string | null;
  backtestStatus: string;
  metrics: Readonly<{
    totalReturn: string;
    winRate: string;
    maximumDrawdown: string;
    numberOfTrades: number;
    metricVersion: string;
  }> | null;
}>;

export function CandidateDetailPanel({
  api,
  experimentId,
  candidateId
}: {
  api: ApiClient;
  experimentId: string;
  candidateId: string;
}) {
  const [detail, setDetail] = useState<CandidateDetail>();
  const [error, setError] = useState<string>();
  useEffect(() => {
    let active = true;
    void api
      .request<CandidateDetail>(
        `/api/v1/experiments/${encodeURIComponent(experimentId)}/candidates/${encodeURIComponent(candidateId)}`
      )
      .then((result) => {
        if (!active) return;
        if (result.ok) setDetail(result.data);
        else setError("Candidate evidence is inaccessible.");
      });
    return () => {
      active = false;
    };
  }, [api, candidateId, experimentId]);

  if (error)
    return (
      <section className="panel error-state" role="alert">
        {error}
      </section>
    );
  if (!detail)
    return (
      <section className="panel" role="status">
        Loading candidate evidence...
      </section>
    );
  return (
    <section className="panel" aria-labelledby="candidate-detail-heading">
      <div className="section-heading">
        <h2 id="candidate-detail-heading">Candidate #{detail.generationIndex + 1}</h2>
        <Link href={`/search/${encodeURIComponent(experimentId)}`}>Close detail</Link>
      </div>
      <dl>
        <dt>Candidate fingerprint</dt>
        <dd className="mono">{detail.candidateFingerprint}</dd>
        <dt>Frozen dataset</dt>
        <dd>
          {detail.dataset.pair} / {detail.dataset.timeframe} / {detail.dataset.candleCount}{" "}
          candles
        </dd>
        <dt>Dataset checksum</dt>
        <dd className="mono">{detail.dataset.checksum}</dd>
        <dt>UTC range</dt>
        <dd>
          {detail.dataset.startTime} to {detail.dataset.endTime}
        </dd>
        <dt>Backtest status</dt>
        <dd>{detail.backtestStatus}</dd>
        <dt>Metric version</dt>
        <dd>{detail.metrics?.metricVersion ?? "Pending"}</dd>
      </dl>
      {detail.metrics && (
        <dl>
          <dt>Total Return</dt>
          <dd>{detail.metrics.totalReturn}</dd>
          <dt>Win Rate</dt>
          <dd>{detail.metrics.winRate}</dd>
          <dt>Maximum Drawdown</dt>
          <dd>{detail.metrics.maximumDrawdown}</dd>
          <dt>Number of Trades</dt>
          <dd>{detail.metrics.numberOfTrades}</dd>
        </dl>
      )}
      <h3>Immutable composite definition</h3>
      <pre>{JSON.stringify(detail.definition, null, 2)}</pre>
      {detail.backtestResultId && (
        <Link href={`/backtests?resultId=${encodeURIComponent(detail.backtestResultId)}`}>
          View authoritative Backtest
        </Link>
      )}
    </section>
  );
}

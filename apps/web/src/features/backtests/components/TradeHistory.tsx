"use client";

import { Fragment, useState } from "react";
import type { TradeViewModel } from "../types/backtest-result";
import {
  formatMoney,
  formatPrice,
  formatQuantity,
  formatUtcDateTime,
  humanizeBacktestValue,
  valueTone
} from "./backtest-presentation";

const PAGE_SIZE = 10;
const COLUMN_COUNT = 10;

export function TradeHistory({
  trades,
  quoteCurrency
}: {
  trades: readonly TradeViewModel[];
  quoteCurrency?: string;
}) {
  const [page, setPage] = useState(0);
  const [expandedTradeId, setExpandedTradeId] = useState<string>();
  const start = page * PAGE_SIZE;
  const visibleTrades = trades.slice(start, start + PAGE_SIZE);
  const end = Math.min(start + visibleTrades.length, trades.length);
  const hasPrevious = page > 0;
  const hasNext = end < trades.length;

  const changePage = (nextPage: number) => {
    setExpandedTradeId(undefined);
    setPage(nextPage);
  };

  return (
    <section className="panel backtest-trades" aria-labelledby="trade-history-heading">
      <div className="section-heading">
        <div>
          <p className="eyebrow">Executed positions</p>
          <h2 id="trade-history-heading">Trade history</h2>
        </div>
        <span>Recorded entry, exit, fees and realized outcome for each trade.</span>
      </div>

      {trades.length === 0 ? (
        <p className="empty-copy">
          No trades were recorded because the strategy generated no signals.
        </p>
      ) : (
        <>
          <div
            className="table-scroll backtest-trade-scroll"
            tabIndex={0}
            role="region"
            aria-label="Scrollable trade history"
          >
            <table className="backtest-trade-table">
              <thead>
                <tr>
                  <th scope="col">#</th>
                  <th scope="col">Side</th>
                  <th scope="col">Entry</th>
                  <th scope="col">Exit</th>
                  <th scope="col">Quantity</th>
                  <th scope="col">Fees</th>
                  <th scope="col">Realized P/L</th>
                  <th scope="col">Cash after trade</th>
                  <th scope="col">Exit reason</th>
                  <th scope="col">Actions</th>
                </tr>
              </thead>
              <tbody>
                {visibleTrades.map((trade) => {
                  const expanded = expandedTradeId === trade.tradeId;
                  const detailsId = `trade-details-${trade.tradeId}`;
                  return (
                    <Fragment key={trade.tradeId}>
                      <tr>
                        <td className="backtest-number">{trade.sequence + 1}</td>
                        <td><span className="backtest-side-badge">{humanizeBacktestValue(trade.side)}</span></td>
                        <td>
                          <TradeEvent
                            time={trade.entryTime}
                            price={trade.entryPrice}
                            quoteCurrency={quoteCurrency}
                          />
                        </td>
                        <td>
                          <TradeEvent
                            time={trade.exitTime}
                            price={trade.exitPrice}
                            quoteCurrency={quoteCurrency}
                          />
                        </td>
                        <td className="backtest-number" title={trade.quantity}>
                          {formatQuantity(trade.quantity)}
                        </td>
                        <td className="backtest-number" title={trade.totalFee}>
                          {formatMoney(trade.totalFee, quoteCurrency)}
                        </td>
                        <td
                          className={`backtest-number ${valueTone(trade.profitLoss)}`}
                          title={trade.profitLoss}
                        >
                          {formatMoney(trade.profitLoss, quoteCurrency, true)}
                        </td>
                        <td className="backtest-number" title={trade.postTradeCash}>
                          {formatMoney(trade.postTradeCash, quoteCurrency)}
                        </td>
                        <td title={trade.exitReason}>{humanizeBacktestValue(trade.exitReason)}</td>
                        <td>
                          <button
                            type="button"
                            className="button secondary backtest-trade-action"
                            aria-expanded={expanded}
                            aria-controls={expanded ? detailsId : undefined}
                            aria-label={`${expanded ? "Close" : "View"} details for trade ${trade.sequence + 1}`}
                            onClick={() => setExpandedTradeId(expanded ? undefined : trade.tradeId)}
                          >
                            {expanded ? "Close details" : "View details"}
                          </button>
                        </td>
                      </tr>
                      {expanded && (
                        <tr className="backtest-trade-detail-row">
                          <td colSpan={COLUMN_COUNT}>
                            <TradeDetails
                              id={detailsId}
                              trade={trade}
                              quoteCurrency={quoteCurrency}
                            />
                          </td>
                        </tr>
                      )}
                    </Fragment>
                  );
                })}
              </tbody>
            </table>
          </div>

          <footer className="pagination backtest-trade-pagination" aria-label="Trade history pagination">
            <button
              type="button"
              disabled={!hasPrevious}
              onClick={() => changePage(page - 1)}
            >
              Previous
            </button>
            <span aria-live="polite">
              Showing {start + 1}–{end} of {trades.length}
            </span>
            <button
              type="button"
              disabled={!hasNext}
              onClick={() => changePage(page + 1)}
            >
              Next
            </button>
          </footer>
        </>
      )}
    </section>
  );
}

function TradeEvent({
  time,
  price,
  quoteCurrency
}: {
  time: string;
  price: string;
  quoteCurrency?: string;
}) {
  return (
    <div className="backtest-trade-event">
      <time dateTime={time}>{formatUtcDateTime(time)}</time>
      <span className="backtest-number" title={price}>{formatPrice(price, quoteCurrency)}</span>
    </div>
  );
}

function TradeDetails({
  id,
  trade,
  quoteCurrency
}: {
  id: string;
  trade: TradeViewModel;
  quoteCurrency?: string;
}) {
  const values = [
    ["Trade ID", trade.tradeId],
    ["Entry timestamp", trade.entryTime],
    ["Exit timestamp", trade.exitTime],
    ["Exact entry price", withOptionalCurrency(trade.entryPrice, quoteCurrency)],
    ["Exact exit price", withOptionalCurrency(trade.exitPrice, quoteCurrency)],
    ["Exact quantity", trade.quantity],
    ["Entry fee", withOptionalCurrency(trade.entryFee, quoteCurrency)],
    ["Exit fee", withOptionalCurrency(trade.exitFee, quoteCurrency)],
    ["Total fee", withOptionalCurrency(trade.totalFee, quoteCurrency)],
    ["Realized P/L", withOptionalCurrency(trade.profitLoss, quoteCurrency)],
    ["Post-trade cash", withOptionalCurrency(trade.postTradeCash, quoteCurrency)],
    ["Raw exit reason", trade.exitReason]
  ] as const;
  return (
    <section id={id} className="backtest-trade-details" aria-label={`Trade ${trade.sequence + 1} details`}>
      <h3>Exact execution details</h3>
      <dl className="backtest-trade-detail-grid">
        {values.map(([label, value]) => (
          <div key={label}>
            <dt>{label}</dt>
            <dd className="mono">{value}</dd>
          </div>
        ))}
      </dl>
    </section>
  );
}

function withOptionalCurrency(value: string, currency?: string) {
  return currency ? `${value} ${currency}` : value;
}

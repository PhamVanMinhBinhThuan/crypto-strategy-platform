import type { NewsItem } from "../model/news";
import type { AsyncState } from "@/src/foundation/ui/async-state";
import { FeatureState } from "../../shared/FeatureState";
import { SentimentStatus } from "./SentimentStatus";
import { DegradedState } from "@/src/components/states/DegradedState";
export function NewsFeed({
  items,
  loading,
  error,
  hasMore,
  pageNumber,
  canPrevious,
  onRetry,
  onPrevious,
  onNext
}: {
  items: readonly NewsItem[];
  loading: boolean;
  error?: string | null;
  hasMore: boolean;
  pageNumber: number;
  canPrevious: boolean;
  onRetry: () => void;
  onPrevious: () => void;
  onNext: () => void;
}) {
  const initialState: AsyncState<readonly NewsItem[]> = loading
    ? { kind: "loading" }
    : error
      ? { kind: "error", message: error, retryable: true }
      : items.length
        ? { kind: "success", data: items }
        : { kind: "empty" };

  if (!items.length)
    return (
      <FeatureState
        state={initialState}
        emptyTitle="No news matches the selected filters."
        onRetry={onRetry}
      >
        {() => null}
      </FeatureState>
    );
  const degradedCount = items.filter((item) => item.analysisStatus !== "ANALYZED").length;
  return (
    <section className="news-feed" aria-busy={loading}>
      {error && (
        <div role="alert">
          {error} <button onClick={onRetry}>Retry</button>
        </div>
      )}
      {degradedCount > 0 && (
        <DegradedState
          state="degraded"
          message={`News content is available. Sentiment is pending or unavailable for ${degradedCount} ${degradedCount === 1 ? "item" : "items"}; technical research remains usable.`}
        />
      )}
      {items.map((item) => (
        <article key={item.newsId} className="news-card">
          <div>
            <p className="eyebrow">{item.source}</p>
            <h2>
              <a href={item.url} target="_blank" rel="noopener noreferrer">
                {item.title}
              </a>
            </h2>
            <p>
              {new Date(item.publishedAt).toLocaleString("en-US")} · {item.relatedAssetIds.length}{" "}
              related {item.relatedAssetIds.length === 1 ? "asset" : "assets"}
            </p>
          </div>
          <SentimentStatus item={item} />
        </article>
      ))}
      <footer className="pagination news-pagination" aria-label="News pagination">
        <button type="button" disabled={!canPrevious || loading} onClick={onPrevious}>
          Previous
        </button>
        <span aria-live="polite">
          Showing {(pageNumber - 1) * 10 + 1}–{(pageNumber - 1) * 10 + items.length} · Page{" "}
          {pageNumber}
        </span>
        <button type="button" disabled={!hasMore || loading} onClick={onNext}>
          Next
        </button>
      </footer>
    </section>
  );
}

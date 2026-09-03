import type { NewsItem } from "../model/news";
import type { AsyncState } from "@/src/foundation/ui/async-state";
import { FeatureState } from "../../shared/FeatureState";
import { SentimentStatus } from "./SentimentStatus";
export function NewsFeed({
  items,
  loading,
  error,
  hasMore,
  onRetry,
  onLoadMore
}: {
  items: readonly NewsItem[];
  loading: boolean;
  error?: string | null;
  hasMore: boolean;
  onRetry: () => void;
  onLoadMore: () => void;
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
        emptyTitle="Không có News phù hợp bộ lọc."
        onRetry={onRetry}
      >
        {() => null}
      </FeatureState>
    );
  return (
    <section className="news-feed" aria-busy={loading}>
      {error && (
        <div role="alert">
          {error} <button onClick={onRetry}>Thử lại</button>
        </div>
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
              {new Date(item.publishedAt).toLocaleString("vi-VN")} · {item.relatedAssetIds.length}{" "}
              asset liên quan
            </p>
          </div>
          <SentimentStatus item={item} />
        </article>
      ))}
      {hasMore && (
        <button className="news-load-more" disabled={loading} onClick={onLoadMore}>
          {loading ? "Đang tải…" : "Tải thêm"}
        </button>
      )}
    </section>
  );
}

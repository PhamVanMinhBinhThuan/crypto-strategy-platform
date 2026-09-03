import type { NewsItem } from "../model/news";
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
  if (loading && !items.length)
    return (
      <div className="news-state" role="status">
        Đang tải News…
      </div>
    );
  if (error && !items.length)
    return (
      <div className="news-state" role="alert">
        <p>{error}</p>
        <button onClick={onRetry}>Thử lại</button>
      </div>
    );
  if (!items.length) return <div className="news-state">Không có News phù hợp bộ lọc.</div>;
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

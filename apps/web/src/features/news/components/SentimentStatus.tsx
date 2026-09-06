import type { NewsItem } from "../model/news";
export function SentimentStatus({ item }: { item: NewsItem }) {
  if (item.analysisStatus === "ANALYZED" && item.sentiment)
    return (
      <div className={`sentiment sentiment-${item.sentiment.label.toLowerCase()}`}>
        <strong>{item.sentiment.label}</strong>
        <span>
          Confidence {item.sentiment.confidence} · Polarity {item.sentiment.polarityScore}
        </span>
        <small>For informational purposes only; not financial advice.</small>
      </div>
    );
  const copy: Record<NewsItem["analysisStatus"], string> = {
    PENDING: "Sentiment analysis is pending.",
    ANALYZING: "Sentiment analysis is in progress.",
    ANALYZED: "Sentiment results are unavailable.",
    FAILED_RETRYABLE: "Sentiment analysis is temporarily unavailable; you can reload the news.",
    FAILED: "Sentiment is unavailable for this article."
  };
  return (
    <div className="sentiment sentiment-degraded" role="status">
      <strong>{item.analysisStatus}</strong>
      <span>{copy[item.analysisStatus]}</span>
    </div>
  );
}

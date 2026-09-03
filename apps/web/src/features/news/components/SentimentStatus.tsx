import type { NewsItem } from "../model/news";
export function SentimentStatus({ item }: { item: NewsItem }) {
  if (item.analysisStatus === "ANALYZED" && item.sentiment)
    return (
      <div className={`sentiment sentiment-${item.sentiment.label.toLowerCase()}`}>
        <strong>{item.sentiment.label}</strong>
        <span>
          Confidence {item.sentiment.confidence} · Polarity {item.sentiment.polarityScore}
        </span>
        <small>Dữ liệu tham khảo, không phải lời khuyên tài chính.</small>
      </div>
    );
  const copy: Record<NewsItem["analysisStatus"], string> = {
    PENDING: "Đang chờ phân tích sentiment.",
    ANALYZING: "Đang phân tích sentiment.",
    ANALYZED: "Kết quả sentiment không khả dụng.",
    FAILED_RETRYABLE: "Sentiment tạm gián đoạn; bạn có thể tải lại News.",
    FAILED: "Sentiment không khả dụng cho tin này."
  };
  return (
    <div className="sentiment sentiment-degraded" role="status">
      <strong>{item.analysisStatus}</strong>
      <span>{copy[item.analysisStatus]}</span>
    </div>
  );
}

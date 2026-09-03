import type { NewsAnalysisStatus } from "../model/news";
export const NEWS_STATUSES = [
  "PENDING",
  "ANALYZING",
  "ANALYZED",
  "FAILED_RETRYABLE",
  "FAILED"
] as const;
export function NewsFilters({
  selected,
  onChange
}: {
  selected: readonly NewsAnalysisStatus[];
  onChange: (values: NewsAnalysisStatus[]) => void;
}) {
  return (
    <fieldset className="news-filters">
      <legend>Lọc trạng thái phân tích</legend>
      {NEWS_STATUSES.map((status) => (
        <label key={status}>
          <input
            type="checkbox"
            checked={selected.includes(status)}
            onChange={(event) =>
              onChange(
                event.target.checked
                  ? [...selected, status]
                  : selected.filter((item) => item !== status)
              )
            }
          />
          {status}
        </label>
      ))}
    </fieldset>
  );
}

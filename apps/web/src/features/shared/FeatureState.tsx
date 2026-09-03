import type { AsyncState } from "@/src/foundation/ui/async-state";

export function FeatureState<T>({
  state,
  children,
  onRetry,
  emptyTitle = "Chưa có dữ liệu."
}: {
  state: AsyncState<T>;
  children: (data: T) => React.ReactNode;
  onRetry?: () => void;
  emptyTitle?: string;
}) {
  if (state.kind === "loading")
    return (
      <div className="feature-state" role="status">
        Đang tải…
      </div>
    );
  if (state.kind === "empty")
    return (
      <div className="feature-state">
        <h2>{emptyTitle}</h2>
      </div>
    );
  if (state.kind === "error")
    return (
      <div className="feature-state" role="alert">
        <h2>Không thể tải dữ liệu</h2>
        <p>{state.message}</p>
        {state.retryable && onRetry && <button onClick={onRetry}>Thử lại</button>}
      </div>
    );
  if (state.kind === "degraded")
    return (
      <section className="feature-degraded">
        <div role="status">
          <strong>Một phần dữ liệu đang gián đoạn</strong>
          <span>{state.message}</span>
        </div>
        {state.data !== undefined && children(state.data)}
      </section>
    );
  return <>{children(state.data)}</>;
}

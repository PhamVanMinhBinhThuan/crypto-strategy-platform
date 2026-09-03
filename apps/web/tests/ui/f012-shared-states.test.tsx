import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { FeatureState } from "@/src/features/shared/FeatureState";
describe("F-012 shared states", () => {
  it("renders consistent loading, empty and bounded retry", async () => {
    const retry = vi.fn(),
      view = render(<FeatureState state={{ kind: "loading" }}>{() => null}</FeatureState>);
    expect(screen.getByRole("status")).toHaveTextContent("Đang tải");
    view.rerender(
      <FeatureState state={{ kind: "empty" }} emptyTitle="Không có kết quả">
        {() => null}
      </FeatureState>
    );
    expect(screen.getByText("Không có kết quả")).toBeInTheDocument();
    view.rerender(
      <FeatureState
        state={{ kind: "error", message: "Tạm gián đoạn", retryable: true }}
        onRetry={retry}
      >
        {() => null}
      </FeatureState>
    );
    await userEvent.click(screen.getByRole("button", { name: "Thử lại" }));
    expect(retry).toHaveBeenCalledOnce();
  });
  it("keeps successful data visible in degraded state", () => {
    render(
      <FeatureState state={{ kind: "degraded", message: "Realtime mất kết nối", data: "snapshot" }}>
        {(data) => <p>{data}</p>}
      </FeatureState>
    );
    expect(screen.getByText("snapshot")).toBeInTheDocument();
  });
});

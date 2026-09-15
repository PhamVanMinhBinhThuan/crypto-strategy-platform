import { expect, test } from "@playwright/test";
import { installF012Adapter } from "./f012-controllable-adapter";

test.beforeEach(async ({ page }) => installF012Adapter(page));

test("Sentiment workflow is keyboard accessible and responsive", async ({ page }, testInfo) => {
  test.skip(
    testInfo.project.name !== "desktop",
    "Viewport matrix runs once in the desktop project."
  );

  for (const width of [360, 768, 1024, 1440]) {
    await page.setViewportSize({ width, height: 900 });
    await page.goto("/news");
    const action = page.getByRole("link", { name: "Use as Strategy" });
    await expect(action).toBeVisible();
    await action.focus();
    await expect(action).toBeFocused();
    await page.keyboard.press("Enter");
    await expect(page).toHaveURL(/\/strategies\?strategy=sentiment-polarity/);
    await expect(page.getByRole("heading", { name: "Sentiment Polarity" }).first()).toBeVisible();
    await expect(
      page.getByRole("complementary", { name: "Sentiment strategy notice" })
    ).toContainText("not financial advice");

    const overflow = await page.evaluate(
      () => document.documentElement.scrollWidth - document.documentElement.clientWidth
    );
    expect(overflow, `page overflow at ${width}px`).toBeLessThanOrEqual(1);
  }
});

test("Sentiment exposes finite domains and can join a Composite Search pool", async ({ page }) => {
  await page.goto("/search?mode=new");
  const sentiment = page.getByRole("checkbox", { name: /Sentiment Polarity/ });
  await expect(sentiment).toBeVisible();
  await sentiment.check();
  await expect(page.getByRole("note")).toContainText("one server-frozen News snapshot");
  await expect(page.getByLabel("Sentiment Polarity lookbackHours minimum")).toHaveValue("6");

  await expect(page.getByText("2 selected")).toBeVisible();
});

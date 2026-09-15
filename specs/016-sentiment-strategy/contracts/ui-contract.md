# UI Contract: F-016

## Strategy Composer

- Show Sentiment as the fifth system Strategy.
- Explain that it is a research signal, not financial advice.
- Parameters come from API schema: lookback, minimum articles, BUY/SELL thresholds.
- Backtests action opens Search/Backtest with the selected immutable version.

## Search & Leaderboard

- Select Sentiment alone or with technical strategies.
- Use the same generic parameter-domain controls.
- State that all Candidates share one frozen snapshot/model release.
- Progress and ranking remain server-authoritative.

## Result

- Show score, thresholds, article count, model/preprocessing release and snapshot/evidence fingerprint.
- Missing/invalid provenance is explicit, never silently replaced.

## News Sentiment

- Add “Use as Strategy” when catalog capability is available.
- Preserve loading, empty, invalid, inaccessible and degraded text states.
- Navigate to Composer; do not calculate or publish a Strategy locally.

## Accessibility and responsive behavior

- Express status with text as well as color.
- All actions/controls are keyboard accessible.
- No page-level overflow at 360, 768, 1024 or 1440 px.
- Fixture data remains visibly labeled.

import { capLeaderboardLimit } from "../types/leaderboard";
export function LeaderboardControls({
  limit,
  configuredTopK,
  onChange
}: {
  limit: number;
  configuredTopK: number;
  onChange: (n: number) => void;
}) {
  return (
    <div className="leaderboard-controls">
      <label htmlFor="leaderboard-limit">Top-K</label>
      <select
        id="leaderboard-limit"
        value={limit}
        onChange={(e) => onChange(capLeaderboardLimit(Number(e.target.value), configuredTopK))}
      >
        {[10, 25, 50]
          .filter((n) => n <= configuredTopK)
          .map((n) => (
            <option value={n} key={n}>
              Top {n}
            </option>
          ))}
      </select>
      <label htmlFor="leaderboard-custom">Custom</label>
      <input
        id="leaderboard-custom"
        type="number"
        min={1}
        max={Math.min(100, configuredTopK)}
        value={limit}
        onChange={(e) => onChange(capLeaderboardLimit(Number(e.target.value), configuredTopK))}
      />
    </div>
  );
}

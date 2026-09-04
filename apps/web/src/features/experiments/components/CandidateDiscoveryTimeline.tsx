import type { Candidate } from "../types/experiment";
export function CandidateDiscoveryTimeline({
  candidates,
  discoveries = []
}: {
  candidates: readonly Candidate[];
  discoveries?: readonly string[];
}) {
  return (
    <section className="panel">
      <h2>Candidate discoveries</h2>
      {!candidates.length && !discoveries.length ? (
        <p>No candidates discovered yet.</p>
      ) : (
        <ol className="timeline">
          {candidates.map((c) => (
            <li key={c.candidateId}>
              <strong>Generation {c.generationIndex}</strong>
              <span className="mono">{c.candidateId}</span>
              <small>{c.fingerprint}</small>
            </li>
          ))}
          {discoveries.map((id) => (
            <li key={id}>
              <strong>Backtest completed</strong>
              <span className="mono">{id}</span>
              <small>Freshness notification; awaiting authoritative candidate read.</small>
            </li>
          ))}
        </ol>
      )}
    </section>
  );
}

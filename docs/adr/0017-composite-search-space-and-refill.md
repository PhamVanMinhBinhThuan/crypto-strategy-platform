# ADR-0017: Versioned Composite Search Space and Durable Window Refill

**Status**: Accepted  
**Date**: 2026-09-05  
**Owners**: Tiến Luật  
**Extends**: ADR-0005, ADR-0006, ADR-0009, ADR-0010, ADR-0013, ADR-0014 and ADR-0016

## Context

F-010 delivered a durable Search Coordinator and replaceable generator boundary, but its executable request maps one strategy and a flat parameter map. The current generated candidate contains only that parameter set, so an empty map has cardinality one and a published composite can produce only one candidate. This does not satisfy ADR-0010's stated candidate contract, which requires exact Strategy/Composite identity, component versions/parameters and combination policy.

The worker currently allocates an initial bounded window. Completion and scheduled reconciliation can persist a `FILL_AVAILABLE_SLOTS` decision, but their callers do not execute a follow-up allocation. The window is also reduced by Top-K even though Top-K describes leaderboard retention. Consequently a request for hundreds of candidates can stop after the first window and a Top-1 run can execute only one active candidate.

F-015 must correct both gaps without breaking historical immutable experiments, moving business state into Redis, or coupling Search to Backtest/Evaluation/Leaderboard implementations.

## Decision

1. `modules/search` publishes a versioned composite Search-space model containing an ordered strategy pool, typed finite parameter domains per exact strategy version, component-count bounds, supported cross-parameter constraints, and a versioned combination policy.
2. New Search manifests and candidates use schema version 2. Each immutable candidate freezes the exact ordered component strategy versions and parameter values, policy configuration, generator identity/version, generation index, and semantic fingerprint.
3. Historical schema-version-1 manifests/candidates remain immutable and readable. A compatibility mapper resolves their single strategy from the old manifest. No migration rewrites old JSON evidence.
4. Majority Vote is the required F-015 policy. Other policies are registered/exposed only after full validation, persistence, fingerprinting, and runtime execution support exists.
5. Random Search traverses a deterministic finite logical index space. Its seeded order and persisted state do not depend on worker completion order and do not require materializing the full candidate space.
6. `modules/experiment-execution` continues to own cross-capability orchestration. Its atomic allocation gateway persists next generator state, candidate, Backtest job, decision, outbox intent, and run progress. Persistence implements the gateway; Worker does not access owner tables directly.
7. The active target window is constrained by requested concurrency, per-experiment capacity, and global capacity. Top-K is not an execution-concurrency input and only bounds the Leaderboard projection.
8. After a durable completion or reconciliation decision requests `FILL_AVAILABLE_SLOTS`, the Worker coordinator immediately invokes bounded allocation against freshly loaded version/fence state. Scheduled reconciliation remains the repair path for lost process-local follow-up.
9. Candidate terminal failure consumes one generation position and one maximum-candidate budget unit. Attempt retries remain attached to the same logical candidate.
10. Production Backtests resolve the candidate's frozen composite definition and read the experiment's frozen Dataset. They never fetch market data from Binance per candidate.
11. Public APIs add owner-scoped dataset/generator discovery, composite Search configuration, authoritative progress, candidate detail, and enriched leaderboard fields through additive/versioned contracts. The browser does not generate candidates or calculate financial metrics.

## Alternatives Considered

- **Treat every selected strategy as an independent Search**: rejected because it does not generate or evaluate composite candidates.
- **Encode component domains in flat dotted parameter names**: rejected because identity, validation and canonical ordering become ambiguous.
- **Rewrite v1 manifests/candidates into v2**: rejected because immutable historical evidence must not change.
- **Generate all candidates and enqueue them at Start**: rejected because it weakens stop behavior and violates bounded backpressure at 10,000 candidates.
- **Poll-only refill**: retained only as recovery; rejected as the primary path because it adds avoidable idle time.
- **Use Top-K as the work window**: rejected because ranking retention and execution concurrency are unrelated concerns.
- **Store coordination state only in Redis**: rejected because Redis Streams is delivery, not authoritative business state.

## Consequences

### Positive

- Search genuinely explores strategy combinations and parameter domains.
- Candidate identity now conforms to ADR-0010 and can be explained from Leaderboard to Dataset.
- Hundreds or thousands of candidates progress through a bounded worker window.
- Worker capacity can scale without changing the deterministic candidate sequence.
- Historical experiments remain reproducible and readable.

### Negative

- Search-space/candidate serialization and execution resolution gain a versioned compatibility path.
- Exact finite cardinality and deterministic traversal require careful overflow and constraint handling.
- Completion/reconciliation paths need additional concurrency tests because refill can race with stop and other allocators.
- Enriched public reads and UI states expand contract and test surface.

## Affected Components

- `modules/search`
- `modules/experiment-execution`
- `modules/persistence`
- `modules/contracts`
- `apps/api`
- `apps/worker`
- `apps/web`
- `supabase/migrations`
- public API/realtime and architecture documentation

## Validation

- Same v2 manifest and seed produces identical ordered candidate fingerprints.
- Different pool/domain/policy/version/dataset/seed changes the appropriate fingerprint.
- A v1 persisted experiment remains readable and reproducible.
- A 100-candidate run processes 100 unique terminal candidates with Top-K 10 and in-flight four.
- Completion and recovery both refill without exceeding global/per-experiment capacity.
- Duplicate, stale and out-of-order events create no duplicate business outcome or over-allocation.
- Stop/deadline/exhaustion racing with refill allocates nothing after the authoritative decision.
- A 10,000-candidate controlled profile uses bounded active/pending work and bounded generator memory.
- Architecture tests preserve one-way module dependencies and forbid Worker direct SQL.
- Browser tests prove the configuration/monitor/leaderboard flow without prototype simulation or unsupported metrics.

## Risks and Mitigations

- **Risk**: Search-space cardinality overflows or constraints make indexed traversal sparse.  
  **Mitigation**: use checked/capped cardinality, canonical validators, bounded no-progress diagnostics, and do not claim an exact preview when only a bound is known.
- **Risk**: Multiple refill callers over-allocate.  
  **Mitigation**: durable version/fencing and unique generation/fingerprint constraints remain the commit boundary.
- **Risk**: Backtest runtime resolves a different strategy version than the candidate.  
  **Mitigation**: resolve only the frozen candidate definition through owner-published registries and verify manifest/candidate compatibility before execution.
- **Risk**: v2 additions silently break old clients.  
  **Mitigation**: additive response fields, explicit request discriminator, compatibility contract tests, and unchanged v1 decoding.

## References

- [F-015 specification](../../specs/015-configurable-composite-search/spec.md)
- [F-015 search coordination contract](../../specs/015-configurable-composite-search/contracts/search-coordination-contract.md)
- [ADR-0010: Strategy Generator Contract](0010-strategy-generator-contract.md)
- [ADR-0016: Search Coordinator Durable Orchestration](0016-search-coordinator-durable-orchestration.md)

## Supersession

- Supersedes: None
- Superseded by: None


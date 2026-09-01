# Persistence and Reproduction Contract

## Ownership ports

- Backtesting owns Result and Trade writes/reads.
- Evaluation owns Evaluation Result writes/reads.
- Leaderboard owns Revision/Entry writes/reads.
- Persistence implements these public ports and contains no execution, metric or ranking policy.

An application service may compose the ports, but each owner persists its own outcome in a separate atomic transaction: Result/Trades/EquityCurveSummary, Evaluation, or Revision/Entries. A later-stage failure does not roll back an already accepted earlier-stage outcome.

## Atomic acceptance

Before writing a new outcome, persistence must verify under the transaction:

- Experiment and Candidate match;
- Job is `BACKTEST` and matches Candidate/Experiment;
- Attempt matches Job/Candidate and is `SUCCEEDED`;
- Candidate has no different canonical Result;
- Evaluation/Revision inputs belong to the same Experiment.

On any failure, no partial outcome remains committed inside the current capability transaction. An already accepted outcome owned by an earlier capability remains immutable and available for a later retry.

## Immutability and idempotency

- Accepted artifacts reject update and delete.
- Repeating the same Candidate outcome returns the same logical result or a stable duplicate conflict.
- Different content for an existing Candidate/fingerprint key is an idempotency conflict.
- Evaluation uniqueness includes Result, metric version and ranking version.
- Revision uniqueness includes Experiment and revision number; canonical fingerprint prevents duplicate content.

## Reproduction

Experiment is the sole owner of the durable Reproduction Run and its link to original evidence. Backtesting, Evaluation and Leaderboard expose immutable verification reports and do not create a competing run record. Reproduction reloads the original frozen Manifest rather than current defaults, then compares ordered Trades, EquityCurveSummary evidence/digest, four metrics and hierarchical fingerprints. A mismatch is explicit evidence and never mutates the original.

## Schema verification

SQL tests must reject:

- non-successful or wrong Candidate Attempt;
- Result/Evaluation/Entry linked across Experiments;
- mutation/deletion of accepted artifacts;
- duplicate Result, Evaluation version or revision content;
- rank greater than Top K or inconsistent entry score/fingerprint;
- incomplete outcomes inside any one capability transaction.

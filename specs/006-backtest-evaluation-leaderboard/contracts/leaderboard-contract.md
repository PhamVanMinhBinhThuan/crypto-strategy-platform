# Leaderboard Public Contract

## Input

A complete set/page stream of immutable, eligible Evaluation Results for exactly one Experiment and one ranking version.

## Projection

- Reject cross-Experiment input and duplicate Evaluation identity.
- Sort by overall score descending, Maximum Drawdown ascending and `evaluationFingerprint` ascending.
- Keep the first 10 entries and assign contiguous ranks starting at 1.
- Canonically encode Experiment identity, ranking version and ordered entries into `leaderboard-v1`.

## Revision behavior

- First projection uses revision 1.
- Changed canonical ordered content creates the next immutable revision.
- Unchanged canonical content returns the current revision and does not create a duplicate.
- Revision and entries are written atomically.
- Entries retain Evaluation identity, exact score, drawdown and fingerprint tie-break evidence.

Worker completion order, database retrieval order and creation timestamps must not affect ranking.

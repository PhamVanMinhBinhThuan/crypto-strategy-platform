# Sentiment model release gates

The upstream MultiChannel source is GPL-3.0 and is pinned to commit
`fd1163a88d04e61e2b19a34e07da99e10acb6288`. It does not contain deployable
weights or a serialized vocabulary. No runtime process may download, train, or
invent either artifact.

A release is deployable only after all of these gates are recorded:

1. GPL-3.0 distribution approval and separate dataset/weight licensing approval.
2. Traceable training or conversion provenance and exact dependency identity.
3. English-trained model weights and a frozen English vocabulary with
   `<PAD>=0`, `<OOV>=1`, preprocessing version
   `multichannel-whitespace-en-1`, pre-padding/pre-truncation length 400, and
   class order POSITIVE, NEGATIVE, NEUTRAL. The bundle has no translation or
   runtime segmenter dependency.
4. Offline bundle verification, digest pinning, warm-up, output-shape validation,
   contract smoke testing, and a measured cold start below 120 seconds.
5. Sequential deployment of one immutable `modelVersion`; historical database
   results and releases are never rewritten.

Runtime label distributions are operational metrics and must never be presented
as model accuracy or evaluation evidence.

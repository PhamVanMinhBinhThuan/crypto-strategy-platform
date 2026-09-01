# Canonical News v1

Java alone applies this pipeline before storage or inference. Provider and Python code must not reimplement it as business normalization.

1. Parse the provider payload within configured byte limits. After normalization, title is 1-1,000 Unicode code points and analysis content is 1-100,000 code points; the internal HTTP body ceiling defaults to 256 KiB.
2. Remove script, style, template, SVG and executable markup; convert remaining permitted HTML to text; decode entities; reject content that cannot be made safe.
3. Convert CRLF/CR to LF and non-breaking spaces to ASCII space, remove C0/C1 controls except LF/TAB, normalize to Unicode NFC, collapse each run of horizontal whitespace to one ASCII space, trim every line, remove leading/trailing blank lines, and collapse more than one blank line to one LF separator.
4. Canonicalize the URL by lowercasing scheme/IDNA ASCII host, removing default ports and fragments, resolving dot segments, using `/` for an empty path, removing `utm_*`, `fbclid` and `gclid` parameters, and sorting the remaining percent-normalized key/value pairs while preserving duplicates. Never use `contentHash` as logical uniqueness.
5. Determine canonical lowercase language. Only `en` is eligible for the initial model release.
6. Encode the hash input as UTF-8: ASCII `news-canonical-v1\n`, followed in order by language, title and content, each encoded as its decimal UTF-8 byte length, ASCII colon, the bytes, then LF. Calculate SHA-256 and render `sha256:` plus lowercase hex.

The implementation must freeze URL, whitespace and hash golden fixtures before collection is enabled. A change that alters canonical URL or hash output requires a new canonicalization version and migration/compatibility review. The title and content transmitted to Python are exactly the stored normalized values used by the hash; Python may only apply its model-release-specific tokenizer.

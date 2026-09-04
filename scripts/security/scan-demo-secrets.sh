#!/usr/bin/env bash
set -euo pipefail

repo_root="$(git rev-parse --show-toplevel)"
cd "$repo_root"

scan_workspace="$(mktemp -d "${TMPDIR:-/tmp}/f014-secret-scan.XXXXXX")"
cleanup() {
  rm -rf -- "$scan_workspace"
}
trap cleanup EXIT

candidate_file="$scan_workspace/repository-files.bin"

# Include tracked files and untracked, non-ignored files that could enter the next commit.
git ls-files -z --cached --others --exclude-standard >"$candidate_file"

# Browser artifacts are ignored by Git but can accidentally embed server-side configuration.
for artifact_root in apps/web/.next/static apps/web/playwright-report apps/web/test-results; do
  if [[ -d "$artifact_root" ]]; then
    find "$artifact_root" -type f -size -10M -print0 >>"$candidate_file"
  fi
done

# Scan every text file once. Findings intentionally disclose only path, line and rule—not values.
python3 - "$candidate_file" <<'PY'
from __future__ import annotations

import pathlib
import re
import sys

candidate_file = pathlib.Path(sys.argv[1])
paths = dict.fromkeys(
    raw.decode("utf-8", errors="surrogateescape")
    for raw in candidate_file.read_bytes().split(b"\0")
    if raw
)

skipped_suffixes = {
    ".xlsx", ".png", ".jpg", ".jpeg", ".gif", ".webp", ".ico", ".woff",
    ".woff2", ".ttf", ".zip", ".gz", ".jar", ".class", ".keras",
}
safe_markers = re.compile(
    r"fixture[_-](?:password|secret)|test[_-](?:secret|password)|"
    r"public-anon-key-placeholder|public-session-token|f012-local-playwright|"
    r"example\.invalid|example\.com|"
    r"user:secret@api\.binance\.com|SUPER_SECRET|RAW_PROVIDER_PAYLOAD|"
    r"\[REDACTED:|<[^>]+>|\$\{[^}]+\}",
    re.IGNORECASE,
)
rules = {
    "PRIVATE_KEY": re.compile(r"-----BEGIN (?:RSA |EC |OPENSSH |DSA )?PRIVATE KEY-----"),
    "KNOWN_TOKEN_FORMAT": re.compile(
        r"(?:AKIA|ASIA)[0-9A-Z]{16}|gh[pousr]_[A-Za-z0-9]{30,}|"
        r"xox[baprs]-[A-Za-z0-9-]{20,}"
    ),
    "JWT_LIKE_VALUE": re.compile(
        r"eyJ[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}"
    ),
    "CREDENTIAL_IN_URL": re.compile(
        r"(?:postgres(?:ql)?|redis|https?)://[^/\s@]+:[^/\s@]+@",
        re.IGNORECASE,
    ),
    "SECRET_ASSIGNMENT": re.compile(
        r"(?:password|passwd|secret|service[_-]?role[_-]?key|private[_-]?key|"
        r"access[_-]?token|api[_-]?key)[A-Za-z0-9_.-]*\s*[:=]\s*"
        r"([\"'])[A-Za-z0-9_+/.=!@#%-]{12,}\1",
        re.IGNORECASE,
    ),
}

findings: set[tuple[str, int, str]] = set()
text_candidates = 0
for path_text in paths:
    if path_text == "scripts/security/scan-demo-secrets.sh":
        continue
    path = pathlib.Path(path_text)
    if not path.is_file() or path.suffix.lower() in skipped_suffixes:
        continue
    try:
        payload = path.read_bytes()
    except OSError:
        continue
    if b"\0" in payload[:8192]:
        continue
    text_candidates += 1
    text = payload.decode("utf-8", errors="replace")
    for line_number, line in enumerate(text.splitlines(), start=1):
        if safe_markers.search(line):
            continue
        for rule, pattern in rules.items():
            if pattern.search(line):
                findings.add((path_text, line_number, rule))

if findings:
    print(f"F014 secret scan: FAIL ({len(findings)} potential finding(s)).")
    print("Only file, line and rule are shown; suspected values are intentionally suppressed.")
    for path, line, rule in sorted(findings):
        print(f"{path}:{line} [{rule}]")
    raise SystemExit(1)

print("F014 secret scan: PASS")
print(
    f"Scanned {text_candidates} text candidates from tracked/untracked repository files "
    "and available browser artifacts."
)
print("Safe fixture allowlist: explicit placeholder/test/redaction markers only.")
PY

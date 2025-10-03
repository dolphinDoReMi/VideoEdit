#!/usr/bin/env bash
set -euo pipefail

# Derive a short, sanitized per-thread suffix from branch + short SHA
# Allowed charset for Android applicationId suffix: [a-z0-9_.]

RAW="${GITHUB_HEAD_REF:-$(git rev-parse --abbrev-ref HEAD)}-$(git rev-parse --short HEAD)"
echo "$RAW" \
  | tr '[:upper:]/-' '[:lower:]__' \
  | sed 's/[^a-z0-9_.]/_/g' \
  | cut -c1-30



#!/usr/bin/env bash
set -euo pipefail

# Derive a sanitized per-thread suffix for Android appId suffixing
# Allowed charset: [a-z0-9_.], max length 30
RAW="${GITHUB_HEAD_REF:-$(git rev-parse --abbrev-ref HEAD)}-$(git rev-parse --short HEAD)"
echo "$RAW" \
  | tr '[:upper:]/-' '[:lower:]__' \
  | sed 's/[^a-z0-9_.]/_/g' \
  | cut -c1-30



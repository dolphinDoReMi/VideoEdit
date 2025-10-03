#!/usr/bin/env bash
set -euo pipefail

# Derive a short, sanitized per-thread suffix from branch + short SHA
# Apple allows [A-Za-z0-9.-]; prefer dots as separators

RAW="${GITHUB_HEAD_REF:-$(git rev-parse --abbrev-ref HEAD)}-$(git rev-parse --short HEAD)"
echo "$RAW" \
  | tr '[:upper:]/_' '[:lower:].-' \
  | sed 's/[^a-z0-9.-]/-/g; s/\.\.\./\./g' \
  | cut -c1-40



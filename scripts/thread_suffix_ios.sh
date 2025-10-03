#!/usr/bin/env bash
set -euo pipefail

# Derive a sanitized per-thread suffix for iOS bundle id suffixing
# Allowed charset: [A-Za-z0-9.-], prefer dots, max length 40
RAW="${GITHUB_HEAD_REF:-$(git rev-parse --abbrev-ref HEAD)}-$(git rev-parse --short HEAD)"
echo "$RAW" \
  | tr '[:upper:]/_' '[:lower:].-' \
  | sed 's/[^a-z0-9.-]/-/g; s/\.{2,}/./g' \
  | cut -c1-40



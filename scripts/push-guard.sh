#!/usr/bin/env bash
# Usage: tools/push_guard.sh [--ci]
set -euo pipefail

RED=$'\e[31m'; GRN=$'\e[32m'; YEL=$'\e[33m'; NC=$'\e[0m'
fail(){ echo "${RED}❌ $*${NC}"; exit 1; }
note(){ echo "${YEL}• $*${NC}"; }
ok(){ echo "${GRN}✓ $*${NC}"; }

APP_ID="com.mira.com"
BASE="$(git merge-base origin/main HEAD 2>/dev/null || true)"
if [[ -z "${BASE}" ]]; then BASE="$(git rev-parse HEAD~1)"; fi
RANGE="${BASE}..HEAD"

CHANGED_FILES=$(git diff --name-only "${RANGE}" || true)
ADDED_LINES(){
  git diff -U0 "${RANGE}" -- "$@" | grep -E '^\+' || true
}
COMMITS_MSG="$(git log --format=%B ${RANGE} || true)"

# 1) Conventional Commit scope check
ALLOWED_SCOPES='(app|core\/infra|core\/media|core\/ml|feature\/whisper|feature\/clip|feature\/retrieval|feature\/ui|tools|docs|ci)'
INVALID=$(git log --format=%s ${RANGE} | \
  grep -Ev "^(feat|fix|perf|refactor|docs|test|chore|ci|build|revert)\\(${ALLOWED_SCOPES}\\)!?: .+")
if [[ -n "${INVALID}" ]]; then
  note "Invalid commit subjects in range ${RANGE}:"
  echo "${INVALID}"
  fail "Use Conventional Commits with an approved scope. Example: feat(feature/clip): add mean-pool unit tests"
fi
ok "Conventional Commit + scope"

# 2) App ID freeze
if ADDED_LINES app/build.gradle.kts | grep -E "applicationId\\s*=\\s*\"(.*)\"" >/dev/null; then
  if ADDED_LINES app/build.gradle.kts | grep -E "\\+\\s*applicationId\\s*=\\s*\"${APP_ID}\"" >/dev/null; then
    ok "applicationId unchanged (${APP_ID})"
  else
    fail "applicationId drift detected in app/build.gradle.kts (must remain ${APP_ID})"
  fi
fi

# 3) Broadcast/action prefix freeze in manifests
ACTION_ADDS=$(git diff -U0 "${RANGE}" -- '**/AndroidManifest.xml' | \
  grep -E '^\+.*<action android:name=' || true)
if [[ -n "${ACTION_ADDS}" ]]; then
  BAD=$(echo "${ACTION_ADDS}" | grep -Ev "${APP_ID//./\\.}\\." || true)
  if [[ -n "${BAD}" ]]; then
    echo "${BAD}"
    fail "New <action android:name> must start with '${APP_ID}.'"
  fi
  ok "Action prefix check"
fi

# 4) Risky toggle gates (Config.kt)
CFG_PATH="core/infra/src/main/java/com/mira/com/core/infra/Config.kt"
if [[ -f "${CFG_PATH}" ]]; then
  if git diff -U0 "${RANGE}" -- "${CFG_PATH}" | grep -E '^\+.*RETR_ENABLE_ANN\s*=\s*true' >/dev/null; then
    echo "${COMMITS_MSG}" | grep -Ei '^Gate:\s*ann' >/dev/null || fail "Set RETR_ENABLE_ANN requires trailer: 'Gate: ann'"
    ok "ANN gate acknowledged"
  fi
  if git diff -U0 "${RANGE}" -- 'core/media/**' | grep -E '^\+.*(resample|AudioResampler)' >/dev/null; then
    echo "${COMMITS_MSG}" | grep -Ei '^Gate:\s*resampler' >/dev/null || fail "Resampler changes require trailer: 'Gate: resampler'"
    ok "Resampler gate acknowledged"
  fi
fi

# 5) Feature edits require tests or Test Plan
need_test_check(){
  local feature="$1"
  local touched_main touched_test
  touched_main=$(echo "${CHANGED_FILES}" | grep -E "^feature/${feature}/src/main/" || true)
  [[ -z "${touched_main}" ]] && return 0
  touched_test=$(echo "${CHANGED_FILES}" | grep -E "^feature/${feature}/src/test/" || true)
  if [[ -z "${touched_test}" ]]; then
    echo "${COMMITS_MSG}" | grep -E '^(Test-Plan:|test:|NO-TEST)' >/dev/null || \
      fail "Changes in feature/${feature} require tests or a trailer: 'Test-Plan:' or 'NO-TEST'"
  fi
  ok "feature/${feature}: test presence or Test-Plan trailer"
}
need_test_check "whisper"
need_test_check "clip"
need_test_check "retrieval"
need_test_check "ui"

# 6) Quick sanity Gradle tasks (fast lane)
if [[ "${1:-}" != "--ci" ]]; then
  ./gradlew -q :app:verifyConfig ktlintCheck detekt testDebugUnitTest -x lint
  ok "Local fast checks passed"
fi

ok "push_guard complete"

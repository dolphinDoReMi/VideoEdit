# Cursor Workspace Rules

**Always keep** `applicationId = "com.mira.com"`. Do not propose changes to it.

## When editing by scope

### app
- Only modify app wiring. Do **not** change `applicationId`.
- If adding broadcast actions, the name must start with `com.mira.com.`

### feature/* (whisper|clip|retrieval|ui)
- If you change any code in `feature/<name>/src/main/**`, also:
  - add/update one test in `feature/<name>/src/test/**`, or
  - include a `Test-Plan:` trailer in the commit.
- For retrieval:
  - Do not change on-disk format `.f32` + JSON without explicit rationale.
- For whisper:
  - JNI bridge = native hot path; avoid allocations in loops.

### core/media
- Prefer pure functions; preserve 16 kHz mono determinism.
- If changing resampler logic, add trailer `Gate: resampler`.

### core/infra
- Only toggle flags via `Config.kt`. Changing `RETR_ENABLE_ANN` requires trailer `Gate: ann`.

## Commit style
Use Conventional Commits with approved scopes:
`feat|fix|perf|refactor|docs|test|chore|ci|build|revert (scope): summary`

## Ready-to-run checks before proposing a diff
- `./gradlew :app:verifyConfig ktlintCheck detekt testDebugUnitTest -x lint`

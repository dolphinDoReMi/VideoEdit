# Change Policy (App & Features)

## Scopes (must be used in commit subjects)
app | core/infra | core/media | core/ml | feature/whisper | feature/clip | feature/retrieval | feature/ui | tools | docs | ci

## App-level guarantees
- `applicationId` is **frozen** at `com.mira.com`.
- New `<action android:name="...">` must start with `com.mira.com.`.
- Build variants keep `.debug` / `.internal` suffixes only.

## Feature changes
- If you touch `feature/*/src/main/**`, you must also:
  - add/modify a test under `feature/*/src/test/**`, **or**
  - include a `Test-Plan:` (or `NO-TEST`) trailer in your commit.

## Risk-gated toggles (require commit trailer)
- Flip `Config.RETR_ENABLE_ANN = true` → add `Gate: ann`
- Modify `AudioResampler` or resample algorithm → add `Gate: resampler`

## Commit format (Conventional Commits)
Examples:
- `feat(feature/clip): mean-pool over 32 frames`
- `fix(core/media): avoid off-by-one in linear resampler`
- `ci(ci): run policy guard on PRs`
Trailers allowed at end:

Test-Plan: unit + adb smoke
Gate: ann

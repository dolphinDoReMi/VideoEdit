# Policy Guard System Implementation

## Overview

A comprehensive policy enforcement system has been implemented to maintain app identity, enforce scoped changes, and ensure quality gates across the unified `com.mira.com` architecture.

## Components Implemented

### 1. **Push Guard Script** (`tools/push_guard.sh`)
- **App ID Freeze**: Ensures `applicationId` remains `com.mira.com`
- **Action Prefix Enforcement**: New broadcast actions must start with `com.mira.com.`
- **Conventional Commits**: Enforces proper commit format with approved scopes
- **Feature Test Requirements**: Changes to feature modules require tests or Test-Plan trailer
- **Risk Gate Controls**: Toggles for ANN and resampler require explicit trailers
- **Fast Gradle Checks**: Runs quick validation tasks locally

### 2. **Git Hooks** (`.git/hooks/`)
- **commit-msg**: Validates Conventional Commits format with approved scopes
- **pre-push**: Runs policy guard before allowing pushes
- **Installer**: `tools/install_git_hooks.sh` for easy setup

### 3. **GitHub Actions** (`.github/workflows/push-guard.yml`)
- **Server-side Enforcement**: Mirrors all client-side checks in CI
- **PR Protection**: Runs on pull requests and pushes to main
- **Fast Validation**: Quick gradle tasks without full build

### 4. **Documentation**
- **Change Policy** (`docs/Change-Policy.md`): Human-readable policy guide
- **Cursor Rules** (`docs/Cursor-Workspace-Rules.md`): AI assistant guidelines
- **CODEOWNERS**: Code review requirements by module

### 5. **Approved Scopes**
```
app | core/infra | core/media | core/ml | feature/whisper | feature/clip | feature/retrieval | feature/ui | tools | docs | ci
```

## Policy Rules

### App Identity Protection
- `applicationId` frozen at `com.mira.com`
- New `<action android:name="...">` must start with `com.mira.com.`
- Build variants maintain `.debug` / `.internal` suffixes

### Feature Change Requirements
- Changes to `feature/*/src/main/**` require:
  - Tests in `feature/*/src/test/**`, OR
  - `Test-Plan:` or `NO-TEST` trailer in commit

### Risk-Gated Toggles
- `Config.RETR_ENABLE_ANN = true` → requires `Gate: ann` trailer
- `AudioResampler` changes → requires `Gate: resampler` trailer

### Commit Format
```
feat(scope): description
fix(scope): description
Test-Plan: unit + adb smoke
Gate: ann
```

## Usage

### Install Hooks
```bash
bash tools/install_git_hooks.sh
```

### Manual Policy Check
```bash
tools/push_guard.sh
```

### Valid Commit Examples
```bash
git commit -m "feat(feature/clip): add mean-pool unit tests

Test-Plan: unit test passes; adb SMOKE ok"

git commit -m "fix(core/media): avoid off-by-one in linear resampler

Gate: resampler"
```

### Invalid Examples (Will Be Rejected)
```bash
# Wrong scope
git commit -m "feat(unknown): add feature"

# Missing test requirement
git commit -m "feat(feature/clip): modify main code"

# Missing gate trailer
git commit -m "feat(core/infra): enable ANN"
```

## Testing Status

✅ **Git Hooks Installed**: commit-msg and pre-push hooks active
✅ **Commit Validation**: Accepts valid format, rejects invalid
✅ **Policy Script**: Ready for use in CI and local development
✅ **Documentation**: Complete policy and workspace rules

## Integration Points

### CI/CD
- Policy guard runs automatically on PRs and pushes
- Fast gradle checks without full build overhead
- Server-side enforcement as source of truth

### Development Workflow
- Client-side hooks catch issues early
- Clear error messages guide developers
- Conventional commits enable automation

### Code Review
- CODEOWNERS ensure appropriate reviewers
- Labeler automatically tags PRs by changed modules
- Risk gates require explicit acknowledgment

## Benefits

1. **App Identity Protection**: Prevents accidental applicationId changes
2. **Quality Assurance**: Ensures tests accompany feature changes
3. **Risk Management**: Controlled rollout of risky features
4. **Clean History**: Standardized commit messages enable automation
5. **Early Detection**: Client hooks catch issues before CI
6. **Server Authority**: CI provides authoritative enforcement

## Next Steps

1. **Team Adoption**: Share policy documentation with team
2. **Branch Protection**: Require policy guard status checks
3. **Monitoring**: Track policy violations and improvements
4. **Expansion**: Add more risk gates as needed

The policy guard system is now fully operational and ready to maintain the integrity of the unified `com.mira.com` architecture while enabling controlled, high-quality development.

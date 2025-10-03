# DEV Changelog Scripts

This directory contains scripts for managing the development changelog and version history.

## Contents

Currently empty - changelog scripts will be added as needed for:
- Changelog generation
- Version bumping
- Release note generation
- Change tracking
- Version history management

## Usage

```bash
# Generate changelog from git commits
./scripts/dev-changelog/generate-changelog.sh

# Bump version and update changelog
./scripts/dev-changelog/bump-version.sh

# Generate release notes
./scripts/dev-changelog/generate-release-notes.sh

# Track changes since last release
./scripts/dev-changelog/track-changes.sh
```

## Integration

These scripts integrate with:
- Git hooks for automatic changelog updates
- CI/CD pipeline for version management
- Release process for automated notes generation
- Development workflow for change tracking

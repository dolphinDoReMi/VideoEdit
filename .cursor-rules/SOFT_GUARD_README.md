# Android VideoEditor Soft Guard System

## Overview

This is a **soft guard compliance system** for Android agent automation that provides governance without blocking autonomous operation. The system analyzes planned commands and code changes, generates compliance reports with clear warnings, and allows you to flip to strict mode for CI/PRs when needed.

## Key Features

- **🔄 Soft by Default**: Never blocks runs; always exits with code 0 unless `--strict true`
- **📊 Risk Scoring**: Analyzes commands and diffs with LOW/MEDIUM/HIGH risk levels
- **🎯 Android-Focused**: Specialized patterns for ADB, Gradle, MediaStore, and scoped storage
- **📋 Compliance Reports**: Human-readable reports with INFO/WARN/ALERT levels
- **🏗️ Layered Rules**: Base → Security → Android → Project → Local hierarchy
- **🔍 Pattern Detection**: Comprehensive trigger detection for Android operations

## Architecture

### Rule Layers (Priority Order)
1. **`rules/10-base.core.json`** - Code quality, testing, performance guidelines
2. **`rules/20-security.overlay.json`** - Tool allowlist, trigger classification, compliance reporting
3. **`rules/30-domain.android.json`** - Android-specific rules (scoped storage, MediaStore, services)
4. **`rules/40-project.app.json`** - Project-specific rules (VideoEditor, ML models, testing)
5. **`rules/99-local.override.json`** - Local development overrides (not committed)

### Pattern Files
- **`scripts/patterns.deny.json`** - 🚨 ALERT patterns (score +5 each)
- **`scripts/patterns.require_approval.json`** - ⚠️ WARN patterns (score +2 each)
- **`scripts/patterns.allow.json`** - ℹ️ INFO patterns (score +0)

### Core Scripts
- **`scripts/compliance-check.js`** - Main soft guard checker
- **`scripts/build-cursor-rules.py`** - Combines layered rules
- **`scripts/validate-cursor-rules.js`** - Validates rule syntax

## Usage

### Agent Workflow

1. **Start with task definition:**
```task
Goal: Add MediaStore query validation to ScopedStorageTestActivity
Success: New test method passes with proper content URI access
Scope: infra-storage/src/main/java/com/mira/videoeditor/infra/storage/
Out-of-scope: app/src/main/java/com/mira/videoeditor/
Budget: tool_calls=10, reads_per_call<=250 lines, max_total_writes=3 files
Risk tolerance: MEDIUM
Network: DENIED
```

2. **Before risky operations, output triggercheck:**
```triggercheck
PlannedCommands: adb shell "am start -n com.mira.whisper/..."
Categories: ADB|GRADLE|MEDIASTORE
DetectedPatterns: app_start|permission_grant|content_query
PolicyIntent: ADB->REVIEW|GRADLE->OBSERVE|MEDIASTORE->APPROVAL_RECOMMENDED
```

3. **Show diffplan and save files:**
```diffplan
Files to modify:
- ScopedStorageTestActivity.kt: Add testMediaStoreQueryValidation()
- Test plan: Run unit tests and verify scoped storage compliance
```

4. **Run compliance check:**
```bash
node scripts/compliance-check.js --diffplan .cursor.diffplan.txt --commands .cursor.commands.txt --risk MEDIUM --strict false
```

5. **Paste compliance report:**
```compliance
Mode: SOFT (strict=false) | Risk=MEDIUM | Score=4 → Level=MEDIUM

⚠️  WARN (REQUIRE_APPROVAL patterns):
   - pm\s+grant\b\s+\S+\s+android\.permission\.[A-Z_]+
   - gradle(w|\.bat)?\b.*\b(assemble|bundle|install|connected.*AndroidTest)\b

📋 Recommendations:
- 👀 Skim the diff and confirm that ADB/Gradle/MediaStore usage matches project policy.
- 📱 Ensure scoped storage patterns are followed.
- 🔒 Review any permission grants for necessity.
```

### Manual Usage

```bash
# Check compliance (soft mode)
node scripts/compliance-check.js \
  --diffplan .cursor.diffplan.txt \
  --commands .cursor.commands.txt \
  --risk MEDIUM \
  --strict false

# Check compliance (strict mode for CI)
node scripts/compliance-check.js \
  --diffplan .cursor.diffplan.txt \
  --commands .cursor.commands.txt \
  --risk HIGH \
  --strict true

# Validate all rules
node scripts/validate-cursor-rules.js

# Build combined rules
python3 scripts/build-cursor-rules.py
```

## Risk Levels

### LOW (Score 0-2)
- ✅ Safe operations, minimal impact
- ℹ️ INFO patterns only
- 📊 Monitor resource usage and performance impact

### MEDIUM (Score 3-5)
- 👀 Skim the diff and confirm ADB/Gradle/MediaStore usage matches project policy
- 📱 Ensure scoped storage patterns are followed
- 🔒 Review any permission grants for necessity

### HIGH (Score 6+)
- 🚨 Review manually before merging
- 🔍 Check for Android scoped storage compliance violations
- 🛡️ Verify permission grants are necessary and properly documented
- Consider re-running with `--strict true` in CI

## Android-Specific Patterns

### DENY Patterns (🚨 ALERT)
- `setprop` commands
- `adb shell su` (root access)
- `rm -rf /sdcard` (destructive operations)
- `/sdcard` path usage with file operations
- `adb shell reboot/shutdown`
- `adb shell kill -9`

### REQUIRE_APPROVAL Patterns (⚠️ WARN)
- `adb shell am start|startservice|broadcast`
- `adb shell content insert|update|delete`
- `pm grant` permission operations
- `gradlew assemble|bundle|install|connectedAndroidTest`
- `adb shell input tap|text` (UI interaction)
- `adb shell am force-stop`
- `adb shell pm install|clear`

### ALLOW Patterns (ℹ️ INFO)
- `adb logcat` (read-only)
- `dumpsys` system information
- `getprop` device properties
- `gradlew test|lint|check|ktlintCheck|detekt`
- `adb devices|shell ps|pidof`
- `adb shell ls|find|cat|head|tail|hexdump`
- `adb shell content query` (read-only)

## Scoped Storage Compliance

The system enforces Android scoped storage requirements:

### Forbidden in Code
- `Environment.getExternalStorageDirectory()`
- Hardcoded `/sdcard/` paths
- `MediaExtractor.setDataSource(String)`
- `new FileInputStream(String)`

### Required Patterns
- `DLStorage.resolveModelPath()` for models
- `DLStorage.openReadFd()` for media
- Content URI → FileDescriptor → JNI FD pattern
- `JNIEXPORT ... Java_...transcribeFromFd(ctx, fd)` in native code

## Configuration

### Environment Variables
- `CURSOR_RULES_STRICT=true` - Enable strict mode globally
- `CURSOR_RULES_RISK=HIGH` - Set default risk level

### CI Integration
```yaml
# .github/workflows/compliance.yml
- name: Check Compliance
  run: |
    node scripts/compliance-check.js \
      --diffplan .cursor.diffplan.txt \
      --commands .cursor.commands.txt \
      --risk HIGH \
      --strict true
```

## Best Practices

### For Agents
1. Always output `triggercheck` before risky operations
2. Save planned commands and diffplan to files
3. Run compliance check and paste report
4. Proceed autonomously unless explicitly told otherwise
5. Never commit unless tests and lint pass

### For Developers
1. Review HIGH risk compliance reports manually
2. Use `--strict true` in CI for production branches
3. Update pattern files when new Android APIs are introduced
4. Keep local overrides in `rules/99-local.override.json` (not committed)

### For CI/CD
1. Run compliance check on all PRs
2. Use `--strict true` for main/master branches
3. Fail builds on HIGH risk with DENY patterns
4. Allow MEDIUM risk with manual review

## Troubleshooting

### Common Issues

**"Invalid regular expression" errors:**
- Check pattern files for proper regex syntax
- Use `node scripts/validate-cursor-rules.js` to validate

**"No known patterns matched":**
- Add new patterns to appropriate pattern files
- Check case sensitivity (patterns are case-insensitive)

**"HIGH risk but proceeding":**
- This is expected in soft mode
- Use `--strict true` to fail on HIGH risk

### Debug Mode
```bash
# Enable debug logging
DEBUG=1 node scripts/compliance-check.js --diffplan .cursor.diffplan.txt --commands .cursor.commands.txt --risk MEDIUM --strict false
```

## Contributing

1. Add new patterns to appropriate pattern files
2. Update rule files for new Android APIs or project requirements
3. Test with `node scripts/validate-cursor-rules.js`
4. Update documentation for new features

## License

This soft guard system is part of the VideoEditor project and follows the same license terms.

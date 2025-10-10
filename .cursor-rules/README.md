# Cursor Rules System - Consolidated Structure

## Overview

All cursor rules and related files are now consolidated in the `.cursor-rules/` directory for better organization and management.

## Directory Structure

```
.cursor-rules/
├── README.md                           # This file
├── .cursorrules.json                   # Main cursor rules (generated)
├── .cursorrules                        # Legacy cursor rules (backup)
├── AGENT_KICKOFF_MACRO.md             # Agent workflow instructions
├── SOFT_GUARD_README.md               # Soft guard system documentation
│
├── Rule Files (Layered Priority):
├── 10-base.core.json                  # Code quality, testing, performance
├── 20-security.overlay.json          # Tool allowlist, trigger classification
├── 30-domain.android.json             # Android-specific rules
├── 30-domain.android.mirastorage.json # MiraStorage policy (CRITICAL)
├── 40-project.app.json                # Project-specific rules
├── 99-local.override.json             # Local development overrides
│
├── Pattern Detection Files:
├── patterns.deny.json                 # 🚨 ALERT patterns (score +5)
├── patterns.require_approval.json     # ⚠️ WARN patterns (score +2)
├── patterns.allow.json               # ℹ️ INFO patterns (score +0)
│
└── Scripts:
    ├── compliance-check.js            # Main soft guard checker
    ├── build-cursor-rules.py          # Combines layered rules
    └── validate-cursor-rules.js       # Validates rule syntax
```

## Quick Commands

### Build Rules
```bash
python3 .cursor-rules/build-cursor-rules.py
```

### Validate Rules
```bash
node .cursor-rules/validate-cursor-rules.js
```

### Check Compliance
```bash
node .cursor-rules/compliance-check.js \
  --diffplan .cursor.diffplan.txt \
  --commands .cursor.commands.txt \
  --risk MEDIUM \
  --strict false
```

## Rule Priority Order

1. **Base Rules** (`10-base.core.json`) - Foundation guidelines
2. **Security Rules** (`20-security.overlay.json`) - Tool restrictions and compliance
3. **Android Rules** (`30-domain.android.json`) - Platform-specific rules
4. **MiraStorage Rules** (`30-domain.android.mirastorage.json`) - File I/O policy
5. **Project Rules** (`40-project.app.json`) - VideoEditor-specific rules
6. **Local Overrides** (`99-local.override.json`) - Development preferences

## Pattern Detection

- **DENY Patterns**: Blocked operations (setprop, su, rm -rf /sdcard, etc.)
- **REQUIRE_APPROVAL Patterns**: Operations requiring review (ADB commands, file I/O, etc.)
- **ALLOW Patterns**: Safe operations (logcat, dumpsys, read-only queries, etc.)

## MiraStorage Policy (CRITICAL)

**ALL Android file I/O MUST use MiraStorageService:**

### Forbidden Patterns
- `java.io.File`, `FileInputStream/OutputStream`, `RandomAccessFile`
- `java.nio.file.Files`, `Paths.get`
- `Environment.getExternalStorageDirectory()`, `/sdcard`
- JNI `fopen()`, `open("/sdcard/...")`

### Required Patterns
- `MiraStorage.openReadFd(uri)`, `MiraStorage.openWriteFd(uri)`
- `MiraStorage.readBytes(uri)`, `MiraStorage.writeBytes(uri, data)`
- JNI: Accept `int fd` parameter; use `read(2)/write(2)/mmap` on FD only

## Agent Workflow

1. Start with task definition in `AGENT_KICKOFF_MACRO.md`
2. Output `triggercheck` for risky operations
3. Show `diffplan` and save to files
4. Run compliance check with soft guard
5. Paste compliance report and proceed

## Benefits of Consolidation

- **Single Location**: All cursor rules in one place
- **Clear Organization**: Layered structure with priority order
- **Easy Maintenance**: Update patterns and rules in one directory
- **Better Version Control**: All related files tracked together
- **Simplified CI/CD**: Single path for rule validation and compliance

## Migration Notes

- Moved from scattered locations (`rules/`, `scripts/`, root directory)
- Updated all script paths to use `.cursor-rules/`
- Maintained all existing functionality
- No breaking changes to agent workflow

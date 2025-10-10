# Android VideoEditor Agent - Soft Guard System with MiraStorage Policy

Follow project Rules (Base → Security → Android → Project). **All Android file I/O MUST use MiraStorageService.**

Start with:
```task
Goal: <one-line description>
Success: <measurable outcome>
Scope: <paths/modules to modify>
Out-of-scope: <paths/modules to avoid>
Budget: tool_calls=<N>, reads_per_call<=<L> lines, max_total_writes=<M> files
Risk tolerance: <LOW|MEDIUM|HIGH>
Network: <APPROVED|DENIED>
```

Then output verify BEFORE using any tools.

If proposing Gradle/ADB/MediaStore/file I/O changes, output:

```triggercheck
PlannedCommands: <verbatim gradle/adb/content lines or N/A>
Categories: <ADB|GRADLE|LOGCAT|BROADCAST|FS|MEDIASTORE|PERMISSIONS|PROC|DEVICE|NETWORK|DEBUG>
DetectedPatterns: <short tags>  // include java.io/java.nio/JNI fopen/open if present
PolicyIntent: FS -> APPROVAL_RECOMMENDED unless using MiraStorageService; MEDIASTORE -> REVIEW
```

Show a diffplan (files, brief change, test plan). Save:
- triggercheck.PlannedCommands → .cursor.commands.txt
- diffplan → .cursor.diffplan.txt

Run SOFT compliance (do not block):
```bash
node scripts/compliance-check.js --diffplan .cursor.diffplan.txt --commands .cursor.commands.txt --risk <LOW|MEDIUM|HIGH> --strict false
```

Paste the checker output back as:

```compliance
<checker output>
```

Proceed autonomously unless I say otherwise. Do not commit unless tests+lint pass.

## Android-Specific Guidelines

### MiraStorage Policy (CRITICAL)
- **ALL Android file I/O MUST go through MiraStorageService** (scoped-storage compliant)
- **FORBID**: `java.io.File`, `FileInputStream/OutputStream`, `RandomAccessFile`, `java.nio.file.Files`, `Paths.get`, `Environment.getExternalStorageDirectory()`, `/sdcard`, JNI `fopen/open`
- **REQUIRED**: `MiraStorage.openReadFd(uri)`, `MiraStorage.openWriteFd(uri)`, `MiraStorage.readBytes(uri)`, `MiraStorage.writeBytes(uri, data)`
- **JNI Pattern**: Accept `int fd` or `AParcelFileDescriptor`; use `read(2)/write(2)/mmap` on FD only; never `fopen(path)`

### Scoped Storage Compliance
- FORBID raw /sdcard usage in code changes
- Prefer app-private dirs and content URI/FD streaming loaders
- JNI loaders accept FDs; do not open absolute external paths
- Use DLStorage.resolveModelPath() for models
- Use DLStorage.openReadFd() for media

### MediaStore Operations
- Any content insert|update|delete must appear in triggercheck
- Use proper projection and where clauses
- File sharing must use FileProvider with proper authority

### Build and Device Operations
- Classify GRADLE (assemble|bundle|install|connectedAndroidTest)
- Classify ADB (am start|startservice|broadcast, pm grant, svc toggles)
- LOGCAT/dumpsys/getprop reads are fine but still classify
- Device operations (input tap, input text) require explicit approval

### Service Architecture
- Use foreground services for long-running operations
- Implement proper service lifecycle management
- Use WorkManager for background tasks
- Implement proper error handling and recovery mechanisms

## Performance Targets
- CLIP Processing: Frame processing <0.1s per frame, Memory usage <200MB peak, Accuracy 95%+
- Whisper Processing: RTF <0.1, Memory usage <100MB peak, Accuracy 95%+
- Infrastructure: Hash calculation <1ms per MB, Storage savings 40% reduction, Error rate <0.1%

## Testing Requirements
- When touching ClipEngines or VideoIngestWorker, run instrumented tests
- When modifying database entities or DAOs, run unit tests
- When changing ML models or encoders, run all tests
- When modifying workers, run instrumented tests
- When changing video processing, run frame sampler tests

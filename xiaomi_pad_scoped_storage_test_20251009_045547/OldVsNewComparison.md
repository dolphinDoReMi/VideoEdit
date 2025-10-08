# Old vs New Approach Comparison

## ❌ Old Approach (Forbidden Patterns)

```kotlin
// OLD: Direct file path access
val modelPath = "/sdcard/MiraWhisper/models/whisper-base.q5_1.bin"
val audioPath = "/sdcard/Download/tennis_interview_clip_002.mp4"

// OLD: Raw file operations
val modelFile = File(modelPath)
val audioFile = File(audioPath)

// OLD: Direct MediaExtractor usage
val extractor = MediaExtractor()
extractor.setDataSource(audioPath)  // ❌ FORBIDDEN

// OLD: Direct model loading
val ctx = NativeWhisper.initFromPath(modelPath)  // ❌ FORBIDDEN

// OLD: Direct file writing
val outputFile = File("/sdcard/MiraWhisper/out/transcript.txt")
outputFile.writeText(transcript)
```

## ✅ New Approach (Scoped Storage Compliant)

```kotlin
// NEW: Capability-based access
val tennisClipUri = Uri.parse("content://media/external/video/media/123")
val modelUri = Uri.parse("content://com.mira.videoeditor.provider/models/whisper-tiny.gguf")

// NEW: Scoped storage infrastructure
val db = EmbeddingDb.getInstance(context)
val storage: DLStorage = DLStorageImpl(context, db)
val whisperBridge = ScopedWhisperBridge(context, storage)

// NEW: Model installation to app-private storage
val modelHandle = storage.installModel(modelUri, "whisper-tiny-en.gguf")
val modelPath = storage.resolveModelPath(modelHandle)  // App-private path

// NEW: FD-based audio access
val fd = storage.openReadFd(tennisClipUri)
val extractor = MediaExtractor()
extractor.setDataSource(fd.toLong(), 0, Long.MAX_VALUE)  // ✅ COMPLIANT

// NEW: Scoped model loading
val ctx = NativeWhisper.initModelFromAppFile(modelPath)  // ✅ COMPLIANT

// NEW: FD-based transcription
val transcript = NativeWhisper.transcribeFromFd(ctx, fd)  // ✅ COMPLIANT

// NEW: App-private output with FileProvider sharing
val transcriptFile = storage.writeTranscript(tennisClipUri.toString(), transcript)
val shareableUri = storage.shareOutput(transcriptFile)  // ✅ COMPLIANT
```

## Key Differences

| Aspect | Old Approach | New Approach |
|--------|-------------|--------------|
| **Model Storage** | Raw `/sdcard/` paths | App-private with SHA-256 validation |
| **Media Access** | Direct file paths | Capability-based FD access |
| **Model Loading** | Direct path loading | mmap from app-private storage |
| **Audio Processing** | `setDataSource(String)` | `setDataSource(fd)` |
| **Output Sharing** | Direct file access | FileProvider with content URIs |
| **Security** | Broad file system access | Scoped, capability-based access |
| **Compliance** | Violates scoped storage | Fully compliant |

## Benefits of New Approach

1. **Security**: No broad file system access required
2. **Compliance**: Meets Android scoped storage requirements
3. **Reliability**: SHA-256 validation prevents corruption
4. **Performance**: mmap for efficient model loading
5. **Flexibility**: Works with any content provider
6. **Future-proof**: Compatible with Android's evolving security model

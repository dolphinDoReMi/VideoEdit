# Live Transcription Workflow for Tennis Interview Clip

## Complete Process Flow

### Phase 1: Preparation
1. **File Analysis**: tennis_interview_clip_002.mp4 (93MB, 5 minutes)
2. **Model Preparation**: whisper-tiny-en.gguf (app-private installation)
3. **Storage Setup**: Scoped storage infrastructure initialization

### Phase 2: Model Installation
```kotlin
val modelHandle = storage.installModel(modelUri, "whisper-tiny-en.gguf")
// → Installs to: /data/user/0/com.mira.videoeditor/files/models/whisper-tiny-en.gguf@<sha256>
// → Validates SHA-256 hash
// → Returns ModelHandle with metadata
```

### Phase 3: Audio Access
```kotlin
val fd = storage.openReadFd(tennisClipUri)
// → Opens: content://media/external/video/media/123
// → Returns file descriptor for JNI
// → No raw path exposure
```

### Phase 4: Model Loading
```kotlin
val ctxPtr = NativeWhisper.initModelFromAppFile(modelPath)
// → Loads from app-private path using mmap
// → Efficient memory mapping for large models
// → Returns context pointer for transcription
```

### Phase 5: Transcription
```kotlin
val transcript = NativeWhisper.transcribeFromFd(ctxPtr, fd)
// → Processes audio via file descriptor
// → No path-based access
// → Returns transcribed text
```

### Phase 6: Output Generation
```kotlin
val transcriptFile = storage.writeTranscript(tennisClipUri.toString(), transcript)
// → Writes to: /data/user/0/com.mira.videoeditor/files/outputs/<sanitized_key>.txt
// → App-private storage only
```

### Phase 7: Sharing
```kotlin
val shareableUri = storage.shareOutput(transcriptFile)
// → Creates: content://com.mira.videoeditor.files/<sanitized_key>.txt
// → FileProvider-based sharing
// → External apps can access via content URI
```

## Expected Results

### Tennis Interview Clip Transcription
- **Input**: 93MB MP4 file (5 minutes)
- **Model**: whisper-tiny-en.gguf
- **Processing Time**: ~30-60 seconds on Xiaomi Pad
- **Output**: Complete transcript in app-private storage
- **Sharing**: FileProvider URI for external access

### Performance Metrics
- **Model Loading**: ~2-3 seconds (mmap)
- **Audio Processing**: ~30-60 seconds (FD-based)
- **Memory Usage**: Efficient (app-private storage)
- **Storage**: App-private only (scoped storage compliant)

## Security & Compliance
- ✅ No MANAGE_EXTERNAL_STORAGE permission required
- ✅ Capability-based access only
- ✅ App-private storage isolation
- ✅ SHA-256 model validation
- ✅ FileProvider-based sharing

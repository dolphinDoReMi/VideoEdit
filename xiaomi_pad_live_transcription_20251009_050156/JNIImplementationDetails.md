# JNI Implementation for Live Transcription

## Key JNI Methods for Tennis Clip Processing

### 1. Model Initialization
```cpp
JNIEXPORT jlong JNICALL
Java_com_mira_videoeditor_infra_storage_NativeWhisper_initModelFromAppFile(
    JNIEnv* env, jclass clazz, jstring modelPath) {
    
    const char* path_chars = env->GetStringUTFChars(modelPath, nullptr);
    std::string path_str(path_chars);
    env->ReleaseStringUTFChars(modelPath, path_chars);
    
    LOGI("Initializing Whisper model from app-private path: %s", path_str.c_str());
    
    // Load model using mmap from app-private storage
    whisper_context* ctx = load_model_or_throw(path_str.c_str());
    if (!ctx) {
        LOGE("Failed to load model from path: %s", path_str.c_str());
        return 0;
    }
    
    // Store context with unique ID
    jlong ctxId = g_next_ctx_id++;
    g_contexts[ctxId] = ctx;
    
    LOGI("Model loaded successfully with context ID: %lld", ctxId);
    return ctxId;
}
```

### 2. FD-Based Transcription
```cpp
JNIEXPORT jstring JNICALL
Java_com_mira_videoeditor_infra_storage_NativeWhisper_transcribeFromFd(
    JNIEnv* env, jclass clazz, jlong ctxPtr, jint fd) {
    
    // Find context
    auto it = g_contexts.find(ctxPtr);
    if (it == g_contexts.end()) {
        LOGE("Invalid context pointer: %lld", ctxPtr);
        return env->NewStringUTF("Error: Invalid context pointer");
    }
    
    whisper_context* ctx = it->second;
    
    LOGI("Transcribing from file descriptor: %d", fd);
    
    // Use fdopen to create FILE* from file descriptor
    FILE* file = fdopen(dup(fd), "rb");
    if (!file) {
        LOGE("Failed to open file descriptor %d: %s", fd, strerror(errno));
        return env->NewStringUTF("Error: Failed to open file descriptor");
    }
    
    // Read audio data from file descriptor
    std::vector<float> audio_data;
    const size_t buffer_size = 4096;
    char buffer[buffer_size];
    ssize_t bytes_read;
    
    // Read raw audio data (assuming 16-bit PCM)
    while ((bytes_read = fread(buffer, 1, buffer_size, file)) > 0) {
        // Convert 16-bit PCM to float
        for (size_t i = 0; i < bytes_read; i += 2) {
            if (i + 1 < bytes_read) {
                int16_t sample = static_cast<int16_t>((buffer[i+1] << 8) | buffer[i]);
                audio_data.push_back(sample / 32768.0f);
            }
        }
    }
    
    fclose(file);
    
    LOGI("Read %zu audio samples from file descriptor", audio_data.size());
    
    // Set up whisper parameters
    struct whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    params.print_realtime = false;
    params.print_progress = false;
    params.print_timestamps = true;
    params.print_special = false;
    params.translate = false;
    params.language = nullptr; // Auto-detect language
    params.n_threads = 4;
    params.offset_ms = 0;
    params.no_context = true;
    params.single_segment = false;
    
    // Run whisper inference
    LOGI("Running whisper inference...");
    whisper_reset_timings(ctx);
    
    int result = whisper_full(ctx, params, audio_data.data(), audio_data.size());
    if (result != 0) {
        LOGE("Whisper inference failed with code: %d", result);
        return env->NewStringUTF("Error: Whisper inference failed");
    }
    
    // Extract results
    int n_segments = whisper_full_n_segments(ctx);
    LOGI("Whisper inference completed with %d segments", n_segments);
    
    std::string all_text;
    for (int i = 0; i < n_segments; i++) {
        const char* text = whisper_full_get_segment_text(ctx, i);
        all_text += std::string(text) + " ";
    }
    
    LOGI("Whisper inference completed successfully");
    return env->NewStringUTF(all_text.c_str());
}
```

## Key Features

1. **App-Private Model Loading**: Models loaded from app-private storage using mmap
2. **FD-Based Audio Processing**: Audio processed via file descriptor, no path exposure
3. **Context Management**: Multiple contexts supported with unique IDs
4. **Resource Cleanup**: Proper cleanup of contexts and file descriptors
5. **Error Handling**: Comprehensive error handling and logging
6. **Scoped Storage Compliance**: No raw file path usage

## Performance Benefits

- **mmap Loading**: Efficient memory mapping for large models
- **FD Processing**: Direct file descriptor access without path resolution
- **Context Reuse**: Multiple contexts can be managed simultaneously
- **Memory Efficiency**: Proper resource cleanup prevents memory leaks

#include "whisper_loader.h"
#include "ggml.h"
#include "whisper.h"
#include <android/log.h>
#include <cstring>
#include <cstdio>

// Undefine the macro to allow direct calls within the loader
#undef whisper_init_from_file_with_params
#undef whisper_init_from_file

static void ggml_log_cb(enum ggml_log_level level, const char* text, void*) {
    __android_log_print(ANDROID_LOG_DEBUG, "WhisperJNI", "[GGML] %s", text);
}

whisper_context* load_model_or_throw(const char* abs_path) {
    __android_log_print(ANDROID_LOG_INFO, "WhisperJNI", "Loading model: %s", abs_path);
    
    // Check if file exists and is readable
    FILE* f = fopen(abs_path, "rb");
    if (!f) { 
        __android_log_print(ANDROID_LOG_ERROR, "WhisperJNI", "fopen failed for: %s", abs_path); 
        return nullptr; 
    }
    
    // Check file size
    fseek(f, 0, SEEK_END);
    long file_size = ftell(f);
    fseek(f, 0, SEEK_SET);
    __android_log_print(ANDROID_LOG_INFO, "WhisperJNI", "File size: %ld bytes", file_size);
    
    if (file_size < 1024) {
        __android_log_print(ANDROID_LOG_ERROR, "WhisperJNI", "Model file too small: %ld bytes", file_size);
        fclose(f);
        return nullptr;
    }
    
    // Check header format
    char hdr[4] = {0};
    size_t bytes_read = fread(hdr, 1, 4, f);
    fseek(f, 0, SEEK_SET);
    
    if (bytes_read != 4) {
        __android_log_print(ANDROID_LOG_ERROR, "WhisperJNI", "Failed to read model header");
        fclose(f);
        return nullptr;
    }
    
    __android_log_print(ANDROID_LOG_INFO, "WhisperJNI", "Header bytes: %02x %02x %02x %02x ('%.4s')", 
                       hdr[0], hdr[1], hdr[2], hdr[3], hdr);
    
    // Support both GGUF and GGML formats temporarily
    if (strncmp(hdr, "GGUF", 4) != 0 && strncmp(hdr, "lmgg", 4) != 0) { 
        __android_log_print(ANDROID_LOG_ERROR, "WhisperJNI", "Bad header: '%.4s' (expected GGUF or GGML)", hdr); 
        fclose(f); 
        return nullptr; 
    }
    
    if (strncmp(hdr, "lmgg", 4) == 0) {
        __android_log_print(ANDROID_LOG_WARN, "WhisperJNI", "Detected GGML format model (legacy support)");
    } else {
        __android_log_print(ANDROID_LOG_INFO, "WhisperJNI", "Detected GGUF format model");
    }
    
    fclose(f);
    
    // Enable GGML logging to see internal loader chatter
    ggml_log_set(ggml_log_cb, nullptr);
    
    // Attempt to load the model
    __android_log_print(ANDROID_LOG_INFO, "WhisperJNI", "Attempting to load model: %s", abs_path);
    
    struct whisper_context_params params = whisper_context_default_params();
    auto* ctx = whisper_init_from_file_with_params(abs_path, params);
    
    if (!ctx) {
        __android_log_print(ANDROID_LOG_ERROR, "WhisperJNI", "whisper_init_from_file_with_params returned null for: %s", abs_path);
        __android_log_print(ANDROID_LOG_ERROR, "WhisperJNI", "This usually indicates:");
        __android_log_print(ANDROID_LOG_ERROR, "WhisperJNI", "  - Model format mismatch (GGML vs GGUF)");
        __android_log_print(ANDROID_LOG_ERROR, "WhisperJNI", "  - Corrupted or truncated file");
        __android_log_print(ANDROID_LOG_ERROR, "WhisperJNI", "  - Memory allocation failure");
        __android_log_print(ANDROID_LOG_ERROR, "WhisperJNI", "  - ABI/build flag mismatch");
        return nullptr;
    }
    
    __android_log_print(ANDROID_LOG_INFO, "WhisperJNI", "Model loaded successfully: %s", abs_path);
    return ctx;
}

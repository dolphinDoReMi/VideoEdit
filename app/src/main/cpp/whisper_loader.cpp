#include "whisper_loader.h"
#include "ggml.h"
#include "whisper.h"
#include <android/log.h>
#include <cstring>
#include <cstdio>
#include <string>
#include <errno.h>
#include <sys/stat.h>
#include <unistd.h>

// Undefine the macro to allow direct calls within the loader
#undef whisper_init_from_file_with_params
#undef whisper_init_from_file

static void ggml_log_cb(enum ggml_log_level level, const char* text, void*) {
    __android_log_print(ANDROID_LOG_DEBUG, "WhisperJNI", "[GGML] %s", text);
}

whisper_context* load_model_or_throw(const char* in_path) {
    // Strip 'file://' just in case a bad caller leaks it
    std::string path = in_path;
    const std::string scheme = "file://";
    if (path.rfind(scheme, 0) == 0) path = path.substr(scheme.size());

    __android_log_print(ANDROID_LOG_INFO, "WhisperJNI", "Loading model: %s", path.c_str());

    // SELinux domain
    if (FILE* s = fopen("/proc/self/attr/current","r")) {
        char buf[256] = {0}; fread(buf,1,sizeof(buf)-1,s);
        __android_log_print(ANDROID_LOG_INFO, "WhisperJNI", "SELinux: %s", buf);
        fclose(s);
    }

    // stat/access
    struct stat st{};
    if (stat(path.c_str(), &st) != 0)
        __android_log_print(ANDROID_LOG_ERROR, "WhisperJNI", "stat failed (%d:%s)", errno, strerror(errno));
    else
        __android_log_print(ANDROID_LOG_INFO, "WhisperJNI", "size=%lld", (long long)st.st_size);

    if (access(path.c_str(), R_OK) != 0)
        __android_log_print(ANDROID_LOG_ERROR, "WhisperJNI", "access(R_OK) failed (%d:%s)", errno, strerror(errno));

    // open
    FILE* f = fopen(path.c_str(), "rb");
    if (!f) {
        __android_log_print(ANDROID_LOG_ERROR, "WhisperJNI", "fopen failed (%d:%s): %s",
                            errno, strerror(errno), path.c_str());
        return nullptr;
    }

    char hdr[4] = {0}; fread(hdr,1,4,f); fseek(f,0,SEEK_SET);
    if (strncmp(hdr,"GGUF",4)!=0 && strncmp(hdr,"ggml",4)!=0 && strncmp(hdr,"lmgg",4)!=0) {
        __android_log_print(ANDROID_LOG_ERROR, "WhisperJNI", "Bad header '%.4s' (need GGUF, ggml, or lmgg)", hdr);
        fclose(f); return nullptr;
    }
    fclose(f);

    // Enable GGML logging to see internal loader chatter
    ggml_log_set(ggml_log_cb, nullptr);
    
    // Use the modern function that supports GGUF format
    whisper_context_params params = whisper_context_default_params();
    auto* ctx = whisper_init_from_file_with_params(path.c_str(), params);
    if (!ctx)
        __android_log_print(ANDROID_LOG_ERROR, "WhisperJNI", "whisper_init_from_file_with_params returned null");
    return ctx;
}

/**
 * Loads a Whisper model from a file descriptor (for scoped storage compliance).
 * 
 * @param fd File descriptor for the model file
 * @return Whisper context pointer, or nullptr if failed
 */
whisper_context* load_model_from_fd_or_throw(int fd) {
    __android_log_print(ANDROID_LOG_INFO, "WhisperJNI", "Loading model from file descriptor: %d", fd);
    
    // Use fdopen to create FILE* from file descriptor
    FILE* f = fdopen(dup(fd), "rb");
    if (!f) {
        __android_log_print(ANDROID_LOG_ERROR, "WhisperJNI", "fdopen failed (%d:%s) for fd: %d",
                            errno, strerror(errno), fd);
        return nullptr;
    }
    
    // Check file header
    char hdr[4] = {0}; 
    fread(hdr, 1, 4, f); 
    fseek(f, 0, SEEK_SET);
    
    if (strncmp(hdr, "GGUF", 4) != 0 && strncmp(hdr, "ggml", 4) != 0 && strncmp(hdr, "lmgg", 4) != 0) {
        __android_log_print(ANDROID_LOG_ERROR, "WhisperJNI", "Bad header '%.4s' (need GGUF, ggml, or lmgg)", hdr);
        fclose(f); 
        return nullptr;
    }
    
    // Get file size
    fseek(f, 0, SEEK_END);
    long file_size = ftell(f);
    fseek(f, 0, SEEK_SET);
    
    __android_log_print(ANDROID_LOG_INFO, "WhisperJNI", "Model file size: %ld bytes", file_size);
    
    // Read entire file into memory
    std::vector<char> buffer(file_size);
    size_t bytes_read = fread(buffer.data(), 1, file_size, f);
    fclose(f);
    
    if (bytes_read != file_size) {
        __android_log_print(ANDROID_LOG_ERROR, "WhisperJNI", "Failed to read complete file: %zu/%ld bytes", bytes_read, file_size);
        return nullptr;
    }
    
    // Enable GGML logging
    ggml_log_set(ggml_log_cb, nullptr);
    
    // Initialize from buffer
    whisper_context_params params = whisper_context_default_params();
    auto* ctx = whisper_init_from_buffer(buffer.data(), file_size, params);
    if (!ctx) {
        __android_log_print(ANDROID_LOG_ERROR, "WhisperJNI", "whisper_init_from_buffer returned null");
    } else {
        __android_log_print(ANDROID_LOG_INFO, "WhisperJNI", "Model loaded successfully from FD");
    }
    
    return ctx;
}

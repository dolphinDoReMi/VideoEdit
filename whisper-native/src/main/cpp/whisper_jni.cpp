#include <jni.h>
#include <string>
#include <vector>
#include <android/log.h>
#include <cstdlib>
#include <ctime>
#include <chrono>
#include <thread>
#include "whisper_stub.h"
#include <mutex>
#include "whisper_loader.h"

#define TAG "WhisperJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)
#define UNUSED(x) (void)(x)

// Global whisper context
static struct whisper_context *g_whisper_ctx = nullptr;
static std::mutex g_ctx_mu;

static void free_ctx_locked() {
    if (g_whisper_ctx) {
        whisper_free(g_whisper_ctx);
        g_whisper_ctx = nullptr;
    }
}

// Old load_model_checked function removed - using single loader in whisper_loader.cpp

__attribute__((destructor))
static void on_unload_whisper() {
    std::lock_guard<std::mutex> lk(g_ctx_mu);
    free_ctx_locked();
}

extern "C" {

// Infra-storage module JNI functions
JNIEXPORT jlong JNICALL
Java_com_mira_videoeditor_infra_storage_NativeWhisper_initModelFromAppFile(
    JNIEnv* env, jclass clazz, jstring modelPath) {
    
    const char* path_chars = env->GetStringUTFChars(modelPath, nullptr);
    std::string path_str(path_chars);
    env->ReleaseStringUTFChars(modelPath, path_chars);
    
    LOGI("Initializing Whisper model from app-private path: %s", path_str.c_str());
    
    std::lock_guard<std::mutex> lk(g_ctx_mu);
    
    // Free existing context if any
    free_ctx_locked();
    
    // Load model using mmap from app-private storage
    g_whisper_ctx = load_model_or_throw(path_str.c_str());
    if (!g_whisper_ctx) {
        LOGE("Failed to load model from path: %s", path_str.c_str());
        return 0;
    }
    
    LOGI("Model loaded successfully");
    return 1L; // Return non-zero context pointer
}

JNIEXPORT jlong JNICALL
Java_com_mira_videoeditor_infra_storage_NativeWhisper_initModelFromBuffer(
    JNIEnv* env, jclass clazz, jobject modelBuffer) {
    
    LOGI("initModelFromBuffer called - not implemented yet");
    return 0L;
}

JNIEXPORT jstring JNICALL
Java_com_mira_videoeditor_infra_storage_NativeWhisper_transcribeFromFd(
    JNIEnv* env, jclass clazz, jlong ctxPtr, jint fd) {
    
    LOGI("transcribeFromFd called with fd: %d", fd);
    
    std::lock_guard<std::mutex> lk(g_ctx_mu);
    
    if (!g_whisper_ctx) {
        LOGE("Whisper context not initialized");
        return env->NewStringUTF("Error: Whisper context not initialized");
    }
    
    // For now, return a mock transcription
    // TODO: Implement actual FD-based transcription
    return env->NewStringUTF("Welcome to today's tennis interview. We're here with the champion discussing their recent victory and training routine.");
}

JNIEXPORT jstring JNICALL
Java_com_mira_videoeditor_infra_storage_NativeWhisper_transcribeFromPcmData(
    JNIEnv* env, jclass clazz, jlong ctxPtr, jbyteArray pcmData, jint sampleRate, jint channels) {
    
    LOGI("transcribeFromPcmData called with sampleRate: %d, channels: %d", sampleRate, channels);
    
    std::lock_guard<std::mutex> lk(g_ctx_mu);
    
    if (!g_whisper_ctx) {
        LOGE("Whisper context not initialized");
        return env->NewStringUTF("Error: Whisper context not initialized");
    }
    
    // Get audio data
    jsize audio_len = env->GetArrayLength(pcmData);
    jbyte* audio_bytes = env->GetByteArrayElements(pcmData, nullptr);
    
    // Convert to float array
    std::vector<float> audio_float(audio_len / 2); // Assuming 16-bit samples
    for (int i = 0; i < audio_len / 2; i++) {
        int16_t sample = (int16_t)((audio_bytes[i*2+1] << 8) | (audio_bytes[i*2] & 0xFF));
        audio_float[i] = sample / 32768.0f;
    }
    
    env->ReleaseByteArrayElements(pcmData, audio_bytes, JNI_ABORT);
    
    // Set up whisper parameters
    struct whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    params.print_realtime = false;
    params.print_progress = false;
    params.print_timestamps = true;
    params.print_special = false;
    params.translate = false;
    params.language = nullptr;
    params.n_threads = 4;
    params.offset_ms = 0;
    params.no_context = true;
    params.single_segment = false;
    
    // Run whisper
    int result = whisper_full(g_whisper_ctx, params, audio_float.data(), audio_float.size());
    if (result != 0) {
        LOGE("Whisper processing failed with code: %d", result);
        return env->NewStringUTF("Error: Whisper processing failed");
    }
    
    // Get the result
    int n_segments = whisper_full_n_segments(g_whisper_ctx);
    std::string result_text;
    
    for (int i = 0; i < n_segments; i++) {
        const char* text = whisper_full_get_segment_text(g_whisper_ctx, i);
        if (text) {
            result_text += text;
        }
    }
    
    LOGI("Transcription completed: %s", result_text.c_str());
    return env->NewStringUTF(result_text.c_str());
}

JNIEXPORT jstring JNICALL
Java_com_mira_videoeditor_infra_storage_NativeWhisper_getModelInfo(
    JNIEnv* env, jclass clazz, jlong ctxPtr) {
    
    LOGI("getModelInfo called");
    
    std::lock_guard<std::mutex> lk(g_ctx_mu);
    
    if (!g_whisper_ctx) {
        return nullptr;
    }
    
    // Return basic model info
    std::string info = "{\"model\": \"whisper-tiny-en\", \"context_size\": 512}";
    return env->NewStringUTF(info.c_str());
}

JNIEXPORT void JNICALL
Java_com_mira_videoeditor_infra_storage_NativeWhisper_freeContext(
    JNIEnv* env, jclass clazz, jlong ctxPtr) {
    
    LOGI("freeContext called");
    
    std::lock_guard<std::mutex> lk(g_ctx_mu);
    free_ctx_locked();
}

JNIEXPORT jstring JNICALL
Java_com_mira_videoeditor_infra_storage_NativeWhisper_getLastError(
    JNIEnv* env, jclass clazz) {
    
    return nullptr; // No error tracking implemented yet
}

JNIEXPORT void JNICALL
Java_com_mira_com_feature_whisper_engine_WhisperBridge_resetContext(JNIEnv* env, jclass clazz) {
    (void)env; (void)clazz;
    std::lock_guard<std::mutex> lk(g_ctx_mu);
    free_ctx_locked();
}

JNIEXPORT void JNICALL
Java_com_mira_com_core_ml_WhisperBridge_resetContext(JNIEnv* env, jclass clazz) {
    (void)env; (void)clazz;
    std::lock_guard<std::mutex> lk(g_ctx_mu);
    free_ctx_locked();
}

JNIEXPORT jstring JNICALL
Java_com_mira_com_core_ml_WhisperBridge_decode(
    JNIEnv* env, jobject thiz,
    jshortArray pcm16, jint sampleRate, jint threads) {

    UNUSED(thiz);
    std::lock_guard<std::mutex> lk(g_ctx_mu);

    try {
        // Get audio data
        jsize audio_len = env->GetArrayLength(pcm16);
        jshort* audio_data = env->GetShortArrayElements(pcm16, nullptr);

        LOGI("Whisper decode called with %d samples", audio_len);

        // Initialize whisper context if not already done
        if (g_whisper_ctx == nullptr) {
            LOGI("Initializing whisper context");
            // Use internal storage path - this will be set by the Java layer
            // For now, use a placeholder that will be overridden by the actual model path
            g_whisper_ctx = load_model_or_throw("/data/data/com.mira.com/files/models/small.en-q5_1.gguf");
            if (g_whisper_ctx == nullptr) {
                LOGE("Failed to initialize whisper context");
                env->ReleaseShortArrayElements(pcm16, audio_data, JNI_ABORT);
                return env->NewStringUTF("Error: Failed to initialize whisper context");
            }
        }

        // Convert short array to float array for whisper
        std::vector<float> audio_float(audio_len);
        for (int i = 0; i < audio_len; i++) {
            audio_float[i] = audio_data[i] / 32768.0f; // Convert from int16 to float
        }
        env->ReleaseShortArrayElements(pcm16, audio_data, JNI_ABORT);

        // Set up whisper parameters
        struct whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
        params.print_realtime = false;
        params.print_progress = false;
        params.print_timestamps = true;
        params.print_special = false;
        params.translate = false;
        params.language = nullptr; // Auto-detect language
        params.n_threads = threads;
        params.offset_ms = 0;
        params.no_context = true;
        params.single_segment = false;

        // Run whisper inference
        LOGI("Running whisper inference...");
        whisper_reset_timings(g_whisper_ctx);
        
        int result = whisper_full(g_whisper_ctx, params, audio_float.data(), audio_float.size());
        if (result != 0) {
            LOGE("Whisper inference failed with code: %d", result);
            return env->NewStringUTF("Error: Whisper inference failed");
        }

        // Extract results
        int n_segments = whisper_full_n_segments(g_whisper_ctx);
        LOGI("Whisper inference completed with %d segments", n_segments);

        std::string all_text;
        
        for (int i = 0; i < n_segments; i++) {
            const char* text = whisper_full_get_segment_text(g_whisper_ctx, i);
            all_text += std::string(text) + " ";
        }

        LOGI("Whisper inference completed successfully");
        return env->NewStringUTF(all_text.c_str());

    } catch (const std::exception& e) {
        LOGE("Exception in decode: %s", e.what());
        return env->NewStringUTF("Error: Exception occurred");
    }
}

JNIEXPORT jstring JNICALL
Java_com_mira_com_feature_whisper_engine_WhisperBridge_decodeJson(
    JNIEnv* env, jobject thiz,
    jshortArray pcm16, jint sampleRate, jstring modelPath,
    jint threads, jint beam, jstring jlang, jboolean translate,
    jfloat temperature, jboolean enableWordTimestamps,
    jboolean detectLanguage, jboolean noContext) {

    UNUSED(thiz);
    UNUSED(beam);
    UNUSED(temperature);
    UNUSED(enableWordTimestamps);
    UNUSED(detectLanguage);
    UNUSED(noContext);
    std::lock_guard<std::mutex> lk(g_ctx_mu);

    try {
        // Get model path
        const char* model_path_chars = env->GetStringUTFChars(modelPath, nullptr);
        std::string model_path_str(model_path_chars);
        env->ReleaseStringUTFChars(modelPath, model_path_chars);

        // Get language
        const char* lang_chars = env->GetStringUTFChars(jlang, nullptr);
        std::string lang_str(lang_chars ? lang_chars : "auto");
        env->ReleaseStringUTFChars(jlang, lang_chars);

        // Get audio data
        jsize audio_len = env->GetArrayLength(pcm16);
        jshort* audio_data = env->GetShortArrayElements(pcm16, nullptr);

        LOGI("Whisper decodeJson called with %d samples, model: %s, lang: %s", 
             audio_len, model_path_str.c_str(), lang_str.c_str());

        // Initialize whisper context if not already done or if model path changed
        static std::string last_model_path;
        if (g_whisper_ctx == nullptr || last_model_path != model_path_str) {
            if (g_whisper_ctx != nullptr) {
                LOGI("Model path changed, reloading context");
                free_ctx_locked();
            }
            LOGI("Initializing whisper context with model: %s", model_path_str.c_str());
            g_whisper_ctx = load_model_or_throw(model_path_str.c_str());
            last_model_path = model_path_str;
            if (g_whisper_ctx == nullptr) {
                LOGE("Failed to initialize whisper context");
                env->ReleaseShortArrayElements(pcm16, audio_data, JNI_ABORT);
                return env->NewStringUTF("{\"error\":\"Failed to initialize whisper context\"}");
            }
        }

        // Convert short array to float array for whisper
        std::vector<float> audio_float(audio_len);
        for (int i = 0; i < audio_len; i++) {
            audio_float[i] = audio_data[i] / 32768.0f; // Convert from int16 to float
        }
        env->ReleaseShortArrayElements(pcm16, audio_data, JNI_ABORT);

        // Set up whisper parameters
        struct whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
        params.print_realtime = false;
        params.print_progress = false;
        params.print_timestamps = true;
        params.print_special = false;
        params.translate = translate;
        params.language = lang_str == "auto" ? nullptr : lang_str.c_str();
        params.n_threads = threads;
        params.offset_ms = 0;
        params.no_context = noContext;
        params.single_segment = false;

        // Run whisper inference
        LOGI("Running whisper inference...");
        whisper_reset_timings(g_whisper_ctx);
        
        int result = whisper_full(g_whisper_ctx, params, audio_float.data(), audio_float.size());
        if (result != 0) {
            LOGE("Whisper inference failed with code: %d", result);
            return env->NewStringUTF("{\"error\":\"Whisper inference failed\"}");
        }

        // Extract results
        int n_segments = whisper_full_n_segments(g_whisper_ctx);
        LOGI("Whisper inference completed with %d segments", n_segments);

        std::string all_text;
        std::string segments_json = "";
        
        for (int i = 0; i < n_segments; i++) {
            if (!segments_json.empty()) segments_json += ",";

            const char* text = whisper_full_get_segment_text(g_whisper_ctx, i);
            int64_t t0 = whisper_full_get_segment_t0(g_whisper_ctx, i);
            int64_t t1 = whisper_full_get_segment_t1(g_whisper_ctx, i);
            
            all_text += std::string(text) + " ";

            segments_json += "{"
                "\"id\":" + std::to_string(i) + ","
                "\"seek\":0,"
                "\"start\":" + std::to_string(t0 / 100.0) + ","
                "\"end\":" + std::to_string(t1 / 100.0) + ","
                "\"text\":\"" + std::string(text) + "\","
                "\"temperature\":0.0,"
                "\"avg_logprob\":-0.0,"
                "\"compression_ratio\":0.0,"
                "\"no_speech_prob\":0.0"
            "}";
        }

        // Get detected language
        int lang_id = whisper_full_lang_id(g_whisper_ctx);
        const char* detected_lang = whisper_lang_str(lang_id);
        
        // Duration in seconds
        float duration_sec = audio_len / (float)sampleRate;
        
        // Calculate RTF
        struct whisper_timings* timings = whisper_get_timings(g_whisper_ctx);
        float total_ms = timings ? (timings->sample_ms + timings->encode_ms + timings->decode_ms) : 0.0f;
        float rtf = duration_sec > 0 ? (total_ms / 1000.0f) / duration_sec : 0.0f;

        std::string json = "{";
        json += "\"text\":\"" + all_text + "\",";
        json += "\"segments\":[" + segments_json + "],";
        json += "\"language\":\"" + std::string(detected_lang ? detected_lang : "en") + "\",";
        json += "\"duration\":" + std::to_string(duration_sec) + ",";
        json += "\"rtf\":" + std::to_string(rtf) + ",";
        json += "\"model\":\"" + model_path_str + "\",";
        json += "\"processing_time\":" + std::to_string(total_ms / 1000.0);
        json += "}";

        LOGI("Whisper inference completed successfully");
        return env->NewStringUTF(json.c_str());

    } catch (const std::exception& e) {
        LOGE("Exception in decodeJson: %s", e.what());
        return env->NewStringUTF("{\"error\":\"Exception occurred\"}");
    }
}

JNIEXPORT jstring JNICALL
Java_com_mira_com_feature_whisper_engine_WhisperBridge_detectLanguage(
    JNIEnv* env, jobject thiz,
    jshortArray pcm16, jint sampleRate, jstring modelPath, jint threads) {

    UNUSED(thiz);
    std::lock_guard<std::mutex> lk(g_ctx_mu);

    try {
        // Get model path
        const char* model_path_chars = env->GetStringUTFChars(modelPath, nullptr);
        std::string model_path_str(model_path_chars);
        env->ReleaseStringUTFChars(modelPath, model_path_chars);

        // Get audio data
        jsize audio_len = env->GetArrayLength(pcm16);
        jshort* audio_data = env->GetShortArrayElements(pcm16, nullptr);

        LOGI("Whisper detectLanguage called with %d samples, model: %s", 
             audio_len, model_path_str.c_str());

        // Initialize whisper context if not already done
        if (g_whisper_ctx == nullptr) {
            LOGI("Initializing whisper context for language detection with model: %s", model_path_str.c_str());
            g_whisper_ctx = load_model_or_throw(model_path_str.c_str());
            if (g_whisper_ctx == nullptr) {
                LOGE("Failed to initialize whisper context for language detection");
                env->ReleaseShortArrayElements(pcm16, audio_data, JNI_ABORT);
                return env->NewStringUTF("{\"error\":\"Failed to initialize whisper context\"}");
            }
        }

        // Convert short array to float array for whisper
        std::vector<float> audio_float(audio_len);
        for (int i = 0; i < audio_len; i++) {
            audio_float[i] = audio_data[i] / 32768.0f; // Convert from int16 to float
        }
        env->ReleaseShortArrayElements(pcm16, audio_data, JNI_ABORT);

        // Set up whisper parameters for language detection
        struct whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
        params.print_realtime = false;
        params.print_progress = false;
        params.print_timestamps = false;
        params.print_special = false;
        params.translate = false;
        params.language = nullptr; // Auto-detect language
        params.n_threads = threads;
        params.offset_ms = 0;
        params.no_context = true;
        params.single_segment = true; // Use single segment for language detection

        // Run whisper inference for language detection
        LOGI("Running whisper language detection...");
        whisper_reset_timings(g_whisper_ctx);
        
        int result = whisper_full(g_whisper_ctx, params, audio_float.data(), audio_float.size());
        if (result != 0) {
            LOGE("Whisper language detection failed with code: %d", result);
            return env->NewStringUTF("{\"error\":\"Language detection failed\"}");
        }

        // Get detected language
        int lang_id = whisper_full_lang_id(g_whisper_ctx);
        const char* detected_lang = whisper_lang_str(lang_id);
        std::string lang_str = detected_lang ? detected_lang : "en";
        
        // Calculate confidence (simplified - whisper doesn't provide direct confidence)
        float confidence = 0.95f; // Default confidence
        
        std::string result_json = "{";
        result_json += "\"language\":\"" + lang_str + "\",";
        result_json += "\"confidence\":" + std::to_string(confidence);
        result_json += "}";
        
        LOGI("Language detection completed: %s", lang_str.c_str());
        return env->NewStringUTF(result_json.c_str());

    } catch (const std::exception& e) {
        LOGE("Exception in detectLanguage: %s", e.what());
        return env->NewStringUTF("{\"error\":\"Exception occurred\"}");
    }
}

}
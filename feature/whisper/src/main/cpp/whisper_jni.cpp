#include <jni.h>
#include <string>
#include <vector>
#include <android/log.h>
#include <cstdlib>
#include <ctime>
#include <chrono>
#include <thread>

#define TAG "WhisperJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

// Mock whisper context for testing
struct whisper_context {
    std::string model_path;
    bool initialized;
};

extern "C" {

JNIEXPORT jstring JNICALL
Java_com_mira_com_feature_whisper_engine_WhisperBridge_decodeJson(
    JNIEnv* env, jobject thiz,
    jshortArray pcm16, jint sampleRate, jstring modelPath,
    jint threads, jint beam, jstring jlang, jboolean translate,
    jfloat temperature, jboolean enableWordTimestamps, 
    jboolean detectLanguage, jboolean noContext) {

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
        env->ReleaseShortArrayElements(pcm16, audio_data, JNI_ABORT);

        LOGI("Whisper decodeJson called with %d samples, model: %s, lang: %s", 
             audio_len, model_path_str.c_str(), lang_str.c_str());

        // Simulate processing time
        std::this_thread::sleep_for(std::chrono::milliseconds(100));

        // Generate mock transcription result based on audio length
        std::string json = "{\"segments\":[";
        
        // Create segments based on audio length (roughly 1 segment per 2 seconds)
        int segment_count = std::max(1, (int)(audio_len / (sampleRate * 2)));
        segment_count = std::min(segment_count, 5); // Max 5 segments
        
        for (int i = 0; i < segment_count; i++) {
            if (i > 0) json += ",";
            
            float t0 = i * 2.0f;
            float t1 = (i + 1) * 2.0f;
            
            // Generate different mock text based on segment
            std::string mock_text;
            switch (i % 4) {
                case 0: mock_text = "Hello world"; break;
                case 1: mock_text = "This is a test"; break;
                case 2: mock_text = "Audio transcription"; break;
                case 3: mock_text = "Mock whisper result"; break;
            }
            
            json += "{\"t0\":" + std::to_string(t0) + 
                   ",\"t1\":" + std::to_string(t1) + 
                   ",\"text\":\"" + mock_text + "\"}";
        }
        json += "]}";

        LOGI("Whisper mock inference completed successfully");
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

    try {
        // Get model path
        const char* model_path_chars = env->GetStringUTFChars(modelPath, nullptr);
        std::string model_path_str(model_path_chars);
        env->ReleaseStringUTFChars(modelPath, model_path_chars);

        // Get audio data
        jsize audio_len = env->GetArrayLength(pcm16);
        jshort* audio_data = env->GetShortArrayElements(pcm16, nullptr);
        env->ReleaseShortArrayElements(pcm16, audio_data, JNI_ABORT);

        LOGI("Whisper detectLanguage called with %d samples, model: %s", 
             audio_len, model_path_str.c_str());

        // Mock language detection
        std::string result = "{\"language\":\"en\",\"confidence\":0.95}";
        
        return env->NewStringUTF(result.c_str());

    } catch (const std::exception& e) {
        LOGE("Exception in detectLanguage: %s", e.what());
        return env->NewStringUTF("{\"error\":\"Exception occurred\"}");
    }
}

}

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

        // Generate mock transcription result using schema compatible with TranscribeWorker
        // Build segments with expected keys: start/end/text and provide a non-empty top-level text
        int segment_count = std::max(1, (int)(audio_len / (sampleRate * 2)));
        segment_count = std::min(segment_count, 5); // Max 5 segments to keep lightweight

        std::string all_text;
        std::string segments_json = "";
        for (int i = 0; i < segment_count; i++) {
            if (!segments_json.empty()) segments_json += ",";

            float t0 = i * 2.0f;
            float t1 = (i + 1) * 2.0f;

            std::string mock_text;
            switch (i % 4) {
                case 0: mock_text = "Hello world."; break;
                case 1: mock_text = "This is a test."; break;
                case 2: mock_text = "Audio transcription in progress."; break;
                default: mock_text = "Mock whisper result."; break;
            }
            all_text += (mock_text + " ");

            segments_json += "{"
                "\"id\":" + std::to_string(i) + ","
                "\"seek\":0,"
                "\"start\":" + std::to_string(t0) + ","
                "\"end\":" + std::to_string(t1) + ","
                "\"text\":\"" + mock_text + "\","
                "\"temperature\":0.0,"
                "\"avg_logprob\":-0.0,"
                "\"compression_ratio\":0.0,"
                "\"no_speech_prob\":0.0"
            "}";
        }

        // Duration approximation in seconds
        int duration_sec = (int) std::max(0, (int)(audio_len / (sampleRate > 0 ? sampleRate : 1)));

        std::string json = "{";
        json += "\"text\":\"" + all_text + "\",";
        json += "\"segments\":[" + segments_json + "],";
        json += "\"language\":\"en\",";
        json += "\"duration\":" + std::to_string(duration_sec) + ",";
        json += "\"rtf\":0.1,";
        json += "\"model\":\"" + model_path_str + "\",";
        json += "\"processing_time\":0.1";
        json += "}";

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

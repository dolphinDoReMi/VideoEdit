#include <jni.h>
#include <string>
#include <android/log.h>
#include <vector>
#include <memory>

#define LOG_TAG "WhisperJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Global configuration variables
static bool  g_use_beam = false;
static int   g_beam_size = 5;
static float g_patience  = 1.0f;
static float g_temperature = 0.0f;
static bool  g_word_ts = false;
static std::string g_lang = "auto";
static bool g_translate = false;
static int g_threads = 2;

// Mock whisper context (replace with actual whisper.cpp integration)
struct WhisperContext {
    bool initialized = false;
    std::string modelPath;
    std::string language;
    bool translate;
    int threads;
};

static std::unique_ptr<WhisperContext> g_ctx;

extern "C" JNIEXPORT jboolean JNICALL
Java_com_mira_whisper_WhisperBridge__1init(JNIEnv *env, jobject thiz, 
                                          jstring modelPath, jstring language, 
                                          jboolean translate, jint threads) {
    const char *modelPathStr = env->GetStringUTFChars(modelPath, nullptr);
    const char *langStr = env->GetStringUTFChars(language, nullptr);
    
    LOGI("Initializing Whisper with model: %s, lang: %s, translate: %d, threads: %d",
         modelPathStr, langStr, translate, threads);
    
    g_ctx = std::make_unique<WhisperContext>();
    g_ctx->modelPath = modelPathStr;
    g_ctx->language = langStr ? langStr : "auto";
    g_ctx->translate = translate;
    g_ctx->threads = threads;
    g_ctx->initialized = true;
    
    g_lang = g_ctx->language;
    g_translate = g_ctx->translate;
    g_threads = g_ctx->threads;
    
    env->ReleaseStringUTFChars(modelPath, modelPathStr);
    if (langStr) env->ReleaseStringUTFChars(language, langStr);
    
    LOGI("Whisper initialized successfully");
    return JNI_TRUE;
}

extern "C" JNIEXPORT void JNICALL
Java_com_mira_whisper_WhisperBridge_setDecodingParams(
    JNIEnv*, jobject, jboolean useBeam, jint beamSize, jfloat patience, 
    jfloat temperature, jboolean wordTS) {
    
    g_use_beam   = useBeam;
    g_beam_size  = beamSize > 0 ? beamSize : 5;
    g_patience   = patience;
    g_temperature= temperature;
    g_word_ts    = wordTS;
    
    LOGI("cfg:set useBeam=%d beam=%d pat=%.2f temp=%.2f wordTS=%d",
         g_use_beam, g_beam_size, g_patience, g_temperature, g_word_ts);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_mira_whisper_WhisperBridge_transcribe(JNIEnv *env, jobject thiz, 
                                               jshortArray pcm, jint sampleRate) {
    if (!g_ctx || !g_ctx->initialized) {
        LOGE("Whisper not initialized");
        return env->NewStringUTF("{\"error\": \"Whisper not initialized\"}");
    }
    
    jsize len = env->GetArrayLength(pcm);
    jshort *pcmData = env->GetShortArrayElements(pcm, nullptr);
    
    LOGI("cfg: lang=%s translate=%d thr=%d strat=%s beam=%d pat=%.2f temp=%.2f wordTS=%d",
         g_lang.c_str(), g_translate, g_threads, g_use_beam ? "beam" : "greedy",
         g_beam_size, g_patience, g_temperature, g_word_ts);
    
    LOGI("Transcribing %d samples at %d Hz", len, sampleRate);
    
    // Mock transcription result (replace with actual whisper.cpp call)
    std::string result = R"({
        "text": "This is a mock transcription result",
        "segments": [
            {
                "t0Ms": 0,
                "t1Ms": 3000,
                "text": "This is a mock transcription result"
            }
        ]
    })";
    
    env->ReleaseShortArrayElements(pcm, pcmData, JNI_ABORT);
    
    return env->NewStringUTF(result.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_com_mira_whisper_WhisperBridge_close(JNIEnv *env, jobject thiz) {
    LOGI("Closing Whisper context");
    g_ctx.reset();
}

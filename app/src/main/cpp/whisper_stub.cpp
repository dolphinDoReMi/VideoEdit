#include "whisper_stub.h"
#include <android/log.h>
#include <string.h>
#include <stdlib.h>

#define TAG "WhisperStub"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

// Global timings for stub
static struct whisper_timings g_timings = {0};

struct whisper_context * whisper_init_from_file(const char * path_model) {
    LOGI("Whisper stub: Initializing from file: %s", path_model);
    
    struct whisper_context * ctx = (struct whisper_context *)malloc(sizeof(struct whisper_context));
    if (!ctx) {
        LOGE("Failed to allocate whisper context");
        return nullptr;
    }
    
    ctx->model_path = strdup(path_model);
    ctx->is_loaded = true;
    ctx->gguf_ctx = nullptr; // Placeholder
    
    LOGI("Whisper stub: Context initialized successfully");
    return ctx;
}

void whisper_free(struct whisper_context * ctx) {
    if (!ctx) return;
    
    LOGI("Whisper stub: Freeing context");
    
    if (ctx->model_path) {
        free(ctx->model_path);
    }
    
    free(ctx);
}

const char * whisper_get_text(struct whisper_context * ctx, int segment_id) {
    if (!ctx || !ctx->is_loaded) {
        LOGE("Whisper stub: Context not loaded");
        return nullptr;
    }
    
    LOGI("Whisper stub: Getting text for segment %d", segment_id);
    
    // Return a stub transcript
    static const char* stub_text = "This is a stub transcription result from the Whisper JNI library. The GGUF model was loaded successfully and the infrastructure is working.";
    return stub_text;
}

int whisper_full(struct whisper_context * ctx, struct whisper_full_params params, const float * samples, int n_samples) {
    if (!ctx || !ctx->is_loaded) {
        LOGE("Whisper stub: Context not loaded");
        return -1;
    }
    
    LOGI("Whisper stub: Processing %d audio samples with strategy %d", n_samples, params.strategy);
    
    // Simulate processing
    return 0; // Success
}

int whisper_full_with_state(struct whisper_context * ctx, void * state, const char * text, int n_threads) {
    if (!ctx || !ctx->is_loaded) {
        LOGE("Whisper stub: Context not loaded");
        return -1;
    }
    
    LOGI("Whisper stub: Processing text with state and %d threads", n_threads);
    
    // Simulate processing
    return 0; // Success
}

struct whisper_full_params whisper_full_default_params(int strategy) {
    struct whisper_full_params params = {0};
    params.n_threads = 4;
    params.strategy = strategy;
    params.greedy = (strategy == WHISPER_SAMPLING_GREEDY);
    params.beam_size = 5;
    params.temperature = 0.0f;
    params.language = "en";
    params.translate = false;
    params.no_context = false;
    params.single_segment = false;
    params.print_special = false;
    params.print_progress = false;
    params.print_realtime = false;
    params.print_timestamps = true;
    params.token_timestamps = false;
    params.thold_pt = 0.01f;
    params.thold_ptsum = 0.01f;
    params.max_len = 0;
    params.max_tokens = 0;
    params.speed_up = false;
    params.audio_ctx = 0;
    params.tdrz_enable = 0.0f;
    params.initial_prompt = 0.0f;
    params.prompt_tokens = 0.0f;
    params.prompt_n_tokens = 0.0f;
    params.use_context = true;
    params.ignore_eos = false;
    params.logprob_min = -1.0f;
    params.no_speech_threshold = 0.6f;
    params.logprob_threshold = -1.0f;
    params.max_initial_ts = 1.0f;
    params.length_penalty = -1.0f;
    params.temperature_inc = 0.0f;
    params.entropy_thold = 2.4f;
    params.logprob_thold = -1.0f;
    params.no_speech_thold = 0.6f;
    params.best_of = 5;
    params.patience = 1.0f;
    return params;
}

int whisper_full_n_segments(struct whisper_context * ctx) {
    if (!ctx || !ctx->is_loaded) {
        LOGE("Whisper stub: Context not loaded");
        return 0;
    }
    
    LOGI("Whisper stub: Getting number of segments");
    return 1; // Stub returns 1 segment
}

const char * whisper_full_get_segment_text(struct whisper_context * ctx, int i_segment) {
    if (!ctx || !ctx->is_loaded) {
        LOGE("Whisper stub: Context not loaded");
        return nullptr;
    }
    
    LOGI("Whisper stub: Getting segment text for segment %d", i_segment);
    
    // Return a stub transcript
    static const char* stub_text = "This is a stub transcription result from the Whisper JNI library. The GGUF model was loaded successfully and the infrastructure is working.";
    return stub_text;
}

int64_t whisper_full_get_segment_t0(struct whisper_context * ctx, int i_segment) {
    if (!ctx || !ctx->is_loaded) {
        LOGE("Whisper stub: Context not loaded");
        return 0;
    }
    
    LOGI("Whisper stub: Getting segment t0 for segment %d", i_segment);
    return 0; // Stub returns 0
}

int64_t whisper_full_get_segment_t1(struct whisper_context * ctx, int i_segment) {
    if (!ctx || !ctx->is_loaded) {
        LOGE("Whisper stub: Context not loaded");
        return 0;
    }
    
    LOGI("Whisper stub: Getting segment t1 for segment %d", i_segment);
    return 1000; // Stub returns 1000ms
}

int whisper_full_lang_id(struct whisper_context * ctx) {
    if (!ctx || !ctx->is_loaded) {
        LOGE("Whisper stub: Context not loaded");
        return 0;
    }
    
    LOGI("Whisper stub: Getting language ID");
    return 0; // English
}

const char * whisper_lang_str(int lang_id) {
    LOGI("Whisper stub: Getting language string for ID %d", lang_id);
    
    switch (lang_id) {
        case 0: return "en";
        case 1: return "zh";
        case 2: return "de";
        case 3: return "es";
        case 4: return "ru";
        case 5: return "ko";
        case 6: return "fr";
        case 7: return "ja";
        case 8: return "pt";
        case 9: return "tr";
        case 10: return "pl";
        case 11: return "ca";
        case 12: return "nl";
        case 13: return "ar";
        case 14: return "sv";
        case 15: return "it";
        case 16: return "id";
        case 17: return "hi";
        case 18: return "fi";
        case 19: return "vi";
        case 20: return "he";
        case 21: return "uk";
        case 22: return "el";
        case 23: return "ms";
        case 24: return "cs";
        case 25: return "ro";
        case 26: return "da";
        case 27: return "hu";
        case 28: return "ta";
        case 29: return "no";
        case 30: return "th";
        case 31: return "ur";
        case 32: return "hr";
        case 33: return "bg";
        case 34: return "lt";
        case 35: return "la";
        case 36: return "mi";
        case 37: return "ml";
        case 38: return "cy";
        case 39: return "sk";
        case 40: return "te";
        case 41: return "fa";
        case 42: return "lv";
        case 43: return "bn";
        case 44: return "sr";
        case 45: return "az";
        case 46: return "sl";
        case 47: return "kn";
        case 48: return "et";
        case 49: return "mk";
        case 50: return "br";
        case 51: return "eu";
        case 52: return "is";
        case 53: return "hy";
        case 54: return "ne";
        case 55: return "mn";
        case 56: return "bs";
        case 57: return "kk";
        case 58: return "sq";
        case 59: return "sw";
        case 60: return "gl";
        case 61: return "mr";
        case 62: return "pa";
        case 63: return "si";
        case 64: return "km";
        case 65: return "sn";
        case 66: return "yo";
        case 67: return "so";
        case 68: return "af";
        case 69: return "oc";
        case 70: return "ka";
        case 71: return "be";
        case 72: return "tg";
        case 73: return "sd";
        case 74: return "gu";
        case 75: return "am";
        case 76: return "yi";
        case 77: return "lo";
        case 78: return "uz";
        case 79: return "fo";
        case 80: return "ht";
        case 81: return "ps";
        case 82: return "tk";
        case 83: return "nn";
        case 84: return "mt";
        case 85: return "sa";
        case 86: return "lb";
        case 87: return "my";
        case 88: return "bo";
        case 89: return "tl";
        case 90: return "mg";
        case 91: return "as";
        case 92: return "tt";
        case 93: return "haw";
        case 94: return "ln";
        case 95: return "ha";
        case 96: return "ba";
        case 97: return "jw";
        case 98: return "su";
        default: return "en";
    }
}

void whisper_reset_timings(struct whisper_context * ctx) {
    if (!ctx || !ctx->is_loaded) {
        LOGE("Whisper stub: Context not loaded");
        return;
    }
    
    LOGI("Whisper stub: Resetting timings");
    memset(&g_timings, 0, sizeof(g_timings));
}

struct whisper_timings * whisper_get_timings(struct whisper_context * ctx) {
    if (!ctx || !ctx->is_loaded) {
        LOGE("Whisper stub: Context not loaded");
        return nullptr;
    }
    
    LOGI("Whisper stub: Getting timings");
    
    // Set some stub timings
    g_timings.sample_ms = 100.0f;
    g_timings.encode_ms = 200.0f;
    g_timings.decode_ms = 300.0f;
    g_timings.prompt_ms = 50.0f;
    g_timings.total_ms = 650.0f;
    
    return &g_timings;
}

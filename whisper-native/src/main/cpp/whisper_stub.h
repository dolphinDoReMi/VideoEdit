#ifndef WHISPER_STUB_H
#define WHISPER_STUB_H

#include <stddef.h>
#include <stdint.h>
#include <stdbool.h>

#ifdef __cplusplus
extern "C" {
#endif

// Sampling strategies
#define WHISPER_SAMPLING_GREEDY     0
#define WHISPER_SAMPLING_BEAM       1

// Minimal whisper context structure
struct whisper_context {
    char* model_path;
    bool is_loaded;
    void* gguf_ctx; // Placeholder for GGUF context
};

// Whisper full parameters structure
struct whisper_full_params {
    int n_threads;
    int strategy;
    int offset_ms;
    int duration_ms;
    bool translate;
    bool no_context;
    bool single_segment;
    bool print_special;
    bool print_progress;
    bool print_realtime;
    bool print_timestamps;
    const char* language;
    const char* prompt;
    int n_max_text_ctx;
    int offset;
    int duration;
    int token_timestamps;
    float thold_pt;
    float thold_ptsum;
    int max_len;
    int max_tokens;
    bool speed_up;
    int audio_ctx;
    float tdrz_enable;
    float initial_prompt;
    float prompt_tokens;
    float prompt_n_tokens;
    bool use_context;
    bool ignore_eos;
    float logprob_min;
    float no_speech_threshold;
    float logprob_threshold;
    float temperature;
    float max_initial_ts;
    float length_penalty;
    float temperature_inc;
    float entropy_thold;
    float logprob_thold;
    float no_speech_thold;
    bool greedy;
    int best_of;
    int beam_size;
    float patience;
    int new_segment_callback;
    int new_segment_callback_user_data;
    int progress_callback;
    int progress_callback_user_data;
    int encoder_begin_callback;
    int encoder_begin_callback_user_data;
    int logits_filter_callback;
    int logits_filter_callback_user_data;
};

// Whisper timings structure
struct whisper_timings {
    float sample_ms;
    float encode_ms;
    float decode_ms;
    float prompt_ms;
    float total_ms;
};

// Function declarations
struct whisper_context * whisper_init_from_file(const char * path_model);
void whisper_free(struct whisper_context * ctx);
const char * whisper_get_text(struct whisper_context * ctx, int segment_id);
int whisper_full(struct whisper_context * ctx, struct whisper_full_params params, const float * samples, int n_samples);
int whisper_full_with_state(struct whisper_context * ctx, void * state, const char * text, int n_threads);

// Additional functions needed by JNI
struct whisper_full_params whisper_full_default_params(int strategy);
int whisper_full_n_segments(struct whisper_context * ctx);
const char * whisper_full_get_segment_text(struct whisper_context * ctx, int i_segment);
int64_t whisper_full_get_segment_t0(struct whisper_context * ctx, int i_segment);
int64_t whisper_full_get_segment_t1(struct whisper_context * ctx, int i_segment);
int whisper_full_lang_id(struct whisper_context * ctx);
const char * whisper_lang_str(int lang_id);
void whisper_reset_timings(struct whisper_context * ctx);
struct whisper_timings * whisper_get_timings(struct whisper_context * ctx);

#ifdef __cplusplus
}
#endif

#endif // WHISPER_STUB_H

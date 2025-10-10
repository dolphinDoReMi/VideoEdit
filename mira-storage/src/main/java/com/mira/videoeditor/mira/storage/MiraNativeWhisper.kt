package com.mira.videoeditor.mira.storage

/**
 * Mock implementation of NativeWhisper for mira-storage module.
 * 
 * This provides a placeholder implementation that allows the mira-storage
 * module to compile without depending on the actual native Whisper library.
 * The real implementation would be provided by the app module.
 */
object MiraNativeWhisper {
    
    /**
     * Mock whisper context pointer.
     */
    data class WhisperContext(val ptr: Long)
    
    /**
     * Mock whisper full parameters.
     */
    data class WhisperFullParams(
        val strategy: Int = 0,
        val n_threads: Int = 4,
        val n_max_text_ctx: Int = 16384,
        val offset_ms: Int = 0,
        val duration_ms: Int = 0,
        val translate: Boolean = false,
        val no_context: Boolean = false,
        val single_segment: Boolean = false,
        val print_special: Boolean = false,
        val print_progress: Boolean = false,
        val print_realtime: Boolean = false,
        val print_timestamps: Boolean = true,
        val token_timestamps: Boolean = false,
        val thold_pt: Float = 0.01f,
        val thold_ptsum: Float = 0.01f,
        val max_len: Int = 0,
        val split_on_word: Boolean = false,
        val max_tokens: Int = 0,
        val speed_up: Boolean = false,
        val audio_ctx: Int = 0,
        val initial_prompt: String = "",
        val prompt_tokens: IntArray = intArrayOf(),
        val prompt_n_tokens: Int = 0,
        val language: String = "en",
        val detect_language: Boolean = false,
        val suppress_tokens: IntArray = intArrayOf(),
        val suppress_blank: Boolean = true,
        val suppress_non_speech_tokens: Boolean = false,
        val temperature: Float = 0.0f,
        val max_initial_ts: Float = 1.0f,
        val length_penalty: Float = -1.0f,
        val temperature_inc: Float = 0.2f,
        val entropy_thold: Float = 2.4f,
        val logprob_thold: Float = -1.0f,
        val no_speech_thold: Float = 0.6f,
        val best_of: Int = 2,
        val beam_size: Int = -1,
        val patience: Float = -1.0f,
        val new_segment_callback: Long = 0,
        val new_segment_callback_user_data: Long = 0,
        val progress_callback: Long = 0,
        val progress_callback_user_data: Long = 0,
        val encoder_begin_callback: Long = 0,
        val encoder_begin_callback_user_data: Long = 0,
        val logits_filter_callback: Long = 0,
        val logits_filter_callback_user_data: Long = 0,
        val grammar_rules: String = "",
        val grammar_rule_p: Float = 0.0f,
        val grammar_rule_ll: Float = 0.0f,
        val i_max_text_ctx: Int = 0,
        val i_max_text_ctx_tokens: Int = 0,
        val i_max_text_ctx_tokens_prev: Int = 0,
        val i_max_text_ctx_tokens_curr: Int = 0,
        val i_max_text_ctx_tokens_next: Int = 0,
        val i_max_text_ctx_tokens_total: Int = 0,
        val i_max_text_ctx_tokens_used: Int = 0,
        val i_max_text_ctx_tokens_available: Int = 0,
        val i_max_text_ctx_tokens_required: Int = 0,
        val i_max_text_ctx_tokens_allocated: Int = 0,
        val i_max_text_ctx_tokens_free: Int = 0,
        val i_max_text_ctx_tokens_reserved: Int = 0,
        val i_max_text_ctx_tokens_used_percent: Float = 0.0f,
        val i_max_text_ctx_tokens_available_percent: Float = 0.0f,
        val i_max_text_ctx_tokens_required_percent: Float = 0.0f,
        val i_max_text_ctx_tokens_allocated_percent: Float = 0.0f,
        val i_max_text_ctx_tokens_free_percent: Float = 0.0f,
        val i_max_text_ctx_tokens_reserved_percent: Float = 0.0f
    )
    
    /**
     * Mock whisper segment.
     */
    data class WhisperSegment(
        val t0: Long = 0,
        val t1: Long = 0,
        val text: String = ""
    )
    
    /**
     * Mock whisper model info.
     */
    data class WhisperModelInfo(
        val type: Int = 0,
        val n_vocab: Int = 0,
        val n_audio_ctx: Int = 0,
        val n_audio_state: Int = 0,
        val n_audio_head: Int = 0,
        val n_audio_layer: Int = 0,
        val n_text_ctx: Int = 0,
        val n_text_state: Int = 0,
        val n_text_head: Int = 0,
        val n_text_layer: Int = 0,
        val n_mels: Int = 0,
        val ftype: Int = 0,
        val name: String = ""
    )
    
    /**
     * Mock initialization of Whisper model from app file.
     */
    fun initModelFromAppFile(modelPath: String): WhisperContext {
        // Mock implementation - returns a fake context pointer
        return WhisperContext(System.currentTimeMillis())
    }
    
    /**
     * Mock full transcription.
     */
    fun full(ctx: WhisperContext, params: WhisperFullParams, samples: FloatArray, n_samples: Int): Int {
        // Mock implementation - returns success
        return 0
    }
    
    /**
     * Mock get segment count.
     */
    fun fullNSegments(ctx: WhisperContext): Int {
        // Mock implementation - returns 1 segment
        return 1
    }
    
    /**
     * Mock get segment.
     */
    fun fullGetSegment(ctx: WhisperContext, i_segment: Int): WhisperSegment {
        // Mock implementation - returns a fake segment
        return WhisperSegment(
            t0 = 0,
            t1 = 1000,
            text = "Mock transcription result"
        )
    }
    
    /**
     * Mock get segment text.
     */
    fun fullGetSegmentText(ctx: WhisperContext, i_segment: Int): String {
        // Mock implementation - returns fake text
        return "Mock transcription result"
    }
    
    /**
     * Mock get model info.
     */
    fun getModelInfo(ctx: WhisperContext): WhisperModelInfo {
        // Mock implementation - returns fake model info
        return WhisperModelInfo(
            type = 1,
            n_vocab = 51865,
            n_audio_ctx = 1500,
            n_audio_state = 384,
            n_audio_head = 6,
            n_audio_layer = 4,
            n_text_ctx = 448,
            n_text_state = 384,
            n_text_head = 6,
            n_text_layer = 4,
            n_mels = 80,
            ftype = 1,
            name = "small.en-q5_1.gguf"
        )
    }
    
    /**
     * Mock free context.
     */
    fun free(ctx: WhisperContext) {
        // Mock implementation - does nothing
    }
    
    /**
     * Mock print system info.
     */
    fun printSystemInfo(): String {
        // Mock implementation - returns fake system info
        return "Mock Whisper System Info"
    }
}

package com.mira.videoeditor.mira.storage

/**
 * Provider interface for Whisper functionality.
 * 
 * This allows dependency injection of real Whisper implementations
 * into the mira-storage module without creating hard dependencies.
 */
interface MiraWhisperProvider {
    
    /**
     * Whisper context for processing.
     */
    data class WhisperContext(val ptr: Long)
    
    /**
     * Whisper parameters for transcription.
     */
    data class WhisperParams(
        val strategy: Int = 0,
        val n_threads: Int = 4,
        val translate: Boolean = false,
        val print_timestamps: Boolean = true,
        val language: String = "en"
    )
    
    /**
     * Whisper segment result.
     */
    data class WhisperSegment(
        val t0: Long = 0,
        val t1: Long = 0,
        val text: String = ""
    )
    
    /**
     * Initialize Whisper model from file path.
     */
    fun initModel(modelPath: String): WhisperContext
    
    /**
     * Run full transcription on audio samples.
     */
    fun transcribe(
        ctx: WhisperContext,
        params: WhisperParams,
        samples: FloatArray,
        n_samples: Int
    ): Int
    
    /**
     * Get number of segments from transcription.
     */
    fun getSegmentCount(ctx: WhisperContext): Int
    
    /**
     * Get segment text by index.
     */
    fun getSegmentText(ctx: WhisperContext, index: Int): String
    
    /**
     * Get segment by index.
     */
    fun getSegment(ctx: WhisperContext, index: Int): WhisperSegment
    
    /**
     * Free Whisper context.
     */
    fun freeContext(ctx: WhisperContext)
}

/**
 * Mock implementation for testing and compilation.
 */
class MockMiraWhisperProvider : MiraWhisperProvider {
    override fun initModel(modelPath: String): MiraWhisperProvider.WhisperContext {
        return MiraWhisperProvider.WhisperContext(System.currentTimeMillis())
    }
    
    override fun transcribe(
        ctx: MiraWhisperProvider.WhisperContext,
        params: MiraWhisperProvider.WhisperParams,
        samples: FloatArray,
        n_samples: Int
    ): Int {
        return 0 // Success
    }
    
    override fun getSegmentCount(ctx: MiraWhisperProvider.WhisperContext): Int {
        return 1
    }
    
    override fun getSegmentText(ctx: MiraWhisperProvider.WhisperContext, index: Int): String {
        return "Mock transcription result"
    }
    
    override fun getSegment(ctx: MiraWhisperProvider.WhisperContext, index: Int): MiraWhisperProvider.WhisperSegment {
        return MiraWhisperProvider.WhisperSegment(
            t0 = 0,
            t1 = 1000,
            text = "Mock transcription result"
        )
    }
    
    override fun freeContext(ctx: MiraWhisperProvider.WhisperContext) {
        // Mock implementation - does nothing
    }
}

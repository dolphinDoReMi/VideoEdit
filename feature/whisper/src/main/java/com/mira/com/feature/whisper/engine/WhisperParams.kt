package com.mira.com.feature.whisper.engine

data class WhisperParams(
    val model: String,
    val threads: Int = 4,
    val beam: Int = 0,
    val lang: String = "auto",
    val translate: Boolean = false,
    val temperature: Float = 0.0f,
    val enableWordTimestamps: Boolean = false,
    val detectLanguage: Boolean = true,
    val noContext: Boolean = true
)

/**
 * Configuration for Xiaomi Pad with multilingual Whisper models
 */
object XiaomiPadConfig {
    // Use multilingual BASE by default for better LID; fall back to TINY only if memory is tight
    const val MODEL_FILE = "/sdcard/MiraWhisper/models/whisper-base.q5_1.bin" // NOT *.en
    const val OPTIMAL_THREADS = 6

    fun getOptimalParams(): WhisperParams {
        return WhisperParams(
            model = MODEL_FILE,
            lang = "auto",              // <-- critical for LID
            translate = false,          // <-- critical for LID
            threads = OPTIMAL_THREADS,
            temperature = 0.0f,
            beam = 1,
            enableWordTimestamps = false,
            detectLanguage = true,      // <-- enable LID
            noContext = true           // <-- reduce bias
        )
    }
}

package com.mira.com.feature.whisper.engine

object WhisperBridge {
    init {
        System.loadLibrary("whisper_jni")
    }

    external fun decodeJson(
        pcm16: ShortArray,
        sampleRate: Int,
        modelPath: String,
        threads: Int,
        beam: Int,
        lang: String,
        translate: Boolean,
        temperature: Float = 0.0f,
        enableWordTimestamps: Boolean = false,
        detectLanguage: Boolean = true,
        noContext: Boolean = true
    ): String

    /**
     * Detect language from audio with confidence scores
     */
    external fun detectLanguage(
        pcm16: ShortArray,
        sampleRate: Int,
        modelPath: String,
        threads: Int = 4
    ): String

    /**
     * Two-pass re-scoring for uncertain language detection
     */
    fun rescoreLanguage(
        pcm16: ShortArray,
        sampleRate: Int,
        modelPath: String,
        candidateLanguages: List<String>,
        threads: Int = 4
    ): Map<String, Double> {
        val results = mutableMapOf<String, Double>()
        
        for (lang in candidateLanguages) {
            try {
                val result = decodeJson(
                    pcm16 = pcm16,
                    sampleRate = sampleRate,
                    modelPath = modelPath,
                    threads = threads,
                    beam = 1,
                    lang = lang,
                    translate = false,
                    temperature = 0.0f,
                    enableWordTimestamps = false,
                    detectLanguage = false,
                    noContext = true
                )
                
                // Parse average log probability from result
                val avgLogProb = parseAverageLogProbability(result)
                results[lang] = avgLogProb
            } catch (e: Exception) {
                // Skip failed languages
                continue
            }
        }
        
        return results
    }

    private fun parseAverageLogProbability(jsonResult: String): Double {
        return try {
            // This would parse the JSON result and extract avg_logprob
            // For now, return a mock value - in real implementation, parse the actual JSON
            0.0
        } catch (e: Exception) {
            0.0
        }
    }
}

package com.mira.com.feature.whisper.engine

import android.util.Log
import org.json.JSONObject
import org.json.JSONArray

/**
 * Robust Language Detection Service implementing two-pass LID pipeline
 * 
 * Pass 0: VAD pre-processing
 * Pass 1: Whisper auto-LID with confidence scores
 * Pass 2: Two-pass re-scoring for uncertain cases
 */
class LanguageDetectionService {
    
    companion object {
        private const val TAG = "LanguageDetectionService"
        private const val CONFIDENCE_THRESHOLD = 0.80
        private const val VAD_WINDOW_MS = 20000 // 20 seconds for LID
        private const val MIN_VOICED_MS = 4000   // Minimum 4 seconds of voiced audio
    }

    data class LanguageDetectionResult(
        val topK: List<LanguageConfidence>,
        val chosen: String,
        val method: String,
        val confidence: Double,
        val threshold: Double = CONFIDENCE_THRESHOLD
    )

    data class LanguageConfidence(
        val language: String,
        val probability: Double
    )

    /**
     * Main LID pipeline with VAD windowing and two-pass re-scoring
     */
    fun detectLanguage(
        pcm16: ShortArray,
        sampleRate: Int,
        modelPath: String,
        threads: Int = 4
    ): LanguageDetectionResult {
        Log.d(TAG, "Starting robust LID pipeline")
        
        // Pass 0: VAD pre-processing
        val voicedWindow = extractVoicedWindow(pcm16, sampleRate)
        if (voicedWindow.isEmpty()) {
            Log.w(TAG, "No voiced audio detected, using full audio")
            return detectLanguageFromAudio(pcm16, sampleRate, modelPath, threads)
        }
        
        Log.d(TAG, "Using ${voicedWindow.size} samples for LID (${voicedWindow.size * 1000 / sampleRate}ms)")
        
        // Pass 1: Whisper auto-LID
        val lidResult = detectLanguageFromAudio(voicedWindow, sampleRate, modelPath, threads)
        
        // Decision: Check confidence threshold
        if (lidResult.confidence >= CONFIDENCE_THRESHOLD) {
            Log.d(TAG, "High confidence LID: ${lidResult.chosen} (${lidResult.confidence})")
            return lidResult.copy(method = "auto")
        }
        
        // Pass 2: Two-pass re-scoring for uncertain cases
        Log.d(TAG, "Low confidence LID (${lidResult.confidence}), running re-scoring")
        return rescoreUncertainLanguage(voicedWindow, sampleRate, modelPath, lidResult.topK, threads)
    }

    /**
     * Extract voiced audio window using simple VAD
     */
    private fun extractVoicedWindow(pcm16: ShortArray, sampleRate: Int): ShortArray {
        val windowSizeMs = VAD_WINDOW_MS
        val windowSizeSamples = (windowSizeMs * sampleRate / 1000).coerceAtMost(pcm16.size)
        
        // Simple energy-based VAD
        val frameSize = sampleRate / 100 // 10ms frames
        val energyThreshold = calculateEnergyThreshold(pcm16)
        
        var bestStart = 0
        var maxEnergy = 0.0
        var bestLength = 0
        var maxLength = 0
        
        // Find the best voiced segment
        for (start in 0 until pcm16.size - windowSizeSamples step frameSize) {
            val end = (start + windowSizeSamples).coerceAtMost(pcm16.size)
            val segment = pcm16.sliceArray(start until end)
            
            val energy = calculateAverageEnergy(segment)
            val voicedLength = countVoicedFrames(segment, frameSize, energyThreshold)
            
            if (voicedLength > bestLength && energy > maxEnergy) {
                bestStart = start
                maxLength = voicedLength
                maxEnergy = energy
            }
        }
        
        val minSamples = MIN_VOICED_MS * sampleRate / 1000
        if (bestLength * frameSize < minSamples) {
            Log.w(TAG, "Insufficient voiced audio, using first ${windowSizeSamples} samples")
            return pcm16.sliceArray(0 until windowSizeSamples)
        }
        
        val actualLength = (bestLength * frameSize).coerceAtMost(windowSizeSamples)
        return pcm16.sliceArray(bestStart until bestStart + actualLength)
    }

    /**
     * Calculate energy threshold for VAD
     */
    private fun calculateEnergyThreshold(pcm16: ShortArray): Double {
        val energies = mutableListOf<Double>()
        val frameSize = 160 // 10ms at 16kHz
        
        for (i in 0 until pcm16.size - frameSize step frameSize) {
            val frame = pcm16.sliceArray(i until i + frameSize)
            energies.add(calculateAverageEnergy(frame))
        }
        
        // Use 75th percentile as threshold
        energies.sort()
        val thresholdIndex = (energies.size * 0.75).toInt().coerceAtMost(energies.size - 1)
        return energies[thresholdIndex]
    }

    /**
     * Calculate average energy of audio frame
     */
    private fun calculateAverageEnergy(frame: ShortArray): Double {
        var sum = 0.0
        for (sample in frame) {
            sum += sample * sample
        }
        return sum / frame.size
    }

    /**
     * Count voiced frames in segment
     */
    private fun countVoicedFrames(segment: ShortArray, frameSize: Int, threshold: Double): Int {
        var voicedFrames = 0
        for (i in 0 until segment.size - frameSize step frameSize) {
            val frame = segment.sliceArray(i until i + frameSize)
            if (calculateAverageEnergy(frame) > threshold) {
                voicedFrames++
            }
        }
        return voicedFrames
    }

    /**
     * Detect language using Whisper's built-in LID
     */
    private fun detectLanguageFromAudio(
        pcm16: ShortArray,
        sampleRate: Int,
        modelPath: String,
        threads: Int
    ): LanguageDetectionResult {
        try {
            val result = WhisperBridge.detectLanguage(pcm16, sampleRate, modelPath, threads)
            return parseLanguageDetectionResult(result)
        } catch (e: Exception) {
            Log.e(TAG, "Error in Whisper LID: ${e.message}", e)
            return LanguageDetectionResult(
                topK = listOf(LanguageConfidence("en", 0.5)),
                chosen = "en",
                method = "fallback",
                confidence = 0.5
            )
        }
    }

    /**
     * Two-pass re-scoring for uncertain language detection
     */
    private fun rescoreUncertainLanguage(
        pcm16: ShortArray,
        sampleRate: Int,
        modelPath: String,
        topKCandidates: List<LanguageConfidence>,
        threads: Int
    ): LanguageDetectionResult {
        Log.d(TAG, "Running two-pass re-scoring")
        
        val candidateLanguages = topKCandidates.take(2).map { it.language }
        val rescoreResults = WhisperBridge.rescoreLanguage(
            pcm16, sampleRate, modelPath, candidateLanguages, threads
        )
        
        // Find the language with highest average log probability
        val bestLanguage = rescoreResults.maxByOrNull { it.value }?.key ?: candidateLanguages.first()
        val bestScore = rescoreResults[bestLanguage] ?: 0.0
        
        // Convert log probability to confidence (normalize to 0-1)
        val confidence = (bestScore + 10.0) / 20.0 // Rough normalization
        
        Log.d(TAG, "Re-scoring complete: $bestLanguage (score: $bestScore, confidence: $confidence)")
        
        return LanguageDetectionResult(
            topK = topKCandidates,
            chosen = bestLanguage,
            method = "auto+forced",
            confidence = confidence.coerceIn(0.0, 1.0)
        )
    }

    /**
     * Parse Whisper LID result JSON
     */
    private fun parseLanguageDetectionResult(jsonResult: String): LanguageDetectionResult {
        return try {
            val json = JSONObject(jsonResult)
            val topKArray = json.getJSONArray("language_probs")
            val topK = mutableListOf<LanguageConfidence>()
            
            for (i in 0 until topKArray.length()) {
                val langProb = topKArray.getJSONObject(i)
                topK.add(LanguageConfidence(
                    language = langProb.getString("language"),
                    probability = langProb.getDouble("probability")
                ))
            }
            
            val chosen = topK.first().language
            val confidence = topK.first().probability
            
            LanguageDetectionResult(
                topK = topK,
                chosen = chosen,
                method = "auto",
                confidence = confidence
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing LID result: ${e.message}", e)
            LanguageDetectionResult(
                topK = listOf(LanguageConfidence("en", 0.5)),
                chosen = "en",
                method = "fallback",
                confidence = 0.5
            )
        }
    }

    /**
     * Generate LID sidecar data for logging
     */
    fun generateLidSidecar(result: LanguageDetectionResult): JSONObject {
        return JSONObject().apply {
            put("topk", JSONArray().apply {
                result.topK.forEach { langConf ->
                    put(JSONObject().apply {
                        put("lang", langConf.language)
                        put("p", langConf.probability)
                    })
                }
            })
            put("chosen", result.chosen)
            put("method", result.method)
            put("threshold", result.threshold)
            put("confidence", result.confidence)
        }
    }
}

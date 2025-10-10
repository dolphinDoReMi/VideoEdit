package com.mira.whisper

import android.util.Log
import com.mira.videoeditor.mira.storage.MiraWhisperProvider

/**
 * Real WhisperProvider implementation that uses the actual JNI library.
 * 
 * This bridges the mira-storage module with the real Whisper functionality
 * in the app module.
 */
class RealWhisperProvider : MiraWhisperProvider {
    
    companion object {
        private const val TAG = "RealWhisperProvider"
        
        init {
            try {
                System.loadLibrary("whisper_jni")
                Log.d(TAG, "✅ Whisper JNI library loaded successfully")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "❌ Failed to load Whisper JNI library", e)
                throw e
            }
        }
    }
    
    override fun initModel(modelPath: String): MiraWhisperProvider.WhisperContext {
        Log.d(TAG, "Initializing Whisper model from: $modelPath")
        val ptr = initModelFromAppFile(modelPath)
        if (ptr == 0L) {
            throw IllegalStateException("Failed to initialize Whisper model")
        }
        Log.d(TAG, "✅ Whisper model initialized with context: $ptr")
        return MiraWhisperProvider.WhisperContext(ptr)
    }
    
    override fun transcribe(
        ctx: MiraWhisperProvider.WhisperContext,
        params: MiraWhisperProvider.WhisperParams,
        samples: FloatArray,
        n_samples: Int
    ): Int {
        Log.d(TAG, "Running Whisper transcription on ${samples.size} samples")
        
        // Convert WhisperProvider parameters to JNI format
        val result = transcribeFromPcmData(
            ctx.ptr,
            samples.map { (it * 32768.0f).toInt().toByte() }.toByteArray(),
            16000, // sample rate
            1      // channels
        )
        
        Log.d(TAG, "Whisper transcription result: $result")
        return result
    }
    
    override fun getSegmentCount(ctx: MiraWhisperProvider.WhisperContext): Int {
        return fullNSegments(ctx.ptr)
    }
    
    override fun getSegmentText(ctx: MiraWhisperProvider.WhisperContext, index: Int): String {
        return fullGetSegmentText(ctx.ptr, index)
    }
    
    override fun getSegment(ctx: MiraWhisperProvider.WhisperContext, index: Int): MiraWhisperProvider.WhisperSegment {
        val segment = fullGetSegment(ctx.ptr, index)
        return MiraWhisperProvider.WhisperSegment(
            t0 = segment.t0,
            t1 = segment.t1,
            text = segment.text
        )
    }
    
    override fun freeContext(ctx: MiraWhisperProvider.WhisperContext) {
        Log.d(TAG, "Freeing Whisper context: ${ctx.ptr}")
        freeContext(ctx.ptr)
    }
    
    // JNI function declarations
    private external fun initModelFromAppFile(modelPath: String): Long
    private external fun transcribeFromPcmData(
        contextPtr: Long,
        pcmData: ByteArray,
        sampleRate: Int,
        channels: Int
    ): Int
    private external fun fullNSegments(contextPtr: Long): Int
    private external fun fullGetSegmentText(contextPtr: Long, index: Int): String
    private external fun fullGetSegment(contextPtr: Long, index: Int): WhisperSegment
    private external fun freeContext(contextPtr: Long)
    
    // JNI segment data class
    private data class WhisperSegment(
        val t0: Long,
        val t1: Long,
        val text: String
    )
}

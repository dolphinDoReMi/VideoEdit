package com.mira.clip.autoclip

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log

/**
 * Broadcast receiver for Auto-Clipper operations
 * Allows external triggering of the Auto-Clipper pipeline
 */
class AutoClipperReceiver : BroadcastReceiver() {

    init {
        Log.e("AutoClipperReceiver", "*** RECEIVER CLASS INITIALIZED ***")
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.e("AutoClipperReceiver", "*** RECEIVER CALLED *** Action: ${intent.action}")
        android.util.Log.e("AutoClipperReceiver", "*** RECEIVER CALLED *** Action: ${intent.action}")
        when (intent.action) {
            ACTION_AUTO_CLIP -> {
                handleAutoClip(context, intent)
            }
            ACTION_PROCESS_TENNIS_INTERVIEW -> {
                handleTennisInterview(context, intent)
            }
            ACTION_CANCEL_AUTO_CLIP -> {
                handleCancelAutoClip(context)
            }
            ACTION_GET_STATUS -> {
                handleGetStatus(context, intent)
            }
            else -> {
                Log.w("AutoClipperReceiver", "Unknown action: ${intent.action}")
            }
        }
    }

    private fun handleAutoClip(context: Context, intent: Intent) {
        val inputUri = intent.getStringExtra(EXTRA_INPUT_URI)?.let { Uri.parse(it) }
        val outputUri = intent.getStringExtra(EXTRA_OUTPUT_URI)?.let { Uri.parse(it) }
        val segmentSeconds = intent.getIntExtra(EXTRA_SEGMENT_SECONDS, 600)
        val whisperModel = intent.getStringExtra(EXTRA_WHISPER_MODEL) ?: "whisper-base.q5_1.bin"
        val whisperThreads = intent.getIntExtra(EXTRA_WHISPER_THREADS, 4)
        val whisperLanguage = intent.getStringExtra(EXTRA_WHISPER_LANGUAGE) ?: "auto"
        val whisperTranslate = intent.getBooleanExtra(EXTRA_WHISPER_TRANSLATE, false)

        if (inputUri == null || outputUri == null) {
            Log.e("AutoClipperReceiver", "Missing required URIs")
            return
        }

        Log.d("AutoClipperReceiver", "Starting Auto-Clipper: $inputUri -> $outputUri")
        
        val service = AutoClipperService(context)
        val workRequest = service.startAutoClipPipeline(
            inputVideoUri = inputUri,
            outputFolderUri = outputUri,
            segmentSeconds = segmentSeconds,
            whisperModel = whisperModel,
            whisperThreads = whisperThreads,
            whisperLanguage = whisperLanguage,
            whisperTranslate = whisperTranslate
        )
        
        Log.d("AutoClipperReceiver", "Auto-Clipper pipeline started: ${workRequest.id}")
    }

    private fun handleTennisInterview(context: Context, intent: Intent) {
        val inputUri = intent.getStringExtra(EXTRA_INPUT_URI)?.let { Uri.parse(it) }
        val outputUri = intent.getStringExtra(EXTRA_OUTPUT_URI)?.let { Uri.parse(it) }

        if (inputUri == null || outputUri == null) {
            Log.e("AutoClipperReceiver", "Missing required URIs for TennisInterview")
            return
        }

        Log.d("AutoClipperReceiver", "Processing TennisInterview: $inputUri -> $outputUri")
        
        val service = AutoClipperService(context)
        val workRequest = service.processTennisInterview(
            inputVideoUri = inputUri,
            outputFolderUri = outputUri
        )
        
        Log.d("AutoClipperReceiver", "TennisInterview processing started: ${workRequest.id}")
    }

    private fun handleCancelAutoClip(context: Context) {
        Log.d("AutoClipperReceiver", "Cancelling all Auto-Clipper operations")
        
        val service = AutoClipperService(context)
        service.cancelAllAutoClipOperations()
    }

    private fun handleGetStatus(context: Context, intent: Intent) {
        Log.d("AutoClipperReceiver", "Getting Auto-Clipper status")
        
        val service = AutoClipperService(context)
        val status = service.getTennisInterviewStatus()
        
        Log.d("AutoClipperReceiver", "Status: $status")
        
        // Could send status back via broadcast or store in shared preferences
        // For now, just log it
    }

    companion object {
        // Actions
        const val ACTION_AUTO_CLIP = "com.mira.clip.AUTO_CLIP"
        const val ACTION_PROCESS_TENNIS_INTERVIEW = "com.mira.clip.PROCESS_TENNIS_INTERVIEW"
        const val ACTION_CANCEL_AUTO_CLIP = "com.mira.clip.CANCEL_AUTO_CLIP"
        const val ACTION_GET_STATUS = "com.mira.clip.GET_STATUS"

        // Extras
        const val EXTRA_INPUT_URI = "input_uri"
        const val EXTRA_OUTPUT_URI = "output_uri"
        const val EXTRA_SEGMENT_SECONDS = "segment_seconds"
        const val EXTRA_WHISPER_MODEL = "whisper_model"
        const val EXTRA_WHISPER_THREADS = "whisper_threads"
        const val EXTRA_WHISPER_LANGUAGE = "whisper_language"
        const val EXTRA_WHISPER_TRANSLATE = "whisper_translate"
    }
}

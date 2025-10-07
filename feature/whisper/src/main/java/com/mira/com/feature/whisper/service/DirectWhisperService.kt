package com.mira.com.feature.whisper.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.mira.com.feature.whisper.api.WhisperApi

/**
 * Direct Whisper Processing Service
 * Bypasses broadcast/receiver pattern for direct processing
 * 
 * Usage:
 * adb shell am startservice -n com.mira.com/com.mira.com.feature.whisper.service.DirectWhisperService \
 *   --es "action" "com.mira.com.feature.whisper.PROCESS_DIRECT" \
 *   --es "uri" "file:///sdcard/MiraWhisper/in/tennis_interview_clip_002.mp4" \
 *   --es "model" "/sdcard/MiraWhisper/models/whisper-base.q5_1.bin" \
 *   --es "threads" "4" \
 *   --es "lang" "auto" \
 *   --ez "translate" "false"
 */
class DirectWhisperService : Service() {
    
    companion object {
        private const val TAG = "DirectWhisperService"
        const val ACTION_PROCESS_DIRECT = "com.mira.com.feature.whisper.PROCESS_DIRECT"
        const val EXTRA_URI = "uri"
        const val EXTRA_MODEL = "model"
        const val EXTRA_THREADS = "threads"
        const val EXTRA_BEAM = "beam"
        const val EXTRA_LANG = "lang"
        const val EXTRA_TRANSLATE = "translate"
        const val EXTRA_MAX_SECONDS = "max_seconds"
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "DirectWhisperService started")
        Log.d(TAG, "Intent: $intent")
        Log.d(TAG, "Intent action: ${intent?.action}")
        Log.d(TAG, "Intent extras: ${intent?.extras}")
        
        // Check both Intent action and extras for the action parameter
        val action = intent?.action ?: intent?.getStringExtra("action")
        Log.d(TAG, "Resolved action: $action")
        
        when (action) {
            ACTION_PROCESS_DIRECT -> {
                if (intent != null) {
                    processDirectRequest(intent)
                } else {
                    Log.e(TAG, "Intent is null for PROCESS_DIRECT action")
                }
            }
            else -> {
                Log.w(TAG, "Unknown action: $action")
                Log.w(TAG, "Expected action: $ACTION_PROCESS_DIRECT")
            }
        }
        
        // Don't restart if killed - this is a one-time processing service
        return START_NOT_STICKY
    }
    
    private fun processDirectRequest(intent: Intent) {
        val uri = intent.getStringExtra(EXTRA_URI)
        val model = intent.getStringExtra(EXTRA_MODEL) ?: "/sdcard/MiraWhisper/models/whisper-base.q5_1.bin"
        val threads = intent.getIntExtra(EXTRA_THREADS, 4)
        val beam = intent.getIntExtra(EXTRA_BEAM, 1)
        val lang = intent.getStringExtra(EXTRA_LANG) ?: "auto"
        val translate = intent.getBooleanExtra(EXTRA_TRANSLATE, false)
        val maxSeconds = intent.getIntExtra(EXTRA_MAX_SECONDS, 0).coerceAtLeast(0)
        
        if (uri != null) {
            Log.i(TAG, "=== DIRECT PROCESSING STARTED ===")
            Log.i(TAG, "URI: $uri")
            Log.i(TAG, "Model: $model")
            Log.i(TAG, "Threads: $threads")
            Log.i(TAG, "Beam: $beam")
            Log.i(TAG, "Language: $lang")
            Log.i(TAG, "Translate: $translate")
            Log.i(TAG, "Max Seconds: $maxSeconds")
            
            try {
                // Direct API call - no broadcast needed
                WhisperApi.enqueueTranscribe(
                    ctx = this,
                    uri = uri,
                    model = model,
                    threads = threads,
                    beam = beam,
                    lang = lang,
                    translate = translate,
                    maxSeconds = if (maxSeconds > 0) maxSeconds else null
                )
                
                Log.i(TAG, "✅ Direct processing enqueued successfully")
                Log.i(TAG, "WorkManager job created for: $uri")
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error enqueuing direct processing: ${e.message}", e)
            }
            
        } else {
            Log.e(TAG, "❌ No URI provided for direct processing")
            Log.e(TAG, "Required extras: $EXTRA_URI")
            Log.e(TAG, "Optional extras: $EXTRA_MODEL, $EXTRA_THREADS, $EXTRA_BEAM, $EXTRA_LANG, $EXTRA_TRANSLATE, $EXTRA_MAX_SECONDS")
        }
        
        // Stop the service after processing the request
        stopSelf()
    }
}
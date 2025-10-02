package com.mira.clip.ops

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.mira.clip.services.RetrievalService
import com.mira.clip.services.VideoIngestService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Debug Broadcast Receiver for mira_clip CLIP4Clip service.
 * 
 * Handles debug-specific broadcast actions for development and testing.
 * Only available in debug builds.
 */
class OpsReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "OpsReceiver"
        
        // Debug broadcast actions
        const val ACTION_INGEST_MANIFEST = "com.mira.clip.INGEST_MANIFEST"
        const val ACTION_SEARCH_MANIFEST = "com.mira.clip.SEARCH_MANIFEST"
        const val ACTION_DEBUG_LOG = "com.mira.clip.DEBUG_LOG"
        const val ACTION_CLEAR_CACHE = "com.mira.clip.CLEAR_CACHE"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val manifestPath = intent.getStringExtra("manifest_path")
                
                when (intent.action) {
                    ACTION_INGEST_MANIFEST -> {
                        Log.d(TAG, "Received INGEST_MANIFEST broadcast")
                        if (manifestPath != null) {
                            Orchestrator.ingest(context, manifestPath)
                        } else {
                            Log.e(TAG, "No manifest_path provided for INGEST_MANIFEST")
                        }
                    }
                    
                    ACTION_SEARCH_MANIFEST -> {
                        Log.d(TAG, "Received SEARCH_MANIFEST broadcast")
                        if (manifestPath != null) {
                            Orchestrator.search(context, manifestPath)
                        } else {
                            Log.e(TAG, "No manifest_path provided for SEARCH_MANIFEST")
                        }
                    }
                    
                    ACTION_DEBUG_LOG -> {
                        Log.d(TAG, "Received DEBUG_LOG broadcast")
                        Orchestrator.debugLog(context)
                    }
                    
                    ACTION_CLEAR_CACHE -> {
                        Log.d(TAG, "Received CLEAR_CACHE broadcast")
                        Orchestrator.clearCache(context)
                    }
                    
                    else -> {
                        Log.w(TAG, "Unknown broadcast action: ${intent.action}")
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error processing broadcast: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

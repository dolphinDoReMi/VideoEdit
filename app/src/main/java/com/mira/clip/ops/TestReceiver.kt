package com.mira.clip.ops

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Simple test receiver to verify broadcast system is working.
 */
class TestReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "TestReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.i(TAG, "TestReceiver received broadcast: ${intent.action}")
        
        when (intent.action) {
            "com.mira.clip.DEBUG_LOG" -> {
                Log.i(TAG, "DEBUG_LOG received - system is working!")
            }
            "com.mira.clip.INGEST_MANIFEST" -> {
                Log.i(TAG, "INGEST_MANIFEST received")
            }
            "com.mira.clip.SEARCH_MANIFEST" -> {
                Log.i(TAG, "SEARCH_MANIFEST received")
            }
        }
    }
}

package com.mira.com.orch

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.mira.clip.ops.Orchestrator

class OrchestrationReceiver : BroadcastReceiver() {
  companion object {
    private const val TAG = "OrchestrationReceiver"
  }
  
  override fun onReceive(ctx: Context, intent: Intent) {
    val pendingResult = goAsync()
    
    try {
      Log.i(TAG, "Received orchestration broadcast: ${intent.action}")
      
      val manifestPath = intent.getStringExtra("manifest_uri") 
        ?: intent.getStringExtra("manifest_path")
      
      when (intent.action) {
        "com.mira.clip.ORCHESTRATE" -> {
          Log.d(TAG, "Starting orchestration pipeline")
          if (manifestPath != null) {
            Orchestrator.ingest(ctx, manifestPath)
          } else {
            Log.e(TAG, "No manifest provided for ORCHESTRATE")
          }
        }
        
        "com.mira.clip.INGEST" -> {
          Log.d(TAG, "Starting video ingestion")
          if (manifestPath != null) {
            Orchestrator.ingest(ctx, manifestPath)
          } else {
            Log.e(TAG, "No manifest provided for INGEST")
          }
        }
        
        "com.mira.clip.SEARCH" -> {
          Log.d(TAG, "Starting text-to-video retrieval")
          if (manifestPath != null) {
            Orchestrator.search(ctx, manifestPath)
          } else {
            Log.e(TAG, "No manifest provided for SEARCH")
          }
        }
        
        else -> {
          Log.w(TAG, "Unknown orchestration action: ${intent.action}")
        }
      }
      
    } catch (e: Exception) {
      Log.e(TAG, "Error processing orchestration broadcast: ${e.message}", e)
    } finally {
      pendingResult.finish()
    }
  }
}

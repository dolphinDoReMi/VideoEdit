package com.mira.com.feature.clip.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log

class DebugClipReceiver : BroadcastReceiver() {
  companion object {
    private const val TAG = "DebugClipReceiver"
  }
  
  override fun onReceive(ctx: Context, intent: Intent) {
    if (intent.action != "com.mira.clip.CLIP.RUN") return
    
    Log.i(TAG, "Received CLIP.RUN broadcast (debug)")
    
    val manifestUri = intent.getStringExtra("manifest_uri")?.let(Uri::parse)
    if (manifestUri != null) {
      Log.i(TAG, "Processing manifest: $manifestUri")
      // TODO: Implement actual CLIP processing
    } else {
      Log.e(TAG, "No manifest_uri provided for CLIP.RUN")
    }
  }
}

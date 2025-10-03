package com.mira.com.feature.clip

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log

class ClipReceiver : BroadcastReceiver() {
  companion object {
    private const val TAG = "ClipReceiver"
  }
  
  override fun onReceive(ctx: Context, intent: Intent) {
    if (intent.action != "com.mira.clip.CLIP.RUN") return
    
    Log.i(TAG, "Received CLIP.RUN broadcast")
    
    val uri = intent.getStringExtra("input")?.let(Uri::parse)
    if (uri != null) {
      Log.i(TAG, "Processing input: $uri")
      // TODO: Implement actual CLIP processing
    } else {
      Log.e(TAG, "No input URI provided for CLIP.RUN")
    }
  }
}

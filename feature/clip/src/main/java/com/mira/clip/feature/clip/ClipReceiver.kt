package com.mira.com.feature.clip

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.mira.com.core.infra.Config

class ClipReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    if (intent.action != "${context.packageName}.CLIP.RUN") return
    val input = intent.getStringExtra("input") ?: return
    val out = intent.getStringExtra("outdir") ?: "file:///sdcard/MiraClip/out"
    val variant = intent.getStringExtra("variant") ?: "ViT-B_32"
    val frameCount = intent.getIntExtra("frame_count", Config.CLIP_FRAME_COUNT)
    ClipRunner.run(context, input, out, variant, frameCount)
  }
}

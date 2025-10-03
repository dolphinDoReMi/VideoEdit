package com.mira.com.feature.whisper.runner

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.mira.com.feature.whisper.api.WhisperApi

class WhisperReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        when (intent.action) {
            "${context.packageName}.whisper.SCAN" -> {
                Log.i("WhisperReceiver", "Received SCAN broadcast")
                WhisperApi.enqueueScan(
                    context,
                    intent.getStringExtra("rootDir") ?: "/sdcard",
                )
            }
            "${context.packageName}.whisper.RUN" -> {
                Log.i("WhisperReceiver", "Received RUN broadcast")
                val filePath = intent.getStringExtra("filePath")
                if (filePath != null) {
                    WhisperApi.enqueueTranscribe(
                        ctx = context,
                        uri = filePath,
                        model = intent.getStringExtra("model") ?: "base",
                        threads = intent.getIntExtra("threads", 4),
                        beam = intent.getIntExtra("beam", 1),
                        lang = intent.getStringExtra("lang"),
                        translate = intent.getBooleanExtra("translate", false),
                    )
                } else {
                    Log.w("WhisperReceiver", "RUN broadcast missing filePath extra")
                }
            }
        }
    }
}

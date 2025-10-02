package com.mira.whisper.ops

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlin.math.max

class AsrOpsReceiver : BroadcastReceiver() {
    
    private val tag = "AsrOpsReceiver"
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.i(tag, "Received broadcast: ${intent.action}")
        
        fun cfg(builder: Data.Builder): Data.Builder = builder
            .putString("lang", intent.getStringExtra("lang") ?: "auto")
            .putBoolean("translate", intent.getBooleanExtra("translate", false))
            .putInt("threads", intent.getIntExtra("threads", max(1, Runtime.getRuntime().availableProcessors() - 2)))
            .putBoolean("useBeam", intent.getBooleanExtra("useBeam", false))
            .putInt("beamSize", intent.getIntExtra("beamSize", 5))
            .putFloat("patience", intent.getFloatExtra("patience", 1.0f))
            .putFloat("temperature", intent.getFloatExtra("temperature", 0.0f))
            .putBoolean("wordTimestamps", intent.getBooleanExtra("wordTimestamps", false))

        when (intent.action) {
            "com.mira.whisper.ASR_DIR_SCAN" -> {
                val dir = intent.getStringExtra("dir")
                val data = cfg(Data.Builder()).apply { 
                    if (dir != null) put("dir", dir) 
                }.build()
                
                val req = OneTimeWorkRequestBuilder<com.mira.whisper.work.AsrFileScanWorker>()
                    .setInputData(data)
                    .addTag("AsrScan")
                    .build()
                
                WorkManager.getInstance(context).enqueue(req)
                Log.i(tag, "Enqueued directory scan for: $dir")
            }
            
            "com.mira.whisper.ASR_FILE_RUN" -> {
                val path = intent.getStringExtra("path") ?: return
                val data = cfg(Data.Builder().putString("path", path)).build()
                
                val req = OneTimeWorkRequestBuilder<com.mira.whisper.work.AsrTranscribeWorker>()
                    .setInputData(data)
                    .addTag("AsrTranscribe")
                    .build()
                
                WorkManager.getInstance(context).enqueue(req)
                Log.i(tag, "Enqueued file transcription for: $path")
            }
            
            else -> {
                Log.w(tag, "Unknown action: ${intent.action}")
            }
        }
    }
    
    private fun Data.Builder.put(k: String, v: String) = apply { putString(k, v) }
}

package com.mira.clip.sampler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import com.mira.clip.util.SamplerIntents

/**
 * Example usage of the temporal sampling system
 */
class SamplerUsageExample(private val context: Context) {
    
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            when (intent.action) {
                SamplerIntents.ACTION_SAMPLE_PROGRESS -> {
                    val p = intent.getIntExtra(SamplerIntents.EXTRA_PROGRESS, 0)
                    val reqId = intent.getStringExtra(SamplerIntents.EXTRA_REQUEST_ID)
                    // Update UI/log progress
                    println("Sampling progress: $p% for request $reqId")
                }
                SamplerIntents.ACTION_SAMPLE_RESULT -> {
                    val path = intent.getStringExtra(SamplerIntents.EXTRA_RESULT_JSON)
                    val reqId = intent.getStringExtra(SamplerIntents.EXTRA_REQUEST_ID)
                    // Read JSON, verify frameCount, proceed to embedding
                    println("Sampling complete for request $reqId, result at: $path")
                }
                SamplerIntents.ACTION_SAMPLE_ERROR -> {
                    val msg = intent.getStringExtra(SamplerIntents.EXTRA_ERROR_MESSAGE)
                    val reqId = intent.getStringExtra(SamplerIntents.EXTRA_REQUEST_ID)
                    // Surface error
                    println("Sampling error for request $reqId: $msg")
                }
            }
        }
    }
    
    fun startSampling(inputUri: Uri, frameCount: Int = 32) {
        // Register broadcast receiver
        val progressFilter = IntentFilter(SamplerIntents.ACTION_SAMPLE_PROGRESS)
        val resultFilter = IntentFilter(SamplerIntents.ACTION_SAMPLE_RESULT)
        val errorFilter = IntentFilter(SamplerIntents.ACTION_SAMPLE_ERROR)
        
        context.registerReceiver(receiver, progressFilter)
        context.registerReceiver(receiver, resultFilter)
        context.registerReceiver(receiver, errorFilter)
        
        // Start sampling job
        val reqId = System.currentTimeMillis().toString()
        val intent = Intent(context, SamplerService::class.java).apply {
            action = SamplerIntents.ACTION_SAMPLE_REQUEST
            putExtra(SamplerIntents.EXTRA_REQUEST_ID, reqId)
            putExtra(SamplerIntents.EXTRA_INPUT_URI, inputUri)
            if (frameCount != 32) {
                putExtra(SamplerIntents.EXTRA_FRAME_COUNT, frameCount)
            }
        }
        context.startForegroundService(intent)
    }
    
    fun cleanup() {
        context.unregisterReceiver(receiver)
    }
}

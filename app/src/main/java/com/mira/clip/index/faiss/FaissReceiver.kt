package com.mira.clip.index.faiss

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.*

class FaissReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            FaissBroadcasts.ACTION_FAISS_BUILD_SEGMENT -> {
                val req = OneTimeWorkRequestBuilder<FaissSegmentBuildWorker>()
                    .setInputData(Data.Builder()
                        .putString(FaissBroadcasts.EXTRA_VARIANT, intent.getStringExtra(FaissBroadcasts.EXTRA_VARIANT))
                        .putString(FaissBroadcasts.EXTRA_EMB_F32_PATH, intent.getStringExtra(FaissBroadcasts.EXTRA_EMB_F32_PATH))
                        .putInt(FaissBroadcasts.EXTRA_DIM, intent.getIntExtra(FaissBroadcasts.EXTRA_DIM, FaissConfig.get().dim))
                        .putInt(FaissBroadcasts.EXTRA_COUNT, intent.getIntExtra(FaissBroadcasts.EXTRA_COUNT, 0))
                        .putLong(FaissBroadcasts.EXTRA_TS, intent.getLongExtra(FaissBroadcasts.EXTRA_TS, System.currentTimeMillis()))
                        .putString(FaissBroadcasts.EXTRA_VIDEO_ID, intent.getStringExtra(FaissBroadcasts.EXTRA_VIDEO_ID))
                        .build())
                    .addTag("faiss-segment-build")
                    .build()
                WorkManager.getInstance(context).enqueue(req)
            }
            FaissBroadcasts.ACTION_FAISS_COMPACT -> {
                val req = OneTimeWorkRequestBuilder<FaissCompactionWorker>().build()
                WorkManager.getInstance(context).enqueue(req)
            }
        }
    }
}

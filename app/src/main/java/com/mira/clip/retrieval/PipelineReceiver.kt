package com.mira.clip.retrieval

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.mira.clip.retrieval.work.IngestWorker
import com.mira.clip.retrieval.work.RetrieveWorker

class PipelineReceiver : BroadcastReceiver() {
  override fun onReceive(ctx: Context, intent: Intent) {
    val manifestPath = intent.getStringExtra(Actions.EXTRA_MANIFEST_PATH)
      ?: return

    val workData = Data.Builder()
      .putString(Actions.EXTRA_MANIFEST_PATH, manifestPath)
      .build()

    val wm = WorkManager.getInstance(ctx)
    when (intent.action) {
      Actions.ACTION_INGEST -> {
        wm.enqueue(OneTimeWorkRequestBuilder<IngestWorker>()
          .setInputData(workData).build())
      }
      Actions.ACTION_RETRIEVE -> {
        wm.enqueue(OneTimeWorkRequestBuilder<RetrieveWorker>()
          .setInputData(workData).build())
      }
    }
  }
}

package com.mira.clip.retrieval.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.mira.clip.retrieval.Actions
import com.mira.clip.retrieval.work.IngestWorker
import com.mira.clip.retrieval.work.RetrieveWorker

class DebugReceiver : BroadcastReceiver() {
  override fun onReceive(ctx: Context, intent: Intent) {
    val mp = intent.getStringExtra(Actions.EXTRA_MANIFEST_PATH) ?: return
    val data = Data.Builder().putString(Actions.EXTRA_MANIFEST_PATH, mp).build()
    val wm = WorkManager.getInstance(ctx)
    when (intent.action) {
      DebugActions.ACTION_INGEST   ->
        wm.enqueue(OneTimeWorkRequestBuilder<IngestWorker>().setInputData(data).build())
      DebugActions.ACTION_RETRIEVE ->
        wm.enqueue(OneTimeWorkRequestBuilder<RetrieveWorker>().setInputData(data).build())
    }
  }
}

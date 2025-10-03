package com.mira.clip.index.faiss.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.*

object DebugFaissActions {
    private const val PKG = "com.mira.clip.index.faiss.debug"
    const val ACTION_FAISS_SELFTEST = "$PKG.ACTION_FAISS_SELFTEST"
}

class DebugFaissReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == DebugFaissActions.ACTION_FAISS_SELFTEST) {
            WorkManager.getInstance(context).enqueue(OneTimeWorkRequestBuilder<FaissSelfTestWorker>().build())
        }
    }
}

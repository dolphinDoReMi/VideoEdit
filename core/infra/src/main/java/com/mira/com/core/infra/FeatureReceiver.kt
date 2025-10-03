package com.mira.com.core.infra
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class FeatureReceiver : BroadcastReceiver() {
    override fun onReceive(
        ctx: Context,
        intent: Intent,
    ) {
        when (intent.action) {
            "${ctx.packageName}.FEATURE.EVENT" -> {
                val kind = intent.getStringExtra("kind")
                when (kind) {
                    "SMOKE" -> {
                        Log.i("FeatureReceiver", "SMOKE test triggered")
                        // Smoke test placeholder - can be implemented later
                    }
                }
            }
        }
    }
}

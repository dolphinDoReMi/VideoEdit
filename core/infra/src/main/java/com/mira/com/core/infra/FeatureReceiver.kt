package com.mira.com.core.infra
import android.content.*
import android.util.Log

class FeatureReceiver : BroadcastReceiver() {
  override fun onReceive(ctx: Context, intent: Intent) {
    when (intent.action) {
      "${ctx.packageName}.FEATURE.EVENT" -> {
        val kind = intent.getStringExtra("kind")
        when (kind) {
          "SMOKE" -> {
            Log.d("FeatureReceiver", "Smoke test triggered")
            // TODO: Implement actual smoke test
          }
        }
      }
    }
  }
}

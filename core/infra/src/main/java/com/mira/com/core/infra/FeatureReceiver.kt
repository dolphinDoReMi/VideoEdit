package com.mira.com.core.infra
import android.content.*
class FeatureReceiver : BroadcastReceiver() {
  override fun onReceive(ctx: Context, intent: Intent) {
    when (intent.action) {
      "${ctx.packageName}.FEATURE.EVENT" -> {
        val kind = intent.getStringExtra("kind")
        when (kind) {
          "SMOKE" -> com.mira.com.Smoke.run() // side-effect or log
        }
      }
    }
  }
}

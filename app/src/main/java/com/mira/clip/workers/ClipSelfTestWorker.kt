package com.mira.clip.workers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mira.clip.usecases.ComputeClipSimilarityUseCase
import kotlin.math.max

class ClipSelfTestWorker(ctx: Context, params: WorkerParameters): CoroutineWorker(ctx, params) {
  override suspend fun doWork(): Result {
    val uc = ComputeClipSimilarityUseCase(applicationContext)
    val bmp = Bitmap.createBitmap(224,224, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.GRAY) }
    return try {
      val (iv, tv) = uc.run(bmp, "a gray square")
      val sim = uc.cosine(iv, tv)
      // we just ensure the pipeline runs; for a gray square the sim threshold is not strict
      if (sim.isFinite()) Result.success() else Result.failure()
    } catch (t: Throwable) {
      Result.failure()
    }
  }
}

package com.mira.clip.index.faiss.debug

import android.content.Context
import android.content.Intent
import androidx.work.*
import com.mira.clip.index.faiss.*
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class FaissSelfTestWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val cfg = FaissConfig.get()
        val variant = "debug"
        val dim = 4
        val vecs = floatArrayOf(1f,0f,0f,0f, 0f,1f,0f,0f) // two basis vectors
        val tmp = File.createTempFile("emb", ".f32", applicationContext.cacheDir)
        tmp.writeBytes(ByteBuffer.allocate(8*4).order(ByteOrder.LITTLE_ENDIAN).apply { asFloatBuffer().put(vecs) }.array())

        val i = Intent(FaissBroadcasts.ACTION_FAISS_BUILD_SEGMENT).apply {
            putExtra(FaissBroadcasts.EXTRA_VARIANT, variant)
            putExtra(FaissBroadcasts.EXTRA_EMB_F32_PATH, tmp.absolutePath)
            putExtra(FaissBroadcasts.EXTRA_DIM, dim)
            putExtra(FaissBroadcasts.EXTRA_COUNT, 2)
            putExtra(FaissBroadcasts.EXTRA_TS, System.currentTimeMillis())
            putExtra(FaissBroadcasts.EXTRA_VIDEO_ID, "selftest")
        }
        applicationContext.sendBroadcast(i)

        // Query
        val q = floatArrayOf(1f,0f,0f,0f)
        FaissShardedSearch(applicationContext, variant).use { s ->
            val top = s.searchTopK(q, 1)
            check(top.isNotEmpty())
        }
        return Result.success()
    }
}

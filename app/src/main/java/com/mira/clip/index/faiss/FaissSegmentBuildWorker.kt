package com.mira.clip.index.faiss

import android.content.Context
import androidx.work.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs

class FaissSegmentBuildWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val cfg = FaissConfig.get()
        val variant = inputData.getString(FaissBroadcasts.EXTRA_VARIANT) ?: cfg.variant
        val embPath = inputData.getString(FaissBroadcasts.EXTRA_EMB_F32_PATH) ?: return Result.failure()
        val dim = inputData.getInt(FaissBroadcasts.EXTRA_DIM, cfg.dim)
        val count = inputData.getInt(FaissBroadcasts.EXTRA_COUNT, 0)
        val ts = inputData.getLong(FaissBroadcasts.EXTRA_TS, System.currentTimeMillis())
        val videoId = inputData.getString(FaissBroadcasts.EXTRA_VIDEO_ID) ?: "unknown"

        try {
            // Load floats (N x dim), ensure L2-normalized if using cosine/IP
            val floats = mmapF32(File(embPath))
            require(floats.size == count * dim) { "emb size != count*dim" }
            if (cfg.metric == "ip") normalizeRows(floats, dim)

            // Build a segment index
            val h = when (cfg.indexType) {
                FaissIndexType.FLAT_IP -> FaissBridge.createFlatIP(dim)
                FaissIndexType.IVF_PQ -> FaissBridge.createIVFPQ(dim, cfg.nlist, cfg.pqM, cfg.pqBits).also {
                    if (!isTrained(variant)) {
                        // Train on this batch (or provide external training vectors)
                        FaissBridge.train(it, floats)
                        markTrained(variant)
                    }
                    FaissBridge.setNProbe(it, cfg.nprobe)
                }
                FaissIndexType.HNSW_IP -> FaissBridge.createHNSWIP(dim, cfg.hnswM).also {
                    FaissBridge.setEfConstruction(it, cfg.efConstruction)
                    FaissBridge.setEfSearch(it, cfg.efSearch)
                }
            }

            // IDs: stable 64-bit for each row (videoId + row)
            val ids = LongArray(count) { row ->
                hash64("${videoId}#${row}", FaissConfig.get().idHashSalt)
            }
            FaissBridge.addWithIds(h, floats, ids)

            // Atomic publish
            val (tmpFaiss, tmpIds) = FaissPaths.segFilesTmp(applicationContext, variant, ts, count)
            FaissBridge.writeIndex(h, tmpFaiss.absolutePath)
            tmpIds.writeText(Json.encodeToString(LongArraySerializer, ids))
            java.io.FileOutputStream(tmpIds).fd.sync()

            if (cfg.atomicPublish) {
                val (finFaiss, finIds) = FaissPaths.segFiles(applicationContext, variant, ts, count)
                tmpFaiss.renameTo(finFaiss); tmpIds.renameTo(finIds)
                updateManifest(variant, finFaiss, finIds, count, ts)
            }

            FaissBridge.freeIndex(h)
            return Result.success()
        } catch (t: Throwable) {
            return Result.retry()
        }
    }

    private fun mmapF32(file: File): FloatArray {
        val ch = file.inputStream().channel
        ch.use {
            val bb = it.map(java.nio.channels.FileChannel.MapMode.READ_ONLY, 0, file.length())
            bb.order(ByteOrder.LITTLE_ENDIAN)
            val arr = FloatArray((file.length() / 4).toInt())
            bb.asFloatBuffer().get(arr)
            return arr
        }
    }

    private fun normalizeRows(v: FloatArray, dim: Int) {
        val rows = v.size / dim
        for (r in 0 until rows) {
            var s = 0.0
            val base = r * dim
            for (i in 0 until dim) s += v[base + i] * v[base + i]
            val inv = if (s > 0.0) (1.0 / kotlin.math.sqrt(s)).toFloat() else 1f
            for (i in 0 until dim) v[base + i] *= inv
        }
    }

    private fun updateManifest(variant: String, finFaiss: File, finIds: File, count: Int, ts: Long) {
        val mfFile = FaissPaths.manifest(applicationContext, variant)
        val cfg = FaissConfig.get()
        val existing = FaissManifest.load(mfFile)
        val params = mapOf(
            "nlist" to cfg.nlist, "nprobe" to cfg.nprobe,
            "pqM" to cfg.pqM, "pqBits" to cfg.pqBits,
            "hnswM" to cfg.hnswM, "efC" to cfg.efConstruction, "efS" to cfg.efSearch
        )
        val seg = SegmentMeta(finFaiss.name, finIds.name, count, ts)
        val updated = if (existing == null)
            FaissManifest(cfg.schemaVersion, cfg.dim, cfg.metric, cfg.indexType.name, variant, params, listOf(seg), trained = isTrained(variant))
        else
            existing.copy(segments = existing.segments + seg, trained = isTrained(variant))
        FaissManifest.save(mfFile, updated)
    }

    // You can persist this flag in MANIFEST or a small local file.
    private fun isTrained(variant: String) =
        FaissManifest.load(FaissPaths.manifest(applicationContext, variant))?.trained == true

    private fun markTrained(variant: String) {
        val mf = FaissManifest.load(FaissPaths.manifest(applicationContext, variant))
        if (mf != null) FaissManifest.save(FaissPaths.manifest(applicationContext, variant), mf.copy(trained = true))
    }

    object LongArraySerializer : KSerializer<LongArray> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("LongArray")
        override fun serialize(encoder: Encoder, value: LongArray) {
            encoder.encodeString(value.joinToString(","))
        }
        override fun deserialize(decoder: Decoder): LongArray =
            decoder.decodeString().split(",").filter { it.isNotBlank() }.map { it.toLong() }.toLongArray()
    }

    private fun hash64(s: String, salt: Long): Long {
        var h = salt
        for (c in s.encodeToByteArray()) { h = (h xor c.toLong()) * 0x100000001B3L }
        return h
    }
}

package com.mira.clip.index.faiss

import android.content.Context
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.PriorityQueue
import kotlin.math.min

class FaissShardedSearch(private val context: Context, private val variant: String = FaissConfig.get().variant) : AutoCloseable {
    data class Handle(val h: Long, val ids: LongArray)
    private val handles = mutableListOf<Pair<Handle, File>>()

    init {
        val root = FaissPaths.segments(context, variant)
        val mf = FaissManifest.load(FaissPaths.manifest(context, variant))
        require(mf != null) { "No MANIFEST for variant=$variant" }
        mf.segments.forEach { s ->
            val idxFile = File(root, s.file)
            val idsFile = File(root, s.ids)
            val h = FaissBridge.readIndex(idxFile.absolutePath)
            applyRuntimeParams(h)
            val ids = readIds(idsFile)
            handles += (Handle(h, ids) to idxFile)
        }
    }

    private fun applyRuntimeParams(h: Long) {
        val cfg = FaissConfig.get()
        if (cfg.indexType == FaissIndexType.IVF_PQ) FaissBridge.setNProbe(h, cfg.nprobe)
        if (cfg.indexType == FaissIndexType.HNSW_IP) FaissBridge.setEfSearch(h, cfg.efSearch)
    }

    fun searchTopK(query: FloatArray, k: Int): List<Pair<Long, Float>> {
        // Normalize if IP/cosine
        val cfg = FaissConfig.get()
        if (cfg.metric == "ip") {
            val dim = cfg.dim
            var s = 0.0
            for (i in 0 until dim) s += query[i]*query[i]
            val inv = if (s > 0.0) (1.0 / kotlin.math.sqrt(s)).toFloat() else 1f
            for (i in 0 until dim) query[i] *= inv
        }
        val distances = FloatArray(k)
        val labels = LongArray(k)
        val heap = PriorityQueue<Pair<Long, Float>>(compareBy { it.second }) // min-heap on distance

        handles.forEach { (handle, _) ->
            val d = FloatArray(k)
            val l = LongArray(k)
            FaissBridge.search(handle.h, query, k, d, l)
            for (i in 0 until k) {
                val id = l[i]; val score = d[i]
                if (id >= 0) {
                    if (heap.size < k) heap.offer(id to score)
                    else if (score > heap.peek().second) { heap.poll(); heap.offer(id to score) }
                }
            }
        }
        val out = mutableListOf<Pair<Long, Float>>()
        while (heap.isNotEmpty()) out += heap.poll()
        return out.sortedByDescending { it.second }
    }

    private fun readIds(file: File): LongArray =
        Json.decodeFromString(FaissSegmentBuildWorker.LongArraySerializer, file.readText())

    override fun close() { handles.forEach { FaissBridge.freeIndex(it.first.h) } }
}

package com.mira.clip.index.faiss

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class SegmentMeta(val file: String, val ids: String, val count: Int, val ts: Long)

@Serializable
data class FaissManifest(
    val schemaVersion: Int,
    val dim: Int,
    val metric: String,
    val indexType: String,
    val variant: String,
    val params: Map<String, Int>,
    val segments: List<SegmentMeta>,
    val trained: Boolean = false,
    val trainInfo: String = ""
) {
    companion object {
        private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
        
        fun load(file: File): FaissManifest? = file.takeIf { it.exists() }?.let {
            json.decodeFromString(serializer(), it.readText())
        }
        
        fun save(file: File, mf: FaissManifest) {
            file.writeText(json.encodeToString(serializer(), mf))
            java.io.FileOutputStream(file).fd.sync()
        }
    }
}

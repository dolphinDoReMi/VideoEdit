package com.mira.clip.index.faiss

import org.junit.Test
import org.junit.Assert.*
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class FaissPipelineTest {

    @Test
    fun testVideoIngestionPipeline() {
        // Test the video ingestion pipeline integration
        val config = FaissConfig.get()
        
        // Verify configuration
        assertEquals("Default variant should be 'base'", "base", config.variant)
        assertEquals("Default dimension should be 512", 512, config.dim)
        assertEquals("Default index type should be IVF_PQ", FaissIndexType.IVF_PQ, config.indexType)
        
        // Test path construction
        val testRoot = File("/tmp/MiraClip/out/faiss")
        val expectedVariantRoot = File(testRoot, config.variant)
        val expectedSegments = File(expectedVariantRoot, "segments")
        val expectedManifest = File(expectedVariantRoot, "MANIFEST.json")
        
        assertEquals("Variant root should be correct", expectedVariantRoot.path, "/tmp/MiraClip/out/faiss/base")
        assertEquals("Segments path should be correct", expectedSegments.path, "/tmp/MiraClip/out/faiss/base/segments")
        assertEquals("Manifest path should be correct", expectedManifest.path, "/tmp/MiraClip/out/faiss/base/MANIFEST.json")
        
        // Test broadcast actions
        assertEquals("Build segment action should be correct", 
            "com.mira.clip.index.faiss.ACTION_FAISS_BUILD_SEGMENT", 
            FaissBroadcasts.ACTION_FAISS_BUILD_SEGMENT)
        
        assertEquals("Compact action should be correct", 
            "com.mira.clip.index.faiss.ACTION_FAISS_COMPACT", 
            FaissBroadcasts.ACTION_FAISS_COMPACT)
        
        assertEquals("Built action should be correct", 
            "com.mira.clip.index.faiss.ACTION_FAISS_BUILT", 
            FaissBroadcasts.ACTION_FAISS_BUILT)
    }

    @Test
    fun testEmbeddingStorageFormat() {
        // Test embedding storage format compatibility
        val dim = 512
        val frameCount = 32
        val totalSize = frameCount * dim
        
        // Create test embedding data
        val embeddings = FloatArray(totalSize) { i ->
            // Create some test data that simulates frame embeddings
            val frameIndex = i / dim
            val featureIndex = i % dim
            (frameIndex * 0.1f + featureIndex * 0.01f) % 1.0f
        }
        
        // Test .f32 file format (little-endian float32)
        val buffer = ByteBuffer.allocate(totalSize * 4).order(ByteOrder.LITTLE_ENDIAN)
        buffer.asFloatBuffer().put(embeddings)
        val f32Data = buffer.array()
        
        assertEquals("F32 data size should be correct", totalSize * 4, f32Data.size)
        
        // Verify we can read it back
        val readBuffer = ByteBuffer.wrap(f32Data).order(ByteOrder.LITTLE_ENDIAN)
        val readEmbeddings = FloatArray(totalSize)
        readBuffer.asFloatBuffer().get(readEmbeddings)
        
        for (i in 0 until totalSize) {
            assertEquals("Embedding values should match", embeddings[i], readEmbeddings[i], 0.001f)
        }
    }

    @Test
    fun testSegmentMetadata() {
        // Test segment metadata structure
        val segmentMeta = SegmentMeta(
            file = "seg-1234567890-100.faiss",
            ids = "seg-1234567890-100.ids.json",
            count = 100,
            ts = 1234567890L
        )
        
        assertEquals("File name should be correct", "seg-1234567890-100.faiss", segmentMeta.file)
        assertEquals("IDs file should be correct", "seg-1234567890-100.ids.json", segmentMeta.ids)
        assertEquals("Count should be 100", 100, segmentMeta.count)
        assertEquals("Timestamp should be correct", 1234567890L, segmentMeta.ts)
    }

    @Test
    fun testManifestStructure() {
        // Test manifest structure
        val manifest = FaissManifest(
            schemaVersion = 1,
            dim = 512,
            metric = "ip",
            indexType = "IVF_PQ",
            variant = "base",
            params = mapOf(
                "nlist" to 4096,
                "nprobe" to 16,
                "pqM" to 64,
                "pqBits" to 8
            ),
            segments = listOf(
                SegmentMeta("seg-123.faiss", "seg-123.ids.json", 100, 1234567890L),
                SegmentMeta("seg-456.faiss", "seg-456.ids.json", 150, 1234567891L)
            ),
            trained = true,
            trainInfo = "Trained on 1000 vectors"
        )
        
        assertEquals("Schema version should be 1", 1, manifest.schemaVersion)
        assertEquals("Dimension should be 512", 512, manifest.dim)
        assertEquals("Metric should be 'ip'", "ip", manifest.metric)
        assertEquals("Index type should be 'IVF_PQ'", "IVF_PQ", manifest.indexType)
        assertEquals("Variant should be 'base'", "base", manifest.variant)
        assertEquals("Should have 2 segments", 2, manifest.segments.size)
        assertEquals("Should be trained", true, manifest.trained)
        assertEquals("Train info should be correct", "Trained on 1000 vectors", manifest.trainInfo)
        
        // Test parameters
        assertEquals("nlist should be 4096", 4096, manifest.params["nlist"])
        assertEquals("nprobe should be 16", 16, manifest.params["nprobe"])
        assertEquals("pqM should be 64", 64, manifest.params["pqM"])
        assertEquals("pqBits should be 8", 8, manifest.params["pqBits"])
    }

    @Test
    fun testPerformanceParameters() {
        // Test performance parameter validation
        val config = FaissConfig.get()
        
        // Test IVF+PQ parameters
        assertTrue("nlist should be positive", config.nlist > 0)
        assertTrue("nprobe should be positive", config.nprobe > 0)
        assertTrue("nprobe should be <= nlist", config.nprobe <= config.nlist)
        assertTrue("pqM should be positive", config.pqM > 0)
        assertTrue("pqBits should be positive", config.pqBits > 0)
        assertTrue("pqBits should be <= 8", config.pqBits <= 8)
        
        // Test HNSW parameters
        assertTrue("hnswM should be positive", config.hnswM > 0)
        assertTrue("efConstruction should be positive", config.efConstruction > 0)
        assertTrue("efSearch should be positive", config.efSearch > 0)
        
        // Test segment parameters
        assertTrue("segmentTargetN should be positive", config.segmentTargetN > 0)
        assertTrue("compactionMinSegments should be positive", config.compactionMinSegments > 0)
        
        // Test dimension
        assertTrue("dim should be positive", config.dim > 0)
        assertTrue("dim should be reasonable", config.dim <= 4096)
    }
}

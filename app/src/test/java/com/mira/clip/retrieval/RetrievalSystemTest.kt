package com.mira.clip.retrieval

import com.mira.clip.retrieval.index.FlatIndexBackend
import com.mira.clip.retrieval.io.EmbeddingStore
import com.mira.clip.retrieval.io.ResultsWriter
import com.mira.clip.retrieval.util.FileIO
import com.mira.clip.retrieval.util.Maths
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import org.junit.After
import java.io.File
import kotlin.random.Random

class RetrievalSystemTest {
    
    private val testDir = File(System.getProperty("java.io.tmpdir"), "retrieval_test")
    
    @Before
    fun setUp() {
        testDir.mkdirs()
    }
    
    @After
    fun tearDown() {
        testDir.deleteRecursively()
    }
    
    @Test
    fun testVectorMathOperations() {
        // Test L2 normalization
        val vec = floatArrayOf(3.0f, 4.0f, 0.0f)
        val normalized = Maths.l2Normalize(vec)
        
        val normSquared = normalized.fold(0f) { acc, v -> acc + v * v }
        assertEquals(1.0f, normSquared, 0.001f)
        assertEquals(0.6f, normalized[0], 0.001f)
        assertEquals(0.8f, normalized[1], 0.001f)
        assertEquals(0.0f, normalized[2], 0.001f)
        
        // Test cosine similarity
        val vec1 = floatArrayOf(1.0f, 0.0f, 0.0f)
        val vec2 = floatArrayOf(0.0f, 1.0f, 0.0f)
        val vec3 = floatArrayOf(1.0f, 0.0f, 0.0f)
        
        val cosine1 = Maths.cosineNormalized(vec1, vec2)
        val cosine2 = Maths.cosineNormalized(vec1, vec3)
        
        assertEquals(0.0f, cosine1, 0.001f)
        assertEquals(1.0f, cosine2, 0.001f)
    }
    
    @Test
    fun testEmbeddingStore() {
        val testVector = floatArrayOf(1.0f, 2.0f, 3.0f, 4.0f)
        val testPath = File(testDir, "test.f32").absolutePath
        
        // Write and read vector
        EmbeddingStore.writeVector(testPath, testVector)
        val readVector = EmbeddingStore.readVector(testPath)
        
        assertArrayEquals(testVector, readVector, 0.001f)
        
        // Test metadata
        val metaPath = File(testDir, "test.json").absolutePath
        EmbeddingStore.writeMetadata(metaPath, "test_id", "test_source", 4, 32, "test_variant")
        
        val metaFile = File(metaPath)
        assertTrue(metaFile.exists())
        assertTrue(metaFile.readText().contains("test_id"))
    }
    
    @Test
    fun testFlatIndexBackend() {
        val embeddingDir = File(testDir, "embeddings")
        embeddingDir.mkdirs()
        
        // Create test vectors
        val vectors = listOf(
            "doc1" to floatArrayOf(1.0f, 0.0f, 0.0f),
            "doc2" to floatArrayOf(0.0f, 1.0f, 0.0f),
            "doc3" to floatArrayOf(0.0f, 0.0f, 1.0f),
            "doc4" to floatArrayOf(0.707f, 0.707f, 0.0f)
        )
        
        vectors.forEach { (id, vec) ->
            val vecPath = File(embeddingDir, "$id.f32").absolutePath
            EmbeddingStore.writeVector(vecPath, vec)
        }
        
        // Test search
        val backend = FlatIndexBackend(embeddingDir.absolutePath)
        val query = floatArrayOf(1.0f, 0.0f, 0.0f)
        val results = backend.searchCosineTopK(query, 3)
        
        assertEquals(3, results.size)
        assertEquals("doc1", results[0].id) // Should be most similar
        assertTrue(results[0].score > results[1].score)
        assertTrue(results[1].score > results[2].score)
    }
    
    @Test
    fun testResultsWriter() {
        val results = listOf(
            com.mira.clip.retrieval.index.SearchHit("doc1", 0.95f),
            com.mira.clip.retrieval.index.SearchHit("doc2", 0.87f),
            com.mira.clip.retrieval.index.SearchHit("doc3", 0.72f)
        )
        
        val outputPath = File(testDir, "results.json").absolutePath
        ResultsWriter.writeJson(outputPath, results)
        
        val resultFile = File(outputPath)
        assertTrue(resultFile.exists())
        val content = resultFile.readText()
        assertTrue(content.contains("doc1"))
        assertTrue(content.contains("0.95"))
    }
    
    @Test
    fun testFileIO() {
        val testPath = File(testDir, "nested/deep/directory").absolutePath
        FileIO.ensureDir(testPath)
        
        val dir = File(testPath)
        assertTrue(dir.exists())
        assertTrue(dir.isDirectory)
    }
    
    @Test
    fun testManifestLoading() {
        val manifestContent = """
        {
          "variant": "test_variant",
          "frame_count": 16,
          "batch_size": 4,
          "index": {
            "dir": "/test/index",
            "type": "FLAT"
          },
          "ingest": {
            "videos": ["/test/video1.mp4"],
            "output_dir": "/test/embeddings"
          },
          "query": {
            "query_vec_path": "/test/query.f32",
            "top_k": 10,
            "output_path": "/test/results.json"
          }
        }
        """.trimIndent()
        
        val manifestPath = File(testDir, "test_manifest.json").absolutePath
        File(manifestPath).writeText(manifestContent)
        
        val manifest = loadManifest(manifestPath)
        
        assertEquals("test_variant", manifest.variant)
        assertEquals(16, manifest.frameCount)
        assertEquals(4, manifest.batchSize)
        assertEquals("FLAT", manifest.index.type)
        assertEquals("/test/index", manifest.index.dir)
        assertEquals(1, manifest.ingest.videos.size)
        assertEquals("/test/video1.mp4", manifest.ingest.videos[0])
        assertEquals(10, manifest.query.topK)
    }
}

package com.mira.clip.index.faiss

import org.junit.Test
import org.junit.Assert.*

class FaissConfigTest {

    @Test
    fun testDefaultConfig() {
        val config = FaissDesignConfig()
        
        assertEquals("MiraClip/out/faiss", config.indexRootSubpath)
        assertEquals("ip", config.metric)
        assertEquals(FaissIndexType.IVF_PQ, config.indexType)
        assertEquals(512, config.dim)
        assertEquals(4096, config.nlist)
        assertEquals(16, config.nprobe)
        assertEquals(64, config.pqM)
        assertEquals(8, config.pqBits)
        assertEquals(32, config.hnswM)
        assertEquals(200, config.efConstruction)
        assertEquals(64, config.efSearch)
        assertEquals(512, config.segmentTargetN)
        assertEquals(false, config.compactionEnabled)
        assertEquals(16, config.compactionMinSegments)
        assertEquals(0x7F4A7C15L, config.idHashSalt)
        assertEquals(true, config.atomicPublish)
        assertEquals(1, config.schemaVersion)
        assertEquals("base", config.variant)
    }

    @Test
    fun testConfigUpdate() {
        val originalConfig = FaissDesignConfig()
        assertEquals(FaissIndexType.IVF_PQ, originalConfig.indexType)
        
        val newConfig = originalConfig.copy(indexType = FaissIndexType.FLAT_IP)
        assertEquals(FaissIndexType.FLAT_IP, newConfig.indexType)
        
        FaissConfig.update(newConfig)
        assertEquals(FaissIndexType.FLAT_IP, FaissConfig.get().indexType)
        
        // Restore original
        FaissConfig.update(originalConfig)
        assertEquals(FaissIndexType.IVF_PQ, FaissConfig.get().indexType)
    }

    @Test
    fun testIndexTypes() {
        assertEquals("FLAT_IP", FaissIndexType.FLAT_IP.name)
        assertEquals("IVF_PQ", FaissIndexType.IVF_PQ.name)
        assertEquals("HNSW_IP", FaissIndexType.HNSW_IP.name)
    }
}

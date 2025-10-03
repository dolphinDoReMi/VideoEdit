package com.mira.clip.index.faiss

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

@RunWith(AndroidJUnit4::class)
class FaissInstrumentedTest {

    @Test
    fun testFaissConfigIntegration() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.mira.clip", appContext.packageName)
        
        // Test that FAISS config can be loaded
        val config = FaissConfig.get()
        assertNotNull("FAISS config should not be null", config)
        assertEquals("base", config.variant)
        assertEquals(512, config.dim)
    }

    @Test
    fun testFaissPaths() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        
        // Test path creation
        val variantRoot = FaissPaths.variantRoot(appContext, "test")
        assertTrue("Variant root should exist", variantRoot.exists())
        
        val segments = FaissPaths.segments(appContext, "test")
        assertTrue("Segments directory should exist", segments.exists())
        
        val manifest = FaissPaths.manifest(appContext, "test")
        assertNotNull("Manifest file should be defined", manifest)
    }

    @Test
    fun testFaissBroadcasts() {
        // Test broadcast action constants
        assertTrue("Build segment action should be defined", 
            FaissBroadcasts.ACTION_FAISS_BUILD_SEGMENT.isNotEmpty())
        assertTrue("Compact action should be defined", 
            FaissBroadcasts.ACTION_FAISS_COMPACT.isNotEmpty())
        assertTrue("Built action should be defined", 
            FaissBroadcasts.ACTION_FAISS_BUILT.isNotEmpty())
        
        // Test extra constants
        assertTrue("Video ID extra should be defined", 
            FaissBroadcasts.EXTRA_VIDEO_ID.isNotEmpty())
        assertTrue("Variant extra should be defined", 
            FaissBroadcasts.EXTRA_VARIANT.isNotEmpty())
    }
}

package com.mira.clip.index.faiss

import org.junit.Test
import org.junit.Assert.*

class FaissBroadcastsTest {

    @Test
    fun testBroadcastActions() {
        assertEquals("com.mira.clip.index.faiss.ACTION_FAISS_BUILD_SEGMENT", FaissBroadcasts.ACTION_FAISS_BUILD_SEGMENT)
        assertEquals("com.mira.clip.index.faiss.ACTION_FAISS_COMPACT", FaissBroadcasts.ACTION_FAISS_COMPACT)
        assertEquals("com.mira.clip.index.faiss.ACTION_FAISS_BUILT", FaissBroadcasts.ACTION_FAISS_BUILT)
    }

    @Test
    fun testBroadcastExtras() {
        assertEquals("videoId", FaissBroadcasts.EXTRA_VIDEO_ID)
        assertEquals("variant", FaissBroadcasts.EXTRA_VARIANT)
        assertEquals("embF32Path", FaissBroadcasts.EXTRA_EMB_F32_PATH)
        assertEquals("dim", FaissBroadcasts.EXTRA_DIM)
        assertEquals("count", FaissBroadcasts.EXTRA_COUNT)
        assertEquals("ts", FaissBroadcasts.EXTRA_TS)
    }
}

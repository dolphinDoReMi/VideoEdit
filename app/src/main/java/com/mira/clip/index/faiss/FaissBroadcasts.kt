package com.mira.clip.index.faiss

object FaissBroadcasts {
    private const val PKG = "com.mira.clip.index.faiss"
    const val ACTION_FAISS_BUILD_SEGMENT = "$PKG.ACTION_FAISS_BUILD_SEGMENT"
    const val ACTION_FAISS_COMPACT = "$PKG.ACTION_FAISS_COMPACT"
    const val ACTION_FAISS_BUILT = "$PKG.ACTION_FAISS_BUILT"
    
    // Extras
    const val EXTRA_VIDEO_ID = "videoId"
    const val EXTRA_VARIANT = "variant"
    const val EXTRA_EMB_F32_PATH = "embF32Path"   // temp .f32 (N x dim)
    const val EXTRA_DIM = "dim"
    const val EXTRA_COUNT = "count"               // N
    const val EXTRA_TS = "ts"
}

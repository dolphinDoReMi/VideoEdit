package com.mira.clip.index.faiss

object FaissBridge {
    private var nativeLibraryLoaded = false
    
    init { 
        try {
            System.loadLibrary("faiss_jni") 
            nativeLibraryLoaded = true
            android.util.Log.i("FaissBridge", "Native FAISS library loaded successfully")
        } catch (e: UnsatisfiedLinkError) {
            // Native library not available, use stub implementation
            nativeLibraryLoaded = false
            android.util.Log.w("FaissBridge", "Native FAISS library not found, using stub implementation")
        }
    }
    
    private fun checkNativeLibrary(): Boolean {
        if (!nativeLibraryLoaded) {
            android.util.Log.w("FaissBridge", "Native library not loaded, using stub implementation")
        }
        return nativeLibraryLoaded
    }

    external fun createFlatIP(dim: Int): Long
    external fun createIVFPQ(dim: Int, nlist: Int, m: Int, nbits: Int): Long
    external fun createHNSWIP(dim: Int, M: Int): Long

    external fun setNProbe(handle: Long, nprobe: Int)
    external fun setEfSearch(handle: Long, efSearch: Int)
    external fun setEfConstruction(handle: Long, efConstruction: Int)

    external fun train(handle: Long, trainVecs: FloatArray)
    external fun addWithIds(handle: Long, vecs: FloatArray, ids: LongArray)
    external fun writeIndex(handle: Long, path: String)
    external fun readIndex(path: String): Long
    external fun search(handle: Long, queries: FloatArray, k: Int, distances: FloatArray, labels: LongArray)
    external fun freeIndex(handle: Long)
}

package com.mira.clip.core

import android.content.Context
import com.mira.storage.adapters.ClipStorageAdapter

/**
 * Core configuration constants for mira_clip CLIP4Clip service.
 * 
 * DEPRECATED: This class is being replaced by ClipStorageAdapter.
 * All new code should use ClipStorageAdapter for storage access.
 * 
 * This class is kept for backward compatibility only.
 */
object Config {
    /**
     * Default CLIP variant for video-text retrieval.
     * ViT-B/32 with mean pooling produces 512-dimensional embeddings.
     */
    const val DEFAULT_VARIANT = "clip_vit_b32_mean_v1"
    
    /**
     * Default number of frames to sample from videos.
     * 32 frames provides good coverage for most video lengths.
     */
    const val DEFAULT_FRAME_COUNT = 32
    
    /**
     * Default video path for testing
     */
    const val DEFAULT_VIDEO_PATH = "video_v1.mp4"
    
    /**
     * Embeddings directory name
     */
    const val EMBEDDINGS_DIR = "embeddings"
    
    /**
     * Search directory name
     */
    const val SEARCH_DIR = "search"
    
    /**
     * DEPRECATED: Use ClipStorageAdapter.getInputDirPath() instead
     */
    @Deprecated("Use ClipStorageAdapter.getInputDirPath() instead")
    fun getDefaultVideoPath(context: Context): String {
        return ClipStorageAdapter.getInputDirPath(context) + "/video_v1.mp4"
    }
    
    /**
     * DEPRECATED: Use ClipStorageAdapter.getOutputDirPath() instead
     */
    @Deprecated("Use ClipStorageAdapter.getOutputDirPath() instead")
    fun getOutRoot(context: Context): String {
        return ClipStorageAdapter.getOutputDirPath(context)
    }
    
    /**
     * DEPRECATED: Use ClipStorageAdapter.getInputDirPath() instead
     */
    @Deprecated("Use ClipStorageAdapter.getInputDirPath() instead")
    fun getInRoot(context: Context): String {
        return ClipStorageAdapter.getInputDirPath(context)
    }
    
    /**
     * DEPRECATED: Use ClipStorageAdapter.getEmbeddingsDirPath() instead
     */
    @Deprecated("Use ClipStorageAdapter.getEmbeddingsDirPath() instead")
    fun getEmbeddingsDir(context: Context): String {
        return ClipStorageAdapter.getEmbeddingsDirPath(context)
    }
    
    /**
     * DEPRECATED: Use ClipStorageAdapter.getOutputDirPath() + "/search" instead
     */
    @Deprecated("Use ClipStorageAdapter.getOutputDirPath() + \"/search\" instead")
    fun getSearchDir(context: Context): String {
        return ClipStorageAdapter.getOutputDirPath(context) + "/search"
    }
    
    /**
     * Expected embedding dimensions for different variants.
     */
    const val EMBEDDING_DIM_VIT_B32 = 512
    const val EMBEDDING_DIM_VIT_L14 = 768
    
    /**
     * CLIP preprocessing constants.
     * Standard CLIP normalization values.
     */
    const val CLIP_IMAGE_SIZE = 224
    const val CLIP_MEAN_R = 0.48145466f
    const val CLIP_MEAN_G = 0.4578275f
    const val CLIP_MEAN_B = 0.40821073f
    const val CLIP_STD_R = 0.26862954f
    const val CLIP_STD_G = 0.26130258f
    const val CLIP_STD_B = 0.27577711f
    
    /**
     * BPE tokenizer constants.
     * CLIP uses max 77 tokens with special padding.
     */
    const val MAX_TEXT_TOKENS = 77
    const val EOT_TOKEN = 49407  // End of text token
    
    // CLIP defaults (mirrors BuildConfig; read BuildConfig at runtime if you prefer)
    const val CLIP_RES = 224
    const val CLIP_DIM = 512  // Will be overridden by BuildConfig.CLIP_DIM
    const val DEFAULT_MEMORY_BUDGET_MB = 512  // Will be overridden by BuildConfig.DEFAULT_MEM_BUDGET_MB
    const val RETR_ENABLE_ANN = false  // Will be overridden by BuildConfig.RETR_ENABLE_ANN
    
    // Retrieval configuration
    const val RETR_USE_L2_NORM = true
    const val RETR_SIMILARITY = "cosine"
    const val RETR_STORAGE_FMT = ".f32"
}

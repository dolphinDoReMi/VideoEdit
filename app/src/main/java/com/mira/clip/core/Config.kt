package com.mira.clip.core

/**
 * Core configuration constants for mira_clip CLIP4Clip service.
 * 
 * Centralizes defaults for variant, frame count, and device paths
 * to ensure services are deterministic and easy to script.
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
     * Default video path on Xiaomi Pad.
     * This is the expected location for test videos.
     */
    const val DEFAULT_VIDEO_PATH = "/sdcard/Movies/video_v1.mp4"
    
    /**
     * Root output directory for all mira_clip artifacts.
     * All embeddings, search results, and logs go under this path.
     */
    const val OUT_ROOT = "/sdcard/MiraClip/out"
    
    /**
     * Input directory for manifest files.
     * Ingest and search manifests are read from this location.
     */
    const val IN_ROOT = "/sdcard/MiraClip/in"
    
    /**
     * Embeddings output directory.
     * Vector files (.f32) and metadata (.json) are stored here.
     */
    const val EMBEDDINGS_DIR = "$OUT_ROOT/embeddings"
    
    /**
     * Search results output directory.
     * Query results and rankings are stored here.
     */
    const val SEARCH_DIR = "$OUT_ROOT/search"
    
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
}

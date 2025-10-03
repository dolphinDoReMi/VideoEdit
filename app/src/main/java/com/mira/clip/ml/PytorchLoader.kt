package com.mira.clip.ml

import android.content.Context
import org.pytorch.Module
import java.io.File
import java.io.FileOutputStream

/**
 * PyTorch asset loader for CLIP models.
 * 
 * Handles copying TorchScript and BPE assets from app assets to device files
 * before Module.load() for reliable inference.
 */
object PytorchLoader {
    
    /**
     * Copy an asset file to app files directory and return the local path.
     * 
     * @param ctx Android context
     * @param name Asset filename (e.g., "clip_image_encoder.ptl")
     * @return Absolute path to the copied file
     */
    fun assetToFiles(ctx: Context, name: String): String {
        val out = File(ctx.filesDir, name)
        
        // Only copy if file doesn't exist
        if (!out.exists()) {
            ctx.assets.open(name).use { input ->
                FileOutputStream(out).use { output ->
                    input.copyTo(output)
                }
            }
        }
        
        return out.absolutePath
    }
    
    /**
     * Load a PyTorch module from assets.
     * 
     * @param ctx Android context
     * @param assetName Asset filename (e.g., "clip_image_encoder.ptl")
     * @return Loaded PyTorch module
     */
    fun loadModule(ctx: Context, assetName: String): Module {
        val modelPath = assetToFiles(ctx, assetName)
        return Module.load(modelPath)
    }
    
    /**
     * Load CLIP image encoder from assets.
     * 
     * @param ctx Android context
     * @return Loaded CLIP image encoder module
     */
    fun loadImageEncoder(ctx: Context): Module {
        return loadModule(ctx, "clip_image_encoder.ptl")
    }
    
    /**
     * Load CLIP text encoder from assets.
     * 
     * @param ctx Android context
     * @return Loaded CLIP text encoder module
     */
    fun loadTextEncoder(ctx: Context): Module {
        return loadModule(ctx, "clip_text_encoder.ptl")
    }
    
    /**
     * Copy BPE vocabulary to files directory.
     * 
     * @param ctx Android context
     * @return Path to copied vocabulary file
     */
    fun copyBpeVocab(ctx: Context): String {
        return assetToFiles(ctx, "bpe_vocab.json")
    }
    
    /**
     * Copy BPE merges to files directory.
     * 
     * @param ctx Android context
     * @return Path to copied merges file
     */
    fun copyBpeMerges(ctx: Context): String {
        return assetToFiles(ctx, "bpe_merges.txt")
    }
    
    /**
     * Check if all required assets are available.
     * 
     * @param ctx Android context
     * @return true if all assets exist
     */
    fun hasAllAssets(ctx: Context): Boolean {
        val requiredAssets = listOf(
            "clip_image_encoder.ptl",
            "clip_text_encoder.ptl", 
            "bpe_vocab.json",
            "bpe_merges.txt"
        )
        
        return requiredAssets.all { assetName ->
            try {
                ctx.assets.open(assetName).use { true }
            } catch (e: Exception) {
                false
            }
        }
    }
}

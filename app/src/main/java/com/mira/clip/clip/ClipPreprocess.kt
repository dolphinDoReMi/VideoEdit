package com.mira.clip.clip

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import com.mira.clip.core.Config
import org.pytorch.IValue
import org.pytorch.Module
import org.pytorch.Tensor
import java.nio.FloatBuffer
import kotlin.math.sqrt

/**
 * CLIP preprocessing utilities for image normalization.
 * 
 * Handles 224x224 resizing, CLIP mean/std normalization, and CHW format conversion.
 */
object ClipPreprocess {
    
    /**
     * Preprocess a bitmap for CLIP image encoder.
     * 
     * @param bitmap Input bitmap
     * @return Preprocessed tensor ready for CLIP encoder
     */
    fun preprocessImage(bitmap: Bitmap): Tensor {
        // Resize to 224x224
        val resized = resizeBitmap(bitmap, Config.CLIP_IMAGE_SIZE, Config.CLIP_IMAGE_SIZE)
        
        // Convert to CHW format and normalize
        val pixels = FloatArray(3 * Config.CLIP_IMAGE_SIZE * Config.CLIP_IMAGE_SIZE)
        val buffer = FloatBuffer.wrap(pixels)
        
        for (y in 0 until Config.CLIP_IMAGE_SIZE) {
            for (x in 0 until Config.CLIP_IMAGE_SIZE) {
                val pixel = resized.getPixel(x, y)
                
                // Extract RGB values (0-255)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                
                // Normalize with CLIP mean/std and convert to CHW
                val rNorm = (r / 255.0f - Config.CLIP_MEAN_R) / Config.CLIP_STD_R
                val gNorm = (g / 255.0f - Config.CLIP_MEAN_G) / Config.CLIP_STD_G
                val bNorm = (b / 255.0f - Config.CLIP_MEAN_B) / Config.CLIP_STD_B
                
                // CHW format: [C, H, W]
                pixels[y * Config.CLIP_IMAGE_SIZE + x] = rNorm
                pixels[Config.CLIP_IMAGE_SIZE * Config.CLIP_IMAGE_SIZE + y * Config.CLIP_IMAGE_SIZE + x] = gNorm
                pixels[2 * Config.CLIP_IMAGE_SIZE * Config.CLIP_IMAGE_SIZE + y * Config.CLIP_IMAGE_SIZE + x] = bNorm
            }
        }
        
        return Tensor.fromBlob(buffer, longArrayOf(1, 3, Config.CLIP_IMAGE_SIZE.toLong(), Config.CLIP_IMAGE_SIZE.toLong()))
    }
    
    /**
     * Resize bitmap to target dimensions.
     */
    private fun resizeBitmap(bitmap: Bitmap, width: Int, height: Int): Bitmap {
        val resized = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(resized)
        
        val matrix = Matrix()
        val scaleX = width.toFloat() / bitmap.width
        val scaleY = height.toFloat() / bitmap.height
        matrix.setScale(scaleX, scaleY)
        
        canvas.drawBitmap(bitmap, matrix, Paint(Paint.ANTI_ALIAS_FLAG))
        return resized
    }
}

package com.mira.videoeditor.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Log
import com.mira.videoeditor.Logger
import org.pytorch.IValue
import org.pytorch.Module
import org.pytorch.Tensor
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.*

/**
 * PyTorch Mobile integration for CLIP models.
 * 
 * This module provides production-ready CLIP model integration using PyTorch Mobile,
 * including proper preprocessing, model loading, and inference for both image and text.
 */
class PyTorchClipEngine(private val context: Context) {

    companion object {
        private const val TAG = "PyTorchClipEngine"
        
        // Model parameters
        private const val IMAGE_SIZE = 224
        private const val EMBEDDING_DIM = 512
        private const val MAX_TEXT_LENGTH = 77
        
        // CLIP normalization constants
        private val IMAGE_MEAN = floatArrayOf(0.48145466f, 0.4578275f, 0.40821073f)
        private val IMAGE_STD = floatArrayOf(0.26862954f, 0.26130258f, 0.27577711f)
        
        // Model file names
        private const val IMAGE_MODEL_FILE = "clip_image_encoder.ptl"
        private const val TEXT_MODEL_FILE = "clip_text_encoder.ptl"
        
        // BPE tokenizer files
        private const val BPE_VOCAB_FILE = "bpe_vocab.json"
        private const val BPE_MERGES_FILE = "bpe_merges.txt"
    }

    private var imageModule: Module? = null
    private var textModule: Module? = null
    private var bpeTokenizer: ClipBPETokenizer? = null

    /**
     * Initialize the CLIP models and tokenizer.
     */
    suspend fun initialize(): Boolean {
        return try {
            Logger.info(Logger.Category.CLIP, "Initializing PyTorch CLIP models", emptyMap())
            
            // Load image encoder
            val imageModelPath = getAssetFilePath(IMAGE_MODEL_FILE)
            imageModule = Module.load(imageModelPath)
            
            // Load text encoder
            val textModelPath = getAssetFilePath(TEXT_MODEL_FILE)
            textModule = Module.load(textModelPath)
            
            // Initialize BPE tokenizer
            bpeTokenizer = ClipBPETokenizer(context)
            
            Logger.info(Logger.Category.CLIP, "PyTorch CLIP models initialized successfully", emptyMap())
            true
            
        } catch (e: Exception) {
            Logger.logError(Logger.Category.CLIP, "Failed to initialize PyTorch CLIP models", 
                e.message ?: "Unknown error", emptyMap(), e)
            false
        }
    }

    /**
     * Encode a single image using CLIP image encoder.
     */
    fun encodeImage(bitmap: Bitmap): FloatArray {
        val module = imageModule ?: throw IllegalStateException("Image module not initialized")
        
        try {
            // Preprocess image
            val preprocessedBitmap = preprocessImage(bitmap)
            val tensor = bitmapToTensor(preprocessedBitmap)
            
            // Run inference
            val output = module.forward(IValue.from(tensor)).toTensor()
            val embedding = output.dataAsFloatArray
            
            // Normalize embedding
            normalizeEmbedding(embedding)
            
            return embedding
            
        } catch (e: Exception) {
            Logger.logError(Logger.Category.CLIP, "Image encoding failed", 
                e.message ?: "Unknown error", emptyMap(), e)
            throw e
        }
    }

    /**
     * Encode multiple images in batch for efficiency.
     */
    fun encodeImages(bitmaps: List<Bitmap>): List<FloatArray> {
        val module = imageModule ?: throw IllegalStateException("Image module not initialized")
        
        try {
            // Preprocess all images
            val preprocessedBitmaps = bitmaps.map { preprocessImage(it) }
            
            // Convert to batch tensor
            val batchTensor = bitmapsToBatchTensor(preprocessedBitmaps)
            
            // Run batch inference
            val output = module.forward(IValue.from(batchTensor)).toTensor()
            val batchEmbeddings = output.dataAsFloatArray
            
            // Split batch results and normalize
            val embeddings = mutableListOf<FloatArray>()
            val embeddingSize = batchEmbeddings.size / bitmaps.size
            
            for (i in bitmaps.indices) {
                val startIdx = i * embeddingSize
                val endIdx = startIdx + embeddingSize
                val embedding = batchEmbeddings.sliceArray(startIdx until endIdx)
                normalizeEmbedding(embedding)
                embeddings.add(embedding)
            }
            
            return embeddings
            
        } catch (e: Exception) {
            Logger.logError(Logger.Category.CLIP, "Batch image encoding failed", 
                e.message ?: "Unknown error", emptyMap(), e)
            throw e
        }
    }

    /**
     * Encode text using CLIP text encoder.
     */
    fun encodeText(text: String): FloatArray {
        val module = textModule ?: throw IllegalStateException("Text module not initialized")
        val tokenizer = bpeTokenizer ?: throw IllegalStateException("Tokenizer not initialized")
        
        try {
            // Tokenize text
            val tokenIds = tokenizer.encode(text, MAX_TEXT_LENGTH)
            
            // Convert to tensor
            val tensor = Tensor.fromBlob(tokenIds, longArrayOf(1, tokenIds.size.toLong()))
            
            // Run inference
            val output = module.forward(IValue.from(tensor)).toTensor()
            val embedding = output.dataAsFloatArray
            
            // Normalize embedding
            normalizeEmbedding(embedding)
            
            return embedding
            
        } catch (e: Exception) {
            Logger.logError(Logger.Category.CLIP, "Text encoding failed", 
                e.message ?: "Unknown error", mapOf("text" to text.take(50)), e)
            throw e
        }
    }

    /**
     * Compute cosine similarity between two normalized embeddings.
     */
    fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        if (a.size != b.size) return 0f
        
        var dotProduct = 0f
        for (i in a.indices) {
            dotProduct += a[i] * b[i]
        }
        
        return dotProduct // Both vectors are normalized
    }

    /**
     * Check if models are loaded and ready.
     */
    fun isReady(): Boolean {
        return imageModule != null && textModule != null && bpeTokenizer != null
    }

    // Private helper methods

    private fun getAssetFilePath(assetName: String): String {
        val file = File(context.filesDir, assetName)
        if (!file.exists()) {
            try {
                context.assets.open(assetName).use { input ->
                    FileOutputStream(file).use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: IOException) {
                throw RuntimeException("Failed to copy asset $assetName", e)
            }
        }
        return file.absolutePath
    }

    private fun preprocessImage(bitmap: Bitmap): Bitmap {
        // Resize to square maintaining aspect ratio
        val size = minOf(bitmap.width, bitmap.height)
        val x = (bitmap.width - size) / 2
        val y = (bitmap.height - size) / 2
        val croppedBitmap = Bitmap.createBitmap(bitmap, x, y, size, size)
        
        // Scale to target size
        return Bitmap.createScaledBitmap(croppedBitmap, IMAGE_SIZE, IMAGE_SIZE, true)
    }

    private fun bitmapToTensor(bitmap: Bitmap): Tensor {
        val pixels = IntArray(IMAGE_SIZE * IMAGE_SIZE)
        bitmap.getPixels(pixels, 0, IMAGE_SIZE, 0, 0, IMAGE_SIZE, IMAGE_SIZE)
        
        val floatBuffer = Tensor.allocateFloatBuffer(3 * IMAGE_SIZE * IMAGE_SIZE)
        
        // Convert to CHW format with normalization
        for (y in 0 until IMAGE_SIZE) {
            for (x in 0 until IMAGE_SIZE) {
                val pixel = pixels[y * IMAGE_SIZE + x]
                val r = ((pixel shr 16) and 0xFF) / 255f
                val g = ((pixel shr 8) and 0xFF) / 255f
                val b = (pixel and 0xFF) / 255f
                
                // Normalize with CLIP mean/std
                floatBuffer.put((r - IMAGE_MEAN[0]) / IMAGE_STD[0])
            }
        }
        
        for (y in 0 until IMAGE_SIZE) {
            for (x in 0 until IMAGE_SIZE) {
                val pixel = pixels[y * IMAGE_SIZE + x]
                val g = ((pixel shr 8) and 0xFF) / 255f
                floatBuffer.put((g - IMAGE_MEAN[1]) / IMAGE_STD[1])
            }
        }
        
        for (y in 0 until IMAGE_SIZE) {
            for (x in 0 until IMAGE_SIZE) {
                val pixel = pixels[y * IMAGE_SIZE + x]
                val b = (pixel and 0xFF) / 255f
                floatBuffer.put((b - IMAGE_MEAN[2]) / IMAGE_STD[2])
            }
        }
        
        return Tensor.fromBlob(floatBuffer, longArrayOf(1, 3, IMAGE_SIZE.toLong(), IMAGE_SIZE.toLong()))
    }

    private fun bitmapsToBatchTensor(bitmaps: List<Bitmap>): Tensor {
        val batchSize = bitmaps.size
        val floatBuffer = Tensor.allocateFloatBuffer(batchSize * 3 * IMAGE_SIZE * IMAGE_SIZE)
        
        bitmaps.forEach { bitmap ->
            val pixels = IntArray(IMAGE_SIZE * IMAGE_SIZE)
            bitmap.getPixels(pixels, 0, IMAGE_SIZE, 0, 0, IMAGE_SIZE, IMAGE_SIZE)
            
            // Convert to CHW format with normalization
            for (y in 0 until IMAGE_SIZE) {
                for (x in 0 until IMAGE_SIZE) {
                    val pixel = pixels[y * IMAGE_SIZE + x]
                    val r = ((pixel shr 16) and 0xFF) / 255f
                    floatBuffer.put((r - IMAGE_MEAN[0]) / IMAGE_STD[0])
                }
            }
            
            for (y in 0 until IMAGE_SIZE) {
                for (x in 0 until IMAGE_SIZE) {
                    val pixel = pixels[y * IMAGE_SIZE + x]
                    val g = ((pixel shr 8) and 0xFF) / 255f
                    floatBuffer.put((g - IMAGE_MEAN[1]) / IMAGE_STD[1])
                }
            }
            
            for (y in 0 until IMAGE_SIZE) {
                for (x in 0 until IMAGE_SIZE) {
                    val pixel = pixels[y * IMAGE_SIZE + x]
                    val b = (pixel and 0xFF) / 255f
                    floatBuffer.put((b - IMAGE_MEAN[2]) / IMAGE_STD[2])
                }
            }
        }
        
        return Tensor.fromBlob(floatBuffer, longArrayOf(batchSize.toLong(), 3, IMAGE_SIZE.toLong(), IMAGE_SIZE.toLong()))
    }

    private fun normalizeEmbedding(embedding: FloatArray) {
        val norm = sqrt(embedding.sumOf { it.toDouble() * it.toDouble() }).toFloat()
        if (norm > 0f) {
            for (i in embedding.indices) {
                embedding[i] /= norm
            }
        }
    }
}

/**
 * CLIP BPE Tokenizer implementation for PyTorch Mobile.
 * 
 * This is a simplified implementation of the CLIP BPE tokenizer
 * optimized for mobile deployment.
 */
class ClipBPETokenizer(private val context: Context) {
    
    companion object {
        private const val END_OF_TEXT_TOKEN = 49407
        private const val START_OF_TEXT_TOKEN = 49406
    }
    
    private val byteEncoder: Map<Int, String> by lazy { buildByteEncoder() }
    private val bpeRanks: Map<Pair<String, String>, Int> by lazy { loadMerges() }
    private val encoder: Map<String, Int> by lazy { loadVocab() }
    private val pattern = Regex("""'s|'t|'re|'ve|'m|'ll|'d| ?\p{L}+| ?\p{N}+| ?[^\s\p{L}\p{N}]+|\s+(?!\S)|\s+""")

    fun encode(text: String, maxLength: Int = 77): LongArray {
        val tokens = mutableListOf<Int>()
        val matches = pattern.findAll(text)
        
        for (match in matches) {
            val token = match.value.toByteArray(Charsets.UTF_8)
                .joinToString("") { byteEncoder[it.toInt() and 0xFF]!! }
            val bpeTokens = bpe(token)
            tokens.addAll(bpeTokens.map { encoder[it] ?: END_OF_TEXT_TOKEN })
        }
        
        // Add end-of-text token
        tokens.add(END_OF_TEXT_TOKEN)
        
        // Pad or truncate to max length
        val result = tokens.take(maxLength).toMutableList()
        while (result.size < maxLength) {
            result.add(END_OF_TEXT_TOKEN)
        }
        
        return result.map { it.toLong() }.toLongArray()
    }

    private fun bpe(token: String): List<String> {
        var word = token.map { it.toString() + "</w>" }.toMutableList()
        if (word.size == 1) return word
        
        var pairs = getPairs(word)
        while (true) {
            val candidate = pairs.minByOrNull { bpeRanks[it] ?: Int.MAX_VALUE } ?: break
            if (!bpeRanks.containsKey(candidate)) break
            
            val (a, b) = candidate
            val newWord = mutableListOf<String>()
            var i = 0
            
            while (i < word.size) {
                val j = word.indexOf(a, i)
                if (j == -1) {
                    newWord.addAll(word.subList(i, word.size))
                    break
                }
                newWord.addAll(word.subList(i, j))
                i = j
                
                if (i < word.size - 1 && word[i] == a && word[i + 1] == b) {
                    newWord.add(a + b)
                    i += 2
                } else {
                    newWord.add(word[i])
                    i += 1
                }
            }
            
            word = newWord
            if (word.size == 1) break
            pairs = getPairs(word)
        }
        
        return word
    }

    private fun getPairs(word: List<String>): Set<Pair<String, String>> {
        return (0 until word.size - 1).map { Pair(word[it], word[it + 1]) }.toSet()
    }

    private fun loadVocab(): Map<String, Int> {
        return try {
            val json = context.assets.open("bpe_vocab.json").bufferedReader().readText()
            val map = mutableMapOf<String, Int>()
            // Simplified JSON parsing - in production, use proper JSON library
            val entries = json.split(",")
            entries.forEach { entry ->
                val parts = entry.split(":")
                if (parts.size == 2) {
                    val key = parts[0].trim().trim('"')
                    val value = parts[1].trim().trim('"', '}').toIntOrNull()
                    if (value != null) {
                        map[key] = value
                    }
                }
            }
            map
        } catch (e: Exception) {
            Logger.warn(Logger.Category.CLIP, "Failed to load BPE vocab, using fallback", emptyMap())
            // Fallback vocabulary
            mapOf(
                "</w>" to END_OF_TEXT_TOKEN,
                "<|startoftext|>" to START_OF_TEXT_TOKEN,
                "<|endoftext|>" to END_OF_TEXT_TOKEN
            )
        }
    }

    private fun loadMerges(): Map<Pair<String, String>, Int> {
        return try {
            val lines = context.assets.open("bpe_merges.txt").bufferedReader().readLines()
            lines.filter { it.isNotBlank() && !it.startsWith("#") }
                .mapIndexed { idx, line ->
                    val parts = line.split(" ")
                    if (parts.size >= 2) {
                        Pair(parts[0], parts[1]) to idx
                    } else null
                }
                .filterNotNull()
                .toMap()
        } catch (e: Exception) {
            Logger.warn(Logger.Category.CLIP, "Failed to load BPE merges, using empty map", emptyMap())
            emptyMap()
        }
    }

    private fun buildByteEncoder(): Map<Int, String> {
        val bs = (0..255).toMutableList()
        val cs = bs.map { it }
        return bs.zip(cs).associate { (b, c) -> b to String(byteArrayOf(c.toByte())) }
    }
}

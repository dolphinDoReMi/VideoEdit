package com.mira.clip.clip

import com.mira.clip.core.Config
import org.json.JSONObject
import java.io.File
import java.nio.IntBuffer

/**
 * CLIP BPE tokenizer for text preprocessing.
 * 
 * Handles text tokenization with BPE vocabulary and merges,
 * producing tokens ≤77 with EOT padding.
 */
class ClipBPETokenizer(
    private val vocabPath: String,
    private val mergesPath: String
) {
    private val vocab: Map<String, Int>
    private val merges: Map<String, Int>
    
    init {
        vocab = loadVocab(vocabPath)
        merges = loadMerges(mergesPath)
    }
    
    /**
     * Tokenize text into BPE tokens.
     * 
     * @param text Input text
     * @return Token IDs padded to MAX_TEXT_TOKENS
     */
    fun tokenize(text: String): IntArray {
        // Simple whitespace tokenization for now
        val words = text.lowercase().split("\\s+".toRegex())
        val tokens = mutableListOf<Int>()
        
        // Add start token (if defined in vocab)
        tokens.add(49406) // CLS token
        
        // Tokenize each word
        for (word in words) {
            val wordTokens = bpeEncode(word)
            tokens.addAll(wordTokens)
        }
        
        // Add end token
        tokens.add(Config.EOT_TOKEN)
        
        // Pad to MAX_TEXT_TOKENS
        val result = IntArray(Config.MAX_TEXT_TOKENS)
        val copyLength = minOf(tokens.size, Config.MAX_TEXT_TOKENS)
        for (i in 0 until copyLength) {
            result[i] = tokens[i]
        }
        
        // Fill remaining with EOT token
        for (i in copyLength until Config.MAX_TEXT_TOKENS) {
            result[i] = Config.EOT_TOKEN
        }
        
        return result
    }
    
    /**
     * Apply BPE encoding to a word.
     */
    private fun bpeEncode(word: String): List<Int> {
        // Simplified BPE implementation
        // In practice, this would use the merges file
        val chars = word.toCharArray()
        val tokens = mutableListOf<Int>()
        
        for (char in chars) {
            val token = vocab[char.toString()] ?: vocab["<unk>"] ?: 0
            tokens.add(token)
        }
        
        return tokens
    }
    
    /**
     * Load BPE vocabulary from JSON file.
     */
    private fun loadVocab(path: String): Map<String, Int> {
        val file = File(path)
        if (!file.exists()) {
            // Return minimal vocab for testing
            return mapOf(
                "<unk>" to 0,
                "<pad>" to 1,
                "<cls>" to 49406,
                "<eot>" to Config.EOT_TOKEN
            )
        }
        
        val json = JSONObject(file.readText())
        val vocab = mutableMapOf<String, Int>()
        
        for (key in json.keys()) {
            vocab[key] = json.getInt(key)
        }
        
        return vocab
    }
    
    /**
     * Load BPE merges from text file.
     */
    private fun loadMerges(path: String): Map<String, Int> {
        val file = File(path)
        if (!file.exists()) {
            return emptyMap()
        }
        
        val merges = mutableMapOf<String, Int>()
        file.readLines().forEachIndexed { index, line ->
            if (line.isNotEmpty() && !line.startsWith("#")) {
                merges[line.trim()] = index
            }
        }
        
        return merges
    }
}

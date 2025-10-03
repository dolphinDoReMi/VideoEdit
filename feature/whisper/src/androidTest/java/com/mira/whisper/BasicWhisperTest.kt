package com.mira.whisper

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mira.com.core.media.AudioResampler
import com.mira.com.feature.whisper.data.io.AudioIO
import com.mira.com.feature.whisper.engine.WhisperBridge
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Minimal Whisper functionality test for Xiaomi Pad Ultra
 */
@RunWith(AndroidJUnit4::class)
class BasicWhisperTest {

    @Test
    fun test_whisper_basic() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        
        Log.i(TAG, "Starting basic Whisper test")
        
        // Test with the sample file we pushed earlier
        val inputPath = "/sdcard/MiraWhisper/in/test_sample.mp4"
        val inputFile = File(inputPath)
        
        assertTrue("Test file should exist", inputFile.exists())
        Log.i(TAG, "Test file exists: ${inputFile.length()} bytes")
        
        // Decode audio
        val inputUri = Uri.fromFile(inputFile)
        val decoded = AudioIO.loadPcm16(ctx, inputUri)
        
        Log.i(TAG, "Decoded: ${decoded.sr}Hz, ${decoded.ch}ch, ${decoded.durationMs}ms")
        
        // Convert to mono 16kHz
        val mono = AudioResampler.downmixToMono(decoded.pcm16, decoded.ch)
        val pcm16k = AudioResampler.resampleLinear(mono, decoded.sr, 16_000)
        
        Log.i(TAG, "Converted to 16kHz mono: ${pcm16k.size} samples")
        
        // Test Whisper
        val start = System.nanoTime()
        val json = WhisperBridge.decodeJson(
            pcm16 = pcm16k,
            sampleRate = 16_000,
            modelPath = "whisper-tiny.en-q5_1",
            threads = 4,
            beam = 0,
            lang = "en",
            translate = false
        )
        val inferMs = ((System.nanoTime() - start) / 1_000_000L)
        
        Log.i(TAG, "Whisper completed in ${inferMs}ms")
        
        // Parse results
        val jsonObj = JSONObject(json)
        val segments = jsonObj.optJSONArray("segments")
        
        assertTrue("Should have segments", segments != null && segments.length() > 0)
        
        Log.i(TAG, "Found ${segments.length()} segments")
        
        // Log first few segments
        for (i in 0 until minOf(3, segments.length())) {
            val segment = segments.getJSONObject(i)
            val t0 = segment.optDouble("t0", 0.0)
            val t1 = segment.optDouble("t1", 0.0)
            val text = segment.optString("text", "").trim()
            Log.i(TAG, "Segment $i: ${t0}s-${t1}s: '$text'")
        }
        
        // Calculate RTF
        val rtf = if (decoded.durationMs > 0) inferMs.toDouble() / decoded.durationMs.toDouble() else Double.POSITIVE_INFINITY
        Log.i(TAG, "RTF: $rtf")
        
        assertTrue("RTF should be reasonable", rtf < 10.0)
        
        Log.i(TAG, "✅ Basic Whisper test passed!")
    }

    companion object {
        private const val TAG = "BasicWhisperTest"
    }
}

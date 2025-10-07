package com.mira.test

import com.mira.com.feature.whisper.engine.WhisperBridge

class SimpleWhisperTest {
    fun testJNILoading() {
        try {
            // This should trigger System.loadLibrary("whisper_jni")
            val result = WhisperBridge.detectLanguage(
                shortArrayOf(1, 2, 3, 4, 5), // dummy audio data
                16000, // sample rate
                "/sdcard/MiraWhisper/models/whisper-base.q5_1.bin", // model path
                4 // threads
            )
            println("JNI loaded successfully: $result")
        } catch (e: Exception) {
            println("JNI loading failed: ${e.message}")
        }
    }
}

package com.mira.whisper

object WhisperBridge {
    init { 
        System.loadLibrary("whisper_jni") 
    }
    
    external fun _init(modelPath: String, language: String?, translate: Boolean, threads: Int): Boolean
    external fun transcribe(pcm: ShortArray, sampleRate: Int): String
    external fun close()
    external fun setDecodingParams(useBeam: Boolean, beamSize: Int, patience: Float, temperature: Float, wordTimestamps: Boolean)
    
    fun init(modelPath: String, language: String?, translate: Boolean, threads: Int) =
        _init(modelPath, language, translate, threads)
}

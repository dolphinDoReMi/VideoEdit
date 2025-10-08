package com.mira.whisper.bus

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * Event bus for real-time communication between Whisper worker and UI
 * Provides live progress updates, logging, and status synchronization
 */
sealed interface WhisperEvent {
    data class Heartbeat(val tMs: Long): WhisperEvent
    data class Stage(val name: String): WhisperEvent      // "Decode","ASR","Serialize"
    data class Progress(val processedMs: Long, val totalMs: Long): WhisperEvent
    data class Log(val line: String): WhisperEvent
    data class Done(val ok: Boolean): WhisperEvent
    data class Error(val message: String): WhisperEvent
    data class Rtf(val rtf: Double): WhisperEvent
}

object WhisperBus {
    private val _events = MutableSharedFlow<WhisperEvent>(replay = 0, extraBufferCapacity = 512)
    val events: SharedFlow<WhisperEvent> = _events
    
    fun emit(e: WhisperEvent) { 
        _events.tryEmit(e) 
    }
    
    // Convenience methods
    fun emitHeartbeat() = emit(WhisperEvent.Heartbeat(System.currentTimeMillis()))
    fun emitStage(stage: String) = emit(WhisperEvent.Stage(stage))
    fun emitProgress(processedMs: Long, totalMs: Long) = emit(WhisperEvent.Progress(processedMs, totalMs))
    fun emitLog(line: String) = emit(WhisperEvent.Log(line))
    fun emitDone(ok: Boolean) = emit(WhisperEvent.Done(ok))
    fun emitError(message: String) = emit(WhisperEvent.Error(message))
    fun emitRtf(rtf: Double) = emit(WhisperEvent.Rtf(rtf))
}

package com.mira.videoeditor

import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

/**
 * Centralized logging system for the CLIP4Clip video-text retrieval system.
 * 
 * Provides categorized logging with different levels and structured data.
 */
object Logger {
    
    enum class Category(val tag: String) {
        CLIP("CLIP4Clip"),
        PERFORMANCE("Performance"),
        SECURITY("Security"),
        ANN("ANN"),
        DATABASE("Database"),
        WORKER("Worker"),
        UI("UI")
    }
    
    enum class Level(val priority: Int) {
        VERBOSE(Log.VERBOSE),
        DEBUG(Log.DEBUG),
        INFO(Log.INFO),
        WARN(Log.WARN),
        ERROR(Log.ERROR)
    }
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    
    /**
     * Log an info message with structured data.
     */
    fun info(category: Category, message: String, data: Map<String, Any> = emptyMap()) {
        log(Level.INFO, category, message, data)
    }
    
    /**
     * Log a warning message with structured data.
     */
    fun warn(category: Category, message: String, data: Map<String, Any> = emptyMap()) {
        log(Level.WARN, category, message, data)
    }
    
    /**
     * Log an error message with structured data and exception.
     */
    fun logError(category: Category, message: String, error: String, data: Map<String, Any> = emptyMap(), throwable: Throwable? = null) {
        val errorData = data + mapOf("error" to error)
        log(Level.ERROR, category, message, errorData)
        
        if (throwable != null) {
            Log.e(category.tag, "Exception details", throwable)
        }
    }
    
    /**
     * Log a debug message with structured data.
     */
    fun debug(category: Category, message: String, data: Map<String, Any> = emptyMap()) {
        log(Level.DEBUG, category, message, data)
    }
    
    /**
     * Log a verbose message with structured data.
     */
    fun verbose(category: Category, message: String, data: Map<String, Any> = emptyMap()) {
        log(Level.VERBOSE, category, message, data)
    }
    
    /**
     * Internal logging method that formats and outputs the log message.
     */
    private fun log(level: Level, category: Category, message: String, data: Map<String, Any>) {
        val timestamp = dateFormat.format(Date())
        val dataString = if (data.isNotEmpty()) {
            " | Data: ${data.entries.joinToString(", ") { "${it.key}=${it.value}" }}"
        } else ""
        
        val fullMessage = "[$timestamp] $message$dataString"
        
        when (level) {
            Level.VERBOSE -> Log.v(category.tag, fullMessage)
            Level.DEBUG -> Log.d(category.tag, fullMessage)
            Level.INFO -> Log.i(category.tag, fullMessage)
            Level.WARN -> Log.w(category.tag, fullMessage)
            Level.ERROR -> Log.e(category.tag, fullMessage)
        }
    }
    
    /**
     * Log performance metrics in a structured format.
     */
    fun logPerformance(category: Category, operation: String, durationMs: Long, additionalData: Map<String, Any> = emptyMap()) {
        val performanceData = additionalData + mapOf(
            "operation" to operation,
            "durationMs" to durationMs
        )
        info(category, "Performance: $operation completed in ${durationMs}ms", performanceData)
    }
    
    /**
     * Log database operation metrics.
     */
    fun logDatabaseOperation(operation: String, recordCount: Int, durationMs: Long, additionalData: Map<String, Any> = emptyMap()) {
        val dbData = additionalData + mapOf(
            "operation" to operation,
            "recordCount" to recordCount,
            "durationMs" to durationMs,
            "recordsPerMs" to (recordCount.toDouble() / durationMs.coerceAtLeast(1))
        )
        info(Category.DATABASE, "Database operation: $operation", dbData)
    }
    
    /**
     * Log security events.
     */
    fun logSecurityEvent(event: String, severity: String, additionalData: Map<String, Any> = emptyMap()) {
        val securityData = additionalData + mapOf(
            "event" to event,
            "severity" to severity
        )
        when (severity.lowercase()) {
            "critical" -> logError(Category.SECURITY, "Security event: $event", "", securityData)
            "warning" -> warn(Category.SECURITY, "Security event: $event", securityData)
            else -> info(Category.SECURITY, "Security event: $event", securityData)
        }
    }
}

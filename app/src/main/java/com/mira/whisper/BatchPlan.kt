package com.mira.whisper

import kotlinx.serialization.Serializable

/**
 * Data model for batch processing plans.
 * 
 * This represents a complete batch processing plan that can be persisted
 * to disk and retrieved by batchId, avoiding the need to pass large URI
 * lists through navigation arguments.
 */
@Serializable
data class BatchPlan(
    val batchId: String,
    val uris: List<String>,       // content:// URIs
    val modelPath: String,
    val preset: String,
    val createdAtMs: Long
)

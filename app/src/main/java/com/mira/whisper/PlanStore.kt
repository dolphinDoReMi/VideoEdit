package com.mira.whisper

import android.content.Context
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Store for persisting and retrieving BatchPlan objects to/from disk.
 * 
 * Uses a simple file-based storage system where each plan is stored
 * as a JSON file keyed by batchId.
 */
object PlanStore {
    private val json = Json { ignoreUnknownKeys = true }
    
    private fun root(ctx: Context): File {
        return File(ctx.filesDir, "plans").apply { mkdirs() }
    }
    
    /**
     * Store a batch plan to disk.
     */
    fun put(ctx: Context, plan: BatchPlan) {
        try {
            val file = File(root(ctx), "${plan.batchId}.json")
            val jsonString = json.encodeToString(BatchPlan.serializer(), plan)
            file.writeText(jsonString)
            android.util.Log.d("PlanStore", "Stored plan for batch: ${plan.batchId} with ${plan.uris.size} files")
        } catch (e: Exception) {
            android.util.Log.e("PlanStore", "Error storing plan for ${plan.batchId}: ${e.message}", e)
        }
    }
    
    /**
     * Retrieve a batch plan from disk by batchId.
     */
    fun get(ctx: Context, batchId: String): BatchPlan? {
        return try {
            val file = File(root(ctx), "$batchId.json")
            if (file.exists()) {
                val jsonString = file.readText()
                val plan = json.decodeFromString(BatchPlan.serializer(), jsonString)
                android.util.Log.d("PlanStore", "Retrieved plan for batch: $batchId with ${plan.uris.size} files")
                plan
            } else {
                android.util.Log.w("PlanStore", "No plan found for batch: $batchId")
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("PlanStore", "Error retrieving plan for $batchId: ${e.message}", e)
            null
        }
    }
    
    /**
     * Remove a batch plan from disk.
     */
    fun remove(ctx: Context, batchId: String) {
        try {
            val file = File(root(ctx), "$batchId.json")
            if (file.exists()) {
                file.delete()
                android.util.Log.d("PlanStore", "Removed plan for batch: $batchId")
            }
        } catch (e: Exception) {
            android.util.Log.e("PlanStore", "Error removing plan for $batchId: ${e.message}", e)
        }
    }
    
    /**
     * List all stored batch plans.
     */
    fun list(ctx: Context): List<BatchPlan> {
        return try {
            val plansDir = root(ctx)
            if (plansDir.exists()) {
                plansDir.listFiles { file ->
                    file.isFile && file.name.endsWith(".json")
                }?.mapNotNull { file ->
                    try {
                        val jsonString = file.readText()
                        json.decodeFromString(BatchPlan.serializer(), jsonString)
                    } catch (e: Exception) {
                        android.util.Log.w("PlanStore", "Error parsing plan file ${file.name}: ${e.message}")
                        null
                    }
                } ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            android.util.Log.e("PlanStore", "Error listing plans: ${e.message}", e)
            emptyList()
        }
    }
}

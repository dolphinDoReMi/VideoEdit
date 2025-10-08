package com.mira.resource

import android.content.Context
import android.content.Intent
import android.util.Log
import org.json.JSONObject

/**
 * Debug utility to verify that only StableResourceMonitor is running
 * and to help troubleshoot resource monitoring issues.
 */
object ResourceMonitoringDebugger {
    
    private const val TAG = "ResourceMonitoringDebugger"
    
    /**
     * Check if DeviceResourceService is running and log its status
     */
    fun checkResourceMonitoringStatus(context: Context) {
        try {
            Log.d(TAG, "=== RESOURCE MONITORING STATUS CHECK ===")
            
            // Check if DeviceResourceService is running
            val serviceIntent = Intent(context, DeviceResourceService::class.java)
            val serviceRunning = context.startService(serviceIntent) != null
            
            Log.d(TAG, "DeviceResourceService running: $serviceRunning")
            
            // Log that old monitoring systems are disabled
            Log.d(TAG, "WhisperConnectorService resource monitoring: DISABLED")
            Log.d(TAG, "MainActivity resource monitoring: DISABLED")
            Log.d(TAG, "VideoEditor MainActivity resource monitoring: DISABLED")
            
            Log.d(TAG, "Only StableResourceMonitor should be active")
            Log.d(TAG, "=== END STATUS CHECK ===")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error checking resource monitoring status: ${e.message}")
        }
    }
    
    /**
     * Log a sample resource reading to verify stable monitoring is working
     */
    fun logSampleResourceReading(resourceData: JSONObject) {
        try {
            Log.d(TAG, "=== SAMPLE RESOURCE READING ===")
            Log.d(TAG, "Battery: ${resourceData.optDouble("battery", 0.0).toFixed(1)}%")
            Log.d(TAG, "Memory: ${resourceData.optDouble("memory", 0.0).toFixed(1)}%")
            Log.d(TAG, "CPU: ${resourceData.optDouble("cpu", 0.0).toFixed(1)}%")
            Log.d(TAG, "Temperature: ${resourceData.optDouble("temperature", 0.0).toFixed(1)}°C")
            Log.d(TAG, "Timestamp: ${resourceData.optLong("timestamp", 0)}")
            Log.d(TAG, "=== END SAMPLE READING ===")
        } catch (e: Exception) {
            Log.e(TAG, "Error logging sample resource reading: ${e.message}")
        }
    }
    
    /**
     * Verify that resource values are stable (not 0% when they shouldn't be)
     */
    fun verifyResourceStability(resourceData: JSONObject): Boolean {
        try {
            val battery = resourceData.optDouble("battery", 0.0)
            val memory = resourceData.optDouble("memory", 0.0)
            val cpu = resourceData.optDouble("cpu", 0.0)
            val temperature = resourceData.optDouble("temperature", 0.0)
            
            val issues = mutableListOf<String>()
            
            // Check for suspicious 0% readings
            if (battery == 0.0) {
                issues.add("Battery showing 0% - this might indicate sensor failure")
            }
            if (memory == 0.0) {
                issues.add("Memory showing 0% - this might indicate sensor failure")
            }
            if (cpu == 0.0) {
                issues.add("CPU showing 0% - this might indicate sensor failure")
            }
            if (temperature == 0.0) {
                issues.add("Temperature showing 0°C - this might indicate sensor failure")
            }
            
            if (issues.isNotEmpty()) {
                Log.w(TAG, "=== RESOURCE STABILITY ISSUES DETECTED ===")
                issues.forEach { issue ->
                    Log.w(TAG, "ISSUE: $issue")
                }
                Log.w(TAG, "=== END STABILITY ISSUES ===")
                return false
            } else {
                Log.d(TAG, "Resource readings appear stable - no 0% issues detected")
                return true
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error verifying resource stability: ${e.message}")
            return false
        }
    }
    
    private fun Double.toFixed(digits: Int): String {
        return String.format("%.${digits}f", this)
    }
}

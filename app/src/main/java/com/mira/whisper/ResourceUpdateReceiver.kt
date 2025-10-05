package com.mira.whisper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import org.json.JSONObject

/**
 * Broadcast receiver for receiving resource updates from DeviceResourceService.
 * UI components can register this receiver to get real-time resource data.
 */
class ResourceUpdateReceiver(
    private val onResourceUpdate: (JSONObject) -> Unit,
    private val onCpuUpdate: (Double) -> Unit = {},
    private val onMemoryUpdate: (Double) -> Unit = {},
    private val onBatteryUpdate: (Int) -> Unit = {},
    private val onTemperatureUpdate: (Double) -> Unit = {}
) : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "ResourceUpdateReceiver"
    }
    
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null) return
        
        try {
            when (intent.action) {
                DeviceResourceService.ACTION_RESOURCE_UPDATE -> {
                    val resourceDataStr = intent.getStringExtra(DeviceResourceService.EXTRA_RESOURCE_DATA)
                    if (resourceDataStr != null) {
                        val resourceData = JSONObject(resourceDataStr)
                        onResourceUpdate(resourceData)
                        Log.d(TAG, "Resource update received")
                    }
                }
                
                DeviceResourceService.ACTION_CPU_UPDATE -> {
                    val cpuUsage = intent.getDoubleExtra(DeviceResourceService.EXTRA_CPU_USAGE, 0.0)
                    onCpuUpdate(cpuUsage)
                    Log.d(TAG, "CPU update: ${cpuUsage.toFixed(2)}%")
                }
                
                DeviceResourceService.ACTION_MEMORY_UPDATE -> {
                    val memoryUsage = intent.getDoubleExtra(DeviceResourceService.EXTRA_MEMORY_USAGE, 0.0)
                    onMemoryUpdate(memoryUsage)
                    Log.d(TAG, "Memory update: ${memoryUsage.toFixed(2)}%")
                }
                
                DeviceResourceService.ACTION_BATTERY_UPDATE -> {
                    val batteryLevel = intent.getIntExtra(DeviceResourceService.EXTRA_BATTERY_LEVEL, 0)
                    onBatteryUpdate(batteryLevel)
                    Log.d(TAG, "Battery update: $batteryLevel%")
                }
                
                DeviceResourceService.ACTION_TEMPERATURE_UPDATE -> {
                    val temperature = intent.getDoubleExtra(DeviceResourceService.EXTRA_TEMPERATURE, 0.0)
                    onTemperatureUpdate(temperature)
                    Log.d(TAG, "Temperature update: ${temperature.toFixed(1)}°C")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing resource update: ${e.message}")
        }
    }
    
    /**
     * Register this receiver with the context.
     * Call this in your Activity/Service onCreate().
     */
    fun register(context: Context) {
        val filter = IntentFilter().apply {
            addAction(DeviceResourceService.ACTION_RESOURCE_UPDATE)
            addAction(DeviceResourceService.ACTION_CPU_UPDATE)
            addAction(DeviceResourceService.ACTION_MEMORY_UPDATE)
            addAction(DeviceResourceService.ACTION_BATTERY_UPDATE)
            addAction(DeviceResourceService.ACTION_TEMPERATURE_UPDATE)
        }
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(this, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(this, filter)
        }
        
        Log.d(TAG, "Resource update receiver registered")
    }
    
    /**
     * Unregister this receiver.
     * Call this in your Activity/Service onDestroy().
     */
    fun unregister(context: Context) {
        try {
            context.unregisterReceiver(this)
            Log.d(TAG, "Resource update receiver unregistered")
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering receiver: ${e.message}")
        }
    }
    
    private fun Double.toFixed(digits: Int): String {
        return String.format("%.${digits}f", this)
    }
}

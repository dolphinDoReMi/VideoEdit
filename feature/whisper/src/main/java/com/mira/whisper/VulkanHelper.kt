package com.mira.whisper

import android.util.Log

class VulkanHelper {
    companion object {
        private const val TAG = "VulkanHelper"
        
        init {
            try {
                System.loadLibrary("whisper_jni")
                Log.i(TAG, "VulkanHelper native library loaded")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "Failed to load VulkanHelper native library", e)
            }
        }
    }
    
    external fun setVulkanDevice(deviceIndex: Int)
    external fun initializeVulkan(): String
    external fun isVulkanAvailable(): Boolean
    external fun getVulkanDeviceCount(): Int
    external fun getVulkanDeviceName(deviceIndex: Int): String
    external fun enableValidationLayers()
    external fun disableValidationLayers()
    
    fun initialize(): VulkanInfo {
        val initResult = initializeVulkan()
        val deviceCount = getVulkanDeviceCount()
        val deviceName = if (deviceCount > 0) getVulkanDeviceName(0) else "No devices"
        
        Log.i(TAG, "Vulkan initialization: $initResult")
        Log.i(TAG, "Device count: $deviceCount")
        Log.i(TAG, "Device name: $deviceName")
        
        return VulkanInfo(
            available = isVulkanAvailable(),
            deviceCount = deviceCount,
            deviceName = deviceName,
            initResult = initResult
        )
    }
}

data class VulkanInfo(
    val available: Boolean,
    val deviceCount: Int,
    val deviceName: String,
    val initResult: String
)

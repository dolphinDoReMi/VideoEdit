package com.mira.resource

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Debug
import android.os.Handler
import android.os.Looper
import android.util.Log
import org.json.JSONObject
import java.io.BufferedReader
import java.io.FileReader
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Stable resource monitoring system with fallback handling and smoothing.
 * 
 * Features:
 * - Multiple fallback methods for each metric
 * - Moving average smoothing to prevent wild fluctuations
 * - Outlier detection and filtering
 * - Last known value preservation when sensors fail
 * - Xiaomi-specific optimizations
 */
class StableResourceMonitor(private val context: Context) {
    
    companion object {
        private const val TAG = "StableResourceMonitor"
        private const val UPDATE_INTERVAL_MS = 1000L // Reduced for more frequent updates
        private const val HISTORY_SIZE = 20 // Increased for better smoothing
        private const val OUTLIER_THRESHOLD = 1.5 // Reduced for less aggressive filtering
        private const val MIN_VALID_TEMPERATURE = 10.0
        private const val MAX_VALID_TEMPERATURE = 80.0
        private const val MIN_VALID_BATTERY = 0
        private const val MAX_VALID_BATTERY = 100
        private const val MAX_CHANGE_PER_UPDATE = 5.0 // Increased for more responsive readings
    }
    
    private val isRunning = AtomicBoolean(false)
    private val handler = Handler(Looper.getMainLooper())
    
    // Resource history for smoothing
    private val batteryHistory = mutableListOf<Double>()
    private val memoryHistory = mutableListOf<Double>()
    private val cpuHistory = mutableListOf<Double>()
    private val temperatureHistory = mutableListOf<Double>()
    
    // Last known valid values (fallback when sensors fail)
    private var lastValidBattery = 50.0
    private var lastValidMemory = 0.0
    private var lastValidCpu = 0.0
    private var lastValidTemperature = 25.0
    
    // CPU monitoring state
    private var lastCpuTotal = 0L
    private var lastCpuIdle = 0L
    private var cpuInitialized = false
    
    // Callbacks
    private var onResourceUpdate: ((JSONObject) -> Unit)? = null
    
    private val updateRunnable = object : Runnable {
        override fun run() {
            if (isRunning.get()) {
                collectAndProcessResources()
                handler.postDelayed(this, UPDATE_INTERVAL_MS)
            }
        }
    }
    
    fun startMonitoring(callback: (JSONObject) -> Unit) {
        if (isRunning.get()) return
        
        onResourceUpdate = callback
        isRunning.set(true)
        handler.post(updateRunnable)
        Log.d(TAG, "Stable resource monitoring started")
        
        // Debug: Check monitoring status
        ResourceMonitoringDebugger.checkResourceMonitoringStatus(context)
    }
    
    fun stopMonitoring() {
        if (!isRunning.get()) return
        
        isRunning.set(false)
        handler.removeCallbacks(updateRunnable)
        Log.d(TAG, "Stable resource monitoring stopped")
    }
    
    private fun collectAndProcessResources() {
        try {
            val resourceData = JSONObject()
            
            // Collect raw values
            val rawBattery = getBatteryLevel()
            val rawMemory = getMemoryUsage()
            val rawCpu = getCpuUsage()
            val rawTemperature = getTemperature()
            
            // Process and smooth values
            val smoothedBattery = processBatteryValue(rawBattery)
            val smoothedMemory = processMemoryValue(rawMemory)
            val smoothedCpu = processCpuValue(rawCpu)
            val smoothedTemperature = processTemperatureValue(rawTemperature)
            
            // Build resource data
            resourceData.put("battery", smoothedBattery)
            resourceData.put("memory", smoothedMemory)
            resourceData.put("cpu", smoothedCpu)
            resourceData.put("temperature", smoothedTemperature)
            resourceData.put("timestamp", System.currentTimeMillis())
            
            // Add detailed information
            resourceData.put("batteryDetails", getBatteryDetails())
            resourceData.put("threadInfo", getThreadInfo())
            resourceData.put("gpuInfo", getGpuInfo())
            
            // Update callbacks
            onResourceUpdate?.invoke(resourceData)
            
            // Debug: Log sample reading and verify stability
            ResourceMonitoringDebugger.logSampleResourceReading(resourceData)
            ResourceMonitoringDebugger.verifyResourceStability(resourceData)
            
            Log.d(TAG, "Resources: Battery=${smoothedBattery.toFixed(1)}%, Memory=${smoothedMemory.toFixed(1)}%, CPU=${smoothedCpu.toFixed(1)}%, Temp=${smoothedTemperature.toFixed(1)}°C")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error collecting resources: ${e.message}")
        }
    }
    
    private fun getBatteryLevel(): Double {
        return try {
            // Method 1: BatteryManager (most reliable)
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val level = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            
            if (level in MIN_VALID_BATTERY..MAX_VALID_BATTERY) {
                Log.d(TAG, "Battery (BatteryManager): $level%")
                return level.toDouble()
            }
            
            // Method 2: Simple fallback - use last valid value
            Log.d(TAG, "BatteryManager failed, using last valid: $lastValidBattery%")
            
            Log.w(TAG, "Battery monitoring failed, using last valid: $lastValidBattery%")
            lastValidBattery
            
        } catch (e: Exception) {
            Log.e(TAG, "Battery monitoring error: ${e.message}")
            lastValidBattery
        }
    }
    
    private fun getMemoryUsage(): Double {
        return try {
            // Method 1: ActivityManager (most accurate for system memory)
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)
            
            val totalMem = memoryInfo.totalMem
            val availableMem = memoryInfo.availMem
            val usedMem = totalMem - availableMem
            
            val memoryPercent = if (totalMem > 0) {
                (usedMem.toDouble() / totalMem.toDouble()) * 100.0
            } else {
                0.0
            }
            
            if (memoryPercent >= 0 && memoryPercent <= 100) {
                Log.d(TAG, "Memory (ActivityManager): ${memoryPercent.toFixed(1)}% (${usedMem / (1024 * 1024)}MB/${totalMem / (1024 * 1024)}MB)")
                return memoryPercent
            }
            
            // Method 2: /proc/meminfo fallback
            try {
                val memInfoFile = FileReader("/proc/meminfo")
                val memInfoReader = BufferedReader(memInfoFile)
                var totalMemKB = 0L
                var availableMemKB = 0L
                
                var line: String?
                while (memInfoReader.readLine().also { line = it } != null) {
                    val currentLine = line
                    when {
                        currentLine?.startsWith("MemTotal:") == true -> {
                            totalMemKB = currentLine.substringAfter("MemTotal:").trim()
                                .replace("kB", "").toLongOrNull() ?: 0L
                        }
                        currentLine?.startsWith("MemAvailable:") == true -> {
                            availableMemKB = currentLine.substringAfter("MemAvailable:").trim()
                                .replace("kB", "").toLongOrNull() ?: 0L
                        }
                    }
                }
                memInfoReader.close()
                
                val usedMemKB = totalMemKB - availableMemKB
                val memPercent = if (totalMemKB > 0) {
                    (usedMemKB.toDouble() / totalMemKB.toDouble()) * 100.0
                } else {
                    0.0
                }
                
                if (memPercent >= 0 && memPercent <= 100) {
                    Log.d(TAG, "Memory (/proc/meminfo): ${memPercent.toFixed(1)}%")
                    return memPercent
                }
            } catch (e: Exception) {
                Log.d(TAG, "/proc/meminfo fallback failed: ${e.message}")
            }
            
            // Method 3: Debug.MemoryInfo (app-specific)
            try {
                val memoryInfo = Debug.MemoryInfo()
                Debug.getMemoryInfo(memoryInfo)
                val pssKB = memoryInfo.totalPss.toLong()
                val pssMB = pssKB / 1024
                
                // Estimate system memory usage based on app memory
                val estimatedSystemUsage = (pssMB * 10).coerceAtMost(100L).toDouble() // Rough estimate
                Log.d(TAG, "Memory (Debug): ${String.format("%.1f", estimatedSystemUsage)}% (app: ${pssMB}MB)")
                return estimatedSystemUsage
            } catch (e: Exception) {
                Log.d(TAG, "Debug.MemoryInfo fallback failed: ${e.message}")
            }
            
            Log.w(TAG, "Memory monitoring failed, using last valid: $lastValidMemory%")
            lastValidMemory
            
        } catch (e: Exception) {
            Log.e(TAG, "Memory monitoring error: ${e.message}")
            lastValidMemory
        }
    }
    
    private fun getCpuUsage(): Double {
        return try {
            // Method 1: /proc/stat with proper calculation
            val process = Runtime.getRuntime().exec("cat /proc/stat")
            val reader = process.inputStream.bufferedReader()
            val firstLine = reader.readLine()
            reader.close()
            process.waitFor()
            
            if (firstLine != null && firstLine.startsWith("cpu ")) {
                val parts = firstLine.split("\\s+".toRegex())
                if (parts.size >= 8) {
                    val user = parts[1].toLong()
                    val nice = parts[2].toLong()
                    val system = parts[3].toLong()
                    val idle = parts[4].toLong()
                    val iowait = parts[5].toLong()
                    val irq = parts[6].toLong()
                    val softirq = parts[7].toLong()
                    
                    val currentTotal = user + nice + system + idle + iowait + irq + softirq
                    val currentIdle = idle + iowait
                    
                    if (cpuInitialized && lastCpuTotal > 0) {
                        val totalDiff = currentTotal - lastCpuTotal
                        val idleDiff = currentIdle - lastCpuIdle
                        val usedDiff = totalDiff - idleDiff
                        
                        val cpuPercent = if (totalDiff > 0) {
                            (usedDiff.toDouble() / totalDiff.toDouble()) * 100.0
                        } else {
                            0.0
                        }
                        
                        lastCpuTotal = currentTotal
                        lastCpuIdle = currentIdle
                        
                        Log.d(TAG, "CPU (/proc/stat): ${cpuPercent.toFixed(2)}%")
                        return cpuPercent.coerceIn(0.0, 100.0)
                    } else {
                        // First run, store values
                        lastCpuTotal = currentTotal
                        lastCpuIdle = currentIdle
                        cpuInitialized = true
                        return 0.0
                    }
                }
            }
            
            // Method 2: /proc/loadavg fallback
            try {
                val loadavgProcess = Runtime.getRuntime().exec("cat /proc/loadavg")
                val loadavgReader = loadavgProcess.inputStream.bufferedReader()
                val loadavgLine = loadavgReader.readLine()
                loadavgReader.close()
                loadavgProcess.waitFor()
                
                if (loadavgLine != null) {
                    val loadavg = loadavgLine.split("\\s+".toRegex())[0].toDoubleOrNull() ?: 0.0
                    val cpuCores = Runtime.getRuntime().availableProcessors()
                    val cpuPercent = (loadavg / cpuCores) * 100.0
                    Log.d(TAG, "CPU (/proc/loadavg): ${cpuPercent.toFixed(2)}%")
                    return cpuPercent.coerceIn(0.0, 100.0)
                }
            } catch (e: Exception) {
                Log.d(TAG, "/proc/loadavg fallback failed: ${e.message}")
            }
            
            Log.w(TAG, "CPU monitoring failed, using last valid: $lastValidCpu%")
            lastValidCpu
            
        } catch (e: Exception) {
            Log.e(TAG, "CPU monitoring error: ${e.message}")
            lastValidCpu
        }
    }
    
    private fun getTemperature(): Double {
        return try {
            // Method 1: Xiaomi thermal zones (most accurate for Xiaomi devices)
            val thermalZones = listOf(
                "/sys/class/thermal/thermal_zone0/temp",
                "/sys/class/thermal/thermal_zone1/temp",
                "/sys/class/thermal/thermal_zone2/temp",
                "/sys/class/thermal/thermal_zone3/temp",
                "/sys/class/thermal/thermal_zone4/temp",
                "/sys/class/thermal/thermal_zone5/temp",
                "/sys/class/thermal/thermal_zone6/temp",
                "/sys/class/thermal/thermal_zone7/temp",
                "/sys/class/thermal/thermal_zone8/temp",
                "/sys/class/thermal/thermal_zone9/temp"
            )
            
            var totalTemp = 0.0
            var validReadings = 0
            
            for (zone in thermalZones) {
                try {
                    val process = Runtime.getRuntime().exec("cat $zone")
                    val reader = process.inputStream.bufferedReader()
                    val tempStr = reader.readLine()
                    reader.close()
                    process.waitFor()
                    
                    if (tempStr != null && tempStr.isNotEmpty()) {
                        val temp = tempStr.trim().toDoubleOrNull()
                        if (temp != null && temp > 0) {
                            val tempCelsius = if (temp > 1000) temp / 1000.0 else temp
                            if (tempCelsius in MIN_VALID_TEMPERATURE..MAX_VALID_TEMPERATURE) {
                                totalTemp += tempCelsius
                                validReadings++
                                Log.d(TAG, "Thermal zone $zone: ${tempCelsius.toFixed(1)}°C")
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Zone not accessible, continue to next
                }
            }
            
            if (validReadings > 0) {
                val avgTemp = totalTemp / validReadings
                Log.d(TAG, "Temperature (thermal zones): ${avgTemp.toFixed(1)}°C from $validReadings zones")
                return avgTemp
            }
            
            // Method 2: Battery temperature fallback
            try {
                val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                if (intent != null) {
                    val batteryTemp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
                    if (batteryTemp > 0) {
                        val tempCelsius = batteryTemp / 10.0
                        if (tempCelsius in MIN_VALID_TEMPERATURE..MAX_VALID_TEMPERATURE) {
                            Log.d(TAG, "Temperature (battery): ${tempCelsius.toFixed(1)}°C")
                            return tempCelsius
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "Battery temperature fallback failed: ${e.message}")
            }
            
            // Method 3: Hardware monitor fallback
            try {
                val process = Runtime.getRuntime().exec("cat /sys/class/hwmon/hwmon*/temp*_input 2>/dev/null | head -1")
                val reader = process.inputStream.bufferedReader()
                val tempStr = reader.readLine()
                reader.close()
                process.waitFor()
                
                if (tempStr != null && tempStr.isNotEmpty()) {
                    val temp = tempStr.trim().toDoubleOrNull()
                    if (temp != null && temp > 0) {
                        val tempCelsius = if (temp > 1000) temp / 1000.0 else temp
                        if (tempCelsius in MIN_VALID_TEMPERATURE..MAX_VALID_TEMPERATURE) {
                            Log.d(TAG, "Temperature (hwmon): ${tempCelsius.toFixed(1)}°C")
                            return tempCelsius
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "Hardware monitor fallback failed: ${e.message}")
            }
            
            Log.w(TAG, "Temperature monitoring failed, using last valid: $lastValidTemperature°C")
            lastValidTemperature
            
        } catch (e: Exception) {
            Log.e(TAG, "Temperature monitoring error: ${e.message}")
            lastValidTemperature
        }
    }
    
    // Processing and smoothing functions
    private fun processBatteryValue(rawValue: Double): Double {
        val processed = validateAndSmooth(batteryHistory, rawValue, lastValidBattery, MIN_VALID_BATTERY.toDouble(), MAX_VALID_BATTERY.toDouble())
        lastValidBattery = processed
        return processed
    }
    
    private fun processMemoryValue(rawValue: Double): Double {
        val processed = validateAndSmooth(memoryHistory, rawValue, lastValidMemory, 0.0, 100.0)
        lastValidMemory = processed
        return processed
    }
    
    private fun processCpuValue(rawValue: Double): Double {
        val processed = validateAndSmooth(cpuHistory, rawValue, lastValidCpu, 0.0, 100.0)
        lastValidCpu = processed
        return processed
    }
    
    private fun processTemperatureValue(rawValue: Double): Double {
        val processed = validateAndSmooth(temperatureHistory, rawValue, lastValidTemperature, MIN_VALID_TEMPERATURE, MAX_VALID_TEMPERATURE)
        lastValidTemperature = processed
        return processed
    }
    
    private fun validateAndSmooth(
        history: MutableList<Double>,
        rawValue: Double,
        lastValid: Double,
        minValid: Double,
        maxValid: Double
    ): Double {
        // Validate raw value
        val validatedValue = if (rawValue in minValid..maxValid) {
            rawValue
        } else {
            Log.w(TAG, "Invalid raw value $rawValue, using last valid $lastValid")
            lastValid
        }
        
        // Add to history
        history.add(validatedValue)
        if (history.size > HISTORY_SIZE) {
            history.removeAt(0)
        }
        
        // Use exponential smoothing with gradual change limiting
        val smoothedValue = if (history.size >= 3) {
            // Calculate exponential moving average (more recent values have higher weight)
            val alpha = 0.2 // Reduced for more smoothing (0.1 = very smooth, 0.5 = less smooth)
            val exponentialAverage = calculateExponentialMovingAverage(history, alpha)
            
            // Apply median filter for additional smoothing
            val medianFiltered = applyMedianFilter(history.takeLast(5)) // Use last 5 values for median
            
            // Combine exponential average with median filter (more weight on median for stability)
            val combinedValue = (exponentialAverage * 0.5) + (medianFiltered * 0.5)
            
            // Apply gradual change limiting to prevent jumps
            val limitedValue = limitGradualChange(combinedValue, lastValid, MAX_CHANGE_PER_UPDATE)
            
            Log.d(TAG, "Raw: ${validatedValue.toFixed(1)}, ExpAvg: ${exponentialAverage.toFixed(1)}, Median: ${medianFiltered.toFixed(1)}, Combined: ${combinedValue.toFixed(1)}, Limited: ${limitedValue.toFixed(1)}")
            limitedValue
        } else {
            // Not enough history yet, use simple average
            val simpleAverage = history.average()
            val limitedValue = limitGradualChange(simpleAverage, lastValid, MAX_CHANGE_PER_UPDATE)
            Log.d(TAG, "Raw: ${validatedValue.toFixed(1)}, SimpleAvg: ${simpleAverage.toFixed(1)}, Limited: ${limitedValue.toFixed(1)}")
            limitedValue
        }
        
        return smoothedValue.coerceIn(minValid, maxValid)
    }
    
    private fun calculateExponentialMovingAverage(values: List<Double>, alpha: Double): Double {
        if (values.isEmpty()) return 0.0
        if (values.size == 1) return values[0]
        
        // Use weighted average with higher weight for more recent values
        var weightedSum = 0.0
        var totalWeight = 0.0
        
        for (i in values.indices) {
            val weight = alpha.pow(values.size - 1 - i)
            weightedSum += values[i] * weight
            totalWeight += weight
        }
        
        return if (totalWeight > 0) weightedSum / totalWeight else values.average()
    }
    
    private fun applyMedianFilter(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        if (values.size == 1) return values[0]
        
        val sortedValues = values.sorted()
        val middle = sortedValues.size / 2
        
        return if (sortedValues.size % 2 == 0) {
            // Even number of values, return average of two middle values
            (sortedValues[middle - 1] + sortedValues[middle]) / 2.0
        } else {
            // Odd number of values, return middle value
            sortedValues[middle]
        }
    }
    
    private fun limitGradualChange(newValue: Double, lastValue: Double, maxChange: Double): Double {
        val difference = newValue - lastValue
        return when {
            difference > maxChange -> lastValue + maxChange
            difference < -maxChange -> lastValue - maxChange
            else -> newValue
        }
    }
    
    private fun getBatteryDetails(): String {
        return try {
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val level = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            
            val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            if (intent != null) {
                val temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
                val voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                
                val tempStr = if (temperature > 0) "${temperature / 10.0}°C" else "N/A"
                val voltStr = if (voltage > 0) "${voltage / 1000.0}V" else "N/A"
                val statusStr = when (status) {
                    BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
                    BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
                    BatteryManager.BATTERY_STATUS_FULL -> "Full"
                    BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not Charging"
                    else -> "Unknown"
                }
                
                "Level: $level%, Temp: $tempStr, Voltage: $voltStr, Status: $statusStr"
            } else {
                "Level: $level%, Status: Unknown"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Battery details error: ${e.message}")
            "Level: N/A, Status: Error"
        }
    }
    
    private fun getThreadInfo(): String {
        return try {
            val process = Runtime.getRuntime().exec("cat /proc/self/status")
            val reader = process.inputStream.bufferedReader()
            var line: String?
            var threadCount = 0
            
            while (reader.readLine().also { line = it } != null) {
                val currentLine = line
                if (currentLine?.startsWith("Threads:") == true) {
                    threadCount = currentLine.substringAfter("Threads:").trim().toIntOrNull() ?: 0
                    break
                }
            }
            reader.close()
            process.waitFor()
            
            val availableProcessors = Runtime.getRuntime().availableProcessors()
            "Threads: $threadCount, CPUs: $availableProcessors"
        } catch (e: Exception) {
            Log.e(TAG, "Thread info error: ${e.message}")
            "Threads: N/A, CPUs: N/A"
        }
    }
    
    private fun getGpuInfo(): String {
        return try {
            val process = Runtime.getRuntime().exec("cat /proc/gpuinfo")
            val reader = process.inputStream.bufferedReader()
            val gpuInfo = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val currentLine = line
                if (currentLine != null && currentLine.isNotEmpty()) {
                    gpuInfo.append(currentLine).append(" | ")
                }
            }
            reader.close()
            process.waitFor()
            
            val result = gpuInfo.toString().take(100)
            if (result.isNotEmpty()) {
                result
            } else {
                "GPU: Not accessible"
            }
        } catch (e: Exception) {
            Log.d(TAG, "GPU info not accessible: ${e.message}")
            "GPU: Not accessible"
        }
    }
    
    private fun Double.toFixed(digits: Int): String {
        return String.format("%.${digits}f", this)
    }
}

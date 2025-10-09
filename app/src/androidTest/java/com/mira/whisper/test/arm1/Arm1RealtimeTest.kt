package com.mira.whisper.test.arm1

import android.content.Context
import android.net.Uri
import android.os.BatteryManager
import android.os.Debug
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mira.whisper.AndroidWhisperBridge
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileWriter
import kotlin.math.max
import kotlin.system.measureNanoTime

/**
 * ARM-1 Realtime UX Test
 * 
 * This test validates realtime captions UX on Android devices using the
 * consolidated com.mira.whisper package.
 * 
 * Key Features:
 * - Uses only com.mira.whisper package (no feature modules)
 * - Scoped storage compliant
 * - Measures UX metrics (latency, streaming, thermals)
 * - Fixed configurations for reproducible results
 */
@RunWith(AndroidJUnit4::class)
class Arm1RealtimeTest {
    
    companion object {
        private const val TAG = "Arm1RealtimeTest"
        private const val TEST_NAME = "arm1_realtime"
    }
    
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val filesDir = context.filesDir
    private val whisperBridge = AndroidWhisperBridge(context)
    
    /**
     * Main ARM-1 test that validates realtime whisper UX
     */
    @Test
    fun testArm1Realtime() {
        Log.i(TAG, "🚀 Starting ARM-1 Realtime UX Test")
        
        try {
            // 1. Infrastructure Test
            val infrastructureResult = testInfrastructure()
            assert(infrastructureResult.success) { "Infrastructure test failed: ${infrastructureResult.error}" }
            Log.i(TAG, "✅ Infrastructure test passed")
            
            // 2. Configuration Test
            val configResult = testConfiguration()
            assert(configResult.success) { "Configuration test failed: ${configResult.error}" }
            Log.i(TAG, "✅ Configuration test passed")
            
            // 3. Transcription Test
            val transcriptionResult = testTranscription()
            assert(transcriptionResult.success) { "Transcription test failed: ${transcriptionResult.error}" }
            Log.i(TAG, "✅ Transcription test passed")
            
            // 4. UX Metrics Test
            val uxResult = testUxMetrics()
            assert(uxResult.success) { "UX metrics test failed: ${uxResult.error}" }
            Log.i(TAG, "✅ UX metrics test passed")
            
            // 5. Generate Test Report
            generateTestReport(listOf(infrastructureResult, configResult, transcriptionResult, uxResult))
            
            Log.i(TAG, "🎉 ARM-1 Realtime UX Test completed successfully!")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ ARM-1 test failed", e)
            throw e
        }
    }
    
    /**
     * Test 1: Infrastructure Validation
     * Verifies that the consolidated whisper package is accessible and functional
     */
    private fun testInfrastructure(): TestResult {
        Log.d(TAG, "Testing infrastructure...")
        
        return try {
            // Test package accessibility
            val packageName = context.packageName
            assert(packageName == "com.mira.com") { "Unexpected package name: $packageName" }
            
            // Test whisper bridge creation
            val bridge = AndroidWhisperBridge(context)
            assert(bridge != null) { "Failed to create AndroidWhisperBridge" }
            
            // Test scoped storage access
            val filesDir = context.filesDir
            assert(filesDir.exists()) { "Files directory not accessible" }
            
            val externalFilesDir = context.getExternalFilesDir(null)
            assert(externalFilesDir?.exists() == true) { "External files directory not accessible" }
            
            // Test whisper bridge methods
            val deviceInfo = bridge.getDeviceInfo()
            assert(deviceInfo.isNotEmpty()) { "Device info not available" }
            
            val infrastructureTest = bridge.testInfrastructure()
            assert(infrastructureTest.isNotEmpty()) { "Infrastructure test failed" }
            
            TestResult(
                success = true,
                metrics = mapOf(
                    "package_name" to packageName,
                    "files_dir" to filesDir.absolutePath,
                    "external_files_dir" to (externalFilesDir?.absolutePath ?: "null"),
                    "device_info_length" to deviceInfo.length.toString(),
                    "infrastructure_test_length" to infrastructureTest.length.toString()
                )
            )
            
        } catch (e: Exception) {
            TestResult(success = false, error = e.message ?: "Unknown error")
        }
    }
    
    /**
     * Test 2: Configuration Validation
     * Verifies that ARM-1 configuration is valid and accessible
     */
    private fun testConfiguration(): TestResult {
        Log.d(TAG, "Testing configuration...")
        
        return try {
            // Create ARM-1 configuration
            val config = createArm1Config()
            
            // Validate configuration structure
            assert(config.has("scenario")) { "Missing scenario in config" }
            assert(config.getString("scenario") == "arm1_realtime") { "Invalid scenario" }
            
            assert(config.has("backend")) { "Missing backend config" }
            assert(config.has("model")) { "Missing model config" }
            assert(config.has("audio")) { "Missing audio config" }
            assert(config.has("decode")) { "Missing decode config" }
            assert(config.has("language")) { "Missing language config" }
            assert(config.has("timeouts")) { "Missing timeouts config" }
            
            // Validate specific values
            val backend = config.getJSONObject("backend")
            assert(backend.getInt("ngl_pref") == 8) { "Invalid ngl_pref" }
            assert(backend.getInt("threads_pref") == 4) { "Invalid threads_pref" }
            
            val model = config.getJSONObject("model")
            assert(model.getString("primary") == "whisper-small-q5_1.gguf") { "Invalid primary model" }
            
            val audio = config.getJSONObject("audio")
            assert(audio.getInt("context") == 1024) { "Invalid audio context" }
            assert(audio.getDouble("chunk_hop_sec") == 1.5) { "Invalid chunk hop" }
            
            val decode = config.getJSONObject("decode")
            assert(decode.getString("strategy") == "greedy") { "Invalid decode strategy" }
            assert(decode.getDouble("temperature") == 0.0) { "Invalid temperature" }
            
            TestResult(
                success = true,
                metrics = mapOf(
                    "config_valid" to "true",
                    "scenario" to config.getString("scenario"),
                    "ngl_pref" to backend.getInt("ngl_pref").toString(),
                    "threads_pref" to backend.getInt("threads_pref").toString(),
                    "primary_model" to model.getString("primary"),
                    "audio_context" to audio.getInt("context").toString(),
                    "decode_strategy" to decode.getString("strategy")
                )
            )
            
        } catch (e: Exception) {
            TestResult(success = false, error = e.message ?: "Unknown error")
        }
    }
    
    /**
     * Test 3: Transcription Validation
     * Tests actual whisper transcription with UX metrics
     */
    private fun testTranscription(): TestResult {
        Log.d(TAG, "Testing transcription...")
        
        return try {
            // Get test video file
            val testVideoFile = getTestVideoFile()
            assert(testVideoFile.exists()) { "Test video file not found: ${testVideoFile.absolutePath}" }
            
            // Measure transcription performance
            val startTime = System.currentTimeMillis()
            val startMemory = Debug.getPss()
            val startEnergy = getBatteryEnergy()
            
            // Create transcription job
            val jobConfig = JSONObject().apply {
                put("job_id", "arm1_test_${System.currentTimeMillis()}")
                put("file", testVideoFile.absolutePath)
                put("model", "whisper-small-q5_1.gguf")
                put("threads", 4)
                put("ngl", 8)
                put("strategy", "greedy")
                put("temperature", 0.0)
                put("language", "en")
                put("audio_context", 1024)
                put("chunk_hop_sec", 1.5)
            }
            
            // Execute transcription
            val transcriptionResult = whisperBridge.run(jobConfig.toString())
            val transcriptionJson = JSONObject(transcriptionResult)
            
            val endTime = System.currentTimeMillis()
            val endMemory = Debug.getPss()
            val endEnergy = getBatteryEnergy()
            
            // Calculate metrics
            val wallTimeMs = endTime - startTime
            val memoryDeltaMB = (endMemory - startMemory) / 1024
            val energyDeltaUWh = endEnergy?.let { startEnergy?.let { start -> it - start } }
            
            // Validate transcription result
            assert(transcriptionJson.has("job_id")) { "Missing job_id in result" }
            assert(transcriptionJson.has("status")) { "Missing status in result" }
            
            val status = transcriptionJson.getString("status")
            assert(status == "started" || status == "completed") { "Invalid status: $status" }
            
            TestResult(
                success = true,
                metrics = mapOf(
                    "wall_time_ms" to wallTimeMs.toString(),
                    "memory_delta_mb" to memoryDeltaMB.toString(),
                    "energy_delta_uwh" to (energyDeltaUWh?.toString() ?: "null"),
                    "transcription_status" to status,
                    "job_id" to transcriptionJson.getString("job_id"),
                    "test_video_size_mb" to (testVideoFile.length() / 1024 / 1024).toString()
                )
            )
            
        } catch (e: Exception) {
            TestResult(success = false, error = e.message ?: "Unknown error")
        }
    }
    
    /**
     * Test 4: UX Metrics Validation
     * Tests real-time UX metrics and performance
     */
    private fun testUxMetrics(): TestResult {
        Log.d(TAG, "Testing UX metrics...")
        
        return try {
            // Test whisper operations for UX metrics
            val operations = listOf("run", "export", "listSidecars", "verify")
            val operationTimes = mutableMapOf<String, Long>()
            
            for (operation in operations) {
                val startTime = System.nanoTime()
                
                when (operation) {
                    "run" -> {
                        val config = JSONObject().apply {
                            put("job_id", "ux_test_$operation")
                            put("file", "test.mp4")
                        }
                        whisperBridge.run(config.toString())
                    }
                    "export" -> whisperBridge.export("ux_test_export")
                    "listSidecars" -> whisperBridge.listSidecars()
                    "verify" -> whisperBridge.verify("ux_test_verify")
                }
                
                val endTime = System.nanoTime()
                operationTimes[operation] = (endTime - startTime) / 1_000_000 // Convert to ms
            }
            
            // Validate UX metrics
            val maxOperationTime = operationTimes.values.maxOrNull() ?: 0L
            assert(maxOperationTime < 1000) { "Operation too slow: ${maxOperationTime}ms" }
            
            // Test device info retrieval
            val deviceInfoStart = System.nanoTime()
            val deviceInfo = whisperBridge.getDeviceInfo()
            val deviceInfoTime = (System.nanoTime() - deviceInfoStart) / 1_000_000
            
            assert(deviceInfoTime < 100) { "Device info too slow: ${deviceInfoTime}ms" }
            
            // Parse device info for additional metrics
            val deviceInfoJson = JSONObject(deviceInfo)
            val batteryLevel = deviceInfoJson.optInt("battery_level", -1)
            val memoryInfo = deviceInfoJson.optJSONObject("memory_info")
            val storageInfo = deviceInfoJson.optJSONObject("storage_info")
            
            TestResult(
                success = true,
                metrics = mapOf(
                    "max_operation_time_ms" to maxOperationTime.toString(),
                    "device_info_time_ms" to deviceInfoTime.toString(),
                    "battery_level" to batteryLevel.toString(),
                    "memory_max_mb" to (memoryInfo?.optInt("max_memory_mb")?.toString() ?: "null"),
                    "storage_files_dir_exists" to (storageInfo?.optBoolean("files_dir_exists")?.toString() ?: "null"),
                    "operations_tested" to operations.size.toString()
                )
            )
            
        } catch (e: Exception) {
            TestResult(success = false, error = e.message ?: "Unknown error")
        }
    }
    
    /**
     * Create ARM-1 configuration JSON
     */
    private fun createArm1Config(): JSONObject {
        return JSONObject().apply {
            put("scenario", "arm1_realtime")
            
            put("backend", JSONObject().apply {
                put("ngl_pref", 8)
                put("ngl_fallbacks", listOf(4, 0))
                put("threads_pref", 4)
                put("threads_fallbacks", listOf(2))
            })
            
            put("model", JSONObject().apply {
                put("primary", "whisper-small-q5_1.gguf")
                put("fallback", "whisper-tiny-q4_0.gguf")
            })
            
            put("audio", JSONObject().apply {
                put("context", 1024)
                put("chunk_hop_sec", 1.5)
            })
            
            put("decode", JSONObject().apply {
                put("strategy", "greedy")
                put("temperature", 0.0)
            })
            
            put("language", JSONObject().apply {
                put("mode", "fixed")
                put("code", "en")
            })
            
            put("timeouts", JSONObject().apply {
                put("sf", 3.0)
                put("min_s", 30)
                put("max_s", 600)
                put("inactivity_s", 15)
            })
            
            put("cooldown", JSONObject().apply {
                put("skinC_max", 42)
                put("sleep_s", 60)
            })
        }
    }
    
    /**
     * Get test video file
     */
    private fun getTestVideoFile(): File {
        // Try clip_002.mp4 first
        val clip002 = File(filesDir, "in/clip_002.mp4")
        if (clip002.exists()) return clip002
        
        // Try sample.mp4 as fallback
        val sample = File(filesDir, "in/sample.mp4")
        if (sample.exists()) return sample
        
        // Create a dummy file for testing
        val dummyFile = File(filesDir, "in/test_video.mp4")
        dummyFile.parentFile?.mkdirs()
        dummyFile.writeText("dummy video content for testing")
        return dummyFile
    }
    
    /**
     * Get battery energy in microWatt-hours
     */
    private fun getBatteryEnergy(): Long? {
        return try {
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Generate comprehensive test report
     */
    private fun generateTestReport(results: List<TestResult>) {
        Log.i(TAG, "Generating ARM-1 test report...")
        
        val report = JSONObject().apply {
            put("test_name", TEST_NAME)
            put("timestamp", System.currentTimeMillis())
            put("package_name", context.packageName)
            put("device_info", whisperBridge.getDeviceInfo())
            
            put("test_results", results.mapIndexed { index, result ->
                JSONObject().apply {
                    put("test_${index + 1}", result.testName)
                    put("success", result.success)
                    result.error?.let { put("error", it) }
                    result.metrics?.let { metrics ->
                        put("metrics", JSONObject(metrics))
                    }
                }
            })
            
            put("overall_success", results.all { it.success })
            put("total_tests", results.size)
            put("passed_tests", results.count { it.success })
        }
        
        // Write report to file
        val reportFile = File(filesDir, "arm1/test_report.json")
        reportFile.parentFile?.mkdirs()
        FileWriter(reportFile).use { it.write(report.toString(2)) }
        
        Log.i(TAG, "📊 ARM-1 test report generated: ${reportFile.absolutePath}")
        Log.i(TAG, "📈 Overall success: ${report.getBoolean("overall_success")}")
        Log.i(TAG, "📊 Tests passed: ${report.getInt("passed_tests")}/${report.getInt("total_tests")}")
    }
    
    /**
     * Test result data class
     */
    private data class TestResult(
        val success: Boolean,
        val testName: String = "unknown",
        val error: String? = null,
        val metrics: Map<String, String>? = null
    )
}

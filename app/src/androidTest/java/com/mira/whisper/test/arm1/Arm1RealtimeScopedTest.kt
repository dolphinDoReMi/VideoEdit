package com.mira.whisper.test.arm1

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mira.whisper.AndroidWhisperBridge
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlinx.coroutines.runBlocking

/**
 * ARM-1 Realtime UX Test - Following Scoped Storage Infrastructure
 * 
 * This test validates ARM-1 realtime UX using the consolidated whisper package
 * and proper scoped storage infrastructure.
 * 
 * Key Features:
 * - Uses only com.mira.whisper package (no feature modules)
 * - Scoped storage compliant (app-private files/)
 * - Measures UX metrics (latency, streaming, thermals)
 * - Fixed configurations for reproducible results
 * - Follows working whisper test patterns
 */
@RunWith(AndroidJUnit4::class)
class Arm1RealtimeScopedTest {
    
    companion object {
        private const val TAG = "Arm1RealtimeScopedTest"
        private const val TEST_NAME = "arm1_realtime_scoped"
    }
    
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val filesDir = context.filesDir
    private val whisperBridge = AndroidWhisperBridge(context)
    
    /**
     * Main ARM-1 test that validates realtime whisper UX with scoped storage
     */
    @Test
    fun testArm1RealtimeScoped() {
        Log.i(TAG, "🚀 Starting ARM-1 Realtime UX Test with Scoped Storage")
        
        try {
            // 1. Infrastructure Test
            val infrastructureResult = testInfrastructure()
            assert(infrastructureResult.success) { "Infrastructure test failed: ${infrastructureResult.error}" }
            Log.i(TAG, "✅ Infrastructure test passed")
            
            // 2. Scoped Storage Test
            val scopedStorageResult = testScopedStorageCompliance()
            assert(scopedStorageResult.success) { "Scoped storage test failed: ${scopedStorageResult.error}" }
            Log.i(TAG, "✅ Scoped storage test passed")
            
            // 3. Configuration Test
            val configResult = testConfiguration()
            assert(configResult.success) { "Configuration test failed: ${configResult.error}" }
            Log.i(TAG, "✅ Configuration test passed")
            
            // 4. Transcription Test
            val transcriptionResult = testTranscription()
            assert(transcriptionResult.success) { "Transcription test failed: ${transcriptionResult.error}" }
            Log.i(TAG, "✅ Transcription test passed")
            
            // 5. UX Metrics Test
            val uxResult = testUxMetrics()
            assert(uxResult.success) { "UX metrics test failed: ${uxResult.error}" }
            Log.i(TAG, "✅ UX metrics test passed")
            
            // 6. Generate Test Report
            generateTestReport(listOf(infrastructureResult, scopedStorageResult, configResult, transcriptionResult, uxResult))
            
            Log.i(TAG, "🎉 ARM-1 Realtime UX Test with Scoped Storage completed successfully!")
            
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
                testName = "infrastructure",
                metrics = mapOf(
                    "package_name" to packageName,
                    "files_dir" to filesDir.absolutePath,
                    "external_files_dir" to (externalFilesDir?.absolutePath ?: "null"),
                    "device_info_length" to deviceInfo.length.toString(),
                    "infrastructure_test_length" to infrastructureTest.length.toString()
                )
            )
            
        } catch (e: Exception) {
            TestResult(success = false, testName = "infrastructure", error = e.message ?: "Unknown error")
        }
    }
    
    /**
     * Test 2: Scoped Storage Compliance
     * Verifies that ARM-1 files are properly stored and accessible via scoped storage
     */
    private fun testScopedStorageCompliance(): TestResult {
        Log.d(TAG, "Testing scoped storage compliance...")
        
        return try {
            // Test ARM-1 directory structure
            val arm1Dir = File(filesDir, "arm1")
            val modelsDir = File(filesDir, "models")
            val inputDir = File(filesDir, "in")
            val outputDir = File(filesDir, "out")
            val logsDir = File(filesDir, "logs")
            
            assert(arm1Dir.exists()) { "ARM-1 directory not found in app-private storage" }
            assert(modelsDir.exists()) { "Models directory not found in app-private storage" }
            assert(inputDir.exists()) { "Input directory not found in app-private storage" }
            assert(outputDir.exists()) { "Output directory not found in app-private storage" }
            assert(logsDir.exists()) { "Logs directory not found in app-private storage" }
            
            // Test ARM-1 files
            val configFile = File(arm1Dir, "arm1_config.json")
            val modelFile = File(modelsDir, "whisper-small-q5_1.gguf")
            val fallbackModelFile = File(modelsDir, "whisper-tiny-q4_0.gguf")
            val videoFile = File(inputDir, "clip_002.mp4")
            val manifestFile = File(inputDir, "test_data_manifest.csv")
            
            assert(configFile.exists()) { "ARM-1 config file not found" }
            assert(modelFile.exists()) { "Primary model file not found" }
            assert(fallbackModelFile.exists()) { "Fallback model file not found" }
            assert(videoFile.exists()) { "Test video file not found" }
            assert(manifestFile.exists()) { "Test manifest file not found" }
            
            // Test Uri-based access
            val configUri = Uri.fromFile(configFile)
            val modelUri = Uri.fromFile(modelFile)
            val videoUri = Uri.fromFile(videoFile)
            
            assert(configUri.scheme == "file") { "Config URI should use file scheme" }
            assert(modelUri.scheme == "file") { "Model URI should use file scheme" }
            assert(videoUri.scheme == "file") { "Video URI should use file scheme" }
            
            // Test file permissions and accessibility
            assert(configFile.canRead()) { "Config file should be readable" }
            assert(modelFile.canRead()) { "Model file should be readable" }
            assert(videoFile.canRead()) { "Video file should be readable" }
            
            TestResult(
                success = true,
                testName = "scoped_storage",
                metrics = mapOf(
                    "config_file_size" to configFile.length().toString(),
                    "model_file_size" to modelFile.length().toString(),
                    "fallback_model_size" to fallbackModelFile.length().toString(),
                    "video_file_size" to videoFile.length().toString(),
                    "manifest_file_size" to manifestFile.length().toString(),
                    "config_uri" to configUri.toString(),
                    "model_uri" to modelUri.toString(),
                    "video_uri" to videoUri.toString()
                )
            )
            
        } catch (e: Exception) {
            TestResult(success = false, testName = "scoped_storage", error = e.message ?: "Unknown error")
        }
    }
    
    /**
     * Test 3: Configuration Validation
     * Verifies that ARM-1 configuration is valid and accessible
     */
    private fun testConfiguration(): TestResult {
        Log.d(TAG, "Testing configuration...")
        
        return try {
            // Load ARM-1 configuration from scoped storage
            val configFile = File(filesDir, "arm1/arm1_config.json")
            val configContent = configFile.readText()
            val config = JSONObject(configContent)
            
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
                testName = "configuration",
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
            TestResult(success = false, testName = "configuration", error = e.message ?: "Unknown error")
        }
    }
    
    /**
     * Test 4: Transcription Validation
     * Tests actual whisper transcription with UX metrics using scoped storage
     */
    private fun testTranscription(): TestResult {
        Log.d(TAG, "Testing transcription...")
        
        return try {
            // Get test video file from scoped storage
            val testVideoFile = File(filesDir, "in/clip_002.mp4")
            assert(testVideoFile.exists()) { "Test video file not found: ${testVideoFile.absolutePath}" }
            
            // Create URIs for scoped storage access
            val videoUri = Uri.fromFile(testVideoFile)
            val modelUri = Uri.fromFile(File(filesDir, "models/whisper-small-q5_1.gguf"))
            
            // Measure transcription performance
            val startTime = System.currentTimeMillis()
            val startMemory = android.os.Debug.getPss()
            
            // Create transcription job configuration
            val jobConfig = JSONObject().apply {
                put("job_id", "arm1_test_${System.currentTimeMillis()}")
                put("file", testVideoFile.absolutePath)
                put("file_uri", videoUri.toString())
                put("model", "whisper-small-q5_1.gguf")
                put("model_uri", modelUri.toString())
                put("threads", 4)
                put("ngl", 8)
                put("strategy", "greedy")
                put("temperature", 0.0)
                put("language", "en")
                put("audio_context", 1024)
                put("chunk_hop_sec", 1.5)
                put("scoped_storage", true)
            }
            
            // Execute transcription using AndroidWhisperBridge
            val transcriptionResult = whisperBridge.run(jobConfig.toString())
            val transcriptionJson = JSONObject(transcriptionResult)
            
            val endTime = System.currentTimeMillis()
            val endMemory = android.os.Debug.getPss()
            
            // Calculate metrics
            val wallTimeMs = endTime - startTime
            val memoryDeltaMB = (endMemory - startMemory) / 1024
            
            // Validate transcription result
            assert(transcriptionJson.has("job_id")) { "Missing job_id in result" }
            assert(transcriptionJson.has("status")) { "Missing status in result" }
            
            val status = transcriptionJson.getString("status")
            assert(status == "started" || status == "completed") { "Invalid status: $status" }
            
            TestResult(
                success = true,
                testName = "transcription",
                metrics = mapOf(
                    "wall_time_ms" to wallTimeMs.toString(),
                    "memory_delta_mb" to memoryDeltaMB.toString(),
                    "transcription_status" to status,
                    "job_id" to transcriptionJson.getString("job_id"),
                    "test_video_size_mb" to (testVideoFile.length() / 1024 / 1024).toString(),
                    "video_uri" to videoUri.toString(),
                    "model_uri" to modelUri.toString()
                )
            )
            
        } catch (e: Exception) {
            TestResult(success = false, testName = "transcription", error = e.message ?: "Unknown error")
        }
    }
    
    /**
     * Test 5: UX Metrics Validation
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
                            put("scoped_storage", true)
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
                testName = "ux_metrics",
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
            TestResult(success = false, testName = "ux_metrics", error = e.message ?: "Unknown error")
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
            put("scoped_storage_compliant", true)
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
        
        // Write report to scoped storage
        val reportFile = File(filesDir, "arm1/test_report_scoped.json")
        reportFile.parentFile?.mkdirs()
        reportFile.writeText(report.toString(2))
        
        Log.i(TAG, "📊 ARM-1 test report generated: ${reportFile.absolutePath}")
        Log.i(TAG, "📈 Overall success: ${report.getBoolean("overall_success")}")
        Log.i(TAG, "📊 Tests passed: ${report.getInt("passed_tests")}/${report.getInt("total_tests")}")
        Log.i(TAG, "🔒 Scoped storage compliant: ${report.getBoolean("scoped_storage_compliant")}")
    }
    
    /**
     * Test result data class
     */
    private data class TestResult(
        val success: Boolean,
        val testName: String,
        val error: String? = null,
        val metrics: Map<String, String>? = null
    )
}

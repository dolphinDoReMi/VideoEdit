import android.content.Context
import android.net.Uri
import com.mira.videoeditor.AutoCutEngine
import com.mira.videoeditor.VideoScorer
import kotlinx.coroutines.runBlocking

fun testEndToEndProcessing(context: Context, videoUri: Uri) {
    println("✅ End-to-End Integration Test:")
    
    runBlocking {
        try {
            val engine = AutoCutEngine(context) { progress ->
                val percentage = (progress * 100).toInt()
                when {
                    progress < 0.1f -> println("   - Analyzing video... $percentage%")
                    progress < 0.3f -> println("   - Selecting segments... $percentage%")
                    else -> println("   - Exporting video... $percentage%")
                }
            }
            
            val outputPath = context.getExternalFilesDir(null)?.resolve("test_output.mp4")?.absolutePath
            if (outputPath != null) {
                engine.autoCutAndExport(
                    input = videoUri,
                    outputPath = outputPath,
                    targetDurationMs = 30000L, // 30 seconds
                    segmentMs = 2000L // 2 second segments
                )
                println("   - ✅ End-to-end test completed successfully!")
                println("   - Output file: $outputPath")
            } else {
                println("   - ❌ Could not create output path")
            }
        } catch (e: Exception) {
            println("   - ❌ End-to-end test failed: ${e.message}")
            e.printStackTrace()
        }
    }
}

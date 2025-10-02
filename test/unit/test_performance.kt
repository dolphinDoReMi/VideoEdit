import android.content.Context
import android.net.Uri
import com.mira.videoeditor.VideoScorer
import kotlin.system.measureTimeMillis

fun testPerformance(context: Context, videoUri: Uri) {
    println("✅ Performance Test:")
    
    val scorer = VideoScorer(context)
    
    // Test analysis performance
    val analysisTime = measureTimeMillis {
        val segments = scorer.scoreSegments(
            uri = videoUri,
            segmentMs = 1000L, // 1 second segments for faster testing
            maxDurationMs = 10000L // First 10 seconds only
        )
        println("   - Analyzed ${segments.size} segments")
    }
    
    println("   - Analysis time: ${analysisTime}ms")
    println("   - Average time per segment: ${analysisTime / 10}ms")
    
    // Memory usage estimation
    val runtime = Runtime.getRuntime()
    val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
    println("   - Memory usage: ${usedMemory}MB")
}

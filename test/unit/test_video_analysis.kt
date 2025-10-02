import android.content.Context
import android.net.Uri
import com.mira.videoeditor.VideoScorer

fun testVideoAnalysis(context: Context, videoUri: Uri) {
    val scorer = VideoScorer(context)
    val segments = scorer.scoreSegments(
        uri = videoUri,
        segmentMs = 2000L, // 2 second segments
        maxDurationMs = 30000L // Analyze first 30 seconds
    )
    
    println("✅ VideoScorer Analysis Results:")
    println("   - Total segments analyzed: ${segments.size}")
    println("   - Motion scores range: ${segments.minOfOrNull { it.score }} - ${segments.maxOfOrNull { it.score }}")
    println("   - Average motion score: ${segments.map { it.score }.average()}")
    
    // Show top 3 segments
    val topSegments = segments.sortedByDescending { it.score }.take(3)
    println("   - Top 3 segments:")
    topSegments.forEachIndexed { index, segment ->
        println("     ${index + 1}. ${segment.startMs}ms-${segment.endMs}ms (score: ${segment.score})")
    }
}

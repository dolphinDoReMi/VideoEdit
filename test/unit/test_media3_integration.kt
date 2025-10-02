import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.transformer.*
import com.mira.videoeditor.AutoCutEngine
import kotlinx.coroutines.runBlocking

fun testMedia3Integration(context: Context, videoUri: Uri) {
    println("✅ Media3 Integration Test:")
    
    // Test MediaItem creation
    val mediaItem = MediaItem.Builder()
        .setUri(videoUri)
        .build()
    println("   - MediaItem created: ${mediaItem.mediaId}")
    
    // Test Transformer creation
    val transformer = Transformer.Builder(context)
        .setVideoMimeType(MimeTypes.VIDEO_H264)
        .build()
    println("   - Transformer created successfully")
    
    // Test AutoCutEngine
    runBlocking {
        try {
            val engine = AutoCutEngine(context) { progress ->
                println("   - Progress: ${(progress * 100).toInt()}%")
            }
            println("   - AutoCutEngine created successfully")
        } catch (e: Exception) {
            println("   - AutoCutEngine error: ${e.message}")
        }
    }
}

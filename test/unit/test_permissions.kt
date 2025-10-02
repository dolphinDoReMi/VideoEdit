import android.content.ContentResolver
import android.net.Uri
import com.mira.videoeditor.MediaStoreExt

fun testFilePermissions(resolver: ContentResolver, videoUri: Uri) {
    println("✅ File Permissions Test:")
    
    // Test MIME type detection
    val mimeType = resolver.getMimeType(videoUri)
    println("   - MIME type: $mimeType")
    
    // Test video validation
    val isValidVideo = resolver.isValidVideoUri(videoUri)
    println("   - Is valid video: $isValidVideo")
    
    // Test file size
    val fileSize = resolver.getFileSize(videoUri)
    println("   - File size: ${fileSize / 1024 / 1024}MB")
    
    // Test permission taking
    try {
        resolver.takePersistableUriPermission(videoUri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        println("   - Permission taken successfully")
    } catch (e: Exception) {
        println("   - Permission error: ${e.message}")
    }
}

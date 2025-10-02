import android.app.Application
import androidx.media3.common.util.Util
import com.mira.videoeditor.AutoCutApplication

fun testApplicationInitialization() {
    println("✅ Application Initialization Test:")
    
    // Test Media3 user agent
    val userAgent = Util.getUserAgent(null, "Mira")
    println("   - Media3 user agent: $userAgent")
    
    // Test AutoCutApplication
    try {
        val app = AutoCutApplication()
        println("   - AutoCutApplication created successfully")
        println("   - App context available: ${app.getAppContext() != null}")
    } catch (e: Exception) {
        println("   - AutoCutApplication error: ${e.message}")
    }
}

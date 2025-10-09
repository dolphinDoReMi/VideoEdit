package com.mira.videoeditor.test

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Automated Gradle test to prevent broadcast code, logs, and patterns
 * from being committed to the repository.
 */
class CodeQualityPreventionTest {

    @TempDir
    lateinit var tempDir: File

    @Test
    fun `should fail build when broadcast code is detected`() {
        // Create a test file with broadcast code
        val testFile = File(tempDir, "src/main/java/TestFile.kt")
        testFile.parentFile.mkdirs()
        testFile.writeText("""
            package com.mira.videoeditor.test
            
            import android.content.Intent
            import android.content.Context
            
            class TestClass {
                fun testMethod() {
                    // This should fail the build
                    context.sendBroadcast(Intent("com.mira.com.feature.whisper.PROCESS_DIRECT"))
                    adb shell "am broadcast -a com.mira.com.feature.whisper.PROCESS_DIRECT"
                }
            }
        """.trimIndent())

        val result = GradleRunner.create()
            .withProjectDir(tempDir)
            .withArguments("checkCodeQuality")
            .withPluginClasspath()
            .buildAndFail()

        assertTrue(result.output.contains("Broadcast code detected"))
    }

    @Test
    fun `should fail build when adb logcat patterns are detected`() {
        // Create a test file with logcat patterns
        val testFile = File(tempDir, "src/main/java/TestFile.kt")
        testFile.parentFile.mkdirs()
        testFile.writeText("""
            package com.mira.videoeditor.test
            
            class TestClass {
                fun testMethod() {
                    // This should fail the build
                    adb logcat -d | grep -i whisper
                    adb logcat | grep -i audioio
                }
            }
        """.trimIndent())

        val result = GradleRunner.create()
            .withProjectDir(tempDir)
            .withArguments("checkCodeQuality")
            .withPluginClasspath()
            .buildAndFail()

        assertTrue(result.output.contains("Logcat patterns detected"))
    }

    @Test
    fun `should fail build when trigger patterns are detected`() {
        // Create a test file with trigger patterns
        val testFile = File(tempDir, "src/main/java/TestFile.kt")
        testFile.parentFile.mkdirs()
        testFile.writeText("""
            package com.mira.videoeditor.test
            
            class TestClass {
                fun testMethod() {
                    // This should fail the build
                    val trigger = "clip_003_step_by_step_trigger.txt"
                    val triggerFile = "/storage/emulated/0/MiraWhisper/trigger_processing.txt"
                }
            }
        """.trimIndent())

        val result = GradleRunner.create()
            .withProjectDir(tempDir)
            .withArguments("checkCodeQuality")
            .withPluginClasspath()
            .buildAndFail()

        assertTrue(result.output.contains("Trigger patterns detected"))
    }

    @Test
    fun `should pass build when no prohibited patterns are detected`() {
        // Create a clean test file
        val testFile = File(tempDir, "src/main/java/TestFile.kt")
        testFile.parentFile.mkdirs()
        testFile.writeText("""
            package com.mira.videoeditor.test
            
            class TestClass {
                fun testMethod() {
                    // Clean code without prohibited patterns
                    val result = processAudio()
                    return result
                }
            }
        """.trimIndent())

        val result = GradleRunner.create()
            .withProjectDir(tempDir)
            .withArguments("checkCodeQuality")
            .withPluginClasspath()
            .build()

        assertFalse(result.output.contains("Broadcast code detected"))
        assertFalse(result.output.contains("Logcat patterns detected"))
        assertFalse(result.output.contains("Trigger patterns detected"))
    }
}

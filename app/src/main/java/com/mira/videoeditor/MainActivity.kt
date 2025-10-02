package com.mira.videoeditor

import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import android.os.Handler
import android.os.Looper

class MainActivity : ComponentActivity() {
  companion object {
    private const val TAG = "MainActivity"
  }
  
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    Log.d(TAG, "MainActivity onCreate")
    setContent {
      MaterialTheme {
        val scope = rememberCoroutineScope()
        var picked by remember { mutableStateOf<Uri?>(null) }
        var progress by remember { mutableStateOf(0f) }
        var status by remember { mutableStateOf("Ready") }
        var outputPath by remember { mutableStateOf<String?>(null) }
        var isProcessing by remember { mutableStateOf(false) }
        
        // Progress state using remember with proper recomposition
        var progressState by remember { mutableStateOf(0f) }
        var statusState by remember { mutableStateOf("Ready") }

        val picker = rememberLauncherForActivityResult(
          contract = ActivityResultContracts.OpenDocument()
        ) { uri ->
          if (uri != null) {
            contentResolver.takePersistableUriPermission(
              uri, IntentFlags.read
            )
            picked = uri
            status = "Selected"
          }
        }

        val downloadPicker = rememberLauncherForActivityResult(
          contract = ActivityResultContracts.OpenDocument()
        ) { uri ->
          if (uri != null) {
            contentResolver.takePersistableUriPermission(
              uri, IntentFlags.read
            )
            picked = uri
            status = "Selected from Download"
          }
        }

        Surface(Modifier.fillMaxSize()) {
          Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text("Mira • AI Video Editor", style = MaterialTheme.typography.h5)

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
              Button(onClick = {
                picker.launch(arrayOf("video/*"))
              }) { Text("Select Video") }

              Button(onClick = {
                downloadPicker.launch(arrayOf("video/*"))
              }) { Text("From Download") }
            }
            
            // Test button to verify UI state updates
            Button(onClick = {
              Log.d(TAG, "Test button clicked")
              status = "Test: ${System.currentTimeMillis()}"
              progress = (progress + 0.1f) % 1.0f
            }) { Text("Test UI Updates") }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
              Button(
                enabled = picked != null && !isProcessing,
                onClick = {
                  Log.d(TAG, "Auto Cut button clicked")
                  isProcessing = true
                  status = "Analyzing..."
                  progress = 0f
                  val out = getExternalFilesDir(null)!!.resolve("mira_output.mp4").absolutePath
                  outputPath = out
                  Log.d(TAG, "Starting AutoCutEngine with output: $out")
                  scope.launch {
                    try {
                      AutoCutEngine(
                        ctx = this@MainActivity,
                        onProgress = { p -> 
                          Log.d(TAG, "Progress callback: $p")
                          // Use Handler to ensure UI updates happen on main thread
                          Handler(Looper.getMainLooper()).post {
                            progressState = p
                            when {
                              p <= 0.05f -> statusState = "Starting analysis..."
                              p <= 0.15f -> statusState = "Analyzing motion... ${"%.0f".format((p-0.05f)/0.10f*100)}%"
                              p <= 0.25f -> statusState = "Selecting best segments..."
                              p <= 0.30f -> statusState = "Preparing export..."
                              p < 1.0f -> statusState = "Exporting video... ${"%.0f".format(p*100)}%"
                              else -> statusState = "Export complete!"
                            }
                            Log.d(TAG, "Updated status: $statusState, progress: $progressState")
                          }
                        }
                      ).autoCutAndExport(
                        input = picked!!,
                        outputPath = out,
                        // MVP: target duration 30s, segment length 2s
                        targetDurationMs = 30_000L,
                        segmentMs = 2_000L
                      )
                      Log.d(TAG, "AutoCutEngine completed successfully")
                      isProcessing = false
                      status = "Done: $out"
                    } catch (e: Exception) {
                      Log.e(TAG, "AutoCutEngine failed", e)
                      isProcessing = false
                      status = "Error: ${e.message}"
                      progress = 0f
                    }
                  }
                }
              ) { Text(if (isProcessing) "Processing..." else "Auto Cut") }
            }

            LinearProgressIndicator(progress = progressState, modifier = Modifier.fillMaxWidth())
            Text("Status: $statusState")
            Text("Progress: ${"%.1f".format(progressState*100)}%")
            if (outputPath != null) {
              Text("Export file: $outputPath")
            }
          }
        }
      }
    }
  }
}

// Helper: IntentFlags combination
private object IntentFlags {
  const val read = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
}
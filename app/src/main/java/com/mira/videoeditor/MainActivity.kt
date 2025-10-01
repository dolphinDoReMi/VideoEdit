package com.mira.videoeditor

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      MaterialTheme {
        val scope = rememberCoroutineScope()
        var picked by remember { mutableStateOf<Uri?>(null) }
        var progress by remember { mutableStateOf(0f) }
        var status by remember { mutableStateOf("Ready") }
        var outputPath by remember { mutableStateOf<String?>(null) }

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

              Button(
                enabled = picked != null,
                onClick = {
                  status = "Analyzing..."
                  progress = 0f
                  val out = getExternalFilesDir(null)!!.resolve("mira_output.mp4").absolutePath
                  outputPath = out
                  scope.launch {
                    // Simple mock processing
                    repeat(10) {
                      kotlinx.coroutines.delay(200)
                      progress = (it + 1) / 10f
                      status = "Export ${(progress * 100).toInt()}%"
                    }
                    status = "Done: $out"
                  }
                }
              ) { Text("Auto Cut") }
            }

            LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth())
            Text(status)
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
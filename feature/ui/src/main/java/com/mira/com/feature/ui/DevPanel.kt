package com.mira.com.feature.ui
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mira.com.core.infra.Config

@Composable
fun DevPanel(onSmoke: (() -> String)?) {
  var last by remember { mutableStateOf("") }
  Column(Modifier.padding(16.dp)) {
    Text("Diagnostics")
    listOf(
      "appId" to "com.mira.com",
      "whisper" to "${Config.WHISPER_SR}Hz/${Config.WHISPER_CH}ch",
      "clip" to "${Config.CLIP_FRAME_COUNT}@${Config.CLIP_RES}/${Config.CLIP_SCHEDULE}",
      "retrieval" to "${Config.RETR_SIMILARITY}/${Config.RETR_STORAGE_FMT}"
    ).forEach { (k,v) -> Text("$k: $v") }
    Button(onClick = { last = onSmoke?.invoke() ?: "n/a" }) { Text("Run smoke") }
    if (last.isNotEmpty()) Text("Result: $last")
  }
}

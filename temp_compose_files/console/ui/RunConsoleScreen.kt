package com.mira.com.whisper.console.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.mira.com.whisper.console.WhisperConsoleViewModel
import com.mira.com.whisper.console.model.JobRow
import com.mira.com.whisper.console.model.JobStatus
import kotlinx.coroutines.launch
import kotlin.math.round

@Composable
fun RunConsoleScreen(vm: WhisperConsoleViewModel) {
    val state by vm.state.collectAsState()

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("Run Console") },
                actions = {
                    TextButton(onClick = { vm.refresh() }) { Text("Refresh") }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            HeaderRow()
            Divider()
            LazyColumn(Modifier.fillMaxSize()) {
                items(state.jobs, key = { it.jobId }) { job ->
                    JobRowItem(job)
                    Divider()
                }
            }
        }
    }
}

@Composable
private fun HeaderRow() {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Job", Modifier.weight(1.2f))
        Text("File", Modifier.weight(1.6f))
        Text("RTF", Modifier.weight(0.6f))
        Text("Status", Modifier.weight(0.9f))
    }
}

@Composable
private fun JobRowItem(job: JobRow) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth()
            .clickable { expanded = !expanded }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(job.jobId, Modifier.weight(1.2f), maxLines = 1)
        Text(job.baseName, Modifier.weight(1.6f), maxLines = 1)
        Text(job.rtf?.let { (round(it * 100) / 100.0).toString() } ?: "–", Modifier.weight(0.6f))
        StatusPill(job.status, Modifier.weight(0.9f))
    }
    if (expanded) {
        Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 12.dp)) {
            KeyVal("audio_sha256", job.audioSha256)
            KeyVal("model_sha256", job.modelSha256)
            KeyVal("transcript_sha256", job.transcriptSha256)
            KeyVal("durationMs", job.durationMs?.toString())
            KeyVal("started_at", job.startedAt?.toString())
            KeyVal("finished_at", job.finishedAt?.toString())
            Text("config:", style = MaterialTheme.typography.labelMedium)
            Box(
                Modifier.fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(8.dp)
            ) {
                Text(
                    text = (job.config ?: emptyMap<String, Any?>()).toString(),
                    fontFamily = FontFamily.Monospace
                )
            }
            Text("dir: ${job.dirPath}", fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
private fun StatusPill(status: JobStatus, modifier: Modifier = Modifier) {
    val (bg, fg) = when (status) {
        JobStatus.COMPLETED -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        JobStatus.RUNNING -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        JobStatus.ERROR -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        JobStatus.TIMEOUT -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
    }
    Box(
        modifier.then(Modifier.padding(4.dp)),
        contentAlignment = Alignment.Center
    ) {
        Surface(color = bg, shape = MaterialTheme.shapes.small) {
            Text(
                status.name,
                color = fg,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable private fun KeyVal(k: String, v: String?) {
    if (!v.isNullOrBlank()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(k, style = MaterialTheme.typography.labelMedium)
            Text(v, fontFamily = FontFamily.Monospace)
        }
    }
}

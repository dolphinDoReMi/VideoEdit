package com.mira.videoeditor

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.mira.videoeditor.usecases.*
import kotlinx.coroutines.launch

/**
 * Example activity demonstrating CLIP4Clip Room database integration.
 * 
 * This activity shows how to:
 * 1. Process videos and generate embeddings
 * 2. Search videos by text queries
 * 3. Monitor processing status
 * 4. Manage the database
 */
class Clip4ClipRoomDemoActivity : AppCompatActivity() {

    private lateinit var videoIngestionUseCase: VideoIngestionUseCase
    private lateinit var videoSearchUseCase: VideoSearchUseCase
    private lateinit var databaseManagementUseCase: DatabaseManagementUseCase
    
    private lateinit var statusText: TextView
    private lateinit var resultsText: TextView
    private lateinit var searchQuery: EditText
    private lateinit var searchButton: Button
    private lateinit var processVideoButton: Button
    private lateinit var databaseStatsButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_clip4clip_room_demo)
        
        initializeComponents()
        setupUI()
    }

    private fun initializeComponents() {
        videoIngestionUseCase = VideoIngestionUseCase(this)
        videoSearchUseCase = VideoSearchUseCase(this)
        databaseManagementUseCase = DatabaseManagementUseCase(this)
        
        statusText = findViewById(R.id.status_text)
        resultsText = findViewById(R.id.results_text)
        searchQuery = findViewById(R.id.search_query)
        searchButton = findViewById(R.id.search_button)
        processVideoButton = findViewById(R.id.process_video_button)
        databaseStatsButton = findViewById(R.id.database_stats_button)
    }

    private fun setupUI() {
        searchButton.setOnClickListener {
            performSearch()
        }
        
        processVideoButton.setOnClickListener {
            selectAndProcessVideo()
        }
        
        databaseStatsButton.setOnClickListener {
            showDatabaseStats()
        }
        
        updateStatus("CLIP4Clip Room Demo Ready")
    }

    private fun performSearch() {
        val query = searchQuery.text.toString().trim()
        if (query.isEmpty()) {
            Toast.makeText(this, "Please enter a search query", Toast.LENGTH_SHORT).show()
            return
        }

        updateStatus("Searching for: \"$query\"")
        
        lifecycleScope.launch {
            try {
                val results = videoSearchUseCase.searchVideos(
                    query = query,
                    topK = 5,
                    searchLevel = "video"
                )
                
                val resultText = buildString {
                    appendLine("🔍 Search Results for: \"$query\"")
                    appendLine("Found ${results.size} results:")
                    appendLine()
                    
                    results.forEachIndexed { index, result ->
                        appendLine("${index + 1}. Similarity: ${String.format("%.3f", result.similarity)}")
                        appendLine("   Duration: ${result.startMs}ms - ${result.endMs}ms")
                        appendLine("   Level: ${result.searchLevel}")
                        appendLine()
                    }
                }
                
                updateResults(resultText)
                updateStatus("Search completed successfully")
                
            } catch (e: Exception) {
                updateStatus("Search failed: ${e.message}")
                updateResults("❌ Search failed: ${e.message}")
            }
        }
    }

    private fun selectAndProcessVideo() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "video/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        
        startActivityForResult(intent, REQUEST_CODE_VIDEO_SELECT, null)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == REQUEST_CODE_VIDEO_SELECT && resultCode == RESULT_OK) {
            data?.data?.let { videoUri ->
                processVideo(videoUri)
            }
        }
    }

    private fun processVideo(videoUri: Uri) {
        updateStatus("Processing video: ${videoUri.lastPathSegment}")
        
        lifecycleScope.launch {
            try {
                val videoId = videoIngestionUseCase.processVideo(
                    videoUri = videoUri,
                    variant = "clip_vit_b32_mean_v1",
                    framesPerVideo = 32,
                    framesPerShot = 12,
                    useBackgroundProcessing = true
                )
                
                updateStatus("Video processing started")
                updateResults("✅ Video processing enqueued\nVideo ID: $videoId")
                
                // Check processing status after a delay
                lifecycleScope.launch {
                    kotlinx.coroutines.delay(5000) // Wait 5 seconds
                    val status = videoIngestionUseCase.getProcessingStatus(videoUri)
                    updateStatus("Processing status: $status")
                }
                
            } catch (e: Exception) {
                updateStatus("Video processing failed: ${e.message}")
                updateResults("❌ Video processing failed: ${e.message}")
            }
        }
    }

    private fun showDatabaseStats() {
        updateStatus("Loading database statistics...")
        
        lifecycleScope.launch {
            try {
                val stats = databaseManagementUseCase.getDatabaseStats()
                val embeddingStats = videoSearchUseCase.getEmbeddingStats()
                
                val statsText = buildString {
                    appendLine("📊 Database Statistics")
                    appendLine("=====================")
                    appendLine()
                    appendLine("Videos: ${stats.videoCount}")
                    appendLine("Embeddings: ${stats.embeddingCount}")
                    appendLine("Database Size: ${stats.databaseSize} bytes")
                    appendLine()
                    appendLine("Embedding Breakdown:")
                    embeddingStats.forEach { (type, count) ->
                        appendLine("  $type: $count")
                    }
                }
                
                updateResults(statsText)
                updateStatus("Database statistics loaded")
                
            } catch (e: Exception) {
                updateStatus("Failed to load database stats: ${e.message}")
                updateResults("❌ Failed to load database stats: ${e.message}")
            }
        }
    }

    private fun updateStatus(message: String) {
        runOnUiThread {
            statusText.text = message
        }
    }

    private fun updateResults(message: String) {
        runOnUiThread {
            resultsText.text = message
        }
    }

    companion object {
        private const val REQUEST_CODE_VIDEO_SELECT = 1001
    }
}

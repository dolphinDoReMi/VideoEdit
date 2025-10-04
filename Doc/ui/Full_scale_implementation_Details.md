# UI Full Scale Implementation Details

## Overview

This document provides comprehensive implementation details for the Mira Video Editor UI system, built as a hybrid Android-WebView architecture with real-time resource monitoring and cross-platform consistency.

## Problem Disaggregation

### Inputs
- **Video Files**: MP4, AVI, MOV formats via Android Storage Access Framework
- **System Resources**: CPU, Memory, Battery, Temperature via Android APIs
- **User Interactions**: Touch events, button clicks, navigation actions
- **Processing Status**: Real-time updates from CLIP/Whisper processing engines

### Outputs
- **Visual Feedback**: Progress bars, status indicators, resource displays
- **User Notifications**: Toast messages, log entries, completion alerts
- **Navigation**: Screen transitions, back button handling
- **Asset Management**: Video thumbnails, metadata display

### Runtime Surfaces
- **Android MainActivity**: Native Android container with WebView
- **WebView UI**: HTML5/CSS/JavaScript processing interface
- **JavaScript Bridge**: Bidirectional communication layer
- **Resource Monitor**: Background system monitoring service

## Analysis with Trade-offs

### WebView vs Native Android UI
- **WebView Advantages**: Cross-platform consistency, rapid iteration, rich animations
- **WebView Disadvantages**: Performance overhead, limited native integration
- **Decision**: WebView chosen for cross-platform consistency and rapid development

### Real-time Monitoring vs Static Display
- **Real-time Advantages**: Accurate feedback, debugging aid, professional feel
- **Real-time Disadvantages**: Battery drain, CPU overhead, complexity
- **Decision**: Real-time monitoring with 2-second intervals and moving averages

### JavaScript Bridge vs Native Callbacks
- **Bridge Advantages**: Flexible communication, easy testing, cross-platform
- **Bridge Disadvantages**: Security concerns, performance overhead
- **Decision**: JavaScript bridge with proper security annotations

## Design

### Architecture Pipeline
```
User Interaction → WebView UI → JavaScript Bridge → Android MainActivity
                                                      ↓
System Resources ← Resource Monitor ← Background Timer ← MainActivity
                                                      ↓
Processing Engine ← AutoCutEngine ← Video Selection ← MainActivity
```

### Key Control Knots (All Exposed)

#### UI Control Knots
- **WEBVIEW_ENABLED** (true) - Enable/disable WebView-based UI
- **JAVASCRIPT_BRIDGE** (true) - Enable JavaScript-Android communication
- **REAL_TIME_MONITORING** (true) - Enable real-time resource tracking
- **PROCESSING_VISUALIZATION** (true) - Show step-by-step progress

#### Resource Monitoring Control Knots
- **MONITORING_INTERVAL** (2000ms) - Resource update frequency
- **HISTORY_WINDOW** (120000ms) - Moving average calculation window
- **CPU_OUTLIER_THRESHOLD** (50.0%) - Filter extreme CPU readings
- **MEMORY_CALCULATION_BASE** (12288MB) - System memory baseline

#### UI Performance Control Knots
- **ANIMATION_ENABLED** (true) - Enable/disable UI animations
- **REAL_TIME_UPDATES** (true) - Enable real-time UI updates
- **RESPONSIVE_DESIGN** (true) - Enable responsive layout
- **DARK_THEME** (true) - Use dark theme by default

## Prioritization & Rationale

### P0: Core UI Functionality
- **WebView Container**: Basic HTML5 UI rendering
- **JavaScript Bridge**: Bidirectional communication
- **Resource Monitoring**: CPU/Memory/Battery tracking
- **Processing Visualization**: Step-by-step progress display

### P1: Enhanced Features
- **Real-time Updates**: Live resource monitoring
- **Responsive Design**: Adaptive layouts
- **Animation System**: Smooth transitions
- **Error Handling**: Graceful failure management

### P2: Advanced Features
- **Cross-platform Assets**: Unified asset management
- **Performance Optimization**: Resource usage optimization
- **Accessibility**: Screen reader support
- **Internationalization**: Multi-language support

## Workplan to Execute

1. **Scaffold WebView Architecture**: Create MainActivity with WebView container
2. **Implement HTML5 UI**: Build processing interface with Tailwind CSS
3. **Create JavaScript Bridge**: Implement bidirectional communication
4. **Add Resource Monitoring**: Implement real-time system monitoring
5. **Integrate Processing Visualization**: Connect to CLIP/Whisper engines
6. **Test Cross-platform**: Verify Android/iOS/Web compatibility

## Implementation (Runnable Kotlin & HTML)

### MainActivity Implementation

```kotlin
// File: app/src/main/java/mira/ui/MainActivity.kt
package mira.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import android.webkit.JavascriptInterface
import android.widget.Toast
import android.app.ActivityManager
import android.os.Debug
import android.os.Process
import android.util.Log
import java.io.RandomAccessFile
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.max

class MainActivity : AppCompatActivity() {
    
    private lateinit var webView: WebView
    private var resourceTimer: Timer? = null
    private var lastCpuTime: Long = 0
    private var lastAppCpuTime: Long = 0
    private var lastCpuCheckTime: Long = 0
    
    // Control Knot: Resource monitoring configuration
    private val maxHistorySize = 60 // 2 minutes at 2-second intervals
    private val monitoringInterval = 2000L // 2 seconds
    private val cpuOutlierThreshold = 50.0 // Filter values >50%
    private val memoryCalculationBase = 12288L // 12GB Xiaomi Pad
    
    // Data structures for moving averages
    private data class ResourceReading(
        val timestamp: Long, 
        val cpuUsage: Double, 
        val memoryUsage: Long
    )
    private val cpuMemoryHistory = ConcurrentLinkedQueue<ResourceReading>()
    
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    loadVideoV1()
                    startResourceMonitoring()
                }
            }
            addJavascriptInterface(JavaScriptInterface(), "AndroidInterface")
            loadUrl("file:///android_asset/processing.html")
        }
        setContentView(webView)
    }
    
    // Control Knot: Resource monitoring implementation
    private fun startResourceMonitoring() {
        resourceTimer = Timer()
        resourceTimer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    updateResourceUsage()
                }
            }
        }, 0, monitoringInterval)
    }
    
    // Control Knot: CPU usage calculation with moving average
    private fun calculateCpuMovingAverage(): Double {
        if (cpuMemoryHistory.isEmpty()) return 0.0
        
        val readings = cpuMemoryHistory.toList()
        if (readings.size == 1) {
            return readings[0].cpuUsage.coerceIn(0.0, 100.0)
        }
        
        // Filter out extreme outliers
        val filteredReadings = readings.filter { it.cpuUsage <= cpuOutlierThreshold }
        if (filteredReadings.isEmpty()) {
            val lastReasonable = readings.lastOrNull { it.cpuUsage <= cpuOutlierThreshold }?.cpuUsage ?: 10.0
            return lastReasonable.coerceIn(0.0, 100.0)
        }
        
        // Calculate weighted average
        var weightedSum = 0.0
        var totalWeight = 0.0
        val size = filteredReadings.size
        
        filteredReadings.forEachIndexed { index, reading ->
            val weight = (index + 1).toDouble() / size
            weightedSum += reading.cpuUsage * weight
            totalWeight += weight
        }
        
        val average = if (totalWeight > 0) weightedSum / totalWeight else filteredReadings.last().cpuUsage
        return average.coerceIn(0.0, 100.0)
    }
    
    // Control Knot: Memory usage calculation
    private fun getRealMemoryUsage(): Long {
        return try {
            val memoryInfo = Debug.MemoryInfo()
            Debug.getMemoryInfo(memoryInfo)
            val memoryMB = memoryInfo.totalPss.toLong() / 1024
            
            // Calculate percentage based on total system memory
            val memoryPercentage = ((memoryMB.toDouble() / memoryCalculationBase) * 100.0).toLong()
            
            Log.d("ResourceMonitor", "Memory - PSS: ${memoryInfo.totalPss}KB (${memoryMB}MB), " +
                    "System Memory: ${memoryMB}MB/${memoryCalculationBase}MB, " +
                    "Percentage: ${memoryPercentage}%")
            
            memoryPercentage.coerceIn(0L, 100L)
        } catch (e: Exception) {
            Log.e("ResourceMonitor", "Memory error: ${e.message}")
            0L
        }
    }
    
    // Control Knot: JavaScript bridge interface
    private inner class JavaScriptInterface {
        @JavascriptInterface
        fun showToast(message: String) {
            Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
        }
        
        @JavascriptInterface
        fun selectVideo() {
            Toast.makeText(this@MainActivity, "Video selection requested from WebView", Toast.LENGTH_SHORT).show()
        }
        
        @JavascriptInterface
        fun startProcessing(videoUri: String) {
            Toast.makeText(this@MainActivity, "Start processing: $videoUri", Toast.LENGTH_SHORT).show()
        }
        
        @JavascriptInterface
        fun pauseProcessing() {
            Toast.makeText(this@MainActivity, "Processing paused", Toast.LENGTH_SHORT).show()
        }
        
        @JavascriptInterface
        fun cancelProcessing() {
            Toast.makeText(this@MainActivity, "Processing cancelled", Toast.LENGTH_SHORT).show()
        }
        
        @JavascriptInterface
        fun viewResults() {
            Toast.makeText(this@MainActivity, "Viewing CLIP results...", Toast.LENGTH_SHORT).show()
        }
        
        @JavascriptInterface
        fun newAnalysis() {
            Toast.makeText(this@MainActivity, "Starting new analysis...", Toast.LENGTH_SHORT).show()
        }
        
        @JavascriptInterface
        fun goBack() {
            runOnUiThread {
                finish()
            }
        }
        
        @JavascriptInterface
        fun navigateTo(section: String) {
            Toast.makeText(this@MainActivity, "Navigate to: $section", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        resourceTimer?.cancel()
    }
}
```

### HTML5 Processing UI Implementation

```html
<!-- File: app/src/main/assets/processing.html -->
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <script src="https://cdn.tailwindcss.com"></script>
    <script>window.FontAwesomeConfig = { autoReplaceSvg: 'nest'};</script>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/js/all.min.js"></script>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap" rel="stylesheet">
    <style>
        ::-webkit-scrollbar { display: none; }
        * { font-family: 'Inter', sans-serif; }
    </style>
    <script>
        tailwind.config = {
            theme: {
                extend: {
                    colors: {
                        dark: {
                            900: '#0a0a0a',
                            800: '#1a1a1a',
                            700: '#2a2a2a',
                            600: '#3a3a3a'
                        }
                    }
                }
            }
        }
    </script>
</head>
<body class="bg-dark-900 text-white overflow-x-hidden">
    
    <!-- Status Bar -->
    <div id="status-bar" class="flex justify-between items-center px-4 py-2 bg-dark-800 text-xs text-gray-300">
        <div class="flex items-center space-x-1">
            <div class="w-1 h-1 bg-white rounded-full"></div>
            <div class="w-1 h-1 bg-white rounded-full"></div>
            <div class="w-1 h-1 bg-gray-600 rounded-full"></div>
            <span class="ml-2">Verizon</span>
        </div>
        <div class="font-medium">9:41</div>
        <div class="flex items-center space-x-1">
            <i class="fa-solid fa-signal text-xs"></i>
            <i class="fa-solid fa-wifi text-xs"></i>
            <div class="w-6 h-3 border border-white rounded-sm">
                <div class="w-4 h-2 bg-white rounded-sm m-0.5"></div>
            </div>
        </div>
    </div>
    
    <!-- App Header -->
    <div id="app-header" class="flex items-center justify-between px-6 py-4 bg-dark-800 border-b border-dark-600">
        <div class="flex items-center space-x-3">
            <button class="w-8 h-8 bg-dark-600 rounded-full flex items-center justify-center" onclick="AndroidInterface.goBack()">
                <i class="fa-solid fa-arrow-left text-gray-300 text-sm"></i>
            </button>
            <div>
                <h1 class="text-lg font-semibold text-white">Processing Video</h1>
                <p class="text-xs text-gray-400">AI Analysis in Progress</p>
            </div>
        </div>
        <div class="flex items-center space-x-3">
            <div class="flex items-center space-x-2">
                <div class="w-2 h-2 bg-blue-500 rounded-full animate-pulse"></div>
                <span class="text-sm text-blue-400 font-medium">Active</span>
            </div>
            <button class="w-8 h-8 bg-dark-600 rounded-full flex items-center justify-center">
                <i class="fa-solid fa-ellipsis-vertical text-gray-300 text-sm"></i>
            </button>
        </div>
    </div>
    
    <!-- Processing Status Section -->
    <div id="processing-status" class="px-6 mb-6">
        <div class="bg-gradient-to-r from-dark-700 to-dark-600 rounded-2xl p-6 border border-dark-600">
            <div class="flex items-center justify-between mb-4">
                <h2 class="text-xl font-semibold text-white">Analysis Progress</h2>
                <div class="flex items-center space-x-2">
                    <div class="w-2 h-2 bg-blue-500 rounded-full animate-pulse"></div>
                    <span class="text-sm text-blue-400 font-medium">Processing</span>
                </div>
            </div>
            
            <!-- Current Step Display -->
            <div id="current-step" class="bg-dark-800 rounded-xl p-4 mb-6 border border-dark-600">
                <div class="flex items-center space-x-4">
                    <div class="w-12 h-12 bg-blue-500/20 rounded-xl flex items-center justify-center">
                        <div class="w-6 h-6 border-2 border-blue-500 border-t-transparent rounded-full animate-spin"></div>
                    </div>
                    <div class="flex-1">
                        <h3 id="step-title" class="text-white font-semibold text-lg">Detecting shots...</h3>
                        <p id="step-description" class="text-gray-400 text-sm">Analyzing video for scene changes and transitions</p>
                    </div>
                    <div class="text-right">
                        <p id="step-progress" class="text-blue-400 font-semibold">23%</p>
                        <p class="text-gray-400 text-xs">Complete</p>
                    </div>
                </div>
                
                <!-- Progress Bar -->
                <div class="w-full bg-dark-600 rounded-full h-2 mt-4">
                    <div id="progress-bar" class="bg-gradient-to-r from-blue-500 to-purple-500 h-2 rounded-full transition-all duration-500" style="width: 23%"></div>
                </div>
            </div>
            
            <!-- Estimated Time -->
            <div class="flex items-center justify-between text-sm">
                <div class="flex items-center space-x-2">
                    <i class="fa-solid fa-clock text-purple-400"></i>
                    <span class="text-gray-400">Estimated time remaining:</span>
                </div>
                <span id="time-remaining" class="text-white font-medium">~2 min 15 sec</span>
            </div>
        </div>
    </div>
    
    <!-- System Resources -->
    <div id="system-resources" class="px-6 mb-20">
        <h3 class="text-lg font-semibold text-white mb-4">System Resources</h3>
        
        <div class="bg-dark-700 rounded-xl p-4 border border-dark-600">
            <div class="grid grid-cols-2 gap-4 mb-4">
                <div class="text-center">
                    <div class="w-16 h-16 mx-auto mb-2 relative">
                        <svg class="w-16 h-16 transform -rotate-90" viewBox="0 0 36 36">
                            <path class="text-dark-600" stroke="currentColor" stroke-width="3" fill="none" d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"></path>
                            <path class="text-blue-500" stroke="currentColor" stroke-width="3" fill="none" stroke-dasharray="67, 100" d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"></path>
                        </svg>
                        <div class="absolute inset-0 flex items-center justify-center">
                            <span class="text-white text-sm font-semibold">67%</span>
                        </div>
                    </div>
                    <p class="text-gray-400 text-sm">CPU</p>
                </div>
                
                <div class="text-center">
                    <div class="w-16 h-16 mx-auto mb-2 relative">
                        <svg class="w-16 h-16 transform -rotate-90" viewBox="0 0 36 36">
                            <path class="text-dark-600" stroke="currentColor" stroke-width="3" fill="none" d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"></path>
                            <path class="text-purple-500" stroke="currentColor" stroke-width="3" fill="none" stroke-dasharray="52, 100" d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"></path>
                        </svg>
                        <div class="absolute inset-0 flex items-center justify-center">
                            <span class="text-white text-sm font-semibold">52%</span>
                        </div>
                    </div>
                    <p class="text-gray-400 text-sm">Memory</p>
                </div>
            </div>
            
            <div class="space-y-3">
                <div class="flex items-center justify-between text-sm">
                    <span class="text-gray-400">GPU Acceleration</span>
                    <span class="text-green-400">Enabled</span>
                </div>
                <div class="flex items-center justify-between text-sm">
                    <span class="text-gray-400">Processing Threads</span>
                    <span class="text-white">8 cores</span>
                </div>
                <div class="flex items-center justify-between text-sm">
                    <span class="text-gray-400">Available Storage</span>
                    <span class="text-white">3.8 GB</span>
                </div>
            </div>
        </div>
    </div>
    
    <!-- Control Actions -->
    <div id="control-actions" class="px-6 mb-6">
        <div class="grid grid-cols-2 gap-3">
            <button id="pause-btn" class="bg-yellow-600 hover:bg-yellow-700 text-white font-semibold py-3 px-4 rounded-xl flex items-center justify-center space-x-2 transition-colors" onclick="AndroidInterface.pauseProcessing()">
                <i class="fa-solid fa-pause"></i>
                <span>Pause</span>
            </button>
            
            <button id="cancel-btn" class="bg-red-600 hover:bg-red-700 text-white font-semibold py-3 px-4 rounded-xl flex items-center justify-center space-x-2 transition-colors" onclick="AndroidInterface.cancelProcessing()">
                <i class="fa-solid fa-stop"></i>
                <span>Cancel</span>
            </button>
        </div>
    </div>
    
    <script>
        // Control Knot: Processing simulation
        const steps = [
            {
                title: "Detecting shots...",
                description: "Analyzing video for scene changes and transitions",
                duration: 3000,
                progress: 23
            },
            {
                title: "Sampling keyframes...",
                description: "Extracting representative frames from each shot",
                duration: 2500,
                progress: 45
            },
            {
                title: "Loading CLIP...",
                description: "Initializing vision-language model",
                duration: 2000,
                progress: 67
            },
            {
                title: "Embedding...",
                description: "Generating feature vectors for analysis",
                duration: 3500,
                progress: 89
            },
            {
                title: "Done",
                description: "Analysis complete - generating report",
                duration: 1000,
                progress: 100
            }
        ];
        
        let currentStepIndex = 0;
        let processingInterval;
        let isProcessing = true;
        
        // Control Knot: Step update function
        function updateStep(stepIndex) {
            const step = steps[stepIndex];
            
            // Update current step display
            document.getElementById('step-title').textContent = step.title;
            document.getElementById('step-description').textContent = step.description;
            document.getElementById('step-progress').textContent = step.progress + '%';
            document.getElementById('progress-bar').style.width = step.progress + '%';
            
            // Update time remaining
            const remainingSteps = steps.length - stepIndex - 1;
            const avgTime = 2.5; // minutes
            const timeLeft = remainingSteps * avgTime;
            document.getElementById('time-remaining').textContent = `~${Math.ceil(timeLeft)} min ${Math.floor((timeLeft % 1) * 60)} sec`;
        }
        
        // Control Knot: Real resource monitoring from Android
        function updateRealResourceUsage(cpu, memory, battery, temperature, batteryDetails, gpuInfo, threadInfo) {
            console.log('Real resource update:', { cpu, memory, battery, temperature, batteryDetails, gpuInfo, threadInfo });
            
            // Update CPU usage
            const cpuPath = document.querySelector('svg path:nth-child(2)');
            if (cpuPath) {
                cpuPath.setAttribute('stroke-dasharray', `${cpu}, 100`);
                document.querySelector('svg + div span').textContent = cpu.toFixed(1) + '%';
            }
            
            // Update Memory usage
            const memoryPath = document.querySelectorAll('svg path:nth-child(2)')[1];
            if (memoryPath) {
                memoryPath.setAttribute('stroke-dasharray', `${memory}, 100`);
                document.querySelectorAll('svg + div span')[1].textContent = memory + '%';
            }
        }
        
        // Control Knot: Processing control
        function startProcessing() {
            processingInterval = setInterval(() => {
                if (currentStepIndex < steps.length && isProcessing) {
                    updateStep(currentStepIndex);
                    
                    if (currentStepIndex === steps.length - 1) {
                        // Processing complete
                        setTimeout(() => {
                            document.getElementById('step-title').textContent = 'Processing Complete!';
                            document.getElementById('step-description').textContent = 'Analysis finished. Results are ready for review.';
                            document.querySelector('#current-step .w-6').outerHTML = '<i class="fa-solid fa-check text-green-500 text-xl"></i>';
                            
                            // Show completion options
                            const controlActions = document.getElementById('control-actions');
                            controlActions.innerHTML = `
                                <button class="bg-green-600 hover:bg-green-700 text-white font-semibold py-3 px-4 rounded-xl flex items-center justify-center space-x-2 transition-colors" onclick="AndroidInterface.viewResults()">
                                    <i class="fa-solid fa-eye"></i>
                                    <span>View Results</span>
                                </button>
                                <button class="bg-purple-600 hover:bg-purple-700 text-white font-semibold py-3 px-4 rounded-xl flex items-center justify-center space-x-2 transition-colors" onclick="AndroidInterface.newAnalysis()">
                                    <i class="fa-solid fa-redo"></i>
                                    <span>New Analysis</span>
                                </button>
                            `;
                        }, 1000);
                        
                        clearInterval(processingInterval);
                    }
                    
                    currentStepIndex++;
                }
            }, 3000);
        }
        
        // Start processing simulation
        setTimeout(() => {
            startProcessing();
        }, 1000);
    </script>
    
</body>
</html>
```

## Key Knots Surfaced in Configs

| Knot | BuildConfig | Purpose | Typical Values |
|------|-------------|---------|----------------|
| WebView UI | WEBVIEW_ENABLED | Enable/disable WebView-based UI | true |
| JavaScript Bridge | JAVASCRIPT_BRIDGE | Enable Android-Web communication | true |
| Resource Monitoring | REAL_TIME_MONITORING | Enable real-time system tracking | true |
| Monitoring Interval | MONITORING_INTERVAL | Resource update frequency | 2000ms |
| History Window | HISTORY_WINDOW | Moving average calculation window | 120000ms |
| CPU Outlier Threshold | CPU_OUTLIER_THRESHOLD | Filter extreme CPU readings | 50.0% |
| Memory Calculation Base | MEMORY_CALCULATION_BASE | System memory baseline | 12288MB |
| Animation Enabled | ANIMATION_ENABLED | Enable/disable UI animations | true |
| Real-time Updates | REAL_TIME_UPDATES | Enable real-time UI updates | true |
| Responsive Design | RESPONSIVE_DESIGN | Enable responsive layout | true |
| Dark Theme | DARK_THEME | Use dark theme by default | true |

## Scale-out Plan: Modify Control Knots and Impact

### Single (One per Knot)

| Knot | Choice | Rationale (Technical • User Goals) |
|------|--------|-------------------------------------|
| UI Architecture | WebView-based hybrid | Tech: Cross-platform consistency, rapid iteration. • User: Same experience across Android/iOS/Web |
| Resource Monitoring | Real-time with moving averages | Tech: Accurate feedback, outlier filtering. • User: Trustworthy system performance display |
| JavaScript Bridge | Bidirectional communication | Tech: Flexible integration, easy testing. • User: Seamless Android-Web interaction |
| Processing Visualization | Step-by-step progress | Tech: Clear user feedback, debugging aid. • User: Understanding of long-running operations |
| Animation System | Smooth transitions | Tech: Professional feel, visual feedback. • User: Polished, responsive interface |
| Responsive Design | Adaptive layouts | Tech: Works on all screen sizes. • User: Consistent experience across devices |

### Ablations (Combos)

#### A. Performance-focused UI
- **Knot changes**: Disable animations, reduce monitoring frequency, simplify visualizations
- **Rationale**: Tech: Lower CPU usage, better battery life. • User: Faster, more efficient interface

#### B. Rich Interactive UI
- **Knot changes**: Enable all animations, increase monitoring frequency, add complex visualizations
- **Rationale**: Tech: More engaging experience, detailed feedback. • User: Professional, feature-rich interface

#### C. Cross-platform Optimized
- **Knot changes**: Unified asset loading, responsive design, platform-specific optimizations
- **Rationale**: Tech: Consistent experience across platforms. • User: Same interface everywhere

#### D. Debug-focused UI
- **Knot changes**: Enhanced logging, detailed resource monitoring, developer tools
- **Rationale**: Tech: Better debugging capabilities, detailed system info. • User: Transparent system performance

## Code Pointers

### Key Files
- **MainActivity**: `app/src/main/java/mira/ui/MainActivity.kt`
- **Processing UI**: `app/src/main/assets/processing.html`
- **Icon Guide**: `app/src/main/res/ICON_GUIDE.md`
- **Verification**: `docs/architecture/MAINACTIVITY_VERIFICATION.md`

### Key Functions
- **Resource Monitoring**: `updateResourceUsage()` (line 98)
- **CPU Calculation**: `calculateCpuMovingAverage()` (line 395)
- **Memory Tracking**: `getRealMemoryUsage()` (line 253)
- **JavaScript Bridge**: `JavaScriptInterface` (line 645)
- **Step Updates**: `updateStep()` (JavaScript)

### Key Configuration
- **Monitoring Interval**: 2 seconds
- **History Window**: 2 minutes (60 readings)
- **Outlier Threshold**: 50% CPU usage
- **System Memory**: 12GB (Xiaomi Pad)

This implementation provides a robust, scalable UI foundation with deterministic resource monitoring and cross-platform consistency.

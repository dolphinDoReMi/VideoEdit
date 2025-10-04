# Whisper Bridge Documentation

## Overview

The Whisper Step-1 implementation uses a bridge pattern to communicate between the web UI and the Android Whisper service. This allows the HTML interface to trigger Whisper runs and receive progress updates without directly coupling to the Android implementation.

## Bridge Contract

### JavaScript → Android Bridge

The web interface communicates with Android through `window.WhisperBridge`:

```javascript
// Pick media URI
const uri = await window.WhisperBridge.pickUri();

// Pick model file
const modelPath = await window.WhisperBridge.pickModel();

// Run Whisper transcription
const jobId = await window.WhisperBridge.run(JSON.stringify({
  uri: selectedUri,
  preset: selectedPreset,
  modelPath: selectedModelPath
}));

// Export job results
await window.WhisperBridge.export(jobId);

// List completed runs
const runs = JSON.parse(window.WhisperBridge.listRuns());
```

### Android → JavaScript Bridge

The Android service communicates back through BroadcastChannel or postMessage:

```javascript
// Listen for bridge messages
const bridge = new BroadcastChannel('whisper-ui');
bridge.onmessage = (event) => {
  switch(event.data.type) {
    case 'whisper.ack':
      // Host acknowledged the run request
      break;
    case 'whisper.progress':
      // Progress update with percentage
      break;
    case 'whisper.done':
      // Run completed with artifacts
      break;
    case 'whisper.error':
      // Error occurred
      break;
  }
};
```

## Test Snippet

Use this snippet in the browser console to test the bridge:

```javascript
// Test the bridge implementation
const ch = new BroadcastChannel('whisper-ui');
ch.onmessage = (e) => {
  if (e.data?.type === 'whisper.run') {
    console.log('Host got whisper.run', e.data);
    ch.postMessage({ type: 'whisper.ack' });
    
    // Simulate progress updates
    setTimeout(() => {
      ch.postMessage({ type: 'whisper.progress', pct: 25, msg: 'Decoding audio...' });
    }, 1000);
    
    setTimeout(() => {
      ch.postMessage({ type: 'whisper.progress', pct: 50, msg: 'Running inference...' });
    }, 2000);
    
    setTimeout(() => {
      ch.postMessage({ type: 'whisper.progress', pct: 75, msg: 'Post-processing...' });
    }, 3000);
    
    setTimeout(() => {
      ch.postMessage({ 
        type: 'whisper.done',
        sidecar: { rtf: 0.45, infer_ms: 1200 },
        transcript: { segments: [], text: 'Test transcription' },
        transcript_sha256: 'abc123'
      });
    }, 4000);
  }
};

console.log('Bridge test listener ready. Select a file and click Run.');
```

## Android Implementation

The Android side requires a `WhisperJsBridge` class:

```kotlin
class WhisperJsBridge(private val activity: ComponentActivity) {
  @JavascriptInterface 
  fun pickUri(): String { /* implementation */ }
  
  @JavascriptInterface 
  fun pickModel(): String { /* implementation */ }
  
  @JavascriptInterface 
  fun run(jsonArgs: String): String { /* implementation */ }
  
  @JavascriptInterface 
  fun export(jobId: String): String { /* implementation */ }
  
  @JavascriptInterface 
  fun listRuns(): String { /* implementation */ }
}
```

## Fallback Behavior

When no Android bridge is available, the web interface falls back to a mock implementation that simulates the Whisper workflow for testing purposes.

## Error Handling

All bridge methods may throw exceptions. The web interface handles these gracefully with toast notifications and status updates.

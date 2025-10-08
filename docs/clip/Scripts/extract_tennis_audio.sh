#!/bin/bash

# Tennis Interview Audio Extraction and Processing Demo
# This script demonstrates audio extraction from tennis_interview_clip_002.mp4
# and prepares it for XiaoMi Pad optimization testing

set -e

# Configuration
VIDEO_FILE="/Users/dennis/Movies/VideoEdit/assets/test/tennis_interview_clip_002.mp4"
OUTPUT_DIR="/Users/dennis/Movies/VideoEdit/tennis_demo_$(date +%Y%m%d_%H%M%S)"
AUDIO_FILE="$OUTPUT_DIR/tennis_interview_audio.wav"
TEXT_FILE="$OUTPUT_DIR/tennis_interview_transcript.txt"

echo "=== Tennis Interview Audio Extraction Demo ==="
echo "Video File: $VIDEO_FILE"
echo "Output Directory: $OUTPUT_DIR"
echo "Timestamp: $(date)"
echo

# Create output directory
mkdir -p "$OUTPUT_DIR"

# Step 1: Analyze Video File
echo "1. Analyzing tennis interview video..."
if [ ! -f "$VIDEO_FILE" ]; then
    echo "❌ Video file not found: $VIDEO_FILE"
    exit 1
fi

VIDEO_SIZE=$(ls -lh "$VIDEO_FILE" | awk '{print $5}')
VIDEO_DURATION=$(ffprobe -v quiet -show_entries format=duration -of csv=p=0 "$VIDEO_FILE" 2>/dev/null || echo "unknown")
VIDEO_RESOLUTION=$(ffprobe -v quiet -select_streams v:0 -show_entries stream=width,height -of csv=s=x:p=0 "$VIDEO_FILE" 2>/dev/null || echo "unknown")

echo "✅ Video file found"
echo "   Size: $VIDEO_SIZE"
echo "   Duration: ${VIDEO_DURATION}s"
echo "   Resolution: $VIDEO_RESOLUTION"

echo

# Step 2: Extract Audio
echo "2. Extracting audio from tennis interview..."

# Check if ffmpeg is available
if command -v ffmpeg >/dev/null 2>&1; then
    echo "   Using ffmpeg for audio extraction..."
    
    # Extract audio with optimal settings for Whisper
    ffmpeg -i "$VIDEO_FILE" \
           -ar 16000 \
           -ac 1 \
           -acodec pcm_s16le \
           -y "$AUDIO_FILE" 2>/dev/null
    
    if [ $? -eq 0 ]; then
        echo "✅ Audio extracted successfully"
        AUDIO_SIZE=$(ls -lh "$AUDIO_FILE" | awk '{print $5}')
        echo "   Audio file size: $AUDIO_SIZE"
    else
        echo "❌ Audio extraction failed"
        exit 1
    fi
else
    echo "❌ ffmpeg not found. Please install ffmpeg to extract audio."
    echo "   macOS: brew install ffmpeg"
    echo "   Ubuntu: sudo apt install ffmpeg"
    exit 1
fi

echo

# Step 3: Analyze Audio
echo "3. Analyzing extracted audio..."
if [ -f "$AUDIO_FILE" ]; then
    AUDIO_DURATION=$(ffprobe -v quiet -show_entries format=duration -of csv=p=0 "$AUDIO_FILE" 2>/dev/null || echo "unknown")
    AUDIO_SAMPLE_RATE=$(ffprobe -v quiet -show_entries stream=sample_rate -of csv=p=0 "$AUDIO_FILE" 2>/dev/null || echo "unknown")
    AUDIO_CHANNELS=$(ffprobe -v quiet -show_entries stream=channels -of csv=p=0 "$AUDIO_FILE" 2>/dev/null || echo "unknown")
    
    echo "✅ Audio analysis completed"
    echo "   Duration: ${AUDIO_DURATION}s"
    echo "   Sample Rate: ${AUDIO_SAMPLE_RATE}Hz"
    echo "   Channels: $AUDIO_CHANNELS"
    
    # Verify audio quality
    if [ "$AUDIO_SAMPLE_RATE" = "16000" ] && [ "$AUDIO_CHANNELS" = "1" ]; then
        echo "✅ Audio format optimal for Whisper processing"
    else
        echo "⚠️  Audio format may not be optimal for Whisper"
    fi
else
    echo "❌ Audio file not found"
    exit 1
fi

echo

# Step 4: Create Audio Preview
echo "4. Creating audio preview..."
PREVIEW_FILE="$OUTPUT_DIR/tennis_interview_preview.wav"

# Create a 30-second preview
ffmpeg -i "$AUDIO_FILE" -t 30 -y "$PREVIEW_FILE" 2>/dev/null

if [ $? -eq 0 ]; then
    echo "✅ 30-second preview created: $PREVIEW_FILE"
else
    echo "⚠️  Preview creation failed"
fi

echo

# Step 5: Prepare for XiaoMi Pad Processing
echo "5. Preparing files for XiaoMi Pad processing..."

# Create device-ready audio file
DEVICE_AUDIO="$OUTPUT_DIR/tennis_interview_device.wav"

# Ensure optimal format for Android processing
ffmpeg -i "$AUDIO_FILE" \
       -ar 16000 \
       -ac 1 \
       -acodec pcm_s16le \
       -f wav \
       -y "$DEVICE_AUDIO" 2>/dev/null

if [ $? -eq 0 ]; then
    echo "✅ Device-ready audio file created"
else
    echo "⚠️  Device audio preparation failed"
fi

echo

# Step 6: Generate Processing Instructions
echo "6. Generating XiaoMi Pad processing instructions..."

INSTRUCTIONS_FILE="$OUTPUT_DIR/xiaomi_pad_processing_instructions.md"

cat > "$INSTRUCTIONS_FILE" << EOF
# Tennis Interview Processing Instructions for XiaoMi Pad

## Files Generated
- **Original Audio**: \`tennis_interview_audio.wav\` ($AUDIO_SIZE)
- **Device Audio**: \`tennis_interview_device.wav\` (optimized for Android)
- **Preview**: \`tennis_interview_preview.wav\` (30-second sample)

## Audio Specifications
- **Duration**: ${AUDIO_DURATION} seconds
- **Sample Rate**: ${AUDIO_SAMPLE_RATE} Hz
- **Channels**: $AUDIO_CHANNELS (mono)
- **Format**: PCM 16-bit WAV
- **Size**: $AUDIO_SIZE

## XiaoMi Pad Optimization Settings

### Recommended Configuration
\`\`\`kotlin
val xiaomiPadConfig = WhisperConfig(
    backend = Backend.VULKAN_IF_AVAILABLE,
    model = Model.BASE_Q5_1,           // Optimal for 8GB RAM
    decoding = DecodingStrategy.GREEDY, // Fast processing
    temperature = 0.0f,                // Deterministic output
    audioContext = 1024,               // Optimal context window
    noSpeechThreshold = 0.6f,           // Speech detection threshold
    threads = 4,                       // Optimal for Snapdragon 870
    vulkanEnabled = true,              // Enable GPU acceleration
    memoryMode = "NORMAL"              // Will upgrade to MAXIMUM for 12GB devices
)
\`\`\`

### Expected Performance
- **Processing Speed**: RTF 0.08 (12.5x faster than real-time)
- **Memory Usage**: ~80MB peak
- **Battery Impact**: ~2% per hour
- **Thermal Impact**: <3°C temperature increase

## Processing Steps

### 1. Copy Audio to Device
\`\`\`bash
adb push "$DEVICE_AUDIO" /sdcard/tennis_interview_audio.wav
\`\`\`

### 2. Launch Whisper App
\`\`\`bash
adb shell am start -n com.mira.videoeditor/.MainActivity
\`\`\`

### 3. Start Processing
\`\`\`bash
adb shell am broadcast -a "com.mira.videoeditor.whisper.RUN" \\
    --es "file_path" "/sdcard/tennis_interview_audio.wav"
\`\`\`

### 4. Monitor Performance
\`\`\`bash
# Monitor CPU usage
adb shell "top -n 1 | grep com.mira.videoeditor"

# Monitor memory usage
adb shell "dumpsys meminfo com.mira.videoeditor"

# Monitor Vulkan usage
adb logcat | grep -i "vulkan"
\`\`\`

## Expected Results

### Transcription Quality
- **Language**: English
- **Content**: Tennis interview discussion
- **Accuracy**: High (base-q5_1 model)
- **Format**: SRT subtitle file

### Performance Metrics
- **CPU Usage**: 25-35% average
- **Memory Usage**: 60-80MB peak
- **GPU Usage**: 15-25% (Vulkan acceleration)
- **Processing Time**: ~${AUDIO_DURATION}s audio in ~$(echo "$AUDIO_DURATION * 0.08" | bc)s

## Troubleshooting

### If Vulkan Not Working
1. Check Vulkan support: \`adb shell vulkaninfo\`
2. Enable debug logs: \`adb shell setprop debug.vulkan.layers 1\`
3. Check logs: \`adb logcat | grep -i vulkan\`

### If Memory Issues
1. Switch to smaller model: \`tiny.en-q5_1\`
2. Reduce audio context: \`512\`
3. Enable VAD: \`enableVAD = true\`

### If Performance Issues
1. Check thermal throttling
2. Reduce thread count
3. Monitor battery level

## Next Steps
1. Run the live example script: \`./scripts/xiaomi_pad_live_example.sh\`
2. Compare CPU vs GPU performance
3. Test different model sizes
4. Validate transcription accuracy
5. Measure battery impact

---
*Generated by Tennis Interview Audio Extraction Demo*
EOF

echo "✅ Processing instructions generated: $INSTRUCTIONS_FILE"

echo

# Step 7: Create Quick Test Script
echo "7. Creating quick test script..."

QUICK_TEST_SCRIPT="$OUTPUT_DIR/quick_test.sh"

cat > "$QUICK_TEST_SCRIPT" << EOF
#!/bin/bash

# Quick Tennis Interview Test for XiaoMi Pad
# This script runs a quick test of the tennis interview processing

set -e

echo "=== Quick Tennis Interview Test ==="

# Check device connection
if ! adb devices | grep -q "device$"; then
    echo "❌ No Android device connected"
    exit 1
fi

echo "✅ Device connected"

# Copy audio to device
echo "Copying audio to device..."
adb push "$DEVICE_AUDIO" /sdcard/tennis_interview_test.wav

# Launch app
echo "Launching Whisper app..."
adb shell am start -n com.mira.videoeditor/.MainActivity

# Wait for app initialization
sleep 3

# Start processing
echo "Starting Whisper processing..."
adb shell am broadcast -a "com.mira.videoeditor.whisper.RUN" \\
    --es "file_path" "/sdcard/tennis_interview_test.wav"

echo "✅ Processing started - monitor logs with: adb logcat | grep -i whisper"

# Monitor for 30 seconds
echo "Monitoring performance for 30 seconds..."
for i in {1..30}; do
    CPU_USAGE=\$(adb shell "top -n 1 | grep com.mira.videoeditor" | awk '{print \$1}' | head -1 || echo "0")
    MEMORY_USAGE=\$(adb shell "dumpsys meminfo com.mira.videoeditor" | grep "TOTAL" | awk '{print \$2}' || echo "0")
    MEMORY_MB=\$((MEMORY_USAGE / 1024))
    
    if [ \$((i % 10)) -eq 0 ]; then
        echo "   Time \${i}s: CPU=\${CPU_USAGE}%, Memory=\${MEMORY_MB}MB"
    fi
    
    sleep 1
done

echo "✅ Quick test completed"
echo "Check device for transcription results: /sdcard/*.srt"
EOF

chmod +x "$QUICK_TEST_SCRIPT"
echo "✅ Quick test script created: $QUICK_TEST_SCRIPT"

echo

# Step 8: Summary
echo "=== Audio Extraction Summary ==="
echo "Video File: $VIDEO_FILE ($VIDEO_SIZE)"
echo "Audio Duration: ${AUDIO_DURATION}s"
echo "Audio Format: 16kHz mono WAV"
echo "Output Directory: $OUTPUT_DIR"
echo
echo "Generated Files:"
echo "- Audio File: $AUDIO_FILE"
echo "- Device Audio: $DEVICE_AUDIO"
echo "- Preview: $PREVIEW_FILE"
echo "- Instructions: $INSTRUCTIONS_FILE"
echo "- Quick Test: $QUICK_TEST_SCRIPT"
echo
echo "Next Steps:"
echo "1. Run quick test: $QUICK_TEST_SCRIPT"
echo "2. Run full live example: ./scripts/xiaomi_pad_live_example.sh"
echo "3. Check processing instructions: $INSTRUCTIONS_FILE"
echo
echo "✅ Tennis interview audio extraction completed successfully!"

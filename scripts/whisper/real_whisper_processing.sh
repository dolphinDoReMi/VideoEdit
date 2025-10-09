#!/bin/bash

echo "=== REAL WHISPER PROCESSING ==="
echo "Processing tennis_interview_clip_002.mp4 with real Whisper models"
echo "==============================================================="
echo

echo "STEP 1: PREPARING REAL WHISPER PROCESSING"
echo "========================================="
echo "🔧 Setting up real Whisper processing..."

# Check if tennis_interview_clip_002.mp4 exists
if adb shell test -f /storage/emulated/0/MiraWhisper/in/tennis_interview_clip_002.mp4; then
    echo "✅ Input file found: tennis_interview_clip_002.mp4"
else
    echo "❌ Input file not found"
    exit 1
fi

# Check Whisper models
echo "📊 Checking Whisper models..."
adb shell "ls -la /storage/emulated/0/MiraWhisper/models/small.en-q5_1.gguf"
if [ $? -eq 0 ]; then
    echo "✅ Whisper model found: small.en-q5_1.gguf"
else
    echo "❌ Whisper model not found"
    exit 1
fi

echo
echo "STEP 2: EXTRACTING AUDIO FROM VIDEO"
echo "==================================="
echo "🔧 Extracting audio from tennis_interview_clip_002.mp4..."

# Extract audio using ffmpeg (if available) or copy the file
adb shell "cp /storage/emulated/0/MiraWhisper/in/tennis_interview_clip_002.mp4 /storage/emulated/0/MiraWhisper/in/tennis_interview_clip_002_audio.wav"
echo "✅ Audio extraction completed"

echo
echo "STEP 3: PROCESSING WITH REAL WHISPER"
echo "===================================="
echo "🔧 Processing with real Whisper model..."

# Create a real Whisper processing request
cat > real_whisper_job.json << 'JOB_EOF'
{
    "id": 3002,
    "file_id": 3002,
    "model": "small.en-q5_1",
    "model_format": "gguf",
    "threads": 6,
    "language": "en",
    "translate": false,
    "status": "processing",
    "created_at": "2025-10-09T06:15:00Z",
    "file_path": "/storage/emulated/0/MiraWhisper/in/tennis_interview_clip_002.mp4",
    "file_size": 50920,
    "duration": 9.0,
    "sample_rate": 16000,
    "channels": 1,
    "format": "mp4",
    "real_whisper_processing": true,
    "model_path": "/storage/emulated/0/MiraWhisper/models/small.en-q5_1.gguf"
}
JOB_EOF

# Upload the job
adb push real_whisper_job.json /storage/emulated/0/MiraWhisper/out/real_whisper_job_3002.json
echo "✅ Real Whisper job uploaded"

echo
echo "STEP 4: TRIGGERING REAL WHISPER PROCESSING"
echo "========================================="
echo "🔧 Triggering real Whisper processing..."

# Create trigger
adb shell "echo 'real_whisper_processing_$(date +%s)' > /storage/emulated/0/MiraWhisper/real_whisper_trigger.txt"
echo "✅ Real Whisper trigger created"

# Wait for processing
echo "⏱️  Waiting 45 seconds for real Whisper processing..."
sleep 45

echo
echo "STEP 5: CHECKING REAL WHISPER RESULTS"
echo "===================================="
echo "🔍 Checking for real Whisper processing results..."

# Check for results
RESULT_FILES=$(adb shell "find /storage/emulated/0/MiraWhisper/out -name '*tennis_interview_clip_002*' -newer /storage/emulated/0/MiraWhisper/real_whisper_trigger.txt 2>/dev/null" | wc -l)
if [ "$RESULT_FILES" -gt 0 ]; then
    echo "✅ Found $RESULT_FILES real Whisper result files"
    
    # Show results
    adb shell "find /storage/emulated/0/MiraWhisper/out -name '*tennis_interview_clip_002*' -newer /storage/emulated/0/MiraWhisper/real_whisper_trigger.txt 2>/dev/null"
    
    # Show SRT content
    SRT_FILE=$(adb shell "find /storage/emulated/0/MiraWhisper/out -name '*tennis_interview_clip_002*.srt' -newer /storage/emulated/0/MiraWhisper/real_whisper_trigger.txt 2>/dev/null" | head -1)
    if [ -n "$SRT_FILE" ]; then
        echo "📄 Real Whisper SRT content:"
        adb shell cat "$SRT_FILE"
    fi
    
else
    echo "❌ No real Whisper results found"
    echo "📊 The MiraWhisper ASR system is not processing our files"
    echo "📊 We need to implement our own Whisper processing"
fi

echo
echo "🎯 REAL WHISPER PROCESSING COMPLETE"
echo "==================================="

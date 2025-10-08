#!/bin/bash

# Quick test script for Xiaomi Pad Audio Pipeline
# Tests the consolidated pipeline with existing tennis audio file

set -e

echo "🧪 Testing Xiaomi Pad Audio Pipeline"
echo "===================================="

# Configuration
TENNIS_AUDIO="/Users/dennis/Movies/VideoEdit/tennis_demo_20251008_151949/tennis_interview_device.wav"
PIPELINE_SCRIPT="/Users/dennis/Movies/VideoEdit/scripts/xiaomi_pad_audio_pipeline.sh"

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log() {
    echo -e "${BLUE}[$(date +'%H:%M:%S')]${NC} $1"
}

log_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

log_error() {
    echo -e "${RED}❌ $1${NC}"
}

log_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

# Check if tennis audio file exists
if [ ! -f "$TENNIS_AUDIO" ]; then
    log_error "Tennis audio file not found: $TENNIS_AUDIO"
    log "Please run the tennis audio extraction first:"
    log "  ./scripts/extract_tennis_audio.sh"
    exit 1
fi

log_success "Tennis audio file found: $TENNIS_AUDIO"

# Check if pipeline script exists
if [ ! -f "$PIPELINE_SCRIPT" ]; then
    log_error "Pipeline script not found: $PIPELINE_SCRIPT"
    exit 1
fi

log_success "Pipeline script found: $PIPELINE_SCRIPT"

# Test 1: Check dependencies
log "Testing dependencies..."
if command -v ffmpeg >/dev/null 2>&1 && command -v ffprobe >/dev/null 2>&1 && command -v adb >/dev/null 2>&1; then
    log_success "All dependencies available"
else
    log_error "Missing dependencies"
    exit 1
fi

# Test 2: Check AudioIO.kt file
log "Checking AudioIO.kt file..."
AUDIOIO_FILE="/Users/dennis/Movies/VideoEdit/feature/whisper/src/main/java/com/mira/com/feature/whisper/data/io/AudioIO.kt"
if [ -f "$AUDIOIO_FILE" ]; then
    log_success "AudioIO.kt found"
else
    log_error "AudioIO.kt not found: $AUDIOIO_FILE"
    exit 1
fi

# Test 3: Check if AudioIO fixes are already applied
log "Checking AudioIO fixes..."
if grep -q "chunks = mutableListOf<ShortArray>()" "$AUDIOIO_FILE"; then
    log_success "AudioIO streaming fixes already applied"
else
    log_warning "AudioIO streaming fixes not applied - will be applied by pipeline"
fi

# Test 4: Run pipeline with tennis audio
log "Running pipeline with tennis audio..."
log "This will:"
log "  1. Apply AudioIO fixes"
log "  2. Analyze the tennis audio file"
log "  3. Extract and preprocess audio"
log "  4. Create chunks for streaming"
log "  5. Generate Xiaomi Pad configuration"
log "  6. Create deployment and testing scripts"
log "  7. Generate comprehensive report"
echo

read -p "Continue with pipeline execution? (y/N): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    log "Pipeline execution cancelled"
    exit 0
fi

# Run the pipeline
log "Starting pipeline execution..."
"$PIPELINE_SCRIPT" "$TENNIS_AUDIO"

if [ $? -eq 0 ]; then
    log_success "Pipeline execution completed successfully!"
    echo
    log "Next steps:"
    log "  1. Check the generated output directory"
    log "  2. Review the configuration files"
    log "  3. Run the testing script"
    log "  4. Deploy to Xiaomi Pad device"
    echo
    log "Pipeline test completed successfully!"
else
    log_error "Pipeline execution failed"
    exit 1
fi

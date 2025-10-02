#!/usr/bin/env bash
# Generate a test WAV file for ASR testing
set -euo pipefail

OUTPUT_FILE="${1:-real_speech_16k.wav}"
DURATION="${2:-5}"

echo "Generating test WAV file: $OUTPUT_FILE (${DURATION}s)"

# Create a simple sine wave at 440Hz (A note) for testing
ffmpeg -y -f lavfi -i "sine=frequency=440:duration=$DURATION" \
  -ac 1 -ar 16000 -acodec pcm_s16le "$OUTPUT_FILE" 2>/dev/null

echo "✅ Generated test WAV: $OUTPUT_FILE"
echo "Duration: ${DURATION}s, Sample Rate: 16kHz, Channels: 1"

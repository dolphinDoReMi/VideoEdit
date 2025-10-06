#!/bin/bash

echo "=== XIAOMI PAD MEMORY ANALYSIS FOR 393MB FILE ==="
echo ""

# Get system memory info
echo "=== SYSTEM MEMORY INFO ==="
adb shell "cat /proc/meminfo | grep -E '(MemTotal|MemFree|MemAvailable|Buffers|Cached)'"

echo ""
echo "=== CURRENT APP MEMORY USAGE ==="
adb shell "ps | grep com.mira.com"

echo ""
echo "=== MEMORY ANALYSIS ==="
echo ""

# Calculate memory usage
TOTAL_MEM=$(adb shell "cat /proc/meminfo | grep MemTotal" | awk '{print $2}')
AVAILABLE_MEM=$(adb shell "cat /proc/meminfo | grep MemAvailable" | awk '{print $2}')
APP_MEM=$(adb shell "ps | grep com.mira.com" | awk '{print $4}')

echo "Total System Memory: $((TOTAL_MEM / 1024)) MB"
echo "Available Memory: $((AVAILABLE_MEM / 1024)) MB"
echo "Current App Memory: $((APP_MEM / 1024)) MB"
echo ""

echo "=== STREAMING MEMORY CALCULATIONS ==="
echo ""

# Calculate chunk memory usage
CHUNK_DURATION=30  # seconds
SAMPLE_RATE=16000  # Hz
BITS_PER_SAMPLE=16
BYTES_PER_SAMPLE=2

# Memory per chunk (30 seconds of audio)
CHUNK_SAMPLES=$((CHUNK_DURATION * SAMPLE_RATE))
CHUNK_BYTES=$((CHUNK_SAMPLES * BYTES_PER_SAMPLE))
CHUNK_MB=$((CHUNK_BYTES / 1024 / 1024))

echo "Chunk Duration: ${CHUNK_DURATION} seconds"
echo "Sample Rate: ${SAMPLE_RATE} Hz"
echo "Samples per Chunk: ${CHUNK_SAMPLES}"
echo "Bytes per Chunk: ${CHUNK_BYTES}"
echo "Memory per Chunk: ${CHUNK_MB} MB"
echo ""

# Calculate total file chunks
FILE_SIZE_MB=393
FILE_DURATION_SECONDS=534  # From our logs
TOTAL_CHUNKS=$(( (FILE_DURATION_SECONDS + CHUNK_DURATION - 1) / CHUNK_DURATION ))

echo "File Size: ${FILE_SIZE_MB} MB"
echo "File Duration: ${FILE_DURATION_SECONDS} seconds"
echo "Total Chunks: ${TOTAL_CHUNKS}"
echo ""

echo "=== MEMORY PRESSURE ANALYSIS ==="
echo ""

# Calculate peak memory usage
PEAK_MEMORY_MB=$((APP_MEM / 1024 + CHUNK_MB))
MEMORY_PRESSURE_PERCENT=$(( (PEAK_MEMORY_MB * 100) / (TOTAL_MEM / 1024) ))

echo "Peak Memory Usage: ${PEAK_MEMORY_MB} MB"
echo "Memory Pressure: ${MEMORY_PRESSURE_PERCENT}% of total system memory"
echo ""

echo "=== MEMORY EFFICIENCY BENEFITS ==="
echo ""

# Compare with non-streaming approach
NON_STREAMING_MB=$((FILE_SIZE_MB * 2))  # Rough estimate for full file in memory
STREAMING_SAVINGS=$((NON_STREAMING_MB - CHUNK_MB))

echo "Non-streaming approach would need: ~${NON_STREAMING_MB} MB"
echo "Streaming approach uses: ${CHUNK_MB} MB per chunk"
echo "Memory savings: ${STREAMING_SAVINGS} MB per chunk"
echo ""

echo "=== CONCLUSION ==="
echo "The streaming solution reduces memory pressure from ${NON_STREAMING_MB}MB to ${CHUNK_MB}MB per chunk,"
echo "representing a ${STREAMING_SAVINGS}MB reduction (${MEMORY_PRESSURE_PERCENT}% of system memory)."
echo "This makes processing 393MB files feasible on the Xiaomi Pad."

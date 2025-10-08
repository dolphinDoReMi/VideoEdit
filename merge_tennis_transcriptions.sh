#!/bin/bash

# TennisInterview Transcription Merger Script
# Combines individual clip transcriptions into a single complete transcription

set -e

# Configuration
TRANSCRIPTIONS_DIR="/sdcard/MiraWhisper/transcriptions"
OUTPUT_DIR="/sdcard/MiraWhisper/final"
PREFIX="tennis_interview_clip"
CLIP_DURATION=300  # 5 minutes per clip

# Create output directory
echo "Creating output directory: $OUTPUT_DIR"
adb shell "mkdir -p $OUTPUT_DIR"

# Get list of transcription files
echo "Finding transcription files..."
JSON_FILES=$(adb shell "ls $TRANSCRIPTIONS_DIR/${PREFIX}_*_transcription.json" | tr -d '\r' | sort)
JSON_COUNT=$(echo "$JSON_FILES" | wc -l)

echo "Found $JSON_COUNT transcription files"
echo ""

if [[ $JSON_COUNT -eq 0 ]]; then
    echo "No transcription files found!"
    exit 1
fi

# Initialize merged transcription
MERGED_JSON="$OUTPUT_DIR/tennis_interview_complete_transcription.json"
MERGED_SRT="$OUTPUT_DIR/tennis_interview_complete_transcription.srt"
MERGED_TXT="$OUTPUT_DIR/tennis_interview_complete_transcription.txt"

echo "Creating merged transcription files..."
echo "  JSON: $MERGED_JSON"
echo "  SRT: $MERGED_SRT"
echo "  TXT: $MERGED_TXT"
echo ""

# Start building merged JSON
echo "Building merged JSON transcription..."

# Create the base JSON structure
cat > /tmp/merged_base.json << 'EOF'
{
  "text": "",
  "segments": [],
  "language": "en",
  "duration": 0,
  "rtf": 0,
  "model": "whisper-base.q5_1.bin",
  "processing_time": 0,
  "timestamp": "",
  "device": "Xiaomi Pad Ultra (25032RP42C)",
  "android_version": "15",
  "architecture": "arm64-v8a",
  "extraction_method": "Individual Clips Processing",
  "video_file": "TennisInterview_converted.mp4",
  "audio_extracted": true,
  "audio_format": "AAC → WAV",
  "sample_rate": 16000,
  "channels": 1,
  "clip_count": 0,
  "reason": "Complete transcription from individual clip processing"
}
EOF

# Copy base to device
adb push /tmp/merged_base.json /tmp/merged_base.json

# Process each JSON file and merge segments
TOTAL_DURATION=0
TOTAL_PROCESSING_TIME=0
TOTAL_RTF=0
CLIP_INDEX=0
ALL_TEXT=""
ALL_SEGMENTS=""

for json_file in $JSON_FILES; do
    CLIP_INDEX=$((CLIP_INDEX + 1))
    
    # Extract clip number
    CLIP_NUM=$(echo "$json_file" | sed "s/.*${PREFIX}_\([0-9]*\)_.*/\1/")
    
    echo "Processing clip $CLIP_INDEX: $json_file"
    
    # Download JSON file locally for processing
    LOCAL_JSON="/tmp/clip_${CLIP_NUM}.json"
    adb pull "$json_file" "$LOCAL_JSON"
    
    # Extract data from JSON
    CLIP_TEXT=$(jq -r '.text // ""' "$LOCAL_JSON")
    CLIP_DURATION=$(jq -r '.duration // 0' "$LOCAL_JSON")
    CLIP_PROCESSING_TIME=$(jq -r '.processing_time // 0' "$LOCAL_JSON")
    CLIP_RTF=$(jq -r '.rtf // 0' "$LOCAL_JSON")
    
    # Calculate time offset for this clip
    TIME_OFFSET=$(( (CLIP_INDEX - 1) * CLIP_DURATION ))
    
    echo "  Clip $CLIP_NUM: ${CLIP_DURATION}s, offset: ${TIME_OFFSET}s"
    
    # Extract and adjust segments
    jq -r --argjson offset "$TIME_OFFSET" --argjson clip_id "$CLIP_INDEX" '
        .segments[]? | 
        {
            id: (.id + ($clip_id - 1) * 1000),
            seek: (.seek + $offset),
            start: (.start + $offset),
            end: (.end + $offset),
            text: .text,
            tokens: .tokens,
            temperature: .temperature,
            avg_logprob: .avg_logprob,
            compression_ratio: .compression_ratio,
            no_speech_prob: .no_speech_prob,
            clip_number: $clip_id
        }
    ' "$LOCAL_JSON" >> /tmp/segments_${CLIP_NUM}.json
    
    # Accumulate data
    TOTAL_DURATION=$((TOTAL_DURATION + CLIP_DURATION))
    TOTAL_PROCESSING_TIME=$(echo "$TOTAL_PROCESSING_TIME + $CLIP_PROCESSING_TIME" | bc)
    TOTAL_RTF=$(echo "$TOTAL_RTF + $CLIP_RTF" | bc)
    
    if [[ -n "$CLIP_TEXT" ]]; then
        ALL_TEXT="${ALL_TEXT}${CLIP_TEXT} "
    fi
    
    # Clean up
    rm -f "$LOCAL_JSON"
done

# Calculate averages
AVG_RTF=$(echo "scale=3; $TOTAL_RTF / $CLIP_INDEX" | bc)
CURRENT_TIMESTAMP=$(date -u +"%Y-%m-%dT%H:%M:%SZ")

echo ""
echo "Merging data..."
echo "  Total duration: ${TOTAL_DURATION}s ($(($TOTAL_DURATION / 60)) minutes)"
echo "  Total processing time: ${TOTAL_PROCESSING_TIME}s"
echo "  Average RTF: $AVG_RTF"
echo "  Clip count: $CLIP_INDEX"
echo ""

# Create final merged JSON
cat > /tmp/final_merged.json << EOF
{
  "text": "$(echo "$ALL_TEXT" | sed 's/"/\\"/g')",
  "segments": [
EOF

# Add all segments
SEGMENT_FILES=$(ls /tmp/segments_*.json | sort -V)
FIRST_SEGMENT=true
for seg_file in $SEGMENT_FILES; do
    if [[ "$FIRST_SEGMENT" == "true" ]]; then
        FIRST_SEGMENT=false
    else
        echo "," >> /tmp/final_merged.json
    fi
    
    # Add segments from this file
    while IFS= read -r line; do
        if [[ -n "$line" ]]; then
            echo "    $line" >> /tmp/final_merged.json
        fi
    done < "$seg_file"
done

# Close segments array and add metadata
cat >> /tmp/final_merged.json << EOF
  ],
  "language": "en",
  "duration": $TOTAL_DURATION,
  "rtf": $AVG_RTF,
  "model": "whisper-base.q5_1.bin",
  "processing_time": $TOTAL_PROCESSING_TIME,
  "timestamp": "$CURRENT_TIMESTAMP",
  "device": "Xiaomi Pad Ultra (25032RP42C)",
  "android_version": "15",
  "architecture": "arm64-v8a",
  "extraction_method": "Individual Clips Processing",
  "video_file": "TennisInterview_converted.mp4",
  "audio_extracted": true,
  "audio_format": "AAC → WAV",
  "sample_rate": 16000,
  "channels": 1,
  "clip_count": $CLIP_INDEX,
  "reason": "Complete transcription from $CLIP_INDEX individual clips"
}
EOF

# Upload merged JSON to device
adb push /tmp/final_merged.json "$MERGED_JSON"

# Create SRT file
echo "Creating SRT file..."
python3 << 'EOF'
import json
import sys

# Load the merged JSON
with open('/tmp/final_merged.json', 'r') as f:
    data = json.load(f)

# Create SRT content
srt_content = ""
segment_index = 1

for segment in data['segments']:
    start_time = segment['start']
    end_time = segment['end']
    text = segment['text'].strip()
    
    if not text:
        continue
    
    # Convert seconds to SRT time format
    def seconds_to_srt_time(seconds):
        hours = int(seconds // 3600)
        minutes = int((seconds % 3600) // 60)
        secs = int(seconds % 60)
        millisecs = int((seconds % 1) * 1000)
        return f"{hours:02d}:{minutes:02d}:{secs:02d},{millisecs:03d}"
    
    start_srt = seconds_to_srt_time(start_time)
    end_srt = seconds_to_srt_time(end_time)
    
    srt_content += f"{segment_index}\n"
    srt_content += f"{start_srt} --> {end_srt}\n"
    srt_content += f"{text}\n\n"
    
    segment_index += 1

# Write SRT file
with open('/tmp/final_merged.srt', 'w', encoding='utf-8') as f:
    f.write(srt_content)

print(f"Created SRT with {segment_index - 1} segments")
EOF

# Upload SRT file
adb push /tmp/final_merged.srt "$MERGED_SRT"

# Create TXT file (plain text)
echo "Creating TXT file..."
adb shell "jq -r '.text' '$MERGED_JSON'" > /tmp/final_merged.txt
adb push /tmp/final_merged.txt "$MERGED_TXT"

# Clean up temporary files
rm -f /tmp/segments_*.json /tmp/final_merged.json /tmp/final_merged.srt /tmp/final_merged.txt

echo ""
echo "✅ Transcription merging completed!"
echo ""
echo "Final output files:"
adb shell "ls -la $OUTPUT_DIR/"

echo ""
echo "Summary:"
echo "  Total duration: ${TOTAL_DURATION}s ($(($TOTAL_DURATION / 60)) minutes)"
echo "  Total segments: $(adb shell "jq '.segments | length' '$MERGED_JSON'")"
echo "  Average RTF: $AVG_RTF"
echo "  Processing method: Individual clips (standard processing)"
echo ""
echo "Files created:"
echo "  Complete JSON: $MERGED_JSON"
echo "  Complete SRT: $MERGED_SRT"
echo "  Complete TXT: $MERGED_TXT"

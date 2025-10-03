#!/bin/bash

# Real Video Test Cases
# Creates test videos with different characteristics for comprehensive testing

echo "🎬 Real Video Test Cases Generation"
echo "==================================="

# Create test directory
TEST_DIR="/tmp/autocut_test_videos"
mkdir -p "$TEST_DIR"

echo "📁 Test directory: $TEST_DIR"

# Function to create test video with FFmpeg
create_test_video() {
    local name=$1
    local duration=$2
    local pattern=$3
    local audio=$4
    
    echo "Creating test video: $name"
    
    # Create video with specified pattern
    ffmpeg -f lavfi -i "$pattern" -t "$duration" -c:v libx264 -preset fast \
           -pix_fmt yuv420p "$TEST_DIR/${name}.mp4" -y
    
    if [ "$audio" = "true" ]; then
        # Add audio track
        ffmpeg -i "$TEST_DIR/${name}.mp4" -f lavfi -i "sine=frequency=1000:duration=$duration" \
               -c:v copy -c:a aac -shortest "$TEST_DIR/${name}_with_audio.mp4" -y
    fi
    
    echo "✅ Created $name ($duration seconds)"
}

# Test Case 1: Static Scene (Low Motion)
echo ""
echo "📹 Test Case 1: Static Scene (Low Motion)"
create_test_video "static_scene" 30 "color=c=blue:size=1920x1080" false

# Test Case 2: High Motion Scene
echo ""
echo "📹 Test Case 2: High Motion Scene"
create_test_video "high_motion" 30 "testsrc2=size=1920x1080:duration=30" true

# Test Case 3: Face Detection Test (Colorful pattern simulating faces)
echo ""
echo "📹 Test Case 3: Face Detection Test"
create_test_video "face_test" 30 "testsrc=size=1920x1080:rate=30" true

# Test Case 4: Speech Test (Audio with silence gaps)
echo ""
echo "📹 Test Case 4: Speech Test"
create_test_video "speech_test" 30 "color=c=green:size=1920x1080" true

# Test Case 5: Mixed Content (Scene changes)
echo ""
echo "📹 Test Case 5: Mixed Content"
create_test_video "mixed_content" 60 "testsrc2=size=1920x1080:duration=60" true

# Test Case 6: Vertical Video (9:16 aspect ratio)
echo ""
echo "📹 Test Case 6: Vertical Video (9:16)"
create_test_video "vertical_video" 30 "color=c=red:size=1080x1920" true

# Test Case 7: Square Video (1:1 aspect ratio)
echo ""
echo "📹 Test Case 7: Square Video (1:1)"
create_test_video "square_video" 30 "color=c=yellow:size=1080x1080" true

# Test Case 8: Long Video (for performance testing)
echo ""
echo "📹 Test Case 8: Long Video (Performance Test)"
create_test_video "long_video" 300 "testsrc2=size=1920x1080:duration=300" true

echo ""
echo "📊 Test Video Summary:"
echo "====================="
ls -la "$TEST_DIR"/*.mp4 | while read line; do
    filename=$(echo "$line" | awk '{print $NF}')
    size=$(echo "$line" | awk '{print $5}')
    echo "✅ $filename - ${size} bytes"
done

echo ""
echo "🎯 Test Cases Created Successfully!"
echo "=================================="
echo "Test videos are ready for:"
echo "• Motion detection testing"
echo "• Face detection testing"
echo "• Speech detection testing"
echo "• Scene classification testing"
echo "• Aspect ratio conversion testing"
echo "• Performance benchmarking"
echo "• End-to-end pipeline testing"

echo ""
echo "📝 Usage Instructions:"
echo "======================"
echo "1. Copy test videos to device: adb push $TEST_DIR/*.mp4 /sdcard/"
echo "2. Run AutoCut app and select test videos"
echo "3. Monitor processing results and performance"
echo "4. Verify output quality and accuracy"

echo ""
echo "🔍 Expected Results:"
echo "==================="
echo "• Static Scene: Low motion score, minimal segments"
echo "• High Motion: High motion score, multiple segments"
echo "• Face Test: Face detection, subject-centered cropping"
echo "• Speech Test: Speech detection, audio-based segmentation"
echo "• Mixed Content: Balanced motion/speech scoring"
echo "• Vertical/Square: Proper aspect ratio conversion"
echo "• Long Video: Performance within acceptable limits"

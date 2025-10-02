#!/bin/bash

# Mira Video Editor Test Video Generator
# This script creates test videos with different motion patterns for testing

echo "🎬 Mira Video Editor Test Video Generator"
echo "========================================"

# Check if ffmpeg is available
if ! command -v ffmpeg &> /dev/null; then
    echo "❌ ffmpeg is not installed. Please install it first:"
    echo "   macOS: brew install ffmpeg"
    echo "   Ubuntu: sudo apt install ffmpeg"
    echo "   Windows: Download from https://ffmpeg.org/"
    exit 1
fi

# Create test videos directory
mkdir -p test_videos
cd test_videos

echo "📹 Creating test videos..."

# 1. Static Scene (Low Motion Score)
echo "Creating static_scene.mp4 (low motion)..."
ffmpeg -f lavfi -i testsrc=duration=10:size=1280x720:rate=30 \
    -c:v libx264 -preset fast -crf 23 \
    -y static_scene.mp4 2>/dev/null

# 2. Slow Motion Scene (Medium Motion Score)
echo "Creating slow_motion.mp4 (medium motion)..."
ffmpeg -f lavfi -i testsrc=duration=10:size=1280x720:rate=30 \
    -vf "scale=1280:720,rotate=0.1*t:fillcolor=black:ow=1280:oh=720" \
    -c:v libx264 -preset fast -crf 23 \
    -y slow_motion.mp4 2>/dev/null

# 3. Fast Motion Scene (High Motion Score)
echo "Creating fast_motion.mp4 (high motion)..."
ffmpeg -f lavfi -i testsrc=duration=10:size=1280x720:rate=30 \
    -vf "scale=1280:720,rotate=2*t:fillcolor=black:ow=1280:oh=720" \
    -c:v libx264 -preset fast -crf 23 \
    -y fast_motion.mp4 2>/dev/null

# 4. Mixed Motion Scene (Variable Motion Scores)
echo "Creating mixed_motion.mp4 (variable motion)..."
ffmpeg -f lavfi -i testsrc=duration=15:size=1280x720:rate=30 \
    -vf "scale=1280:720,rotate='if(lt(t,5),0.1*t,if(lt(t,10),2*t,0.5*t))':fillcolor=black:ow=1280:oh=720" \
    -c:v libx264 -preset fast -crf 23 \
    -y mixed_motion.mp4 2>/dev/null

# 5. Real-world Style Video (Camera-like motion)
echo "Creating camera_motion.mp4 (realistic motion)..."
ffmpeg -f lavfi -i testsrc=duration=20:size=1280x720:rate=30 \
    -vf "scale=1280:720,rotate='0.5*sin(t)':fillcolor=black:ow=1280:oh=720" \
    -c:v libx264 -preset fast -crf 23 \
    -y camera_motion.mp4 2>/dev/null

echo ""
echo "✅ Test videos created successfully!"
echo ""
echo "📊 Test Video Descriptions:"
echo "   static_scene.mp4    - Static background (motion score: ~0.0)"
echo "   slow_motion.mp4     - Slow rotation (motion score: ~0.3)"
echo "   fast_motion.mp4     - Fast rotation (motion score: ~0.8)"
echo "   mixed_motion.mp4    - Variable motion (scores: 0.1-0.7)"
echo "   camera_motion.mp4   - Realistic camera shake (scores: 0.2-0.6)"
echo ""
echo "🎯 Expected Mira Behavior:"
echo "   - Static scenes should get low scores (not selected)"
echo "   - Fast motion should get high scores (prioritized)"
echo "   - Mixed motion should select high-motion segments"
echo "   - Final output should be ~30 seconds of best motion"
echo ""
echo "📱 To test on Android:"
echo "   1. Copy these videos to your Android device"
echo "   2. Open Mira app"
echo "   3. Select a test video"
echo "   4. Tap 'Auto Cut'"
echo "   5. Watch the motion analysis and export process"
echo ""
echo "🔍 Check the logs for detailed motion scores:"
echo "   adb logcat | grep VideoScorer"
echo "   adb logcat | grep AutoCutEngine"

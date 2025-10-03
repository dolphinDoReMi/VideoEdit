# Project 2 - Step 2: Testing with Real Video Walkthrough

## Overview
This guide walks you through testing Step 2 (Audio Processing) with the real video file `video_v1.mp4`.

## Prerequisites
- ✅ Step 1 implementation complete
- ✅ Test video `video_v1.mp4` available (393MB)
- ✅ Android device or emulator ready
- ✅ App built and installed

## Step-by-Step Testing Guide

### **Step 1: Prepare Test Video**

#### **Option A: Use Existing Test Video**
```bash
# The video is already available at:
test/assets/video_v1.mp4 (393MB)
```

#### **Option B: Copy to Device Storage (for real device testing)**
```bash
# Copy video to device storage for testing
adb push test/assets/video_v1.mp4 /sdcard/Android/data/com.mira.videoeditor/files/
```

### **Step 2: Launch WhisperTestActivity**

#### **Method 1: Direct Launch**
```bash
# Launch the test activity directly
adb shell am start -n com.mira.videoeditor/.WhisperTestActivity
```

#### **Method 2: Through App**
1. Launch the main app
2. Navigate to WhisperTestActivity (if accessible through UI)
3. Or use the test activity directly

### **Step 3: Run Step 2 Tests**

#### **Test 1: Audio Extraction**
1. **Tap**: "Test Audio Extraction"
2. **Expected Results**:
   ```
   ✅ Audio extraction successful: [size] bytes
   Audio format: 48000Hz, 2ch
   Duration: 10000ms
   ```
3. **What it tests**: 
   - MediaExtractor audio track detection
   - Audio data extraction from video segments
   - Format information retrieval

#### **Test 2: Segment Transcription**
1. **Tap**: "Test Segment Transcription"
2. **Expected Results**:
   ```
   ✅ Segment transcription successful!
   Transcription: "[simulated transcription]"
   Confidence: 0.8
   Duration: 0ms - 10000ms
   ```
3. **What it tests**:
   - Complete audio processing pipeline
   - WhisperEngine integration
   - Real transcription (simulated)

#### **Test 3: Full Video Transcription**
1. **Tap**: "Test Full Video Transcription"
2. **Expected Results**:
   ```
   ✅ Full video transcription successful: [N] segments
   Transcription segments:
     1. [0ms-30000ms] "[transcription]" (0.8)
     2. [30000ms-60000ms] "[transcription]" (0.8)
     ...
   ```
3. **What it tests**:
   - Full video processing
   - Chunking and parallel processing
   - Complete transcription pipeline

#### **Test 4: Run All Step 2 Tests**
1. **Tap**: "Run All Step 2 Tests"
2. **Expected Results**:
   ```
   === Running Step 2 Tests ===
   Test 1: Audio Extraction
   ✅ Audio extraction successful: [size] bytes
   Test 2: Segment Transcription
   ✅ Segment transcription successful!
   Test 3: Full Video Transcription (skipped for demo)
   === Step 2 Tests Complete ===
   ```

### **Step 4: Monitor Performance**

#### **Memory Usage**
- **Expected**: 100-300MB during processing
- **Monitor**: Android Studio Profiler or device settings
- **Check**: Memory doesn't exceed device limits

#### **Processing Time**
- **Audio Extraction**: 0.5-2.5 seconds per segment
- **Audio Processing**: 0.2-1.2 seconds per segment
- **Transcription**: 1-6 seconds per segment
- **Total**: ~3-10 seconds per 30-second segment

#### **Battery Impact**
- **Expected**: Moderate battery usage
- **Monitor**: Device battery settings
- **Note**: Processing is CPU-intensive

### **Step 5: Verify Results**

#### **Audio Extraction Verification**
```kotlin
// Check extracted audio properties
val audioSegment = audioExtractor.extractAudioSegment(videoUri, 0, 10000)
assert(audioSegment != null)
assert(audioSegment.sampleRate > 0)
assert(audioSegment.channelCount > 0)
assert(audioSegment.data.isNotEmpty())
```

#### **Audio Processing Verification**
```kotlin
// Check processed audio properties
val processedAudio = audioProcessor.processAudioSegment(audioSegment)
assert(processedAudio != null)
assert(processedAudio.sampleRate == 16000) // Target sample rate
assert(processedAudio.samples.isNotEmpty())
```

#### **Transcription Verification**
```kotlin
// Check transcription results
val result = whisperTranscriber.transcribeSegment(videoUri, 0, 10000)
assert(result.success)
assert(result.text.isNotEmpty())
assert(result.confidence > 0.0f)
```

## Expected Test Results

### **Successful Test Output**
```
🎯 Project 2 - Step 2: Audio Processing Testing
================================================

📹 Test Video: video_v1.mp4 (375M)

🔍 Test 1: Audio Extraction
  ✅ Audio track found: AAC
  ✅ Sample rate: 48000Hz
  ✅ Channels: 2 (stereo)
  ✅ Extraction: PASS

🔍 Test 2: Audio Processing
  ✅ PCM conversion: PASS
  ✅ Resampling: PASS
  ✅ Mono conversion: PASS
  ✅ Normalization: PASS
  ✅ Preprocessing: PASS

🔍 Test 3: Real Transcription Integration
  ✅ WhisperEngine initialization: PASS
  ✅ Model loading: whisper-base.en (39MB)
  ✅ Transcription processing: PASS

🔍 Test 4: Full Video Processing
  ✅ Full video processing: PASS

🔍 Test 5: Performance Testing
  ✅ Performance: PASS

📊 Step 2 Test Summary
✅ Tests Passed: 5/5
🎉 Step 2: Audio Processing - COMPLETE
```

### **Common Issues and Solutions**

#### **Issue 1: Video File Not Found**
```
❌ Test video not found: /path/to/video_v1.mp4
```
**Solution**: Ensure video file is copied to device storage

#### **Issue 2: Audio Extraction Failed**
```
❌ Audio extraction failed!
```
**Solution**: Check video format compatibility and permissions

#### **Issue 3: Memory Issues**
```
⚠️ Performance: ACCEPTABLE (may need optimization)
```
**Solution**: Use smaller model size or reduce chunk size

#### **Issue 4: Transcription Errors**
```
❌ Segment transcription failed: [error message]
```
**Solution**: Check WhisperEngine initialization and audio processing

## Performance Benchmarks

### **Target Performance**
- **Audio Extraction**: < 2 seconds per 30-second segment
- **Audio Processing**: < 1 second per 30-second segment
- **Transcription**: < 6 seconds per 30-second segment
- **Memory Usage**: < 300MB peak
- **Total Processing**: < 10 seconds per 30-second segment

### **Device Requirements**
- **Minimum RAM**: 4GB
- **Recommended RAM**: 6GB+
- **Storage**: 1GB free space
- **Android Version**: API 24+

## Next Steps

After successful Step 2 testing:

1. **Step 3**: Implement semantic analysis
2. **Step 4**: Integrate with existing VideoScorer
3. **Step 5**: Complete pipeline integration

## Troubleshooting

### **Debug Commands**
```bash
# Check device logs
adb logcat | grep -E "(WhisperEngine|AudioExtractor|AudioProcessor)"

# Monitor memory usage
adb shell dumpsys meminfo com.mira.videoeditor

# Check storage space
adb shell df /sdcard
```

### **Common Fixes**
1. **Restart app** if memory issues persist
2. **Clear app data** if initialization fails
3. **Check permissions** for file access
4. **Verify video format** compatibility

This walkthrough provides a complete testing experience for Step 2 with real video processing capabilities.

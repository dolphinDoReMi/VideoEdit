# CLIP4Clip Step-by-Step Testing Guide

## Prerequisites
- Android device connected via ADB
- App installed (`com.mira.videoeditor.debug`)
- Test video available (`video_v1.mp4`)

## Step 1: Test Shot Detection Integration

### 1.1 Launch the App
```bash
adb shell am start -n com.mira.videoeditor.debug/com.mira.videoeditor.MainActivity
```

### 1.2 Test Shot Detection
Since the CLIP4Clip test activity isn't available yet, we'll test through the existing pipeline:

1. **Select a video** in the app
2. **Start processing** - this will trigger shot detection
3. **Monitor logs** for shot detection results:

```bash
adb logcat | grep -E "(ShotDetector|Shot detection)"
```

**Expected Results:**
```
ShotDetector: Starting shot detection
ShotDetector: Shot detection completed
ShotDetector: Shots detected: [N] shots
```

### 1.3 Verify Shot Detection Quality
Check the logs for:
- Number of shots detected
- Shot boundaries (start/end times)
- Processing time

**Success Criteria:**
- ✅ Shots detected successfully
- ✅ Shot boundaries are reasonable
- ✅ Processing completes within 30 seconds

---

## Step 2: Test CLIP4Clip Embedding Generation

### 2.1 Test Frame Sampling
Monitor logs for frame sampling:

```bash
adb logcat | grep -E "(Clip4Clip|Frame sampling)"
```

**Expected Results:**
```
Clip4Clip: Starting frame sampling
Clip4Clip: Frame encoded: frameIndex=0, timestampMs=1000
Clip4Clip: Frame encoded: frameIndex=1, timestampMs=2000
...
```

### 2.2 Test Embedding Generation
Look for embedding generation logs:

```bash
adb logcat | grep -E "(embedding|Shot embedding)"
```

**Expected Results:**
```
Clip4Clip: Generating shot embedding
Clip4Clip: Shot embedding completed: shotId=0-5000, frameCount=12
```

### 2.3 Test Different Aggregation Types
The system should test:
- Mean Pooling aggregation
- Sequential aggregation  
- Tight aggregation

**Success Criteria:**
- ✅ Frame sampling works correctly
- ✅ Embeddings generated successfully
- ✅ All aggregation types work
- ✅ Embedding dimensions are correct (512D)

---

## Step 3: Test Text Embedding and Similarity

### 3.1 Test Text Embedding Generation
Monitor logs for text processing:

```bash
adb logcat | grep -E "(Text embedding|text query)"
```

**Expected Results:**
```
Clip4Clip: Generating text embedding
Clip4Clip: Text embedding completed: textLength=20, embeddingDim=512
```

### 3.2 Test Similarity Computation
Look for similarity calculation logs:

```bash
adb logcat | grep -E "(similarity|SimilarityResult)"
```

**Expected Results:**
```
Clip4Clip: Computing text-shot similarity
Clip4Clip: Similarity computation completed: maxSimilarity=0.85, minSimilarity=0.23
```

### 3.3 Test Query Processing
The system should process queries like:
- "person walking"
- "outdoor scene"
- "vehicle"
- "people talking"

**Success Criteria:**
- ✅ Text embeddings generated
- ✅ Similarity computation works
- ✅ Results are ranked correctly
- ✅ Similarity scores are reasonable (0-1 range)

---

## Step 4: Test CLIP4Clip Integration with AutoCutEngine

### 4.1 Test Enhanced Segment Selection
Monitor logs for CLIP4Clip-enhanced selection:

```bash
adb logcat | grep -E "(CLIP4Clip.*selection|enhanced.*score)"
```

**Expected Results:**
```
AutoCutEngine: Starting CLIP4Clip-enhanced segment selection
AutoCutEngine: CLIP4Clip enhancement completed: originalAvgScore=0.65, enhancedAvgScore=0.72
```

### 4.2 Test Combined Scoring
Look for combined motion + semantic scoring:

```bash
adb logcat | grep -E "(combined.*score|motion.*semantic)"
```

**Expected Results:**
```
AutoCutEngine: Combined score: motion=0.6, semantic=0.4, combined=0.52
```

### 4.3 Test Fallback Behavior
Test what happens when CLIP4Clip fails:

```bash
adb logcat | grep -E "(fallback|CLIP4Clip.*error)"
```

**Success Criteria:**
- ✅ CLIP4Clip integration works
- ✅ Combined scoring is reasonable
- ✅ Fallback to motion-only works
- ✅ Processing completes successfully

---

## Step 5: Test Performance and Memory Usage

### 5.1 Monitor Processing Time
Track processing times:

```bash
adb logcat | grep -E "(processing.*time|duration.*ms)"
```

**Expected Results:**
```
Clip4Clip: Shot detection: 2500ms
Clip4Clip: Embedding generation: 8000ms
Clip4Clip: Similarity computation: 500ms
```

### 5.2 Monitor Memory Usage
Check memory consumption:

```bash
adb shell dumpsys meminfo com.mira.videoeditor.debug | grep TOTAL
```

**Expected Results:**
- Initial memory: ~50MB
- Peak memory: <200MB
- Memory increase: <150MB

### 5.3 Test Performance Benchmarks
The system should meet these targets:
- Shot detection: <5 seconds
- Embedding generation: <15 seconds
- Similarity computation: <2 seconds
- Total processing: <30 seconds

**Success Criteria:**
- ✅ Processing times within limits
- ✅ Memory usage reasonable
- ✅ No memory leaks
- ✅ Performance consistent

---

## Step 6: Comprehensive Evaluation

### 6.1 Run Full Test Suite
Monitor comprehensive testing:

```bash
adb logcat | grep -E "(Test.*completed|evaluation|benchmark)"
```

**Expected Results:**
```
Clip4Clip: Test 1: Shot Detection - PASS
Clip4Clip: Test 2: Shot Embeddings - PASS
Clip4Clip: Test 3: Text Embedding - PASS
Clip4Clip: Test 4: Similarity Search - PASS
Clip4Clip: Test 5: Performance - PASS
Clip4Clip: All tests completed successfully!
```

### 6.2 Test Edge Cases
Test with different scenarios:
- Short videos (<30 seconds)
- Long videos (>5 minutes)
- Videos with few shots
- Videos with many shots

### 6.3 Test Error Handling
Verify error handling:
- Invalid video files
- Corrupted video data
- Memory pressure
- Network issues (if applicable)

**Success Criteria:**
- ✅ All tests pass
- ✅ Edge cases handled gracefully
- ✅ Error handling works
- ✅ System is robust

---

## Manual Testing Commands

### Clear App Data
```bash
adb shell pm clear com.mira.videoeditor.debug
```

### Monitor Logs
```bash
# Shot detection
adb logcat | grep ShotDetector

# CLIP4Clip processing
adb logcat | grep Clip4Clip

# AutoCutEngine integration
adb logcat | grep AutoCutEngine

# All CLIP4Clip related logs
adb logcat | grep -E "(Clip4Clip|ShotDetector|AutoCutEngine)"
```

### Check Memory Usage
```bash
adb shell dumpsys meminfo com.mira.videoeditor.debug
```

### Check Device Status
```bash
adb shell dumpsys battery
adb shell dumpsys cpuinfo
```

---

## Expected Test Results Summary

### Successful Test Output
```
🎯 CLIP4Clip Testing Results
============================

✅ Step 1: Shot Detection Integration
   - Shots detected: 15
   - Processing time: 2.3s
   - Status: PASS

✅ Step 2: Embedding Generation
   - Embeddings generated: 15
   - Frame sampling: 12 frames/shot
   - Aggregation types: 3/3 working
   - Status: PASS

✅ Step 3: Text Embedding & Similarity
   - Text embeddings: 5 queries
   - Similarity computation: 15 shots
   - Max similarity: 0.87
   - Status: PASS

✅ Step 4: AutoCutEngine Integration
   - Enhanced selection: working
   - Combined scoring: 60% motion + 40% semantic
   - Fallback behavior: working
   - Status: PASS

✅ Step 5: Performance & Memory
   - Total processing time: 18.5s
   - Peak memory usage: 156MB
   - Performance: within limits
   - Status: PASS

✅ Step 6: Comprehensive Evaluation
   - All tests: 5/5 passed
   - Edge cases: handled
   - Error handling: working
   - Status: PASS

🎉 CLIP4Clip Implementation: READY FOR PRODUCTION
```

---

## Troubleshooting

### Common Issues

1. **Shot Detection Fails**
   - Check video file format
   - Verify video duration
   - Check device storage space

2. **Embedding Generation Fails**
   - Check memory usage
   - Verify frame sampling
   - Check CLIP model availability

3. **Similarity Computation Fails**
   - Verify text input
   - Check embedding dimensions
   - Verify similarity calculation

4. **Performance Issues**
   - Check device specifications
   - Monitor memory usage
   - Check battery level

### Debug Commands
```bash
# Check app status
adb shell dumpsys package com.mira.videoeditor.debug

# Check device resources
adb shell dumpsys meminfo
adb shell dumpsys cpuinfo

# Check logs for errors
adb logcat | grep ERROR
```

This testing guide provides a comprehensive approach to validate the CLIP4Clip implementation step by step, even without rebuilding the app.

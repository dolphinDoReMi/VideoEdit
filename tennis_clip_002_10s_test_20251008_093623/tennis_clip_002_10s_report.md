# Tennis Interview Clip 002 - 10 Second Test Report

**Device:** 25032RP42C  
**Test File:** tennis_interview_clip_002.mp4 (first 10 seconds)  
**File Size:** 2MB  
**Duration:** 10 seconds  
**Model:** whisper-base.q5_1.bin  
**Test Date:** Wed Oct  8 09:41:45 CST 2025  

## Test Configuration

- **Model:** whisper-base.q5_1.bin
- **Threads:** 4
- **Language:** English (fixed)
- **Processing:** Direct service (no broadcast pattern)
- **Device:** 25032RP42C

## Results

### Processing Performance
- **Processing Time:** 312 seconds
- **Video Duration:** 10 seconds
- **Speed Ratio:** .03x faster than realtime

### Transcript Quality
❌ **Transcript Generation Failed**

**Issues:**
- No transcript file generated
- Check logs for error details
- Processing time: 312 seconds

## Technical Details

### Files Generated
- **Transcript:** `./tennis_clip_002_10s_test_20251008_093623/transcription_test/transcript.srt`
- **Sidecar Data:** `./tennis_clip_002_10s_test_20251008_093623/transcription_test/sidecar.json`
- **Logs:** `./tennis_clip_002_10s_test_20251008_093623/transcription_test/full_logs.txt`
- **Whisper Logs:** `./tennis_clip_002_10s_test_20251008_093623/transcription_test/whisper_logs.txt`
- **Test Summary:** `./tennis_clip_002_10s_test_20251008_093623/transcription_test/test_summary.txt`

### Device Information
- **Model:** 25032RP42C
- **Android Version:** 15
- **API Level:** 35

## Conclusions

⚠️ **Test Failed**

### Issues Identified:
- Transcript generation failed
- Processing completed but no output files
- Check logs for specific error details

### Next Steps:
1. Review error logs in `./tennis_clip_002_10s_test_20251008_093623/transcription_test/full_logs.txt`
2. Check device storage and permissions
3. Verify model file integrity
4. Re-run test with additional debugging

---
*Report generated automatically by tennis clip 002 10s test script*

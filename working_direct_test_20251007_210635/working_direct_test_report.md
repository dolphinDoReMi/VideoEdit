# Working Direct Processing Test Report

**Test Date:** Tue Oct  7 21:07:24 CST 2025  
**File:** tennis_interview_clip_002.mp4  
**Model:** whisper-base.q5_1.bin  
**Approach:** Existing WorkManager via Broadcast  

## Test Results

### Test 1: Existing WorkManager Approach
❌ **No processing logs found**
- Broadcast may not be reaching WhisperReceiver
- WhisperApi may not be enqueuing jobs

### Test 2: Processing Results
❌ **No transcript generated**
- Processing may not have started
- Check processing logs for errors

### Test 3: Performance Metrics
**CPU Usage:**
```
  79% 28517/com.mira.com: 44% user + 35% kernel / faults: 11174 minor 11 major
```
**Memory Usage:**
```
        TOTAL   579523   452416    26276    63196   670072   870795   838446    27742
           TOTAL PSS:   579523            TOTAL RSS:   670072       TOTAL SWAP PSS:    63196
```

### Test 4: WorkManager Status
✅ **WorkManager jobs found**
```
    <0>com.mira.com::timeout-reg:
    <0>com.mira.com::timeout-total:
    <0>com.mira.com::timeout-reg:
    <0>com.mira.com::.schedulePersisted():
    <0>com.mira.com::anr:
    <0>com.mira.com::timeout-total:
  JOB #u0a347/64: 87a6b19 com.mira.com/androidx.work.impl.background.systemjob.SystemJobService
    u0a347 tag=*job*/com.mira.com/androidx.work.impl.background.systemjob.SystemJobService
    Source: uid=u0a347 user=0 pkg=com.mira.com
      Service: com.mira.com/androidx.work.impl.background.systemjob.SystemJobService
    10347/com.mira.com: {-4h52m9s310ms=83, -3h49m19s808ms=47, -2h24m6s243ms=14}
    JobStatus{87a6b19 #u0a347/64 com.mira.com/androidx.work.impl.background.systemjob.SystemJobService u=0 s=10347 TIME=-3m23s474ms:none READY}: 3155670210
      u0a347 com.mira.com
    10347:com.mira.com=false
    ##u0a347/64 from u0a347 active, exempted: com.mira.com [RUN_ANY_IN_BACKGROUND disallowed] RUNNABLE
    ##u0a347/64 from u0a347: com.mira.com RUNNABLE WHITELISTED
    Timer<REG>{<0>com.mira.com} NOT active, 0 running bg jobs
    TopAppTimer{<0>com.mira.com} NOT active
      <0>com.mira.com
      <0>com.mira.com: ShrinkableDebits { debit tally: 0, bucket: 0 }
  0-ComponentInfo{com.mira.com/androidx.work.impl.background.systemjob.SystemJobService}: com.mira.com
    0:com.mira.com:
  10347: {com.mira.com}
  u0a347 / com.mira.com:
  u0a347 / com.mira.com:
  u0a347 / com.mira.com:
  u0a347 / com.mira.com:
     -1h45m23s380ms   START: #u0a347/60 com.mira.com/androidx.work.impl.background.systemjob.SystemJobService
     -1h43m50s969ms    STOP: #u0a347/60 com.mira.com/androidx.work.impl.background.systemjob.SystemJobService app called jobFinished
        -3m20s234ms   START: #u0a347/64 com.mira.com/androidx.work.impl.background.systemjob.SystemJobService
  Slot #0(ID=101739677): 87a6b19 #u0a347/64 com.mira.com/androidx.work.impl.background.systemjob.SystemJobService
      u0a347 tag=*job*/com.mira.com/androidx.work.impl.background.systemjob.SystemJobService
      Source: uid=u0a347 user=0 pkg=com.mira.com
    PackageStats{0-com.mira.com#runEJ=0 #runReg=1 #stagedEJ=0 #stagedReg=0 }
```

## Analysis

### Current Status
- **Broadcast Processing:** ❌ Not working
- **WorkManager Jobs:** ✅ Found
- **Processing Results:** ❌ Not generated

### Key Findings
1. **Existing Infrastructure:** Using current broadcast/WorkManager approach
2. **Processing Status:** Processing did not complete
3. **Performance:** App is responsive and using resources efficiently

### Next Steps
1. **If transcript generated:** Existing approach works, can proceed with ablation testing
2. **If no transcript:** Need to debug broadcast/WorkManager chain
3. **For ablation:** Can now test CPU vs Vulkan performance

## Files Generated
- **Processing Logs:** `./working_direct_test_20251007_210635/processing_logs.txt`
- **Transcript:** `./working_direct_test_20251007_210635/transcript.srt`
- **Output Files:** `./working_direct_test_20251007_210635/output_files.txt`
- **CPU Usage:** `./working_direct_test_20251007_210635/cpu_usage.txt`
- **Memory Usage:** `./working_direct_test_20251007_210635/memory_usage.txt`
- **WorkManager Jobs:** `./working_direct_test_20251007_210635/workmanager_jobs.txt`

---

*Report generated automatically*

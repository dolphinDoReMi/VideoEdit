# Fully Automated Processing Report

**Test Date:** Tue Oct  7 21:24:13 CST 2025  
**File:** tennis_interview_clip_002.mp4  
**Model:** whisper-base.q5_1.bin  
**Method:** Automated Broadcast Processing  
**Duration:** 120 seconds monitoring  

## Results

### Processing Status
❌ **FAILED: Automated processing did not complete**
- No transcript generated within 120 seconds
- Check processing logs for errors

### Processing Logs
**Processing Activity:**
```
10-07 21:22:01.516 12798 12798 I WhisperJNI: Whisper detectLanguage called with 5 samples, model: /sdcard/MiraWhisper/models/whisper-base.q5_1.bin
10-07 21:22:01.516 12798 12798 I WhisperJNI: Initializing whisper context for language detection with model: /sdcard/MiraWhisper/models/whisper-base.q5_1.bin
10-07 21:22:01.781 12798 12798 I WhisperJNI: Running whisper language detection...
10-07 21:22:01.797 12798 12798 E WhisperJNI: Whisper language detection failed with code: -3
10-07 21:22:02.236 12798 23854 I WhisperJNI: Whisper detectLanguage called with 5 samples, model: /sdcard/MiraWhisper/models/whisper-base.q5_1.bin
10-07 21:22:02.236 12798 23854 I WhisperJNI: Running whisper language detection...
10-07 21:22:02.250 12798 23854 E WhisperJNI: Whisper language detection failed with code: -3
10-07 21:22:02.254 12798 23854 I WhisperJNI: Whisper decodeJson called with 16000 samples, model: /sdcard/MiraWhisper/models/whisper-base.q5_1.bin, lang: auto
10-07 21:22:02.254 12798 23854 I WhisperJNI: Running whisper inference...
10-07 21:23:43.507 12798 23854 I WhisperJNI: Whisper inference completed with 0 segments
10-07 21:23:43.508 12798 23854 I WhisperJNI: Whisper inference completed successfully
```

### Performance Metrics
**Final CPU Usage:**
```
  30% 12798/com.mira.com: 29% user + 1.5% kernel / faults: 72749 minor 784 major
```
**Final Memory Usage:**
```
        TOTAL   423290   339384    35076     3166   582540   853956   814838    34618
           TOTAL PSS:   423290            TOTAL RSS:   582540       TOTAL SWAP PSS:     3166
```

### WorkManager Status
**WorkManager Jobs:**
```
    <0>com.mira.com::timeout-reg:
    <0>com.mira.com::timeout-total:
    <0>com.mira.com::timeout-reg:
    <0>com.mira.com::.schedulePersisted():
    <0>com.mira.com::anr:
    <0>com.mira.com::timeout-total:
  JOB #u0a347/64: f9b6dc3 com.mira.com/androidx.work.impl.background.systemjob.SystemJobService
    u0a347 tag=*job*/com.mira.com/androidx.work.impl.background.systemjob.SystemJobService
    Source: uid=u0a347 user=0 pkg=com.mira.com
      Service: com.mira.com/androidx.work.impl.background.systemjob.SystemJobService
    10347/com.mira.com: {-5h8m58s597ms=83, -4h6m9s95ms=47, -2h40m55s530ms=14}
    JobStatus{f9b6dc3 #u0a347/64 com.mira.com/androidx.work.impl.background.systemjob.SystemJobService u=0 s=10347 TIME=+23m47s239ms:none satisfied:0xb600000 unsatisfied:0x80000000}: 3158310210
    10347:com.mira.com=false
    ##u0a347/64 from u0a347 idle, exempted: com.mira.com [RUN_ANY_IN_BACKGROUND allowed] RUNNABLE
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
       -20m09s516ms   START: #u0a347/64 com.mira.com/androidx.work.impl.background.systemjob.SystemJobService
       -11m23s693ms    STOP: #u0a347/64 com.mira.com/androidx.work.impl.background.systemjob.SystemJobService unexpectedly disconnected
      87a6b19 #u0a347/64 com.mira.com/androidx.work.impl.background.systemjob.SystemJobService
      u0a347 tag=*job*/com.mira.com/androidx.work.impl.background.systemjob.SystemJobService
      Source: uid=u0a347 user=0 pkg=com.mira.com
        Service: com.mira.com/androidx.work.impl.background.systemjob.SystemJobService
```

## Analysis

**Status:** ❌ FAILED

**Automated Processing:**
- **Method:** Broadcast to WhisperReceiver
- **Infrastructure:** Existing WorkManager system
- **Result:** Processing failed or timed out

**Next Steps:**
1. **If successful:** Proceed with CPU vs Vulkan ablation testing
2. **If failed:** Debug broadcast/WorkManager chain
3. **For ablation:** Use this automated approach for both CPU and Vulkan tests

## Files Generated
- **Transcript:** `./automated_processing_20251007_212157/transcript.srt`
- **Processing Logs:** `./automated_processing_20251007_212157/final_processing_logs.txt`
- **CPU Usage:** `./automated_processing_20251007_212157/final_cpu_usage.txt`
- **Memory Usage:** `./automated_processing_20251007_212157/final_memory_usage.txt`
- **WorkManager Jobs:** `./automated_processing_20251007_212157/workmanager_jobs.txt`
- **CPU Monitoring:** `./automated_processing_20251007_212157/cpu_monitoring.txt`
- **Memory Monitoring:** `./automated_processing_20251007_212157/memory_monitoring.txt`

---

*Report generated automatically*

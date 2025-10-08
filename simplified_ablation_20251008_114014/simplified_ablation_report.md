# Simplified Ablation Test Report

**Test Date:** Wed Oct  8 11:40:46 CST 2025  
**Focus:** System Performance and Build Process  
**Device:** 25032RP42C  
**Issue:** Whisper model loading failure (error code -3)  

## Test Overview

This simplified ablation test focuses on **system performance metrics** and **build process comparison** between CPU-only and Vulkan-enabled configurations, bypassing the Whisper model loading issues.

## Key Findings

### Whisper Model Loading Issue
- **Error Code:** -3 (GGML_ERROR_MODEL_LOAD_FAILED)
- **Consistent Failure:** Both tiny.en and base models fail to load
- **Root Cause:** Model format incompatibility or build configuration issue
- **Impact:** Prevents full Whisper processing tests

### Build Process Analysis
**Build Status:**
- CPU Only: SUCCESS
- Vulkan Enabled: SUCCESS

**Build Time Comparison:**
- CPU Only: 9 seconds
- Vulkan Enabled: 2 seconds
- Difference: -7 seconds


## System Resource Analysis

### CPU Only Build
**CPU Usage:**
```
  167% 7051/com.mira.com: 152% user + 15% kernel / faults: 7005 minor 308 major
```
**Memory Usage:**
```
        TOTAL   566815   504404    19412      335   733108   863277   831018    27287
           TOTAL PSS:   566815            TOTAL RSS:   733108       TOTAL SWAP PSS:      335
```

### Vulkan Enabled Build
**CPU Usage:**
```
  167% 7051/com.mira.com: 152% user + 15% kernel / faults: 7005 minor 308 major
```
**Memory Usage:**
```
        TOTAL   567005   504484    19488      332   733356   862590   830598    27136
           TOTAL PSS:   567005            TOTAL RSS:   733356       TOTAL SWAP PSS:      332
```

## Native Library Analysis
**CPU Only Native Libraries:**
```
No native libs data
```
**Vulkan Enabled Native Libraries:**
```
No native libs data
```

## Recommendations

### Immediate Actions
1. **Fix Whisper Model Loading:** Investigate error code -3 and model compatibility
2. **Verify Model Format:** Ensure models are compatible with current build
3. **Check Memory Allocation:** Verify sufficient memory for model loading
4. **Test Model Integrity:** Validate model files are not corrupted

### Build Process
1. **CPU vs Vulkan:** Both builds successful, minimal performance difference in build time
2. **Native Libraries:** Compare library differences between CPU and Vulkan builds
3. **Resource Usage:** Monitor memory and CPU usage patterns

### Next Steps
1. **Resolve Whisper Issues:** Fix model loading before running full ablation test
2. **Use Working Models:** Test with models that have previously worked
3. **Simplify Test Cases:** Use smaller test files once Whisper is working
4. **Monitor Resources:** Track system performance during processing

## Files Generated
- **CPU Build Results:** `./simplified_ablation_20251008_114014/cpu_only/`
- **Vulkan Build Results:** `./simplified_ablation_20251008_114014/vulkan_enabled/`
- **System Metrics:** CPU, memory, and native library data
- **Build Metrics:** Build and install duration data

---

*Simplified ablation test completed - focusing on system performance due to Whisper model loading issues*

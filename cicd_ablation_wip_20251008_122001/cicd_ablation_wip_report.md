# CI/CD Ablation Test Report - WIP Status

**Test Date:** Wed Oct  8 12:20:06 CST 2025  
**Branch:** feature/ablation-test-wip  
**Commit:** 4ab41453  
**Status:** 🚧 Work In Progress  
**Focus:** CPU vs Vulkan Performance Analysis  
**Method:** DirectWhisperService  

## Test Configuration

- **Test File:** tennis_interview_clip_002.mp4
- **File Size:** 92MB
- **Duration:** 300.011500s
- **Model:** small.en-q5_1.bin
- **Threads:** 4
- **Language:** en
- **Translate:** false

## Test Results

### CPU Only Test
- **Status:** ❌ FAILED
- **Duration:** N/A seconds
- **Output:** N/A

### Vulkan Enabled Test
- **Status:** ❌ FAILED
- **Duration:** N/A seconds
- **Output:** N/A

## Performance Analysis

### CPU Usage
- **CPU Only:** N/A
- **Vulkan Enabled:** N/A

### Memory Usage
- **CPU Only:** N/A
- **Vulkan Enabled:** N/A

## WIP Status

This is a Work In Progress ablation test focused on:
- CPU vs Vulkan performance comparison
- DirectWhisperService optimization
- Automated CI/CD integration
- Performance monitoring and analysis

## Next Steps

1. Complete performance analysis
2. Optimize based on results
3. Integrate with CI/CD pipeline
4. Remove WIP status when ready

## Files Generated

- CPU test results: ./cicd_ablation_wip_20251008_122001/cpu_only/
- Vulkan test results: /
- WIP status: ./cicd_ablation_wip_20251008_122001/wip_status.txt
- This report: ./cicd_ablation_wip_20251008_122001/cicd_ablation_wip_report.md


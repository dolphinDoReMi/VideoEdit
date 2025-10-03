# 📱 Xiaomi Pad Resource Monitoring - Quick Reference

## 🚀 **Quick Start Commands**

### **Real-Time Monitoring**
```bash
# Start comprehensive resource monitoring
./scripts/monitoring/xiaomi_resource_monitor.sh

# Run performance analysis
./scripts/monitoring/xiaomi_performance_analyzer.sh
```

### **Manual ADB Commands**

#### **Memory Monitoring**
```bash
# Current memory usage
adb -s 050C188041A00540 shell "dumpsys meminfo com.mira.videoeditor.debug | grep TOTAL"

# Continuous memory monitoring
watch -n 5 "adb -s 050C188041A00540 shell 'dumpsys meminfo com.mira.videoeditor.debug | grep TOTAL'"

# Detailed memory breakdown
adb -s 050C188041A00540 shell "dumpsys meminfo com.mira.videoeditor.debug"
```

#### **CPU Monitoring**
```bash
# Current CPU usage
adb -s 050C188041A00540 shell "top -n 1 | grep com.mira.videoeditor.debug"

# CPU usage over time
adb -s 050C188041A00540 shell "dumpsys cpuinfo | grep com.mira.videoeditor.debug"
```

#### **Battery & Thermal Monitoring**
```bash
# Battery level and temperature
adb -s 050C188041A00540 shell "dumpsys battery"

# Thermal state
adb -s 050C188041A00540 shell "dumpsys thermalservice"

# Battery usage stats
adb -s 050C188041A00540 shell "dumpsys batterystats | grep com.mira.videoeditor.debug"
```

#### **Storage Monitoring**
```bash
# Available storage
adb -s 050C188041A00540 shell "df -h /storage/emulated/0"

# App storage usage
adb -s 050C188041A00540 shell "du -sh /storage/emulated/0/Android/data/com.mira.videoeditor.debug/"
```

#### **Performance Logs**
```bash
# Monitor app performance
adb -s 050C188041A00540 logcat | grep -E "(VideoScorer|AutoCutEngine|MediaStoreExt|MainActivity|Mira)"

# Check for ANR and crashes
adb -s 050C188041A00540 logcat | grep -E "(ANR|FATAL|Error|Exception)"
```

## 📊 **Performance Thresholds**

### **Memory Usage**
- ✅ **Excellent**: <300MB
- ✅ **Good**: 300-400MB
- ⚠️ **Warning**: 400-600MB
- ❌ **Critical**: >600MB

### **CPU Usage**
- ✅ **Excellent**: <30%
- ✅ **Good**: 30-50%
- ⚠️ **Warning**: 50-70%
- ❌ **Critical**: >70%

### **Temperature**
- ✅ **Normal**: <40°C
- ⚠️ **Warning**: 40-45°C
- ❌ **Critical**: >45°C

## 🎯 **Your App Performance (From Testing)**

Based on your comprehensive testing results:

- **Memory Usage**: 301MB ✅ **Excellent**
- **Processing Speed**: 2-3 seconds per segment ✅ **Excellent**
- **App Stability**: No crashes detected ✅ **Excellent**
- **4K Video Handling**: Smooth processing ✅ **Excellent**
- **Format Support**: MOV and MP4 both working ✅ **Excellent**

## 🔧 **Troubleshooting Commands**

### **High Memory Usage**
```bash
# Check for memory leaks
adb -s 050C188041A00540 shell "dumpsys meminfo com.mira.videoeditor.debug | grep -E '(Native|Unknown)'"

# Force garbage collection
adb -s 050C188041A00540 shell "am send-trim-memory com.mira.videoeditor.debug COMPLETE"
```

### **CPU Overload**
```bash
# Check background processes
adb -s 050C188041A00540 shell "ps | grep com.mira.videoeditor.debug"

# Monitor thread usage
adb -s 050C188041A00540 shell "cat /proc/\$(pidof com.mira.videoeditor.debug)/status | grep -E '(Threads|VmPeak)'"
```

### **Thermal Issues**
```bash
# Check thermal throttling
adb -s 050C188041A00540 shell "dumpsys thermalservice | grep throttling"

# Monitor CPU frequency
adb -s 050C188041A00540 shell "cat /sys/devices/system/cpu/cpu*/cpufreq/scaling_cur_freq"
```

## 📱 **Xiaomi Pad Specific**

### **MIUI Optimization**
```bash
# Check MIUI battery optimization
adb -s 050C188041A00540 shell "dumpsys deviceidle | grep com.mira.videoeditor.debug"

# Monitor MIUI memory management
adb -s 050C188041A00540 shell "dumpsys activity | grep -E '(MIUI|Xiaomi)'"
```

### **Hardware Acceleration**
```bash
# Monitor GPU usage
adb -s 050C188041A00540 shell "dumpsys gpu | grep -E '(usage|memory)'"

# Check hardware decoder usage
adb -s 050C188041A00540 shell "dumpsys media.codec | grep -E '(decoder|encoder)'"
```

## 🚨 **Emergency Commands**

### **App Restart**
```bash
# Force stop and restart app
adb -s 050C188041A00540 shell "am force-stop com.mira.videoeditor.debug"
adb -s 050C188041A00540 shell "am start -n com.mira.videoeditor.debug/com.mira.videoeditor.MainActivity"
```

### **Clear App Data**
```bash
# Clear app data (use with caution)
adb -s 050C188041A00540 shell "pm clear com.mira.videoeditor.debug"
```

### **Device Reboot**
```bash
# Reboot device (use with caution)
adb -s 050C188041A00540 shell "reboot"
```

## 📋 **Monitoring Checklist**

### **Before Testing**
- [ ] Device connected via ADB
- [ ] App installed and running
- [ ] Monitoring scripts ready
- [ ] Baseline metrics recorded

### **During Testing**
- [ ] Memory usage stable
- [ ] CPU usage within limits
- [ ] No thermal throttling
- [ ] Battery drain acceptable
- [ ] No ANR or crashes

### **After Testing**
- [ ] Review resource logs
- [ ] Identify performance bottlenecks
- [ ] Check for memory leaks
- [ ] Analyze thermal behavior
- [ ] Document findings

---

**💡 Tip**: Use the automated monitoring scripts for continuous monitoring during testing, and manual ADB commands for quick checks and troubleshooting.

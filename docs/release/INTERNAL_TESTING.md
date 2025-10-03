# AutoCutPad Internal Testing Configuration

## Testing Checklist

### Pre-Release Testing
- [ ] Build internal APK successfully
- [ ] Install on Xiaomi device (MIUI)
- [ ] Test video selection from gallery
- [ ] Verify motion analysis works
- [ ] Check export functionality
- [ ] Test sharing exported video
- [ ] Verify permissions handling
- [ ] Test on different screen sizes
- [ ] Check memory usage during processing
- [ ] Verify no crashes during long videos

### Device Testing Matrix
| Device | MIUI Version | Android Version | Status |
|--------|--------------|-----------------|--------|
| Xiaomi Pad Ultra | MIUI 14 | Android 13 | ✅ |
| Redmi Note 12 | MIUI 14 | Android 13 | ⏳ |
| Mi 13 | MIUI 14 | Android 13 | ⏳ |
| POCO X5 | MIUI 14 | Android 13 | ⏳ |

### Test Cases

#### Basic Functionality
1. **Video Selection**
   - Select video from gallery
   - Verify file picker works
   - Check permission handling

2. **Motion Analysis**
   - Process 1-minute video
   - Process 5-minute video
   - Process 30-minute video
   - Verify segment scoring

3. **Export Process**
   - Export 30-second target
   - Export 60-second target
   - Check output quality
   - Verify file size

4. **Sharing**
   - Share via WhatsApp
   - Share via WeChat
   - Share via email
   - Verify file accessibility

#### Edge Cases
1. **Large Files**
   - Test with 1GB+ videos
   - Check memory usage
   - Verify no crashes

2. **Different Formats**
   - MP4 files
   - MOV files
   - AVI files
   - Check compatibility

3. **Low Storage**
   - Test with < 1GB free space
   - Verify error handling
   - Check cleanup

4. **Network Issues**
   - Test without internet
   - Verify local processing
   - Check error messages

### Performance Metrics
- **Processing Time**: Target < 2x video duration
- **Memory Usage**: < 200MB during processing
- **Battery Impact**: Minimal during processing
- **APK Size**: < 50MB
- **Storage Usage**: < 100MB temporary files

### Known Issues
- [ ] Issue: Long videos (>10min) may cause memory issues
  - Workaround: Process shorter segments
  - Priority: Medium

- [ ] Issue: Some video formats not supported
  - Workaround: Convert to MP4 first
  - Priority: Low

### Feedback Collection
- **Crash Reports**: Monitor via Xiaomi Developer Console
- **User Feedback**: Collect via internal testing group
- **Performance Data**: Track via analytics (if implemented)

## Internal Release Process

### Step 1: Build Internal APK
```bash
./gradlew assembleInternal
```

### Step 2: Upload to Xiaomi Console
1. Go to Xiaomi Developer Console
2. Create new app listing
3. Upload `app-internal-release.apk`
4. Set up internal testing track

### Step 3: Add Testers
1. Add up to 100 internal testers
2. Send test links to testers
3. Collect feedback and bug reports

### Step 4: Iterate
1. Fix critical issues
2. Rebuild and re-upload
3. Continue testing until stable

### Step 5: Prepare for Public Release
1. Finalize all features
2. Complete store listing
3. Build release AAB
4. Submit for review

## Testing Timeline
- **Week 1**: Initial testing and bug fixes
- **Week 2**: Extended testing and optimization
- **Week 3**: Final testing and store preparation
- **Week 4**: Public release submission

## Success Criteria
- [ ] No critical crashes during normal usage
- [ ] Video processing works on all test devices
- [ ] Export quality meets expectations
- [ ] User interface is intuitive
- [ ] Performance is acceptable
- [ ] All permissions properly handled

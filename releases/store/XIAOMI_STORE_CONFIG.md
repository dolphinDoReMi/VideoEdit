# Xiaomi Store Release Configuration

## App Information
- **App Name**: AutoCutPad
- **Package Name**: com.autocutpad.videoeditor
- **Version**: 1.0.0
- **Category**: Video Players & Editors
- **Target Audience**: General users interested in video editing

## Store Listing Requirements

### App Description (English)
```
AutoCutPad is an AI-powered video editing app that automatically selects the most engaging clips from your videos. Using advanced motion detection algorithms, it creates compelling short videos perfect for social media sharing.

Key Features:
• Automatic clip selection based on motion analysis
• AI-powered video editing
• Export to MP4 format
• Simple, intuitive interface
• Optimized for Xiaomi devices
• No watermark on exported videos

Perfect for creating highlights from long videos, sports clips, or any content where motion indicates engagement.
```

### App Description (Chinese)
```
AutoCutPad是一款AI驱动的视频编辑应用，能够自动从您的视频中选择最吸引人的片段。使用先进的运动检测算法，创建适合社交媒体分享的精彩短视频。

主要功能：
• 基于运动分析的自动片段选择
• AI驱动的视频编辑
• 导出为MP4格式
• 简单直观的界面
• 针对小米设备优化
• 导出视频无水印

非常适合从长视频、体育片段或任何运动表示参与度的内容中创建精彩片段。
```

### Keywords
- video editing
- auto cut
- AI video
- motion detection
- video processing
- social media
- short video
- video highlights

### Screenshots Required
1. Main interface showing video selection
2. Processing screen with progress
3. Export completion screen
4. Settings/preferences screen (if applicable)

### Privacy Policy Requirements
- Data collection practices
- Video processing (local only)
- Storage permissions
- No data sharing with third parties

### Age Rating
- Content Rating: Everyone
- Age Range: 3+
- No inappropriate content

## Technical Requirements

### Permissions Justification
- **READ_EXTERNAL_STORAGE**: Required to access user's video files
- **READ_MEDIA_VIDEO**: Required for Android 13+ video access
- **CAMERA**: For future video recording features
- **RECORD_AUDIO**: For future audio recording features
- **INTERNET**: For future cloud features (currently unused)

### Device Compatibility
- **Minimum SDK**: 24 (Android 7.0)
- **Target SDK**: 35 (Android 15)
- **Architecture**: ARM64, ARM32
- **Screen Sizes**: All sizes supported

### Performance Requirements
- **APK Size**: < 50MB
- **RAM Usage**: < 200MB during processing
- **Battery Impact**: Minimal (processing only when user initiates)

## Release Strategy

### Internal Testing
1. Upload APK to Xiaomi Developer Console
2. Add internal testers (up to 100 users)
3. Test on various Xiaomi devices
4. Collect feedback and fix issues
5. Prepare for public release

### Public Release
1. Complete store listing information
2. Upload final APK/AAB
3. Submit for review
4. Monitor reviews and ratings
5. Plan updates based on user feedback

## Compliance Notes
- All video processing happens locally on device
- No user data is transmitted to external servers
- App follows Android privacy guidelines
- Compatible with Xiaomi MIUI system

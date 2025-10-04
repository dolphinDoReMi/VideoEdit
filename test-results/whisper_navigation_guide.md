# Whisper Step Flow - Complete Navigation Guide

**Date**: $(date)
**Device**: Xiaomi Pad Ultra
**App**: Mira Video Editor
**Flow**: 3-Step Whisper Processing Pipeline

## Overview

The Whisper Step Flow consists of 3 interconnected web pages that guide users through the complete video transcription process:

1. **Step 1**: Setup & Run (Media Selection, Model Selection, Processing Start)
2. **Step 2**: Processing & Export (Status Monitoring, Data Export)
3. **Step 3**: Results & Export (Results Display, Format Export, New Analysis)

---

## Step 1: Setup & Run

### 📱 **Page Location**
- **File**: `app/src/main/assets/web/whisper-step1.html`
- **Activity**: `com.mira.clip.Clip4ClipActivity`
- **URL**: `file:///android_asset/web/whisper-step1.html`

### 🎯 **Purpose**
- Select video/audio media file
- Choose Whisper model
- Configure processing preset
- Start transcription job

### 🔧 **Key Features**

#### **Media Selection**
- **Button**: "Pick Media (URI)"
- **Functionality**: 
  - Opens file picker for video/audio files
  - Supports: mp4, avi, mov, mkv, webm, wav, mp3
  - Shows video preview with file information
  - Displays filename, duration, and size

#### **Model Selection**
- **Button**: "Pick Model"
- **Functionality**:
  - Selects Whisper model file
  - Default: `/sdcard/Models/ggml-small.en.bin`
  - Supports various model sizes (tiny, base, small, medium, large)

#### **Preset Configuration**
- **Options**: Single, Accuracy
- **Single**: Faster processing, lower accuracy
- **Accuracy**: Slower processing, higher accuracy

#### **Run Button**
- **Functionality**: Starts Whisper processing job
- **Navigation**: Automatically moves to Step 2 after successful submission

### 🧭 **Navigation**
- **To Step 2**: Click "Run" button → Automatic navigation
- **Manual Navigation**: `window.AndroidInterface.openWhisperStep2()`

---

## Step 2: Processing & Export

### 📱 **Page Location**
- **File**: `app/src/main/assets/web/whisper-step2.html`
- **Activity**: `com.mira.com.whisper.WhisperStep2Activity`
- **URL**: `file:///android_asset/web/whisper-step2.html`

### 🎯 **Purpose**
- Monitor processing status
- View job progress
- Export processing data
- Navigate to results

### 🔧 **Key Features**

#### **Processing Status**
- **Real-time Updates**: Shows current processing stage
- **Progress Indicators**: Visual progress bars
- **Job Information**: Displays job ID and status

#### **Export Functionality**
- **Export All Data**: Exports complete processing results
- **Individual Exports**: Export specific data types
- **File Formats**: JSON, SRT, TXT formats

#### **Job Management**
- **Job List**: Shows all processing jobs
- **Status Tracking**: Real-time status updates
- **Error Handling**: Displays any processing errors

### 🧭 **Navigation**
- **To Step 3**: Click "Export All Data" → Automatic navigation
- **Back to Step 1**: Use back button or `window.AndroidInterface.goBack()`
- **Manual Navigation**: `window.AndroidInterface.openWhisperStep3()`

---

## Step 3: Results & Export

### 📱 **Page Location**
- **File**: `app/src/main/assets/web/whisper-step3.html`
- **Activity**: `com.mira.com.whisper.WhisperStep3Activity`
- **URL**: `file:///android_asset/web/whisper-step3.html`

### 🎯 **Purpose**
- Display transcription results
- Export in multiple formats
- Start new analysis
- View processing details

### 🔧 **Key Features**

#### **Results Display**
- **Transcript**: Full text transcription
- **Segments**: Timestamped segments
- **Confidence**: Processing confidence scores
- **Metadata**: Job information and statistics

#### **Export Options**
- **JSON Export**: Complete data in JSON format
- **SRT Export**: Subtitle format with timestamps
- **TXT Export**: Plain text transcription
- **Export All**: All formats at once

#### **Quick Actions**
- **New Analysis**: Start a new transcription job
- **Go Back**: Return to previous step
- **Processing Details**: View detailed job information

### 🧭 **Navigation**
- **To Step 1**: Click "New Analysis" → Returns to Step 1
- **Back to Step 2**: Use back button or `window.AndroidInterface.goBack()`
- **Manual Navigation**: `window.AndroidInterface.openWhisperStep1()`

---

## Complete Navigation Flow

### 🔄 **Forward Flow**
```
Step 1 (Setup) → Step 2 (Processing) → Step 3 (Results)
     ↓                    ↓                    ↓
Pick Media          Monitor Status        View Results
Pick Model          Export Data           Export Formats
Run Job             Navigate to Step 3    New Analysis
```

### 🔙 **Backward Flow**
```
Step 3 (Results) → Step 2 (Processing) → Step 1 (Setup)
     ↓                    ↓                    ↓
Go Back             Go Back              Start Over
New Analysis        Continue Processing  New Job
```

### 🎯 **Navigation Methods**

#### **Automatic Navigation**
- **Step 1 → Step 2**: After successful "Run" button click
- **Step 2 → Step 3**: After "Export All Data" button click

#### **Manual Navigation**
- **JavaScript Bridge**: `window.AndroidInterface.openWhisperStep1/2/3()`
- **Android Activities**: Direct activity launches
- **Back Button**: Standard Android back navigation

#### **Programmatic Navigation**
```javascript
// Navigate to Step 1
window.AndroidInterface.openWhisperStep1();

// Navigate to Step 2  
window.AndroidInterface.openWhisperStep2();

// Navigate to Step 3
window.AndroidInterface.openWhisperStep3();

// Go back
window.AndroidInterface.goBack();
```

---

## Screenshots

### 📸 **Step 1: Setup & Run**
- **File**: `test-results/whisper_step1_$(date +%Y%m%d_%H%M%S).png`
- **Shows**: Media selection, model picker, preset options, run button

### 📸 **Step 2: Processing & Export**
- **File**: `test-results/whisper_step2_$(date +%Y%m%d_%H%M%S).png`
- **Shows**: Processing status, export options, job management

### 📸 **Step 3: Results & Export**
- **File**: `test-results/whisper_step3_$(date +%Y%m%d_%H%M%S).png`
- **Shows**: Results display, export formats, quick actions

---

## User Workflow

### 🚀 **Complete Process**
1. **Start**: Launch app → Step 1 appears
2. **Setup**: Pick media file, select model, choose preset
3. **Run**: Click "Run" → Navigate to Step 2
4. **Process**: Monitor status, export data → Navigate to Step 3
5. **Results**: View transcription, export formats
6. **Repeat**: Click "New Analysis" → Return to Step 1

### ⚡ **Quick Actions**
- **Skip to Results**: Direct navigation to Step 3
- **Start Over**: Return to Step 1 from any step
- **Export Only**: Go directly to Step 2 for exports
- **View Results**: Jump to Step 3 for results

### 🔧 **Technical Details**
- **WebView**: Each step runs in Android WebView
- **JavaScript Bridge**: Communication between web and native
- **File Access**: Direct file system access for media/model files
- **Real-time Updates**: BroadcastChannel for live status updates

---

## Troubleshooting

### ❌ **Common Issues**
1. **Navigation Not Working**: Check JavaScript bridge availability
2. **File Selection Failed**: Verify file permissions and paths
3. **Preview Not Loading**: Check video format compatibility
4. **Export Failed**: Ensure sufficient storage space

### ✅ **Solutions**
1. **Restart App**: Relaunch the application
2. **Check Logs**: Use `adb logcat` to debug issues
3. **Verify Files**: Ensure video/model files exist
4. **Clear Cache**: Clear WebView cache if needed

---

## Conclusion

The Whisper Step Flow provides a complete, user-friendly interface for video transcription with seamless navigation between all three steps. Each step has a specific purpose and clear navigation paths, making the entire process intuitive and efficient.

**Key Benefits**:
- ✅ **Intuitive Flow**: Clear step-by-step process
- ✅ **Seamless Navigation**: Automatic and manual navigation options
- ✅ **Real-time Updates**: Live status monitoring
- ✅ **Multiple Export Formats**: Flexible output options
- ✅ **Error Handling**: Robust error management
- ✅ **User Control**: Easy navigation and restart options

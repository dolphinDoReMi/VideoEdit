# JavaScript Bridge Debugging Guide

## Quick Debugging Checklist

### 1. **Check JavaScript Console**
```bash
adb logcat | grep -E "(console|javascript|webview|chrome)"
```
Look for:
- `TypeError: function is not a function`
- `Navigation error:`
- `Bridge method not available:`

### 2. **Verify Bridge Method Existence**
```javascript
// In browser console or JavaScript
console.log('Available methods:', Object.keys(window.WhisperBridge));
console.log('openStep2WithBatchId exists:', typeof window.WhisperBridge.openStep2WithBatchId);
```

### 3. **Monitor Android Logs**
```bash
adb logcat | grep -E "(AndroidWhisperBridge|WhisperBridge|runBatch|getBatchInfo)"
```
Look for:
- `=== runBatch called with args:`
- `=== getBatchInfo called for:`
- `Opening Whisper Processing Activity`

### 4. **Check Method Annotations**
Ensure Android methods have `@JavascriptInterface`:
```kotlin
@JavascriptInterface
fun methodName(args: String): String {
    // implementation
}
```

### 5. **Verify JavaScript Wrapper**
Ensure JavaScript wrapper includes all methods:
```javascript
const bridge = {
    methodName(args) { 
        return window.WhisperBridge.methodName(args);
    }
};
```

## Common Issues & Solutions

### Issue: `TypeError: function is not a function`
**Cause**: Missing JavaScript bridge method
**Solution**: Add method to JavaScript wrapper

### Issue: Navigation fails silently
**Cause**: JavaScript error preventing navigation
**Solution**: Add try-catch and error logging

### Issue: Android method not called
**Cause**: Missing `@JavascriptInterface` annotation
**Solution**: Add annotation to Android method

### Issue: Bridge undefined
**Cause**: WebView not properly initialized
**Solution**: Check WebView setup and timing

## Debugging Commands

### Monitor All App Logs
```bash
adb logcat | grep "com.mira.com"
```

### Monitor Specific Methods
```bash
adb logcat | grep -E "(runBatch|getBatchInfo|openStep2)"
```

### Check WebView Errors
```bash
adb logcat | grep -E "(console|javascript|webview)"
```

### Monitor Navigation
```bash
adb logcat | grep -E "(Opening.*Activity|Navigation|Intent)"
```

## Testing Flow

1. **Clear logs**: `adb logcat -c`
2. **Start monitoring**: `adb logcat | grep -E "(AndroidWhisperBridge|WhisperBridge)"`
3. **Perform user action**
4. **Check for expected log entries**
5. **Verify JavaScript console for errors**

## Prevention

### 1. **Method Registry**
Maintain a list of all bridge methods:
```javascript
const BRIDGE_METHODS = [
    'runBatch',
    'getBatchInfo',
    'openStep2WithBatchId',
    // ... add new methods here
];
```

### 2. **Validation Function**
```javascript
function validateBridge() {
    BRIDGE_METHODS.forEach(method => {
        if (typeof window.WhisperBridge[method] !== 'function') {
            console.error('Missing bridge method:', method);
        }
    });
}
```

### 3. **Error Boundaries**
```javascript
async function safeBridgeCall(method, args) {
    try {
        if (typeof window.WhisperBridge[method] === 'function') {
            return await window.WhisperBridge[method](args);
        } else {
            throw new Error(`Bridge method ${method} not available`);
        }
    } catch (error) {
        console.error('Bridge call failed:', error);
        throw error;
    }
}
```

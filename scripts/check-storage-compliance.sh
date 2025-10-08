#!/usr/bin/env bash
set -euo pipefail

# Guard script to prevent forbidden storage API usage
# This script checks for patterns that violate scoped storage requirements

echo "🔍 Checking for forbidden storage API usage..."

fail=0

# Check for forbidden patterns in Kotlin/Java files
echo "Checking Kotlin/Java files for forbidden patterns..."
if grep -r -n 'Environment\.getExternalStorageDirectory(\|"/sdcard/\|MediaExtractor\.setDataSource(\s*String\|FileInputStream(\s*String' app/ feature/whisper/ infra-storage/; then
    echo "❌ Forbidden storage APIs detected in Kotlin/Java files:"
    echo "   - Environment.getExternalStorageDirectory()"
    echo "   - Hardcoded /sdcard/ paths"
    echo "   - MediaExtractor.setDataSource(String)"
    echo "   - FileInputStream(String)"
    echo "   Use DLStorage + FD + app-private mmap instead."
    fail=1
fi

# Check for forbidden patterns in C/C++ files
echo "Checking C/C++ files for forbidden patterns..."
if grep -r -n 'fopen\s*(' feature/whisper/src/main/cpp/ infra-storage/src/main/cpp/; then
    echo "❌ Forbidden fopen() usage detected in C/C++ files:"
    echo "   - fopen() for media files"
    echo "   Accept int fd from Java and use fdopen/read instead."
    fail=1
fi

# Check for required patterns
echo "Checking for required scoped storage patterns..."
if ! grep -r -n 'DLStorage\.resolveModelPath(' app/ feature/whisper/ infra-storage/; then
    echo "⚠️  Warning: No DLStorage.resolveModelPath() usage found"
    echo "   Model loading should use DLStorage.resolveModelPath()"
fi

if ! grep -r -n 'DLStorage\.openReadFd(' app/ feature/whisper/ infra-storage/; then
    echo "⚠️  Warning: No DLStorage.openReadFd() usage found"
    echo "   Media access should use DLStorage.openReadFd()"
fi

if ! grep -r -n 'JNIEXPORT.*Java_.*transcribeFromFd(' feature/whisper/src/main/cpp/ infra-storage/src/main/cpp/; then
    echo "⚠️  Warning: No transcribeFromFd JNI method found"
    echo "   JNI should expose transcribeFromFd(ctx, fd)"
fi

if [ $fail -eq 1 ]; then
    echo ""
    echo "❌ Storage API check FAILED. Fix forbidden patterns before proceeding."
    echo ""
    echo "Required refactoring patterns:"
    echo "  Kotlin (media):"
    echo "    // BEFORE: extractor.setDataSource(path)"
    echo "    // AFTER: val fd = contentResolver.openFileDescriptor(uri, \"r\")!!.detachFd()"
    echo "    //         extractor.setDataSource(fd.toLong(), 0, Long.MAX_VALUE)"
    echo ""
    echo "  Kotlin (model):"
    echo "    // BEFORE: NativeWhisper.initFromPath(\"/sdcard/Models/whisper-small.bin\")"
    echo "    // AFTER: val handle = storage.installModel(modelUri, \"whisper-small.gguf\")"
    echo "    //         val ctx = NativeWhisper.initModelFromAppFile(storage.resolveModelPath(handle))"
    echo ""
    echo "  C++ (JNI media):"
    echo "    // BEFORE: FILE* f = fopen(path, \"rb\");"
    echo "    // AFTER: FILE* f = fdopen(dup(fd), \"rb\");"
    exit 1
else
    echo "✅ Storage API check passed."
    echo "✅ All file/media I/O uses capability-based access patterns."
fi

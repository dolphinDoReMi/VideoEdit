#!/bin/bash
# ops/storage_compliance.sh
# Companion script for the Gradle compliance gate
# Provides equivalent patterns for CI or local quick checks

set -e

echo "🔍 Storage Compliance Check (ripgrep companion)"
echo "=============================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Track violations
VIOLATIONS=0

# Function to check for violations
check_violation() {
    local pattern="$1"
    local description="$2"
    local files="$3"
    
    if grep -q "$pattern" $files 2>/dev/null; then
        echo -e "${RED}❌ FAIL: $description${NC}"
        echo "   Pattern: $pattern"
        grep -n "$pattern" $files | head -5
        echo ""
        VIOLATIONS=$((VIOLATIONS + 1))
    else
        echo -e "${GREEN}✅ PASS: $description${NC}"
    fi
}

# Check source files
SOURCE_FILES=$(find infra-storage/src/main/java -name "*.kt" -o -name "*.java" | tr '\n' ' ')

echo "Checking source files: $SOURCE_FILES"
echo ""

# 1. Raw external paths
check_violation "/sdcard" "Raw external storage paths" "$SOURCE_FILES"
check_violation "/storage/emulated" "Raw external storage paths" "$SOURCE_FILES"

# 2. Environment.getExternalStorageDirectory()
check_violation "Environment\.getExternalStorageDirectory" "Legacy external storage access" "$SOURCE_FILES"

# 3. MediaExtractor.setDataSource(String)
check_violation "MediaExtractor.*setDataSource.*String" "String-based MediaExtractor" "$SOURCE_FILES"

# 4. FileInputStream/RandomAccessFile with strings
check_violation "FileInputStream\s*\(" "String-based FileInputStream" "$SOURCE_FILES"
check_violation "RandomAccessFile\s*\(" "String-based RandomAccessFile" "$SOURCE_FILES"

# 5. C/C++ file operations (if any)
check_violation "fopen\s*\(" "C fopen with string" "$SOURCE_FILES"
check_violation "open\s*\(" "C open with string" "$SOURCE_FILES"
check_violation "std::ifstream" "C++ ifstream with string" "$SOURCE_FILES"

# 6. Broadcasts/receivers
check_violation "sendBroadcast" "Broadcast sending" "$SOURCE_FILES"
check_violation "registerReceiver" "Receiver registration" "$SOURCE_FILES"

# 7. Dangerous permissions
check_violation "MANAGE_EXTERNAL_STORAGE" "Dangerous external storage permission" "$SOURCE_FILES"
check_violation "READ_EXTERNAL_STORAGE" "Legacy external storage permission" "$SOURCE_FILES"

# 8. Check for proper scoped storage usage
echo ""
echo "Checking for proper scoped storage usage..."

# Check for FileProvider usage
if grep -q "FileProvider" $SOURCE_FILES 2>/dev/null; then
    echo -e "${GREEN}✅ PASS: FileProvider usage found${NC}"
else
    echo -e "${YELLOW}⚠️  WARN: No FileProvider usage found${NC}"
fi

# Check for app-private storage usage
if grep -q "\.filesDir" $SOURCE_FILES 2>/dev/null; then
    echo -e "${GREEN}✅ PASS: App-private storage usage found${NC}"
else
    echo -e "${RED}❌ FAIL: No app-private storage usage found${NC}"
    VIOLATIONS=$((VIOLATIONS + 1))
fi

# Check for URI-based access
if grep -q "contentResolver" $SOURCE_FILES 2>/dev/null; then
    echo -e "${GREEN}✅ PASS: ContentResolver usage found${NC}"
else
    echo -e "${YELLOW}⚠️  WARN: No ContentResolver usage found${NC}"
fi

# Check for file descriptor usage
if grep -q "openFileDescriptor" $SOURCE_FILES 2>/dev/null; then
    echo -e "${GREEN}✅ PASS: File descriptor usage found${NC}"
else
    echo -e "${YELLOW}⚠️  WARN: No file descriptor usage found${NC}"
fi

echo ""
echo "=============================================="

if [ $VIOLATIONS -eq 0 ]; then
    echo -e "${GREEN}🎉 ALL COMPLIANCE CHECKS PASSED!${NC}"
    echo "The codebase appears to be scoped storage compliant."
    exit 0
else
    echo -e "${RED}❌ $VIOLATIONS COMPLIANCE VIOLATIONS FOUND${NC}"
    echo "Please fix the violations above to ensure scoped storage compliance."
    exit 1
fi

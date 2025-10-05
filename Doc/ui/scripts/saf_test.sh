#!/bin/bash

echo "Testing SAF Permissions..."

# Test if we can access external storage
if adb shell ls /storage/emulated/0/ > /dev/null 2>&1; then
    echo "✅ External storage accessible"
else
    echo "❌ External storage not accessible"
fi

# Test if we can access Downloads folder
if adb shell ls /storage/emulated/0/Download/ > /dev/null 2>&1; then
    echo "✅ Downloads folder accessible"
else
    echo "❌ Downloads folder not accessible"
fi

# Test if we can access DCIM folder
if adb shell ls /storage/emulated/0/DCIM/ > /dev/null 2>&1; then
    echo "✅ DCIM folder accessible"
else
    echo "❌ DCIM folder not accessible"
fi

echo "SAF permissions test complete"

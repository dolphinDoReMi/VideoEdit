#!/bin/bash

echo "🔍 Debugging Progress Indication Issue"
echo "======================================"

echo "1. Checking if app is installed..."
adb -s emulator-5554 shell pm list packages | grep mira

echo -e "\n2. Starting app..."
adb -s emulator-5554 shell am start -n com.mira.clip.debug/com.mira.clip.MainActivity

echo -e "\n3. Monitoring logs (press Ctrl+C to stop)..."
echo "   Please test the autocut functionality in the app now!"
echo "   Look for MainActivity, AutoCutEngine, and VideoScorer logs"
echo ""

adb -s emulator-5554 logcat -s MainActivity AutoCutEngine VideoScorer

#!/bin/bash

echo "=== 测试处理修复 ==="
echo "1. 启动应用..."
adb shell monkey -p com.mira.com -c android.intent.category.LAUNCHER 1
sleep 3

echo "2. 清除日志并开始监控..."
adb logcat -c

echo "3. 等待用户选择文件并点击 Start Processing..."
echo "   请在设备上："
echo "   - 选择一个短视频文件"
echo "   - 点击 Start Processing 按钮"
echo "   - 观察处理页面的 Files 数量是否显示正确"
echo ""
echo "4. 监控日志中的关键信息..."

# 监控关键日志
adb logcat | grep -E "(runBatch|PlanStore|getBatchInfo|setBatchId|Files|permission|batchId)" --line-buffered

#!/bin/bash

# Vulkan Integration Quick Start Script
# This script provides a complete workflow for Vulkan integration on Xiaomi Pad 7 Ultra

set -e

echo "=== Vulkan Integration Quick Start ==="
echo "Timestamp: $(date)"
echo

# Configuration
PACKAGE_NAME="com.mira.com.debug"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Function to show usage
show_usage() {
    echo "Usage: $0 [COMMAND]"
    echo
    echo "Commands:"
    echo "  build       - Build app with Vulkan support"
    echo "  install     - Install app on device"
    echo "  validate    - Validate Vulkan support on device"
    echo "  test        - Run comprehensive Vulkan tests"
    echo "  benchmark   - Run performance benchmark"
    echo "  troubleshoot - Run troubleshooting diagnostics"
    echo "  all         - Run complete workflow (build + install + validate + test)"
    echo "  help        - Show this help message"
    echo
    echo "Examples:"
    echo "  $0 build                    # Build app with Vulkan"
    echo "  $0 validate                 # Check device Vulkan support"
    echo "  $0 all                      # Complete workflow"
}

# Function to build app
build_app() {
    echo "1. Building app with Vulkan support..."
    
    # Clean and build
    ./gradlew clean
    ./gradlew :app:assembleDebug
    
    if [ $? -eq 0 ]; then
        echo "✅ Build successful"
    else
        echo "❌ Build failed"
        exit 1
    fi
}

# Function to install app
install_app() {
    echo "2. Installing app on device..."
    
    if [ ! -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
        echo "❌ APK not found. Run 'build' first."
        exit 1
    fi
    
    adb install -r app/build/outputs/apk/debug/app-debug.apk
    echo "✅ App installed"
}

# Function to validate device
validate_device() {
    echo "3. Validating Vulkan support on device..."
    
    if [ -f "$SCRIPT_DIR/validate_vulkan_device.sh" ]; then
        "$SCRIPT_DIR/validate_vulkan_device.sh"
    else
        echo "❌ Validation script not found"
        exit 1
    fi
}

# Function to run tests
run_tests() {
    echo "4. Running comprehensive Vulkan tests..."
    
    if [ -f "$SCRIPT_DIR/build_and_test_vulkan.sh" ]; then
        "$SCRIPT_DIR/build_and_test_vulkan.sh"
    else
        echo "❌ Test script not found"
        exit 1
    fi
}

# Function to run benchmark
run_benchmark() {
    echo "5. Running performance benchmark..."
    
    if [ -f "$SCRIPT_DIR/benchmark_vulkan_performance.sh" ]; then
        "$SCRIPT_DIR/benchmark_vulkan_performance.sh"
    else
        echo "❌ Benchmark script not found"
        exit 1
    fi
}

# Function to troubleshoot
troubleshoot() {
    echo "6. Running troubleshooting diagnostics..."
    
    if [ -f "$SCRIPT_DIR/troubleshoot_vulkan.sh" ]; then
        "$SCRIPT_DIR/troubleshoot_vulkan.sh"
    else
        echo "❌ Troubleshooting script not found"
        exit 1
    fi
}

# Function to run complete workflow
run_all() {
    echo "Running complete Vulkan integration workflow..."
    echo
    
    build_app
    echo
    
    install_app
    echo
    
    validate_device
    echo
    
    run_tests
    echo
    
    echo "=== Complete Workflow Finished ==="
    echo "Next steps:"
    echo "1. Review test results"
    echo "2. Run benchmark: $0 benchmark"
    echo "3. If issues: $0 troubleshoot"
}

# Function to check prerequisites
check_prerequisites() {
    echo "Checking prerequisites..."
    
    # Check if device is connected
    if ! adb devices | grep -q "device$"; then
        echo "❌ No Android device connected"
        echo "   Connect your Xiaomi Pad 7 Ultra via USB and enable USB debugging"
        exit 1
    fi
    
    # Check if gradlew exists
    if [ ! -f "gradlew" ]; then
        echo "❌ gradlew not found. Run from project root directory."
        exit 1
    fi
    
    # Check if scripts exist
    if [ ! -d "$SCRIPT_DIR" ]; then
        echo "❌ Scripts directory not found: $SCRIPT_DIR"
        exit 1
    fi
    
    echo "✅ Prerequisites check passed"
}

# Main execution
main() {
    case "${1:-help}" in
        "build")
            check_prerequisites
            build_app
            ;;
        "install")
            check_prerequisites
            install_app
            ;;
        "validate")
            check_prerequisites
            validate_device
            ;;
        "test")
            check_prerequisites
            run_tests
            ;;
        "benchmark")
            check_prerequisites
            run_benchmark
            ;;
        "troubleshoot")
            check_prerequisites
            troubleshoot
            ;;
        "all")
            check_prerequisites
            run_all
            ;;
        "help"|*)
            show_usage
            ;;
    esac
}

# Run main function
main "$@"

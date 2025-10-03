#!/bin/bash

# AutoCut Step-by-Step Testing Framework
# Master test runner for comprehensive pipeline verification

echo "🎬 AutoCut Step-by-Step Testing Framework"
echo "========================================="
echo "Comprehensive testing of all pipeline components"
echo ""

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TEST_DIR="/tmp/autocut_comprehensive_test"
mkdir -p "$TEST_DIR"

# Test steps available
declare -A TEST_STEPS=(
    ["1"]="Video Input Testing"
    ["2"]="Thermal State Testing"
    ["3"]="Content Analysis Testing"
    ["4"]="EDL Generation Testing"
    ["5"]="Thumbnail Generation Testing"
    ["6"]="Subject-Centered Cropping Testing"
    ["7"]="Multi-Rendition Export Testing"
    ["8"]="Cloud Upload Testing"
    ["9"]="Performance Testing"
    ["10"]="Output Verification Testing"
)

# Function to display usage
show_usage() {
    echo "Usage: $0 [OPTIONS] [STEP_NUMBERS]"
    echo ""
    echo "Options:"
    echo "  -h, --help          Show this help message"
    echo "  -a, --all           Run all test steps"
    echo "  -l, --list          List available test steps"
    echo "  -s, --summary       Show test summary only"
    echo "  -c, --clean         Clean test directories before running"
    echo "  -v, --verbose       Enable verbose output"
    echo ""
    echo "Step Numbers:"
    for step in "${!TEST_STEPS[@]}"; do
        echo "  $step  ${TEST_STEPS[$step]}"
    done
    echo ""
    echo "Examples:"
    echo "  $0 --all                    # Run all tests"
    echo "  $0 1 2 3                    # Run steps 1, 2, and 3"
    echo "  $0 --clean --all            # Clean and run all tests"
    echo "  $0 --list                   # List available steps"
}

# Function to list available steps
list_steps() {
    echo "📋 Available Test Steps:"
    echo "======================="
    for step in $(printf '%s\n' "${!TEST_STEPS[@]}" | sort -n); do
        echo "  Step $step: ${TEST_STEPS[$step]}"
    done
    echo ""
    echo "Total Steps: ${#TEST_STEPS[@]}"
}

# Function to run a specific test step
run_test_step() {
    local step_num="$1"
    local step_name="${TEST_STEPS[$step_num]}"
    
    if [ -z "$step_name" ]; then
        echo "❌ Error: Invalid step number '$step_num'"
        return 1
    fi
    
    local script_path="$SCRIPT_DIR/step${step_num}_*_test.sh"
    local script_file=$(ls $script_path 2>/dev/null | head -n1)
    
    if [ -z "$script_file" ] || [ ! -f "$script_file" ]; then
        echo "❌ Error: Test script for step $step_num not found"
        echo "Expected: $script_path"
        return 1
    fi
    
    echo ""
    echo "🚀 Running Step $step_num: $step_name"
    echo "======================================"
    echo "Script: $(basename "$script_file")"
    echo "Time: $(date)"
    echo ""
    
    # Run the test script
    if bash "$script_file"; then
        echo ""
        echo "✅ Step $step_num completed successfully"
        return 0
    else
        echo ""
        echo "❌ Step $step_num failed"
        return 1
    fi
}

# Function to clean test directories
clean_test_dirs() {
    echo "🧹 Cleaning test directories..."
    rm -rf /tmp/autocut_step*_test
    rm -rf /tmp/autocut_comprehensive_test
    echo "✅ Test directories cleaned"
}

# Function to show test summary
show_summary() {
    echo ""
    echo "📊 Test Summary"
    echo "==============="
    echo "Total Steps Available: ${#TEST_STEPS[@]}"
    echo "Test Directory: $TEST_DIR"
    echo "Script Directory: $SCRIPT_DIR"
    echo ""
    echo "Available Test Scripts:"
    for step in $(printf '%s\n' "${!TEST_STEPS[@]}" | sort -n); do
        local script_path="$SCRIPT_DIR/step${step}_*_test.sh"
        local script_file=$(ls $script_path 2>/dev/null | head -n1)
        if [ -f "$script_file" ]; then
            echo "  ✅ Step $step: $(basename "$script_file")"
        else
            echo "  ❌ Step $step: Script not found"
        fi
    done
}

# Function to run all tests
run_all_tests() {
    echo "🎯 Running All Test Steps"
    echo "========================"
    echo "Starting comprehensive testing at $(date)"
    echo ""
    
    local passed=0
    local failed=0
    local start_time=$(date +%s)
    
    for step in $(printf '%s\n' "${!TEST_STEPS[@]}" | sort -n); do
        if run_test_step "$step"; then
            ((passed++))
        else
            ((failed++))
        fi
    done
    
    local end_time=$(date +%s)
    local duration=$((end_time - start_time))
    
    echo ""
    echo "🏁 All Tests Completed"
    echo "====================="
    echo "Duration: ${duration}s"
    echo "Passed: $passed"
    echo "Failed: $failed"
    echo "Total: $((passed + failed))"
    
    if [ $failed -eq 0 ]; then
        echo ""
        echo "🎉 ALL TESTS PASSED!"
        echo "AutoCut pipeline is fully verified and ready for production!"
        return 0
    else
        echo ""
        echo "⚠️ Some tests failed. Please review the output above."
        return 1
    fi
}

# Parse command line arguments
VERBOSE=false
CLEAN=false
RUN_ALL=false
SHOW_SUMMARY=false
SHOW_LIST=false
STEPS_TO_RUN=()

while [[ $# -gt 0 ]]; do
    case $1 in
        -h|--help)
            show_usage
            exit 0
            ;;
        -a|--all)
            RUN_ALL=true
            shift
            ;;
        -l|--list)
            SHOW_LIST=true
            shift
            ;;
        -s|--summary)
            SHOW_SUMMARY=true
            shift
            ;;
        -c|--clean)
            CLEAN=true
            shift
            ;;
        -v|--verbose)
            VERBOSE=true
            shift
            ;;
        [0-9]*)
            STEPS_TO_RUN+=("$1")
            shift
            ;;
        *)
            echo "❌ Unknown option: $1"
            show_usage
            exit 1
            ;;
    esac
done

# Handle special cases
if [ "$SHOW_LIST" = true ]; then
    list_steps
    exit 0
fi

if [ "$SHOW_SUMMARY" = true ]; then
    show_summary
    exit 0
fi

# Clean if requested
if [ "$CLEAN" = true ]; then
    clean_test_dirs
fi

# Run tests based on arguments
if [ "$RUN_ALL" = true ]; then
    run_all_tests
elif [ ${#STEPS_TO_RUN[@]} -gt 0 ]; then
    echo "🎯 Running Selected Test Steps"
    echo "============================="
    echo "Steps to run: ${STEPS_TO_RUN[*]}"
    echo ""
    
    local passed=0
    local failed=0
    
    for step in "${STEPS_TO_RUN[@]}"; do
        if run_test_step "$step"; then
            ((passed++))
        else
            ((failed++))
        fi
    done
    
    echo ""
    echo "🏁 Selected Tests Completed"
    echo "==========================="
    echo "Passed: $passed"
    echo "Failed: $failed"
    echo "Total: $((passed + failed))"
    
    if [ $failed -eq 0 ]; then
        echo ""
        echo "✅ All selected tests passed!"
    else
        echo ""
        echo "⚠️ Some tests failed. Please review the output above."
    fi
else
    echo "❌ No test steps specified"
    echo ""
    show_usage
    exit 1
fi

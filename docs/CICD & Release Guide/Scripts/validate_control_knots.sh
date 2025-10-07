#!/bin/bash
# docs/CICD & Release Guide/Scripts/validate_control_knots.sh

echo "🔍 Validating control knots format..."

features=("clip" "whisper" "infra")
for feature in "${features[@]}"; do
    echo "🔍 Checking control knots for: $feature"
    file="docs/$feature/Architecture Design and Control Knot.md"
    
    # Check for required sections
    if ! grep -q "Status: READY FOR VERIFICATION" "$file"; then
        echo "❌ Missing status in $file"
        exit 1
    fi
    
    if ! grep -q "Control knots:" "$file"; then
        echo "❌ Missing control knots section in $file"
        exit 1
    fi
    
    if ! grep -q "Implementation:" "$file"; then
        echo "❌ Missing implementation section in $file"
        exit 1
    fi
    
    if ! grep -q "Verification:" "$file"; then
        echo "❌ Missing verification section in $file"
        exit 1
    fi
    
    # Check for proper formatting
    if ! grep -q "^\*\*Status: READY FOR VERIFICATION\*\*" "$file"; then
        echo "❌ Status format incorrect in $file (should be **Status: READY FOR VERIFICATION**)"
        exit 1
    fi
    
    if ! grep -q "^\*\*Control knots:\*\*" "$file"; then
        echo "❌ Control knots format incorrect in $file (should be **Control knots:**)"
        exit 1
    fi
    
    if ! grep -q "^\*\*Implementation:\*\*" "$file"; then
        echo "❌ Implementation format incorrect in $file (should be **Implementation:**)"
        exit 1
    fi
    
    if ! grep -q "^\*\*Verification:\*\*" "$file"; then
        echo "❌ Verification format incorrect in $file (should be **Verification:**)"
        exit 1
    fi
    
    echo "✅ Control knots format valid for $feature"
done

echo "✅ Control knots format validation passed"

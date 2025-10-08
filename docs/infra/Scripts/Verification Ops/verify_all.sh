#!/bin/bash
# ops/verify_all.sh

echo "🧪 Running Complete DLStorage Verification..."
echo "=============================================="

# Run all verification scripts
scripts=(
    "verify_A_scoped_storage.sh"
    "verify_B_capability_access.sh"
    "verify_C_model_integrity.sh"
    "verify_D_memory_mapping.sh"
    "verify_E_embedding_storage.sh"
    "verify_F_fd_processing.sh"
    "verify_G_output_generation.sh"
    "verify_H_secure_sharing.sh"
    "verify_I_cleanup_operations.sh"
    "verify_J_error_handling.sh"
    "verify_K_api_surface.sh"
    "verify_L_dependency_management.sh"
    "verify_M_testing_coverage.sh"
)

passed=0
total=${#scripts[@]}

for script in "${scripts[@]}"; do
    echo ""
    if bash "ops/$script"; then
        ((passed++))
    else
        echo "❌ $script FAILED"
    fi
done

echo ""
echo "=============================================="
echo "📊 Verification Results: $passed/$total passed"

if [ $passed -eq $total ]; then
    echo "🎉 ALL VERIFICATIONS PASSED!"
    echo "✅ DLStorage implementation is fully compliant"
    exit 0
else
    echo "❌ SOME VERIFICATIONS FAILED!"
    echo "🔧 Please review the implementation"
    exit 1
fi

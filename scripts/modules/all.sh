#!/bin/bash
# ops/all.sh - Complete CLIP4Clip workflow

set -e

PKG=${PKG:-"com.mira.clip"}

echo "=== CLIP4Clip Complete Workflow ==="
echo "Package: $PKG"
echo "Starting at: $(date)"

# Step 1: Prepare device
echo ""
echo "Step 1: Preparing device directories..."
bash ops/00_prep.sh

# Step 2: Push manifests
echo ""
echo "Step 2: Pushing manifests..."
bash ops/01_push_manifests.sh

# Step 3: Install APK (if needed)
echo ""
echo "Step 3: Installing APK..."
adb install -r app/build/outputs/apk/debug/app-debug.apk || echo "APK already installed"

# Step 4: Verify package
echo ""
echo "Step 4: Verifying package installation..."
adb shell pm list packages | grep "$PKG" || {
    echo "ERROR: Package $PKG not found"
    exit 1
}

# Step 5: Run ingestion
echo ""
echo "Step 5: Running video ingestion..."
bash ops/02_ingest.sh

# Step 6: Run search
echo ""
echo "Step 6: Running text-to-video search..."
bash ops/03_search.sh

# Step 7: Verify results
echo ""
echo "Step 7: Verifying results..."

# Check embedding files
EMBEDDING_FILES=$(adb shell "find /sdcard/MiraClip/out/embeddings -name '*.f32' 2>/dev/null | wc -l")
echo "Embedding files found: $EMBEDDING_FILES"

# Check search results
SEARCH_RESULTS=$(adb shell "test -f /sdcard/MiraClip/out/search/results_q.json && echo 'YES' || echo 'NO'")
echo "Search results available: $SEARCH_RESULTS"

# Step 8: Python verification (if available)
echo ""
echo "Step 8: Running Python verification..."

python3 - << 'PY'
import os, struct, math, json, subprocess, tempfile
import sys

def verify_embeddings():
    """Verify embedding files are normalized and correct dimension"""
    try:
        VAR = "clip_vit_b32_mean_v1"
        DIR = "/sdcard/MiraClip/out/embeddings"
        
        # Find .f32 files
        result = subprocess.run(["adb", "shell", "find", f"{DIR}/{VAR}", "-name", "*.f32"], 
                              capture_output=True, text=True)
        
        if not result.stdout.strip():
            print("No embedding files found")
            return False
            
        files = result.stdout.strip().split('\n')
        print(f"Found {len(files)} embedding files")
        
        for file_path in files:
            if not file_path.strip():
                continue
                
            # Pull file to temp location
            tmp = tempfile.mkdtemp()
            local_path = os.path.join(tmp, os.path.basename(file_path))
            
            subprocess.run(["adb", "pull", file_path, local_path], check=True)
            
            # Read and verify
            with open(local_path, "rb") as f:
                data = f.read()
            
            # Parse float32 array
            arr = [x[0] for x in struct.iter_unpack("<f", data)]
            
            # Check dimension
            if len(arr) not in (512, 768):
                print(f"ERROR: Unexpected embedding dimension: {len(arr)}")
                return False
                
            # Check L2 normalization
            norm = math.sqrt(sum(v*v for v in arr))
            if abs(norm - 1.0) > 1e-2:
                print(f"ERROR: Embedding not L2 normalized: {norm}")
                return False
                
            print(f"✓ Embedding {os.path.basename(file_path)}: dim={len(arr)}, norm={norm:.6f}")
        
        return True
        
    except Exception as e:
        print(f"ERROR: {e}")
        return False

def verify_search_results():
    """Verify search results are valid"""
    try:
        src = "/sdcard/MiraClip/out/search/results_q.json"
        tmp = tempfile.mkdtemp()
        local_path = os.path.join(tmp, "results_q.json")
        
        subprocess.run(["adb", "pull", src, local_path], check=True)
        
        with open(local_path) as f:
            data = json.load(f)
        
        results = data.get("results", [])
        print(f"Found {len(results)} query results")
        
        for i, query in enumerate(results):
            scores = [entry["score"] for entry in query.get("top_k", [])]
            
            # Check for NaN
            if any(s != s for s in scores):
                print(f"ERROR: Query {i} has NaN scores")
                return False
                
            # Check sorting
            if scores != sorted(scores, reverse=True):
                print(f"ERROR: Query {i} scores not sorted descending")
                return False
                
            print(f"✓ Query {i}: {len(scores)} results, scores={[f'{s:.4f}' for s in scores[:3]]}")
        
        return True
        
    except Exception as e:
        print(f"ERROR: {e}")
        return False

# Run verifications
print("Verifying embeddings...")
embeddings_ok = verify_embeddings()

print("\nVerifying search results...")
search_ok = verify_search_results()

if embeddings_ok and search_ok:
    print("\n✓ All verifications passed!")
    sys.exit(0)
else:
    print("\n✗ Some verifications failed!")
    sys.exit(1)
PY

VERIFICATION_RESULT=$?

echo ""
echo "=== CLIP4Clip Workflow Complete ==="
echo "Finished at: $(date)"

if [ $VERIFICATION_RESULT -eq 0 ]; then
    echo "✓ SUCCESS: All tests passed!"
    exit 0
else
    echo "✗ FAILURE: Some tests failed!"
    exit 1
fi

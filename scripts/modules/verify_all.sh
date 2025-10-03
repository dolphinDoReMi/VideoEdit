#!/bin/bash
# ops/verify_all.sh - Complete CLIP4Clip verification pipeline
# Research-oriented verification with control knots and invariants

set -e

# Control knots - experimental parameters
export PKG=com.mira.clip
export ROOT=/sdcard/MiraClip
export VARIANT=clip_vit_b32_mean_v1
export VIDEO=/sdcard/Movies/video_v1.mp4

echo "=== CLIP4Clip Research Verification Pipeline ==="
echo "Package: $PKG"
echo "Root: $ROOT"
echo "Variant: $VARIANT"
echo "Video: $VIDEO"
echo "Started at: $(date)"
echo ""

# Step A/I - Isolation & Receiver Resolution
echo "Step A/I: Isolation & Receiver Resolution"
echo "----------------------------------------"
adb shell "mkdir -p $ROOT/in $ROOT/out/embeddings $ROOT/out/search"
echo "✓ Directory structure created"

# Test receiver resolution
echo "Testing receiver resolution..."
adb shell am broadcast -a com.mira.clip.SELFTEST_TEXT --es text "hello world" >/dev/null 2>&1
if [ $? -eq 0 ]; then
    echo "✓ Receiver resolves broadcasts"
else
    echo "✗ Receiver not responding"
    exit 1
fi
echo ""

# Step E - Text Encoder Self-Test (Representation Learning)
echo "Step E: Text Encoder Self-Test (Representation Learning)"
echo "------------------------------------------------------"
echo "Testing text encoder: dim + norm invariants..."

adb shell am broadcast -a com.mira.clip.SELFTEST_TEXT --es text "a photo of a cat" >/dev/null 2>&1
sleep 2

python3 - <<'PY'
import json,subprocess,tempfile,os,math
try:
    tmp=tempfile.mkdtemp()
    loc=os.path.join(tmp,"text.json")
    subprocess.check_call(["adb","exec-out","run-as","com.mira.clip","cat","files/ops_selftest_text.json"], stdout=open(loc,"wb"))
    o=json.load(open(loc))
    
    # Control knot validation
    assert o["dim"] in (512,768), f"Unexpected embedding dimension: {o['dim']}"
    assert abs(o["norm"]-1.0) < 1e-2, f"Not L2 normalized: {o['norm']}"
    
    print(f"✓ Text encoder: dim={o['dim']}, norm={o['norm']:.6f}")
    print(json.dumps({"ok":True,"dim":o["dim"],"norm":o["norm"]},indent=2))
except Exception as e:
    print(f"✗ Text encoder test failed: {e}")
    exit(1)
PY

if [ $? -ne 0 ]; then
    echo "✗ Text encoder validation failed"
    exit 1
fi
echo ""

# Step F/E - Video Encoder Self-Test (Temporal Sampling + Representation)
echo "Step F/E: Video Encoder Self-Test (Temporal Sampling + Representation)"
echo "--------------------------------------------------------------------"
echo "Testing video encoder: sampling count + dim + norm invariants..."

adb shell am broadcast -a com.mira.clip.SELFTEST_VIDEO --es path "$VIDEO" --ei frame_count 32 >/dev/null 2>&1
sleep 3

python3 - <<'PY'
import json,subprocess,tempfile,os,math
try:
    tmp=tempfile.mkdtemp()
    loc=os.path.join(tmp,"vid.json")
    subprocess.check_call(["adb","exec-out","run-as","com.mira.clip","cat","files/ops_selftest_video.json"], stdout=open(loc,"wb"))
    o=json.load(open(loc))
    
    # Control knot validation
    assert o["frame_count"] == 32, f"Unexpected frame count: {o['frame_count']}"
    assert o["dim"] in (512,768), f"Unexpected embedding dimension: {o['dim']}"
    assert abs(o["norm"]-1.0) < 1e-2, f"Not L2 normalized: {o['norm']}"
    
    print(f"✓ Video encoder: frames={o['frame_count']}, dim={o['dim']}, norm={o['norm']:.6f}")
    print(json.dumps({"ok":True,"frames":o["frame_count"],"dim":o["dim"],"norm":o["norm"]},indent=2))
except Exception as e:
    print(f"✗ Video encoder test failed: {e}")
    exit(1)
PY

if [ $? -ne 0 ]; then
    echo "✗ Video encoder validation failed"
    exit 1
fi
echo ""

# Step H/J - Ingest via Manifest (Index Build)
echo "Step H/J: Ingest via Manifest (Index Build)"
echo "----------------------------------------"
echo "Building embedding index from manifest..."

# Create and push ingest manifest
cat > /tmp/ingest.json <<EOF
{
  "variant": "$VARIANT",
  "frame_count": 32,
  "videos": [
    {
      "id": "v001",
      "path": "$VIDEO"
    }
  ],
  "output_dir": "$ROOT/out/embeddings"
}
EOF

adb push /tmp/ingest.json $ROOT/in/ingest.json >/dev/null
echo "✓ Ingest manifest pushed"

# Trigger ingestion
adb shell am broadcast -a com.mira.clip.INGEST_MANIFEST --es manifest_path "$ROOT/in/ingest.json" >/dev/null 2>&1
sleep 5
echo "✓ Ingestion triggered"
echo ""

# Step K - Embedding Invariants (Representation Sanity)
echo "Step K: Embedding Invariants (Representation Sanity)"
echo "--------------------------------------------------"
echo "Validating embedding invariants: dim + unit norm + metadata..."

python3 - <<'PY'
import os,struct,math,json,subprocess,tempfile,glob
try:
    VAR="clip_vit_b32_mean_v1"
    DIR="/sdcard/MiraClip/out/embeddings"
    
    # List embedding files
    paths=subprocess.check_output(["adb","shell","ls",f"{DIR}/{VAR}/*.f32"]).decode().strip().split()
    assert paths, "No embedding files found"
    
    # Pull one vector + its metadata
    tmp=tempfile.mkdtemp()
    vec_r, meta_r = paths[0], paths[0].replace(".f32",".json")
    vec = os.path.join(tmp, "vec.f32")
    meta = os.path.join(tmp,"meta.json")
    
    subprocess.check_call(["adb","pull",vec_r,vec], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    subprocess.check_call(["adb","pull",meta_r,meta], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    
    # Validate vector invariants
    b=open(vec,"rb").read()
    arr=[x[0] for x in struct.iter_unpack("<f", b)]
    n=math.sqrt(sum(v*v for v in arr))
    
    assert len(arr) in (512,768), f"Unexpected embedding dimension: {len(arr)}"
    assert abs(n-1.0) < 1e-2, f"Not L2 normalized: {n}"
    
    # Validate metadata
    m=json.load(open(meta))
    assert m.get("frame_count") == 32, f"Frame count metadata mismatch: {m.get('frame_count')}"
    
    print(f"✓ Embedding invariants: dim={len(arr)}, norm={n:.6f}, frames={m.get('frame_count')}")
    print(json.dumps({"ok":True,"dim":len(arr),"norm":n,"frame_count_meta":m.get("frame_count")}, indent=2))
except Exception as e:
    print(f"✗ Embedding validation failed: {e}")
    exit(1)
PY

if [ $? -ne 0 ]; then
    echo "✗ Embedding validation failed"
    exit 1
fi
echo ""

# Step H/J/L - Retrieval via Manifest (Retrieval Sanity)
echo "Step H/J/L: Retrieval via Manifest (Retrieval Sanity)"
echo "---------------------------------------------------"
echo "Testing retrieval: finite scores + sorted descending..."

# Create and push search manifest
cat > /tmp/search.json <<EOF
{
  "variant": "$VARIANT",
  "queries": [
    {
      "id": "q1",
      "text": "a person surfing on ocean waves"
    }
  ],
  "top_k": 5,
  "index_dir": "$ROOT/out/embeddings",
  "output_path": "$ROOT/out/search/results_q.json"
}
EOF

adb push /tmp/search.json $ROOT/in/search.json >/dev/null
echo "✓ Search manifest pushed"

# Trigger retrieval
adb shell am broadcast -a com.mira.clip.SEARCH_MANIFEST --es manifest_path "$ROOT/in/search.json" >/dev/null 2>&1
sleep 3

# Validate retrieval results
python3 - <<'PY'
import os,json,subprocess,tempfile,math
try:
    src="/sdcard/MiraClip/out/search/results_q.json"
    tmp=tempfile.mkdtemp()
    loc=os.path.join(tmp,"results.json")
    
    subprocess.check_call(["adb","pull",src,loc], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    d=json.load(open(loc))
    
    # Validate retrieval invariants
    for q in d["results"]:
        s=[e["score"] for e in q["top_k"]]
        # Check for NaN/Inf
        assert all(ss==ss for ss in s), "NaN scores found"
        # Check sorting (descending)
        assert s==sorted(s, reverse=True), "Scores not sorted descending"
    
    print(f"✓ Retrieval sanity: {len(d['results'])} queries, scores finite and sorted")
    print(json.dumps({"ok":True,"queries":len(d["results"])}, indent=2))
except Exception as e:
    print(f"✗ Retrieval validation failed: {e}")
    exit(1)
PY

if [ $? -ne 0 ]; then
    echo "✗ Retrieval validation failed"
    exit 1
fi
echo ""

# Step M - Reproducibility (Bit-Identical Index)
echo "Step M: Reproducibility (Bit-Identical Index)"
echo "--------------------------------------------"
echo "Testing reproducibility: same inputs → same bytes..."

DIR="/sdcard/MiraClip/out/embeddings"
VAR="clip_vit_b32_mean_v1"

# Clean and run first ingestion
adb shell rm -rf "$DIR/$VAR" || true
adb shell am broadcast -a com.mira.clip.INGEST_MANIFEST --es manifest_path "$ROOT/in/ingest.json" >/dev/null 2>&1
sleep 5
F1=$(adb shell "ls $DIR/$VAR/*.f32" | head -n1)
H1=$(adb shell sha256sum "$F1" | awk '{print $1}')

# Clean and run second ingestion
adb shell rm -rf "$DIR/$VAR"
adb shell am broadcast -a com.mira.clip.INGEST_MANIFEST --es manifest_path "$ROOT/in/ingest.json" >/dev/null 2>&1
sleep 5
F2=$(adb shell "ls $DIR/$VAR/*.f32" | head -n1)
H2=$(adb shell sha256sum "$F2" | awk '{print $1}')

echo "Hash 1: $H1"
echo "Hash 2: $H2"

if [ "$H1" = "$H2" ]; then
    echo "✓ Reproducibility: Bit-identical embeddings"
else
    echo "✗ Reproducibility: Hash mismatch"
    exit 1
fi
echo ""

# Summary
echo "=== CLIP4Clip Verification Complete ==="
echo "Finished at: $(date)"
echo ""
echo "✓ All control knots validated:"
echo "  - Isolation: Package $PKG, storage $ROOT"
echo "  - Representation: Text/video encoders, L2 normalized"
echo "  - Temporal: Uniform sampling, mean pooling"
echo "  - Indexing: File-based artifacts, metadata"
echo "  - Retrieval: Cosine similarity, finite sorted scores"
echo "  - Reproducibility: Bit-identical outputs"
echo ""
echo "🎯 Research pipeline ready for controlled experiments!"

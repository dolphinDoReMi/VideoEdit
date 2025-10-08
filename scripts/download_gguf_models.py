#!/usr/bin/env python3
"""
Helper script for downloading GGUF models by size
"""

import sys
import os
from huggingface_hub import hf_hub_download

def download_gguf_models(size):
    """Download GGUF models for a specific size"""
    
    models_dir = os.path.join(os.path.dirname(os.path.dirname(__file__)), "whisper_models", "gguf")
    os.makedirs(models_dir, exist_ok=True)
    
    # Define model sizes and quantizations
    size_configs = {
        "tiny": ["tiny", "tiny.en"],
        "small": ["small", "small.en"],
        "medium": ["medium", "medium.en"],
        "large": ["large-v2", "large-v3"]
    }
    
    quants = ["q5_1"]
    
    if size not in size_configs:
        print(f"❌ Unknown size: {size}")
        print(f"Available sizes: {', '.join(size_configs.keys())}")
        return False
    
    sizes = size_configs[size]
    success_count = 0
    
    for s in sizes:
        for q in quants:
            filename = f"ggml-{s}-{q}.bin"
            try:
                path = hf_hub_download(
                    repo_id="ggerganov/whisper.cpp",
                    filename=filename,
                    local_dir=models_dir
                )
                print(f"✅ {filename}")
                success_count += 1
            except Exception as e:
                print(f"❌ {filename}: {e}")
    
    print(f"\n📊 Downloaded {success_count}/{len(sizes) * len(quants)} models")
    return success_count > 0

if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("Usage: python3 download_gguf_models.py <size>")
        print("Sizes: tiny, small, medium, large")
        sys.exit(1)
    
    size = sys.argv[1]
    success = download_gguf_models(size)
    sys.exit(0 if success else 1)

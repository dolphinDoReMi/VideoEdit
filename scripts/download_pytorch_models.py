#!/usr/bin/env python3
"""
Helper script for downloading PyTorch models by size
"""

import sys
import os
from huggingface_hub import snapshot_download

def download_pytorch_models(size):
    """Download PyTorch models for a specific size"""
    
    models_dir = os.path.join(os.path.dirname(os.path.dirname(__file__)), "whisper_models", "transformers")
    os.makedirs(models_dir, exist_ok=True)
    
    # Define model configurations
    size_configs = {
        "tiny": ["openai/whisper-tiny", "openai/whisper-tiny.en"],
        "small": ["openai/whisper-small", "openai/whisper-small.en"],
        "medium": ["openai/whisper-medium", "openai/whisper-medium.en"],
        "large": ["openai/whisper-large", "openai/whisper-large-v2", "openai/whisper-large-v3", "openai/whisper-large-v3-turbo"]
    }
    
    if size not in size_configs:
        print(f"❌ Unknown size: {size}")
        print(f"Available sizes: {', '.join(size_configs.keys())}")
        return False
    
    models = size_configs[size]
    success_count = 0
    
    for model_id in models:
        model_name = model_id.split("/")[-1]
        local_dir = os.path.join(models_dir, model_name)
        
        try:
            path = snapshot_download(
                repo_id=model_id,
                local_dir=local_dir
            )
            print(f"✅ {model_id}")
            success_count += 1
        except Exception as e:
            print(f"❌ {model_id}: {e}")
    
    print(f"\n📊 Downloaded {success_count}/{len(models)} models")
    return success_count > 0

if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("Usage: python3 download_pytorch_models.py <size>")
        print("Sizes: tiny, small, medium, large")
        sys.exit(1)
    
    size = sys.argv[1]
    success = download_pytorch_models(size)
    sys.exit(0 if success else 1)

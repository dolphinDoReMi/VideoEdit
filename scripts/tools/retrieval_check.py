#!/usr/bin/env python3
"""
CLIP Embedding Validator
Validates .f32 embedding files and computes cosine similarity with query vectors.
"""

import struct
import json
import sys
import math
from pathlib import Path

def read_f32_embedding(file_path, expected_dim=None):
    """Read a .f32 embedding file (little-endian float32)."""
    with open(file_path, 'rb') as f:
        data = f.read()
    
    # Each float is 4 bytes
    num_floats = len(data) // 4
    if len(data) % 4 != 0:
        raise ValueError(f"File size {len(data)} is not multiple of 4")
    
    if expected_dim and num_floats != expected_dim:
        raise ValueError(f"Expected {expected_dim} floats, got {num_floats}")
    
    # Unpack as little-endian float32
    embedding = struct.unpack(f'<{num_floats}f', data)
    return list(embedding)

def l2_norm(vec):
    """Compute L2 norm of a vector."""
    return math.sqrt(sum(x * x for x in vec))

def cosine_similarity(a, b):
    """Compute cosine similarity between two vectors."""
    if len(a) != len(b):
        raise ValueError(f"Vector dimensions don't match: {len(a)} vs {len(b)}")
    
    dot_product = sum(x * y for x, y in zip(a, b))
    norm_a = l2_norm(a)
    norm_b = l2_norm(b)
    
    if norm_a == 0 or norm_b == 0:
        return 0.0
    
    return dot_product / (norm_a * norm_b)

def validate_embedding(embedding_path, expected_dim=None):
    """Validate an embedding file."""
    print(f"🔍 Validating: {embedding_path}")
    
    try:
        embedding = read_f32_embedding(embedding_path, expected_dim)
        print(f"  ✅ Dimensions: {len(embedding)}")
        print(f"  ✅ L2 norm: {l2_norm(embedding):.6f}")
        print(f"  ✅ Min value: {min(embedding):.6f}")
        print(f"  ✅ Max value: {max(embedding):.6f}")
        print(f"  ✅ Mean value: {sum(embedding)/len(embedding):.6f}")
        
        # Check if normalized (L2 norm should be close to 1.0)
        norm = l2_norm(embedding)
        if abs(norm - 1.0) < 0.01:
            print(f"  ✅ Normalized (L2 norm ≈ 1.0)")
        else:
            print(f"  ⚠️  Not normalized (L2 norm = {norm:.6f})")
        
        return embedding
        
    except Exception as e:
        print(f"  ❌ Error: {e}")
        return None

def main():
    if len(sys.argv) < 2:
        print("Usage: python3 retrieval_check.py <embedding.f32> [expected_dim] [query.json]")
        print("  embedding.f32: Path to .f32 embedding file")
        print("  expected_dim: Expected embedding dimension (optional)")
        print("  query.json: Path to query vector JSON file (optional)")
        sys.exit(1)
    
    embedding_path = sys.argv[1]
    expected_dim = int(sys.argv[2]) if len(sys.argv) > 2 else None
    query_path = sys.argv[3] if len(sys.argv) > 3 else None
    
    if not Path(embedding_path).exists():
        print(f"❌ Embedding file not found: {embedding_path}")
        sys.exit(1)
    
    # Validate embedding
    embedding = validate_embedding(embedding_path, expected_dim)
    if embedding is None:
        sys.exit(1)
    
    # Load and validate query if provided
    if query_path and Path(query_path).exists():
        print(f"\n🔍 Validating query: {query_path}")
        try:
            with open(query_path, 'r') as f:
                query_data = json.load(f)
            
            if 'vector' in query_data:
                query_vec = query_data['vector']
            elif isinstance(query_data, list):
                query_vec = query_data
            else:
                print(f"  ❌ Invalid query format. Expected 'vector' key or array.")
                sys.exit(1)
            
            if len(query_vec) != len(embedding):
                print(f"  ❌ Query dimension {len(query_vec)} doesn't match embedding dimension {len(embedding)}")
                sys.exit(1)
            
            similarity = cosine_similarity(embedding, query_vec)
            print(f"  ✅ Cosine similarity: {similarity:.6f}")
            
        except Exception as e:
            print(f"  ❌ Error loading query: {e}")
            sys.exit(1)
    
    print(f"\n🎉 Validation complete!")

if __name__ == "__main__":
    main()

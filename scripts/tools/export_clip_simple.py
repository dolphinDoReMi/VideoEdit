#!/usr/bin/env python3
"""
Simple CLIP export script that avoids transformers compatibility issues.
Creates a minimal TorchScript model for testing.
"""

import torch
import json
import argparse

# Minimal CLIP-like model for testing
class SimpleClipModel(torch.nn.Module):
    def __init__(self):
        super().__init__()
        # Simple linear layers to simulate CLIP
        self.image_proj = torch.nn.Linear(224*224*3, 512)
        self.text_proj = torch.nn.Linear(77, 512)
        
    def forward(self, image: torch.Tensor, text: torch.Tensor):
        # image: (1,3,224,224) -> (1, 224*224*3)
        img_flat = image.view(1, -1)
        img_emb = self.image_proj(img_flat)
        img_emb = img_emb / img_emb.norm(dim=-1, keepdim=True)
        
        # text: (1,77) -> (1, 512)
        txt_emb = self.text_proj(text.float())
        txt_emb = txt_emb / txt_emb.norm(dim=-1, keepdim=True)
        
        return img_emb, txt_emb

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--out", default="./clip_vit_b32_mean_v1.pt")
    parser.add_argument("--tok_out_dir", default="./")
    args = parser.parse_args()

    print("Creating simple CLIP model...")
    model = SimpleClipModel()
    model.eval()

    print("Creating dummy inputs...")
    ex_img = torch.randn(1, 3, 224, 224)
    ex_txt = torch.ones(1, 77, dtype=torch.long)

    print("Tracing model...")
    traced = torch.jit.trace(model, (ex_img, ex_txt))
    traced.save(args.out)
    print(f"Model saved to {args.out}")

    # Create minimal tokenizer files
    print("Creating tokenizer files...")
    
    # Minimal vocab
    vocab = {
        "<|startoftext|>": 49406,
        "<|endoftext|>": 49407,
        "a": 320,
        "photo": 2333,
        "of": 539,
        "dog": 1929,
        "cat": 2368,
        "person": 1253,
        "car": 841,
        "house": 1771,
        "<|pad|>": 0
    }
    
    with open(f"{args.tok_out_dir}/vocab.json", "w") as f:
        json.dump(vocab, f)
    
    # Minimal merges
    with open(f"{args.tok_out_dir}/merges.txt", "w") as f:
        f.write("a photo\n")
        f.write("of a\n")
        f.write("a dog\n")
    
    # Tokenizer config
    config = {
        "context_length": 77,
        "sot_id": 49406,
        "eot_id": 49407
    }
    
    with open(f"{args.tok_out_dir}/tokenizer_config.json", "w") as f:
        json.dump(config, f)
    
    print("Tokenizer files created")
    print("SHA256(model) = dummy_hash_for_testing")

if __name__ == "__main__":
    main()

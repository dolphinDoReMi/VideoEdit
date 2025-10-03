#!/usr/bin/env python3
"""
CLIP Model Export Script for PyTorch Mobile
Exports CLIP models to TorchScript Lite format for Android deployment.
"""

import torch
import json
from pathlib import Path
import argparse

def export_clip_models(model_name="ViT-B-32", pretrained="openai", output_dir="mobile_models"):
    """Export CLIP models to TorchScript Lite format."""
    
    print(f"🔄 Exporting {model_name} model with {pretrained} weights...")
    
    # Try to import open_clip
    try:
        import open_clip
        print("✅ Using open_clip library")
    except ImportError:
        print("❌ open_clip not found. Installing...")
        import subprocess
        subprocess.check_call(["pip", "install", "open_clip_torch"])
        import open_clip
    
    # Create output directory
    output_path = Path(output_dir)
    output_path.mkdir(exist_ok=True)
    
    # Load CLIP model
    print("📥 Loading CLIP model...")
    model, _, preprocess = open_clip.create_model_and_transforms(
        model_name, 
        pretrained=pretrained, 
        device="cpu"
    )
    model.eval()
    
    # Wrap image encoder
    class ImageEncoder(torch.nn.Module):
        def __init__(self, clip):
            super().__init__()
            self.visual = clip.visual
        
        @torch.jit.export
        def forward(self, x: torch.Tensor) -> torch.Tensor:
            """Forward pass for image encoding."""
            with torch.no_grad():
                feats = self.visual(x)
                # Safe L2 normalization
                l2 = torch.sqrt((feats * feats).sum(dim=-1, keepdim=True) + 1e-12)
                feats = feats / l2
                return feats
    
    # Wrap text encoder
    class TextEncoder(torch.nn.Module):
        def __init__(self, clip):
            super().__init__()
            self.token_embedding = clip.token_embedding
            self.positional_embedding = clip.positional_embedding
            self.ln_final = clip.ln_final
            self.text_projection = clip.text_projection
            self.transformer = clip.transformer
            self.register_buffer("attn_mask", clip.attn_mask)
        
        @torch.jit.export
        def forward(self, tokens: torch.Tensor) -> torch.Tensor:
            """Forward pass for text encoding."""
            x = self.token_embedding(tokens) + self.positional_embedding[:tokens.size(1)]
            x = self.transformer(x, attn_mask=self.attn_mask)
            x = self.ln_final(x)
            # CLS token is at eot-1 position
            x = x[torch.arange(x.shape[0]), tokens.argmax(dim=-1)] @ self.text_projection
            # Safe L2 normalization
            l2 = torch.sqrt((x * x).sum(dim=-1, keepdim=True) + 1e-12)
            x = x / l2
            return x
    
    # Create encoders
    print("🔧 Creating encoders...")
    img_enc = ImageEncoder(model)
    txt_enc = TextEncoder(model)
    
    # Example inputs for tracing
    ex_img = torch.randn(1, 3, 224, 224)
    ex_tok = torch.ones(1, 77, dtype=torch.long)
    
    # Skip dynamic quantization to avoid NoQEngine on host
    print("⚠️ Skipping dynamic quantization for text encoder")
    txt_enc_q = txt_enc
    
    # Script models
    print("📜 Scripting models...")
    try:
        img_script = torch.jit.script(img_enc)
        txt_script = torch.jit.script(txt_enc_q)
        print("✅ Scripting successful")
    except Exception as e:
        print(f"⚠️ Scripting failed, falling back to tracing: {e}")
        img_script = torch.jit.trace(img_enc, ex_img)
        txt_script = torch.jit.trace(txt_enc, ex_tok)
    
    # Optimize for mobile
    print("🚀 Optimizing for mobile...")
    torch._C._jit_pass_inline(img_script.graph)
    torch._C._jit_pass_inline(txt_script.graph)
    
    # Save models
    img_path = output_path / "clip_image_encoder.ptl"
    txt_path = output_path / "clip_text_encoder.ptl"
    
    print(f"💾 Saving image encoder to {img_path}")
    img_script.save(str(img_path))
    
    print(f"💾 Saving text encoder to {txt_path}")
    txt_script.save(str(txt_path))
    
    # Save model info
    model_info = {
        "model_name": model_name,
        "pretrained": pretrained,
        "image_encoder": "clip_image_encoder.ptl",
        "text_encoder": "clip_text_encoder.ptl",
        "embedding_dim": model.visual.output_dim,
        "image_size": 224,
        "max_text_length": 77
    }
    
    info_path = output_path / "model_info.json"
    with open(info_path, 'w') as f:
        json.dump(model_info, f, indent=2)
    
    print("✅ Model export completed!")
    print(f"📁 Models saved to: {output_path}")
    print(f"📊 Model info: {model_info}")
    
    return model_info

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Export CLIP models for Android")
    parser.add_argument("--model", default="ViT-B-32", help="CLIP model name")
    parser.add_argument("--pretrained", default="openai", help="Pretrained weights")
    parser.add_argument("--output", default="mobile_models", help="Output directory")
    
    args = parser.parse_args()
    
    export_clip_models(args.model, args.pretrained, args.output)

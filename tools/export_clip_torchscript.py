import torch, json, argparse
import warnings
warnings.filterwarnings("ignore", category=FutureWarning)
from transformers import CLIPModel, CLIPProcessor, AutoTokenizer

# Wrapper so forward(image, text) works on-device
class ClipMobile(torch.nn.Module):
    def __init__(self, model):
        super().__init__()
        self.model = model

    @torch.jit.export
    def forward(self, image: torch.Tensor, text: torch.Tensor):
        # image: (1,3,224,224) float32 normalized
        # text:  (1,77) int64 token ids
        out_img = self.model.get_image_features(image)
        out_txt = self.model.get_text_features(text)
        # L2-normalize to match CLIP usage
        out_img = out_img / out_img.norm(dim=-1, keepdim=True)
        out_txt = out_txt / out_txt.norm(dim=-1, keepdim=True)
        return out_img, out_txt

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--hf", default="openai/clip-vit-base-patch32")
    ap.add_argument("--out", default="./clip_vit_b32_mean_v1.pt")
    ap.add_argument("--tok_out_dir", default="./")
    args = ap.parse_args()

    print("Loading CLIP model...")
    model = CLIPModel.from_pretrained(args.hf)
    print("Loading tokenizer...")
    tokenizer = AutoTokenizer.from_pretrained(args.hf)
    processor = CLIPProcessor.from_pretrained(args.hf)

    print("Creating mobile wrapper...")
    mobile = ClipMobile(model.eval())
    
    print("Creating dummy inputs...")
    ex_img = torch.randn(1,3,224,224)  # dummy normalized input
    ex_txt = torch.ones(1,77, dtype=torch.long) * tokenizer.eos_token_id
    
    print("Tracing model...")
    try:
        # Try to trace with strict=False to handle warnings
        ts = torch.jit.trace(mobile, (ex_img, ex_txt), strict=False)
        ts.save(args.out)
        print(f"Model saved to {args.out}")
    except Exception as e:
        print(f"Tracing failed: {e}")
        print("Trying script instead of trace...")
        # Fallback to script
        ts = torch.jit.script(mobile)
        ts.save(args.out)
        print(f"Model saved to {args.out}")

    # Save tokenizer artifacts
    # We'll export the "vocab.json" + "merges.txt" + special token ids explicitly.
    vocab = tokenizer.get_vocab()
    with open(f"{args.tok_out_dir}/vocab.json", "w", encoding="utf-8") as f:
        json.dump(vocab, f, ensure_ascii=False)

    merges = tokenizer.backend_tokenizer.model.get_merges()
    with open(f"{args.tok_out_dir}/merges.txt", "w", encoding="utf-8") as f:
        for a,b in merges:
            f.write(f"{a} {b}\n")

    meta = {
        "context_length": tokenizer.model_max_length,
        "sot_id": getattr(tokenizer, "bos_token_id", 49406),
        "eot_id": getattr(tokenizer, "eos_token_id", 49407)
    }
    with open(f"{args.tok_out_dir}/tokenizer_config.json", "w", encoding="utf-8") as f:
        json.dump(meta, f)

    # Optional: print SHA256 to record
    import hashlib, pathlib
    def sha(p): 
        h = hashlib.sha256(); 
        h.update(pathlib.Path(p).read_bytes()); 
        return h.hexdigest()
    print("SHA256(model) =", sha(args.out))

if __name__ == "__main__":
    main()

#!/usr/bin/env python3
"""
Compute a CLIP image-encoder mean-pooled embedding for a local video using the
TorchScript image encoder shipped in this repo, without requiring Android.

Usage:
  python3 tools/local_video_clip_embed.py \
    --video app/src/main/assets/video_v1.mp4 \
    --model mobile_models/clip_image_encoder.ptl \
    --frames 8

Outputs a JSON object: { "dim": 512, "vector": [ ... ] }
"""
import argparse
import json
import math
import os
import subprocess
import sys
from typing import List

try:
    import numpy as np
    import torch
except Exception as e:
    print(f"ERROR: Missing dependency: {e}. Please ensure numpy and torch are installed.", file=sys.stderr)
    sys.exit(1)


def run(cmd: List[str]) -> subprocess.CompletedProcess:
    return subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)


def ffprobe_duration(video_path: str) -> float:
    p = run([
        "ffprobe", "-v", "error", "-show_entries", "format=duration",
        "-of", "default=noprint_wrappers=1:nokey=1", video_path,
    ])
    if p.returncode != 0:
        raise RuntimeError(f"ffprobe failed: {p.stderr.strip()}")
    try:
        return float(p.stdout.strip())
    except Exception as ex:
        raise RuntimeError(f"Unable to parse duration: '{p.stdout.strip()}'") from ex


def extract_frame_rgb(video_path: str, t_seconds: float, out_raw: str, side: int = 256) -> None:
    # Letterbox to side x side, then write RGB24 raw
    vf = f"scale={side}:{side}:force_original_aspect_ratio=decrease,pad={side}:{side}:(ow-iw)/2:(oh-ih)/2"
    p = run([
        "ffmpeg", "-y", "-ss", str(t_seconds), "-i", video_path,
        "-frames:v", "1", "-vf", vf, "-pix_fmt", "rgb24", "-f", "rawvideo", out_raw,
    ])
    if p.returncode != 0:
        raise RuntimeError(f"ffmpeg failed at t={t_seconds}: {p.stderr}")


def normalize_clip(img_chw: np.ndarray) -> np.ndarray:
    # img_chw: 3xH xW in [0,1]
    mean = np.array([0.48145466, 0.4578275, 0.40821073], dtype=np.float32)[:, None, None]
    std  = np.array([0.26862954, 0.26130258, 0.27577711], dtype=np.float32)[:, None, None]
    return (img_chw - mean) / std


def encode_video(
    video_path: str,
    model_path: str,
    frames: int,
    crop_size: int = 224,
    temp_dir: str = "tmp_frames",
) -> np.ndarray:
    os.makedirs(temp_dir, exist_ok=True)
    duration = ffprobe_duration(video_path)
    # Avoid endpoints
    stamps = [duration * (i + 1) / (frames + 1) for i in range(frames)]

    raw_side = 256
    raw_paths: List[str] = []
    for i, t in enumerate(stamps):
        raw = os.path.join(temp_dir, f"f{i:02d}.rgb")
        extract_frame_rgb(video_path, t, raw, side=raw_side)
        raw_paths.append(raw)

    module = torch.jit.load(model_path, map_location="cpu")
    module.eval()

    # Center crop 224x224 from 256x256 letterboxed frame
    margin = (raw_side - crop_size) // 2
    per_frame: List[np.ndarray] = []
    for rp in raw_paths:
        arr = np.fromfile(rp, dtype=np.uint8)
        if arr.size != raw_side * raw_side * 3:
            raise RuntimeError(f"Unexpected raw size at {rp}: {arr.size}")
        img = (arr.reshape(raw_side, raw_side, 3).astype(np.float32) / 255.0)
        img = img[margin: margin + crop_size, margin: margin + crop_size, :]
        chw = np.transpose(img, (2, 0, 1))  # 3x224x224
        chw = normalize_clip(chw)
        tensor = torch.from_numpy(chw).unsqueeze(0)  # 1x3x224x224
        with torch.no_grad():
            out = module(tensor)
            if isinstance(out, (tuple, list)):
                out = out[0]
            vec = out.squeeze().float().cpu().numpy()
        # L2 normalize frame embedding
        n = np.linalg.norm(vec) + 1e-12
        per_frame.append(vec / n)

    mean = np.mean(np.stack(per_frame, axis=0), axis=0)
    final = mean / (np.linalg.norm(mean) + 1e-12)
    return final.astype(np.float32)


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--video", required=True)
    ap.add_argument("--model", required=True)
    ap.add_argument("--frames", type=int, default=8)
    args = ap.parse_args()

    vec = encode_video(args.video, args.model, args.frames)
    print(json.dumps({"dim": int(vec.shape[0]), "vector": vec.tolist()}))


if __name__ == "__main__":
    main()



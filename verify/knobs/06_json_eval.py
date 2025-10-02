#!/usr/bin/env python3
import json, sys, re, unicodedata

p = sys.argv[1] if len(sys.argv)>1 else "real_speech_16k.json"
r = json.load(open(p))
segs = r.get("segments", [])
assert isinstance(segs, list)
assert all( s.get("t0Ms") <= s.get("t1Ms") for s in segs )
txt = " ".join(s.get("text","") for s in segs).strip()
print(f"segments={len(segs)} chars={len(txt)}")
assert len(segs)>=1 and len(txt)>=5, "Transcript too short"
print("✅ JSON sane")

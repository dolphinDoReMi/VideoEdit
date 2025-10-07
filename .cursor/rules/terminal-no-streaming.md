# Terminal Safety: No Streaming/Interactive Commands

**Scope: terminal**

**Never run** long-lived or streaming commands automatically. Examples (not exhaustive):
- `adb logcat` **without** `-d`, `-t`, or `-T` (streams forever)
- `tail -f`, `watch`, `journalctl -f`
- Pipelines where the first stage is streaming, e.g.:
  - `adb logcat | grep -E "(WhisperBridge|File picker|Opening file picker)" | head -5`

**Instead (policy):**
1) **Do not execute** these in Auto/YOLO. **Print** the command in a fenced code block with a short note: "manual run only".
2) If proposing `adb logcat`, **bound it** so it exits:
   - Prefer `adb logcat -d | grep -E "(WhisperBridge|File picker|Opening file picker)" | head -5`
   - Or wrap with a timeout: `timeout 5s adb logcat | grep -E "(...)" | head -5`
   - Or limit by time/lines: `adb logcat -T '5s'` / `adb logcat -t 200`
3) If a streaming command was inadvertently started, suggest cleanup: `pkill -f "adb.*logcat"`.

**Rationale:** Streaming processes can leave upstream stages running after downstream tools exit (e.g., `head` quits but `adb logcat` keeps writing), which hangs the terminal session in Auto flows.

**Examples**

❌ **Do not run automatically**
```sh
adb logcat | grep -E "(WhisperBridge|File picker|Opening file picker)" | head -5
```

✅ **Suggest as bounded / manual**
```sh
# manual run only
adb logcat -d | grep -E "(WhisperBridge|File picker|Opening file picker)" | head -5
```

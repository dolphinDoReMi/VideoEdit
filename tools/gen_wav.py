#!/usr/bin/env python3
"""Generate a 1-second 16kHz WAV tone for testing."""
import sys
import wave
import struct
import math

def generate_tone(output_path, duration=1.0, sample_rate=16000, frequency=440.0):
    """Generate a sine wave tone and save as WAV."""
    num_samples = int(duration * sample_rate)
    samples = []
    
    for i in range(num_samples):
        t = i / sample_rate
        sample = math.sin(2 * math.pi * frequency * t)
        # Convert to 16-bit PCM
        pcm_sample = int(sample * 32767)
        samples.append(pcm_sample)
    
    with wave.open(output_path, 'wb') as wav_file:
        wav_file.setnchannels(1)  # Mono
        wav_file.setsampwidth(2)  # 16-bit
        wav_file.setframerate(sample_rate)
        
        for sample in samples:
            wav_file.writeframes(struct.pack('<h', sample))

if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("Usage: python3 gen_wav.py <output.wav>")
        sys.exit(1)
    
    output_path = sys.argv[1]
    generate_tone(output_path)
    print(f"Generated 1s 16kHz tone: {output_path}")

#!/bin/bash

echo "🔧 Applying AudioIO streaming fix..."

# Fix the first occurrence in decodeAacMp4 function
sed -i '' 's/val outPcm = ArrayList<Short>()/val chunks = mutableListOf<ShortArray>()\n        var totalSamples = 0/' feature/whisper/src/main/java/com/mira/com/feature/whisper/data/io/AudioIO.kt

# Fix the problematic line
sed -i '' 's/outPcm.addAll(tmp.toList())/chunks.add(tmp)\n                    totalSamples += tmp.size/' feature/whisper/src/main/java/com/mira/com/feature/whisper/data/io/AudioIO.kt

# Fix the final concatenation
sed -i '' 's/val pcm = outPcm.toShortArray()/val pcm = ShortArray(totalSamples)\n        var offset = 0\n        for (chunk in chunks) {\n            System.arraycopy(chunk, 0, pcm, offset, chunk.size)\n            offset += chunk.size\n        }/' feature/whisper/src/main/java/com/mira/com/feature/whisper/data/io/AudioIO.kt

echo "✅ AudioIO streaming fix applied"

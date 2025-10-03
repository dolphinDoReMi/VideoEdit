#!/usr/bin/env node
// Minimal schema validator for Whisper sidecar JSON
const fs = require('fs');

function validateSidecar(filePath) {
  try {
    const content = fs.readFileSync(filePath, 'utf8');
    const data = JSON.parse(content);
    
    // Check required top-level fields
    const required = ['version', 'audio', 'job', 'segments'];
    for (const field of required) {
      if (!(field in data)) {
        throw new Error(`Missing required field: ${field}`);
      }
    }
    
    // Check audio object
    const audio = data.audio;
    const audioRequired = ['uri', 'sr_hz', 'channels', 'duration_ms'];
    for (const field of audioRequired) {
      if (!(field in audio)) {
        throw new Error(`Missing audio.${field}`);
      }
    }
    
    // Check job object
    const job = data.job;
    const jobRequired = ['model', 'threads', 'beam', 'lang', 'translate', 'rtf', 'infer_ms'];
    for (const field of jobRequired) {
      if (!(field in job)) {
        throw new Error(`Missing job.${field}`);
      }
    }
    
    // Check segments array
    if (!Array.isArray(data.segments)) {
      throw new Error('segments must be an array');
    }
    
    for (let i = 0; i < data.segments.length; i++) {
      const segment = data.segments[i];
      const segmentRequired = ['t0_ms', 't1_ms', 'text'];
      for (const field of segmentRequired) {
        if (!(field in segment)) {
          throw new Error(`Missing segments[${i}].${field}`);
        }
      }
    }
    
    console.log(`✅ Valid sidecar: ${filePath}`);
    console.log(`   Audio: ${audio.duration_ms}ms @ ${audio.sr_hz}Hz, ${audio.channels}ch`);
    console.log(`   Job: ${job.model}, RTF=${job.rtf}, ${job.infer_ms}ms`);
    console.log(`   Segments: ${data.segments.length}`);
    
    return true;
  } catch (error) {
    console.error(`❌ Invalid sidecar: ${filePath}`);
    console.error(`   Error: ${error.message}`);
    return false;
  }
}

if (require.main === module) {
  if (process.argv.length !== 3) {
    console.error('Usage: node sidecar_check.js <sidecar.json>');
    process.exit(1);
  }
  
  const filePath = process.argv[2];
  const isValid = validateSidecar(filePath);
  process.exit(isValid ? 0 : 1);
}

#!/usr/bin/env node
/**
 * Soft compliance checker for Android agent automation.
 * - Reads proposed commands + diff plan.
 * - Matches against DENY / REQUIRE_APPROVAL / ALLOW packs.
 * - Emits a human-readable report with INFO/WARN/ALERT and a risk score.
 * - Exit code is 0 by default (soft). Use --strict true to fail on ALERT.
 *
 * Usage:
 *   node scripts/compliance-check.js \
 *     --diffplan .cursor.diffplan.txt \
 *     --commands .cursor.commands.txt \
 *     --risk LOW|MEDIUM|HIGH \
 *     --strict false
 */
const fs = require('fs');
const path = require('path');

// Parse command line arguments
const argv = Object.fromEntries(
  process.argv.slice(2).map((v, i, a) => 
    i % 2 ? [[a[i-1].replace(/^--/, ''), v]] : []
  ).flat()
);

const read = p => (p && fs.existsSync(p)) ? fs.readFileSync(p, 'utf8') : '';
const risk = (argv.risk || 'LOW').toUpperCase();
const strict = (argv.strict || 'false') === 'true';

const text = [read(argv.diffplan), read(argv.commands)].join('\n');

function loadMode(file, mode) {
  try {
    const obj = JSON.parse(fs.readFileSync(path.resolve(file), 'utf8'));
    const list = obj[mode] || [];
    return list.map(s => new RegExp(s, 'mi'));
  } catch (e) {
    console.warn(`Warning: Could not load ${file}: ${e.message}`);
    return [];
  }
}

function match(list) { 
  return list.filter(rx => rx.test(text)).map(rx => rx.source); 
}

// Load pattern files
const denyHits = match(loadMode('.cursor-rules/patterns.deny.json', 'DENY'));
const reqHits = match(loadMode('.cursor-rules/patterns.require_approval.json', 'REQUIRE_APPROVAL'));
const okHits = match(loadMode('.cursor-rules/patterns.allow.json', 'ALLOW'));

// Score heuristic
let score = 0;
score += denyHits.length * 5;
score += reqHits.length * 2;
score += (risk === 'HIGH' ? 1 : 0);

// Level determination
let level = 'LOW';
if (score >= 6) level = 'HIGH';
else if (score >= 3) level = 'MEDIUM';

// Render report
const lines = [];
lines.push(`Mode: SOFT (strict=${strict}) | Risk=${risk} | Score=${score} → Level=${level}`);
lines.push('');

if (denyHits.length) {
  lines.push(`🚨 ALERT (DENY patterns matched):`);
  denyHits.forEach(hit => lines.push(`   - ${hit}`));
  lines.push('');
}

if (reqHits.length) {
  lines.push(`⚠️  WARN (REQUIRE_APPROVAL patterns):`);
  reqHits.forEach(hit => lines.push(`   - ${hit}`));
  lines.push('');
}

if (okHits.length) {
  lines.push(`ℹ️  INFO (Allowed patterns observed):`);
  okHits.forEach(hit => lines.push(`   - ${hit}`));
  lines.push('');
}

if (!denyHits.length && !reqHits.length && !okHits.length) {
  lines.push('ℹ️  INFO: No known patterns matched.');
  lines.push('');
}

lines.push('📋 Recommendations:');
if (level === 'HIGH') {
  lines.push('- 🚨 Review manually before merging. Consider re-running with --strict true in CI for this change set.');
  lines.push('- 🔍 Check for Android scoped storage compliance violations.');
  lines.push('- 🛡️  Verify permission grants are necessary and properly documented.');
} else if (level === 'MEDIUM') {
  lines.push('- 👀 Skim the diff and confirm that ADB/Gradle/MediaStore usage matches project policy.');
  lines.push('- 📱 Ensure scoped storage patterns are followed.');
  lines.push('- 🔒 Review any permission grants for necessity.');
} else {
  lines.push('- ✅ Looks fine; keep an eye on budgets and scoped-storage constraints.');
  lines.push('- 📊 Monitor resource usage and performance impact.');
}

lines.push('');
lines.push('🔧 Android-Specific Checks:');
if (text.includes('/sdcard/') || text.includes('Environment.getExternalStorageDirectory')) {
  lines.push('- ⚠️  Raw external storage usage detected - consider scoped storage alternatives');
}
if (text.includes('MediaExtractor.setDataSource(String') || text.includes('FileInputStream(String')) {
  lines.push('- ⚠️  Direct file path usage detected - use FD-based access instead');
}
if (text.includes('DLStorage.resolveModelPath') && text.includes('DLStorage.openReadFd')) {
  lines.push('- ✅ Scoped storage patterns detected - good compliance');
}

console.log(lines.join('\n'));

// Soft exit; optionally fail in strict mode
if (strict && level === 'HIGH') {
  console.log('\n🚨 STRICT MODE: Exiting with error code due to HIGH risk level');
  process.exit(1);
}

console.log('\n✅ SOFT MODE: Proceeding with exit code 0');
process.exit(0);

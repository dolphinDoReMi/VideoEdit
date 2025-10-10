#!/usr/bin/env node
/**
 * Validation script for cursor rules.
 * Checks rule syntax and pattern validity.
 */
const fs = require('fs');
const path = require('path');

function validateRuleFile(filePath) {
  try {
    const content = fs.readFileSync(filePath, 'utf8');
    const rules = JSON.parse(content);
    
    console.log(`✅ ${filePath}: Valid JSON`);
    
    if (!rules.version) {
      console.warn(`⚠️  ${filePath}: Missing version field`);
    }
    
    if (!rules.rules || !Array.isArray(rules.rules)) {
      console.warn(`⚠️  ${filePath}: Missing or invalid rules array`);
      return false;
    }
    
    // Validate each rule
    rules.rules.forEach((rule, index) => {
      if (!rule.name) {
        console.warn(`⚠️  ${filePath}: Rule ${index} missing name`);
      }
      if (!rule.audience) {
        console.warn(`⚠️  ${filePath}: Rule ${index} missing audience`);
      }
      if (!rule.trigger) {
        console.warn(`⚠️  ${filePath}: Rule ${index} missing trigger`);
      }
      if (!rule.content) {
        console.warn(`⚠️  ${filePath}: Rule ${index} missing content`);
      }
    });
    
    return true;
  } catch (e) {
    console.error(`❌ ${filePath}: ${e.message}`);
    return false;
  }
}

function validatePatternFile(filePath) {
  try {
    const content = fs.readFileSync(filePath, 'utf8');
    const patterns = JSON.parse(content);
    
    console.log(`✅ ${filePath}: Valid JSON`);
    
    // Check for required pattern categories
    const categories = ['DENY', 'REQUIRE_APPROVAL', 'ALLOW'];
    categories.forEach(category => {
      if (patterns[category] && Array.isArray(patterns[category])) {
        console.log(`   ${category}: ${patterns[category].length} patterns`);
        
        // Validate regex patterns
        patterns[category].forEach((pattern, index) => {
          try {
            new RegExp(pattern);
          } catch (e) {
            console.warn(`⚠️  ${filePath}: Invalid regex in ${category}[${index}]: ${pattern}`);
          }
        });
      }
    });
    
    return true;
  } catch (e) {
    console.error(`❌ ${filePath}: ${e.message}`);
    return false;
  }
}

function main() {
  console.log('🔍 Validating cursor rules system...\n');
  
  let allValid = true;
  
  // Validate rule files
  const rulesDir = '.cursor-rules';
  if (fs.existsSync(rulesDir)) {
    const ruleFiles = fs.readdirSync(rulesDir)
      .filter(f => f.endsWith('.json') && !f.startsWith('patterns.'))
      .map(f => path.join(rulesDir, f));
    
    ruleFiles.forEach(file => {
      if (!validateRuleFile(file)) {
        allValid = false;
      }
    });
  }
  
  // Validate pattern files
  const patternsDir = '.cursor-rules';
  if (fs.existsSync(patternsDir)) {
    const patternFiles = fs.readdirSync(patternsDir)
      .filter(f => f.startsWith('patterns.') && f.endsWith('.json'))
      .map(f => path.join(patternsDir, f));
    
    patternFiles.forEach(file => {
      if (!validatePatternFile(file)) {
        allValid = false;
      }
    });
  }
  
  // Validate main cursor rules
  if (fs.existsSync('.cursor-rules/.cursorrules.json')) {
    if (!validateRuleFile('.cursor-rules/.cursorrules.json')) {
      allValid = false;
    }
  }
  
  console.log('\n📊 Validation Summary:');
  if (allValid) {
    console.log('✅ All files are valid');
    process.exit(0);
  } else {
    console.log('❌ Some files have issues');
    process.exit(1);
  }
}

main();

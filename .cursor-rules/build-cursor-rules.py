#!/usr/bin/env python3
"""
Build script for cursor rules system.
Combines layered rules into a single .cursorrules.json file.
"""
import json
import os
import sys
from pathlib import Path

def load_rule_file(file_path):
    """Load and parse a rule file."""
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            return json.load(f)
    except (FileNotFoundError, json.JSONDecodeError) as e:
        print(f"Warning: Could not load {file_path}: {e}")
        return None

def build_cursor_rules():
    """Build the complete cursor rules from layered files."""
    rules_dir = Path(".cursor-rules")
    if not rules_dir.exists():
        print("Error: .cursor-rules/ directory not found")
        return False
    
    # Load rule files in order
    rule_files = sorted(rules_dir.glob("*.json"))
    combined_rules = {
        "version": 1,
        "rules": []
    }
    
    for rule_file in rule_files:
        print(f"Loading {rule_file.name}...")
        rule_data = load_rule_file(rule_file)
        if rule_data and "rules" in rule_data:
            combined_rules["rules"].extend(rule_data["rules"])
    
    # Write combined rules
    output_file = ".cursor-rules/.cursorrules.json"
    with open(output_file, 'w', encoding='utf-8') as f:
        json.dump(combined_rules, f, indent=2)
    
    print(f"✅ Combined {len(combined_rules['rules'])} rules into {output_file}")
    return True

if __name__ == "__main__":
    success = build_cursor_rules()
    sys.exit(0 if success else 1)

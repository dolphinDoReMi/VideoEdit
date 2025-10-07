# Documentation Validation Workflow

## Overview
This workflow validates the documentation structure and format according to the VideoEdit project standards.

## Documentation Structure Validation

### Required Folder Structure
```
docs/
├── clip/                                    # CLIP Visual Understanding
│   ├── Scripts/                             # CLIP-specific scripts
│   ├── Architecture Design and Control Knot.md
│   ├── Full scale implementation Details.md
│   ├── XiaoMi Pad Inference Optimization.md
│   ├── iPad Inference Optimization.md
│   └── README.md
├── whisper/                                 # Whisper Speech Recognition
│   ├── Scripts/                             # Whisper-specific scripts
│   ├── Architecture Design and Control Knot.md
│   ├── Full scale implementation Details.md
│   ├── XiaoMi Pad Inference Optimization.md
│   ├── iPad Inference Optimization.md
│   └── README.md
├── infra/                                   # Infrastructure Services
│   ├── Scripts/                             # Infrastructure scripts
│   ├── Architecture Design and Control Knot.md
│   ├── Full scale implementation Details.md
│   ├── XiaoMi Pad Inference Optimization.md
│   ├── iPad Inference Optimization.md
│   └── README.md
├── CICD & Release Guide/                    # CI/CD Documentation
│   ├── GitHub Guide.md
│   ├── XiaoMi Pad Inference Optimization.md
│   ├── iPad Inference Optimization.md
│   └── README.md
├── cursor_rule.md
└── README.md
```

## Control Knots Format Validation

### Required Format for Architecture Design and Control Knot
Each `Architecture Design and Control Knot.md` file must contain:

```markdown
**Status: READY FOR VERIFICATION**

**Control knots:**
- [Control knot 1]: [Description]
- [Control knot 2]: [Description]
- [Control knot 3]: [Description]

**Implementation:**
- [Implementation detail 1]: [Description]
- [Implementation detail 2]: [Description]
- [Implementation detail 3]: [Description]

**Verification:** [Script path] in `[path/to/script.sh]`
```

### Required Format for Full Scale Implementation Details
Each `Full scale implementation Details.md` file must contain:

1. **Problem Disaggregation**
2. **Analysis with Trade-offs**
3. **Design Pipeline**
4. **Key Control Knots**
5. **Implementation Architecture**
6. **Scale-out Plan**
7. **Code Pointers**

### Required Format for Device Optimization
Each device optimization file must contain:

1. **Control Knots** (device-specific)
2. **Implementation** (device-specific)
3. **Verification** (device-specific scripts)
4. **Device Specifications**
5. **Performance Optimization**
6. **Deployment Scripts**
7. **Performance Metrics**
8. **Troubleshooting**

## Multi-Lens Expert Communication Format

### Required Format for README.md Files
Each feature README.md must contain:

1. **Plain-text**: Step-by-step how it works
2. **For a Recommendation System Expert**: Indexing, ANN, similarity search
3. **For a Deep Learning Expert**: Model architecture, numerical hygiene
4. **For a Content Understanding Expert**: Primitive outputs, diagnostics
5. **For an Audio/Video Expert**: Domain-specific implementation details

## Validation Scripts

### Documentation Structure Validator
```bash
#!/bin/bash
# docs/CICD & Release Guide/Scripts/validate_docs_structure.sh

echo "Validating documentation structure..."

# Check required folders
required_folders=("clip" "whisper" "infra" "CICD & Release Guide")
for folder in "${required_folders[@]}"; do
    if [ ! -d "docs/$folder" ]; then
        echo "❌ Missing required folder: docs/$folder"
        exit 1
    fi
    echo "✅ Found folder: docs/$folder"
done

# Check required files in each feature folder
features=("clip" "whisper" "infra")
for feature in "${features[@]}"; do
    required_files=(
        "Architecture Design and Control Knot.md"
        "Full scale implementation Details.md"
        "XiaoMi Pad Inference Optimization.md"
        "iPad Inference Optimization.md"
        "README.md"
    )
    
    for file in "${required_files[@]}"; do
        if [ ! -f "docs/$feature/$file" ]; then
            echo "❌ Missing required file: docs/$feature/$file"
            exit 1
        fi
        echo "✅ Found file: docs/$feature/$file"
    done
    
    # Check Scripts folder
    if [ ! -d "docs/$feature/Scripts" ]; then
        echo "❌ Missing Scripts folder: docs/$feature/Scripts"
        exit 1
    fi
    echo "✅ Found Scripts folder: docs/$feature/Scripts"
done

echo "✅ Documentation structure validation passed"
```

### Control Knots Format Validator
```bash
#!/bin/bash
# docs/CICD & Release Guide/Scripts/validate_control_knots.sh

echo "Validating control knots format..."

features=("clip" "whisper" "infra")
for feature in "${features[@]}"; do
    file="docs/$feature/Architecture Design and Control Knot.md"
    
    # Check for required sections
    if ! grep -q "Status: READY FOR VERIFICATION" "$file"; then
        echo "❌ Missing status in $file"
        exit 1
    fi
    
    if ! grep -q "Control knots:" "$file"; then
        echo "❌ Missing control knots section in $file"
        exit 1
    fi
    
    if ! grep -q "Implementation:" "$file"; then
        echo "❌ Missing implementation section in $file"
        exit 1
    fi
    
    if ! grep -q "Verification:" "$file"; then
        echo "❌ Missing verification section in $file"
        exit 1
    fi
    
    echo "✅ Control knots format valid for $feature"
done

echo "✅ Control knots format validation passed"
```

### Multi-Lens Communication Validator
```bash
#!/bin/bash
# docs/CICD & Release Guide/Scripts/validate_multi_lens.sh

echo "Validating multi-lens expert communication format..."

features=("clip" "whisper" "infra")
for feature in "${features[@]}"; do
    file="docs/$feature/README.md"
    
    # Check for required expert sections
    required_sections=(
        "Plain-text:"
        "For a Recommendation System Expert"
        "For a Deep Learning Expert"
        "For a Content Understanding Expert"
        "For an Audio/Video Expert"
    )
    
    for section in "${required_sections[@]}"; do
        if ! grep -q "$section" "$file"; then
            echo "❌ Missing section '$section' in $file"
            exit 1
        fi
    done
    
    echo "✅ Multi-lens format valid for $feature"
done

echo "✅ Multi-lens expert communication validation passed"
```

## GitHub Actions Workflow

### Documentation Validation Workflow
```yaml
# .github/workflows/docs-validation.yml
name: Documentation Validation

on:
  push:
    branches: [main, develop]
    paths:
      - 'docs/**'
      - 'README.md'
  pull_request:
    branches: [main, develop]
    paths:
      - 'docs/**'
      - 'README.md'

jobs:
  docs-structure:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Validate Documentation Structure
        run: |
          chmod +x docs/CICD\ \&\ Release\ Guide/Scripts/validate_docs_structure.sh
          ./docs/CICD\ \&\ Release\ Guide/Scripts/validate_docs_structure.sh

  control-knots:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Validate Control Knots Format
        run: |
          chmod +x docs/CICD\ \&\ Release\ Guide/Scripts/validate_control_knots.sh
          ./docs/CICD\ \&\ Release\ Guide/Scripts/validate_control_knots.sh

  multi-lens:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Validate Multi-Lens Communication
        run: |
          chmod +x docs/CICD\ \&\ Release\ Guide/Scripts/validate_multi_lens.sh
          ./docs/CICD\ \&\ Release\ Guide/Scripts/validate_multi_lens.sh

  markdown-lint:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Markdown Lint
        uses: DavidAnson/markdownlint-cli-action@v1
        with:
          files: 'docs/**/*.md'
          config: '.markdownlint.yml'

  spell-check:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Spell Check
        uses: streetsidesoftware/cspell-action@v4
        with:
          files: 'docs/**/*.md'
          config: 'cspell.json'
```

## Push Guard Configuration

### Pre-commit Hook
```bash
#!/bin/bash
# .git/hooks/pre-commit

echo "Running documentation validation..."

# Run documentation structure validation
if ! ./docs/CICD\ \&\ Release\ Guide/Scripts/validate_docs_structure.sh; then
    echo "❌ Documentation structure validation failed"
    exit 1
fi

# Run control knots format validation
if ! ./docs/CICD\ \&\ Release\ Guide/Scripts/validate_control_knots.sh; then
    echo "❌ Control knots format validation failed"
    exit 1
fi

# Run multi-lens communication validation
if ! ./docs/CICD\ \&\ Release\ Guide/Scripts/validate_multi_lens.sh; then
    echo "❌ Multi-lens communication validation failed"
    exit 1
fi

echo "✅ All documentation validations passed"
```

### Branch Protection Rules
```yaml
# GitHub branch protection configuration
branch_protection:
  main:
    required_status_checks:
      strict: true
      contexts:
        - "Documentation Structure Validation"
        - "Control Knots Format Validation"
        - "Multi-Lens Communication Validation"
        - "Markdown Lint"
        - "Spell Check"
    enforce_admins: true
    required_pull_request_reviews:
      required_approving_review_count: 2
      dismiss_stale_reviews: true
      require_code_owner_reviews: true
    restrictions:
      users: []
      teams: []
```

## Quality Gates

### Documentation Quality Metrics
- **Structure Compliance**: 100% compliance with required folder structure
- **Format Compliance**: 100% compliance with control knots format
- **Content Quality**: Multi-lens expert communication format
- **Link Validation**: All internal links must be valid
- **Spell Check**: No spelling errors in documentation

### Validation Results
- **Pass**: All validations pass, merge allowed
- **Fail**: Any validation fails, merge blocked
- **Warning**: Non-critical issues, merge allowed with warning

## Troubleshooting

### Common Validation Failures
1. **Missing Files**: Required documentation files missing
2. **Format Issues**: Control knots format not compliant
3. **Structure Issues**: Folder structure not compliant
4. **Content Issues**: Multi-lens communication format issues

### Resolution Steps
1. **Check Error Messages**: Review validation error output
2. **Fix Structure**: Ensure all required files and folders exist
3. **Fix Format**: Ensure control knots follow required format
4. **Fix Content**: Ensure multi-lens communication format is correct
5. **Re-run Validation**: Test fixes locally before pushing

---

**Last Updated**: October 7, 2025  
**Version**: 1.0  
**Status**: Production Ready

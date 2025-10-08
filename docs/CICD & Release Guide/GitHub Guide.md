# GitHub Guide

## Repository Structure

This repository follows a structured approach to documentation and development:

```
docs/
├── clip/                           # CLIP video clipping documentation
│   ├── Architecture Design and Control Knot.md
│   ├── Full scale implementation Details.md
│   ├── Device Deployment.md
│   ├── XiaoMi Pad Inference Optimization.md
│   ├── iPad Inference Optimization.md
│   ├── README.md
│   └── Scripts/                    # CLIP-related scripts
├── whisper/                        # Whisper audio transcription documentation
│   ├── Architecture Design and Control Knot.md
│   ├── Full scale implementation Details.md
│   ├── Device Deployment.md
│   ├── XiaoMi Pad Inference Optimization.md
│   ├── iPad Inference Optimization.md
│   ├── README.md
│   └── Scripts/                    # Whisper-related scripts
├── infra/                         # Infrastructure documentation
│   ├── Architecture Design and Control Knot.md
│   ├── XiaoMi Pad Inference Optimization.md
│   ├── iPad Inference Optimization.md
│   ├── README.md
│   └── Scripts/                   # Infrastructure scripts
├── CICD & Release Guide/         # CI/CD and release documentation
│   ├── GitHub Guide.md
│   ├── XiaoMi Pad Inference Optimization.md
│   ├── iPad Inference Optimization.md
│   ├── README.md
│   └── Scripts/                   # CI/CD scripts
├── cursor_rule.md                 # Cursor IDE rules
└── README.md                     # Main documentation
```

## Branch Strategy

### Main Branches
- **main**: Production-ready code
- **develop**: Integration branch for features

### Feature Branches
- **feature/clip-***: CLIP-related features
- **feature/whisper-***: Whisper-related features
- **feature/infra-***: Infrastructure features
- **feature/ui-***: UI-related features

### Release Branches
- **release/***: Release preparation branches

## Pull Request Process

### 1. Create Feature Branch
```bash
git checkout develop
git pull origin develop
git checkout -b feature/your-feature-name
```

### 2. Make Changes
- Follow coding standards
- Write comprehensive tests
- Update documentation
- Follow security best practices

### 3. Submit Pull Request
- Clear description of changes
- Link to related issues
- Request appropriate reviewers
- Ensure CI/CD passes

### 4. Code Review
- Address review feedback
- Update documentation if needed
- Ensure all tests pass
- Verify performance impact

### 5. Merge
- Squash commits if needed
- Delete feature branch
- Update documentation

## CI/CD Workflows

### Documentation Validation
- **Trigger**: Push to main/develop, PRs
- **Validation**: Structure, content quality, links
- **Tools**: Markdown lint, spell check, link validation

### Build and Test
- **Trigger**: Push to main/develop, PRs
- **Actions**: Build, unit tests, integration tests
- **Platforms**: Android, iOS, Web

### Performance Testing
- **Trigger**: Release branches
- **Actions**: Performance benchmarks, device testing
- **Platforms**: Xiaomi Pad, iPad

### Release Automation
- **Trigger**: Release tags
- **Actions**: Build, test, deploy
- **Platforms**: All supported platforms

## Documentation Standards

### File Naming
- Use descriptive names
- Follow kebab-case for scripts
- Use PascalCase for markdown files
- Include device/platform suffixes

### Content Structure
- Clear overview and objectives
- Detailed implementation guides
- Performance metrics and benchmarks
- Troubleshooting and debugging guides
- Integration points and dependencies

### Quality Requirements
- Comprehensive coverage
- Clear explanations
- Code examples
- Performance data
- Error handling

## Code Standards

### Kotlin
- Follow Kotlin coding conventions
- Use meaningful variable names
- Write comprehensive tests
- Document public APIs

### Shell Scripts
- Use kebab-case naming
- Include error handling
- Add usage documentation
- Follow POSIX compliance

### Markdown
- Use proper heading hierarchy
- Include code examples
- Add cross-references
- Follow markdown best practices

## Security Guidelines

### Code Security
- Follow security best practices
- Use secure coding patterns
- Validate all inputs
- Handle errors securely

### Documentation Security
- Don't expose sensitive information
- Use placeholder values for secrets
- Follow security documentation guidelines
- Review for information disclosure

## Performance Guidelines

### Code Performance
- Optimize for target platforms
- Use appropriate data structures
- Minimize memory allocations
- Profile and benchmark regularly

### Documentation Performance
- Keep files focused and concise
- Use efficient cross-references
- Optimize for readability
- Include performance metrics

## Troubleshooting

### Common Issues
1. **Build Failures**: Check dependencies and configuration
2. **Test Failures**: Verify test environment and data
3. **Documentation Errors**: Check markdown syntax and links
4. **Performance Issues**: Profile and optimize code

### Getting Help
- Check existing documentation
- Search for similar issues
- Ask in team channels
- Create detailed issue reports

## Best Practices

### Development
- Write tests first
- Document as you code
- Review code regularly
- Follow established patterns

### Documentation
- Keep documentation current
- Write for your audience
- Include examples
- Test documentation

### Collaboration
- Communicate clearly
- Be respectful and constructive
- Share knowledge
- Help others learn

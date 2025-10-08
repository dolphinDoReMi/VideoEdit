# Commit Message Policy

## 🎯 Focus on Whisper Flow Implementation

This repository enforces commit messages that focus on **feature implementation** rather than CI/CD setup.

## ✅ Allowed Commit Messages

Focus on these areas:
- **Feature implementation** (whisper flow, audio processing, transcription)
- **Bug fixes** (error handling, resource management)
- **Code improvements** (refactoring, optimization)
- **Whisper flow enhancements** (UI improvements, workflow updates)

### Examples of Good Commit Messages:
```
feat(whisper): add audio processing pipeline
fix(whisper): resolve transcription accuracy issue
refactor(whisper): improve error handling in TranscribeWorker
feat(whisper): enhance file selection UI
fix(whisper): resolve memory leak in TranscribeWorker
feat(whisper): add batch processing support
```

## ❌ Prohibited Commit Messages

Avoid mentioning these CI/CD-related terms:
- `cicd`, `ci/cd`, `ci-cd`
- `github actions`
- `automated pipeline`, `build pipeline`, `test pipeline`
- `validation pipeline`, `deployment pipeline`
- `workflow automation`, `automated workflow`

### Examples of Rejected Commit Messages:
```
❌ feat: add CI/CD pipeline
❌ fix: update GitHub Actions workflow
❌ feat: implement automated testing pipeline
❌ fix: resolve deployment pipeline issue
```

## 🔧 Pre-commit Hook

A pre-commit hook (`.git/hooks/commit-msg`) automatically validates commit messages and will reject any that contain CI/CD-related content.

### How It Works:
1. **Automatic Detection**: Scans commit messages for prohibited patterns
2. **Case Insensitive**: Detects patterns regardless of capitalization
3. **Regex Support**: Uses extended regex patterns for precise matching
4. **Helpful Feedback**: Provides clear guidance on rewriting rejected messages

### Testing the Hook:
```bash
# This will be rejected:
echo "feat: add CI/CD pipeline" | .git/hooks/commit-msg

# This will be approved:
echo "feat(whisper): add audio processing pipeline" | .git/hooks/commit-msg
```

## 🎯 Why This Policy?

1. **Focus on Features**: Keeps commit history focused on actual code changes
2. **Clear Intent**: Makes it easier to understand what each commit does
3. **Better Documentation**: Commit messages become a clear record of feature development
4. **Team Alignment**: Ensures all team members follow the same commit message standards

## 📝 Commit Message Format

Follow the [Conventional Commits](https://www.conventionalcommits.org/) format:

```
<type>(<scope>): <description>

[optional body]

[optional footer(s)]
```

### Types:
- `feat`: New feature
- `fix`: Bug fix
- `refactor`: Code refactoring
- `perf`: Performance improvement
- `docs`: Documentation changes
- `test`: Adding or updating tests
- `chore`: Maintenance tasks

### Scope:
- `whisper`: Whisper-related features
- `ui`: User interface changes
- `storage`: Storage-related changes
- `audio`: Audio processing changes

---

**Remember**: Keep commit messages focused on **what the code does**, not **how it's deployed**.

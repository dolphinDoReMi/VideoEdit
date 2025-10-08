# GitHub Branch Protection Configuration
# This file documents the recommended branch protection settings for the repository

## Main Branch Protection Rules

### Required Status Checks
The following status checks must pass before merging to main:

1. **CI** - Main CI/CD pipeline (`.github/workflows/ci.yml`)
   - Must pass: ✅
   - Required for: All branches

2. **whisper-validation** - Whisper implementation validation
   - Must pass: ✅
   - Required for: All branches

3. **android-build** - Android build verification
   - Must pass: ✅
   - Required for: All branches

4. **android-unit-tests** - Unit test verification
   - Must pass: ✅
   - Required for: All branches

5. **deployment-status** - Deployment status check
   - Must pass: ✅
   - Required for: All branches

### Branch Protection Settings

#### Required Settings
- ✅ **Require a pull request before merging**
  - Required approving reviews: 2
  - Dismiss stale PR approvals when new commits are pushed: ✅
  - Require review from code owners: ✅

- ✅ **Require status checks to pass before merging**
  - Require branches to be up to date before merging: ✅
  - Status checks required: All of the above

- ✅ **Require conversation resolution before merging**
  - All conversations on the PR must be resolved: ✅

- ✅ **Require signed commits**
  - Commits must be signed: ✅

- ✅ **Require linear history**
  - No merge commits allowed: ✅

- ✅ **Restrict pushes that create files**
  - Prevent direct pushes to main: ✅

#### Additional Protection
- ✅ **Restrict pushes that create files**
  - Prevent creation of new files without PR: ✅

- ✅ **Restrict pushes that delete files**
  - Prevent deletion of files without PR: ✅

- ✅ **Restrict pushes that modify files**
  - Prevent direct file modifications without PR: ✅

### Code Owner Requirements

#### CI/CD Files
The following files require approval from CI/CD maintainers:
```
.github/workflows/ci.yml @dennis @team-lead
.github/workflows/whisper-branch-ci.yml @dennis @team-lead
.github/workflows/feature-branch-ci.yml @dennis @team-lead
.github/workflows/release.yml @dennis @team-lead
scripts/tools/cicd_protection_hook.sh @dennis @team-lead
scripts/tools/validate_cicd.sh @dennis @team-lead
scripts/tools/install_git_hooks.sh @dennis @team-lead
docs/ci-cd/ @dennis @team-lead
```

#### Core Application Files
The following files require approval from core maintainers:
```
app/src/main/java/com/mira/videoeditor/ @team-lead @core-maintainer
app/src/main/java/com/mira/whisper/ @dennis @team-lead
feature/whisper/ @dennis @team-lead
feature/clip/ @team-lead @core-maintainer
```

## Feature Branch Protection

### Feature Branch Naming Convention
- Must start with `feature/` or `feat/`
- Examples: `feature/whisper-improvement`, `feat/ui-enhancement`

### Feature Branch Requirements
- ✅ Must be up to date with main before merging
- ✅ Must pass all CI/CD checks
- ✅ Must have at least 1 approving review
- ✅ Must resolve all conversations

### Feature Branch CI/CD
- Feature branches can modify `.github/workflows/feature-branch-ci.yml`
- Changes to main CI/CD (`.github/workflows/ci.yml`) require special approval
- All CI/CD changes must be documented and tested

## Release Branch Protection

### Release Branch Requirements
- ✅ Must be created from main branch
- ✅ Must pass all CI/CD checks
- ✅ Must have release documentation
- ✅ Must have version bump commits
- ✅ Must be approved by release manager

## Hotfix Branch Protection

### Hotfix Branch Requirements
- ✅ Must be created from main branch
- ✅ Must pass all CI/CD checks
- ✅ Must have emergency justification
- ✅ Must be approved by technical lead
- ✅ Must be merged back to main and develop

## CI/CD Change Process

### For Main Branch CI/CD Changes
1. **Create Feature Branch**
   ```bash
   git checkout -b feature/cicd-improvement
   ```

2. **Document Changes**
   - Update `docs/ci-cd/CI-CD-PROTECTION-POLICY.md`
   - Document the change rationale
   - Include testing procedures

3. **Test Thoroughly**
   ```bash
   ./scripts/tools/validate_cicd.sh
   ./scripts/tools/cicd_change_check.sh
   ```

4. **Create Pull Request**
   - Include detailed justification
   - Reference related issues
   - Include testing results

5. **Code Review**
   - Require 2+ approvals from CI/CD maintainers
   - All conversations must be resolved
   - All status checks must pass

6. **Merge**
   - Only after all checks pass
   - Monitor CI/CD pipeline after merge

### Emergency Procedures
If main branch CI/CD is broken:

1. **Create Hotfix Branch**
   ```bash
   git checkout -b hotfix/cicd-emergency-fix
   ```

2. **Apply Minimal Fix**
   - Smallest possible change
   - Well-tested solution
   - Clear documentation

3. **Emergency Review**
   - Get approval from technical lead
   - Document emergency justification

4. **Merge and Monitor**
   - Merge hotfix immediately
   - Monitor CI/CD pipeline
   - Conduct post-mortem

## Monitoring and Alerts

### CI/CD Health Monitoring
- **Pipeline Status**: Real-time monitoring via GitHub Actions
- **Build Success Rate**: Tracked over time
- **Performance Metrics**: Build duration trends
- **Failure Analysis**: Common failure patterns

### Alert Conditions
- 🔴 CI/CD pipeline failures
- 🟡 Performance degradation (>50% increase in build time)
- 🟠 Security vulnerabilities in CI/CD
- 🔵 Configuration drift

### Notification Channels
- **GitHub Issues**: Automatic issue creation for CI/CD failures
- **Slack/Teams**: Real-time notifications for critical failures
- **Email**: Daily summary reports

## Compliance and Audit

### Audit Trail
- All CI/CD changes are tracked in git history
- Pull request reviews provide audit trail
- GitHub Actions logs provide execution audit

### Compliance Requirements
- **Security**: All CI/CD changes must pass security review
- **Performance**: No degradation in build performance
- **Reliability**: Maintain >95% CI/CD success rate
- **Documentation**: All changes must be documented

## Maintenance Schedule

### Regular Tasks
- **Weekly**: Review CI/CD performance metrics
- **Monthly**: Update CI/CD dependencies
- **Quarterly**: Review protection policies
- **Annually**: Full CI/CD architecture review

### Emergency Contacts
- **CI/CD Issues**: Create GitHub issue with `cicd` label
- **Emergency Contact**: @dennis
- **Technical Lead**: @team-lead
- **Security Issues**: @security-team

---

**Last Updated**: $(date -u +"%Y-%m-%d %H:%M:%S UTC")
**Version**: 1.0
**Status**: ACTIVE PROTECTION

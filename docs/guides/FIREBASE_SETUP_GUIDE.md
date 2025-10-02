# Firebase Setup Guide

This guide explains how to set up Firebase for the AutoCutPad Android project.

## Prerequisites

- Android Studio
- Firebase account
- Google Cloud Console access

## Setup Steps

### 1. Create Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click "Create a project"
3. Enter project name: `AutoCutPad` (or your preferred name)
4. Enable Google Analytics (optional)
5. Create project

### 2. Add Android App

1. In Firebase Console, click "Add app" → Android
2. Enter package name: `com.mira.videoeditor`
3. Enter app nickname: `AutoCutPad`
4. Download `google-services.json`
5. Place the file in `app/google-services.json`

### 3. Enable Required Services

#### Firebase App Distribution
1. Go to Firebase Console → App Distribution
2. Enable App Distribution
3. Create test groups:
   - `testers` - General testers
   - `internal-testers` - Internal team
   - `beta-testers` - Beta testers

#### Firebase Analytics (Optional)
1. Go to Firebase Console → Analytics
2. Enable Analytics
3. Configure events tracking

### 4. Configure CI/CD

#### GitHub Secrets
Add these secrets to your GitHub repository:

```
FIREBASE_APP_ID         # Found in Firebase Console → Project Settings → General
FIREBASE_TOKEN          # Generate with: firebase login:ci
```

#### Generate Firebase Token
```bash
# Install Firebase CLI
npm install -g firebase-tools

# Login and generate token
firebase login:ci
```

### 5. Local Development

#### Option 1: Use Real Firebase Project
1. Download `google-services.json` from Firebase Console
2. Place in `app/google-services.json`
3. Build and run the app

#### Option 2: Use Template (Development Only)
1. Copy `app/google-services.json.template` to `app/google-services.json`
2. Replace placeholder values with your actual Firebase configuration
3. Build and run the app

## File Structure

```
app/
├── google-services.json          # Real Firebase config (gitignored)
├── google-services.json.template # Template for developers
└── build.gradle.kts              # Firebase plugin configuration
```

## Security Notes

- **Never commit** `google-services.json` to version control
- The file contains sensitive API keys
- Use the template file for development setup
- CI/CD will use template if real file is not available

## Troubleshooting

### Build Errors
- Ensure `google-services.json` is in the correct location (`app/` directory)
- Check that package name matches Firebase project configuration
- Verify Firebase project is properly set up

### CI/CD Issues
- Verify Firebase secrets are correctly configured
- Check Firebase token is valid and not expired
- Ensure Firebase App Distribution is enabled

### Missing Services
- Enable required Firebase services in console
- Check service configuration matches app requirements
- Verify API keys are correctly configured

## Development Workflow

1. **Setup**: Use template or real Firebase config
2. **Development**: Build and test locally
3. **Testing**: Use Firebase App Distribution for internal testing
4. **Release**: CI/CD automatically handles Firebase deployment

## Additional Resources

- [Firebase Android Setup](https://firebase.google.com/docs/android/setup)
- [Firebase App Distribution](https://firebase.google.com/docs/app-distribution)
- [Firebase CLI](https://firebase.google.com/docs/cli)
- [GitHub Actions Firebase](https://github.com/marketplace/actions/firebase-distribution-github-action)
